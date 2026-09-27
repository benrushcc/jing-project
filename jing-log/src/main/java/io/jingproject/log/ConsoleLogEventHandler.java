package io.jingproject.log;

import io.jingproject.bindings.PosixLogIoBindings;
import io.jingproject.bindings.WinLogIoBindings;
import io.jingproject.bindings.alloc.Allocator;
import io.jingproject.common.ConfigurationFactory;
import io.jingproject.common.Os;
import io.jingproject.common.log.LogLevel;
import io.jingproject.ffm.Libs;
import io.jingproject.ffm.NativeSegmentAccess;

import java.lang.foreign.MemorySegment;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// console log handler: routes events to stdout/stderr by level and writes the
// formatted batches straight from the shared arena region with at most one
// write per target per batch (two on overflow). level styling (width padding
// plus optional ansi colors) is pre-computed per level and handed to the
// pattern engine inside the level texts.
public final class ConsoleLogEventHandler implements LogEventHandler {
    private static final long REGION_SIZE = 1024 * 1024;
    private static final String DEFAULT_GLOBAL_COLOR = "";

    // write target for one console stream, overridable for tests
    public interface Output {
        // writes len bytes, returns the byte count written or a negative error code
        long write(MemorySegment segment, long len);
    }

    // testable settings seam; null errLevel means "none" (everything to stdout),
    // a null color entry inherits the global color, null outputs select the
    // platform stdout/stderr handles
    public record ConsoleSettings(
            boolean ansi,
            String pattern,
            LogLevel errLevel,
            String globalColor,
            String[] levelColors,
            ZoneId zone,
            Output stdout,
            Output stderr) {

    }

    // ansi color name to sgr foreground code
    private static final Map<String, String> COLOR_CODES = Map.of(
            "black", "30",
            "red", "31",
            "green", "32",
            "yellow", "33",
            "blue", "34",
            "magenta", "35",
            "cyan", "36",
            "white", "37",
            "bold", "1");

    private static final boolean IS_WINDOWS = Os.current() == Os.WINDOWS;
    private static final WinLogIoBindings WIN_IO = IS_WINDOWS ? Libs.impl(WinLogIoBindings.class) : null;
    private static final PosixLogIoBindings POSIX_IO = IS_WINDOWS ? null : Libs.impl(PosixLogIoBindings.class);

    private final ConsoleSettings settings;
    private final FormatPattern fpattern;
    private final Output stdout;
    private final Output stderr;
    private volatile boolean dead;
    private volatile boolean degraded;

    public ConsoleLogEventHandler() {
        this(fromConfiguration());
    }

    public ConsoleLogEventHandler(ConsoleSettings settings) {
        if (settings == null) {
            throw new IllegalArgumentException("settings must not be null");
        }
        this.settings = settings;
        this.stdout = settings.stdout() != null ? settings.stdout() : stdoutOutput();
        this.stderr = settings.stderr() != null ? settings.stderr() : stderrOutput();
        int w = FormatPattern.levelWidth(settings.pattern());
        int width = w < 0 ? -w : w;
        boolean leftAlign = w < 0;
        String[] styled = styledLevelTexts(settings.ansi(), settings.globalColor(), settings.levelColors(),
                width, leftAlign);
        this.fpattern = FormatPattern.compile(settings.pattern(), settings.zone(), styled);
    }

    public static ConsoleSettings fromConfiguration() {
        boolean ansi = ConfigurationFactory.confAsBoolean("jing.log.ansi", true);
        String pattern = ConfigurationFactory.conf("jing.log.console.pattern", FormatPattern.DEFAULT_PATTERN);
        String errValue = ConfigurationFactory.conf("jing.log.console.err.level", "WARN");
        LogLevel errLevel;
        if (errValue.equalsIgnoreCase("none")) {
            errLevel = null;
        } else {
            try {
                errLevel = LogLevel.fromString(errValue);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("jing.log.console.err.level : unsupported value '" + errValue + "'");
            }
        }
        String globalRaw = ConfigurationFactory.conf("jing.log.level.color");
        String globalColor = globalRaw == null ? DEFAULT_GLOBAL_COLOR : globalRaw;
        String[] levelColors = new String[LogLevel.values().length];
        LogLevel[] levels = LogLevel.values();
        for (int i = 0; i < levels.length; i++) {
            levelColors[i] = ConfigurationFactory.conf("jing.log.level." + levels[i].name().toLowerCase(Locale.ROOT)
                    + ".color");
        }
        ZoneId zone = zoneConfig();
        return new ConsoleSettings(ansi, pattern, errLevel, globalColor, levelColors, zone, null, null);
    }

    @Override
    public String id() {
        return "console";
    }

    @Override
    public void handle(List<LogEvent> events) {
        if (dead || events == null || events.isEmpty()) {
            return;
        }
        try {
            List<LogEvent> toStdout = null;
            List<LogEvent> toStderr = null;
            for (LogEvent e : events) {
                if (routesToStderr(e.level())) {
                    toStderr = toStderr == null ? new ArrayList<>() : toStderr;
                    toStderr.add(e);
                } else {
                    toStdout = toStdout == null ? new ArrayList<>() : toStdout;
                    toStdout.add(e);
                }
            }
            writeTarget(stdout, toStdout);
            writeTarget(stderr, toStderr);
        } catch (Throwable t) {
            disable("jing-log: console handler failed, disabled, reason : " + t);
        }
    }

    @Override
    public void flush() {
        // terminal writes carry no buffering semantics
    }

    // pure routing rule: an event goes to stderr when its level reaches the
    // configured err level; null err level routes everything to stdout
    public static boolean routesToStderr(LogLevel level, LogLevel errLevel) {
        if (errLevel == null) {
            return false;
        }
        return level.value() >= errLevel.value();
    }

    private boolean routesToStderr(LogLevel level) {
        return routesToStderr(level, settings.errLevel());
    }

    // pure: resolves a color configuration value to its sgr code sequence,
    // empty input yields an empty sequence, unknown names fail fast
    public static String resolveColor(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String part : raw.split(",")) {
            String code = COLOR_CODES.get(part.trim().toLowerCase(Locale.ROOT));
            if (code == null) {
                throw new IllegalArgumentException("jing.log.level.color : unsupported color name : " + part);
            }
            if (!sb.isEmpty()) {
                sb.append(';');
            }
            sb.append(code);
        }
        return sb.toString();
    }

    // pure: applies the ansi wrap for one level text; a present per-level color
    // overrides the global one (doc D7), absent colors yield the plain text
    public static String styleLevel(boolean ansi, String globalColor, String levelColor, String text) {
        String globalCodes = resolveColor(globalColor);
        String levelCodes = resolveColor(levelColor);
        String codes = levelCodes.isEmpty() ? globalCodes : levelCodes;
        if (!ansi || codes.isEmpty()) {
            return text;
        }
        return "\u001b[" + codes + "m" + text + "\u001b[0m";
    }

    // pure: builds the final per-level texts, width padded then optionally ansi
    // wrapped, indexed by LogLevel.ordinal()
    public static String[] styledLevelTexts(boolean ansi, String globalColor, String[] levelColors,
                                            int width, boolean leftAlign) {
        LogLevel[] levels = LogLevel.values();
        String[] result = new String[levels.length];
        for (int i = 0; i < levels.length; i++) {
            String name = levels[i].name();
            String padded = pad(name, width, leftAlign);
            String levelColor = levelColors == null ? null : levelColors[i];
            result[i] = styleLevel(ansi, globalColor, levelColor, padded);
        }
        return result;
    }

    private static String pad(String text, int width, boolean leftAlign) {
        if (width <= 0 || text.length() >= width) {
            return text;
        }
        int spaces = width - text.length();
        StringBuilder sb = new StringBuilder(width);
        if (!leftAlign) {
            sb.append(" ".repeat(spaces));
        }
        sb.append(text);
        if (leftAlign) {
            sb.append(" ".repeat(spaces));
        }
        return sb.toString();
    }

    private static ZoneId zoneConfig() {
        String value = ConfigurationFactory.conf("jing.log.timezone", "");
        if (value == null || value.isBlank()) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(value);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("jing.log.timezone : unsupported zone id '" + value + "'");
        }
    }

    private void writeTarget(Output target, List<LogEvent> routed) {
        if (routed == null || routed.isEmpty()) {
            return;
        }
        Allocator alloc = Allocator.newAlloc();
        MemorySegment region = alloc.allocate(REGION_SIZE);
        long cursor = 0;
        try {
            for (LogEvent e : routed) {
                long end = fpattern.fill(region, cursor, e);
                if (end == -1) {
                    writeRegion(target, region, cursor);
                    alloc.close();
                    alloc = Allocator.newAlloc();
                    region = alloc.allocate(REGION_SIZE);
                    end = fpattern.fill(region, 0, e);
                    if (end == -1) {
                        // a single event beyond the region: drop and report once
                        degrade("jing-log: dropping a console event larger than the write region");
                        alloc.close();
                        alloc = Allocator.newAlloc();
                        region = alloc.allocate(REGION_SIZE);
                        cursor = 0;
                        continue;
                    }
                }
                cursor = end;
            }
            if (cursor > 0) {
                writeRegion(target, region, cursor);
            }
        } finally {
            alloc.close();
        }
    }

    private void writeRegion(Output target, MemorySegment region, long len) {
        if (len <= 0) {
            return;
        }
        long v = target.write(region.asSlice(0, len), len);
        if (v != len) {
            if (v < 0) {
                throw new IllegalStateException("console write failed, err : " + -v);
            }
            throw new IllegalStateException("console write failed, wrote " + v + " of " + len + " bytes");
        }
    }

    private static Output stdoutOutput() {
        if (IS_WINDOWS) {
            return winOutput("stdout", true);
        }
        return posixOutput(1);
    }

    private static Output stderrOutput() {
        if (IS_WINDOWS) {
            return winOutput("stderr", false);
        }
        return posixOutput(2);
    }

    private static Output winOutput(String which, boolean isStdout) {
        if (WIN_IO == null) {
            throw new IllegalStateException("jing-log: WinLogIoBindings unavailable, native library not loaded");
        }
        long handle = isStdout ? WIN_IO.stdoutHandle() : WIN_IO.stderrHandle();
        if (NativeSegmentAccess.isErrPtr(handle)) {
            throw new IllegalStateException("jing-log: failed to obtain std handle, code="
                    + NativeSegmentAccess.errCode(handle));
        }
        return (segment, len) -> WIN_IO.write(handle, segment, len);
    }

    private static Output posixOutput(int fd) {
        if (POSIX_IO == null) {
            throw new IllegalStateException("jing-log: PosixLogIoBindings unavailable, native library not loaded");
        }
        return (segment, len) -> POSIX_IO.write(fd, segment, len);
    }

    private void degrade(String msg) {
        if (!degraded) {
            degraded = true;
            System.err.println(msg);
        }
    }

    private void disable(String msg) {
        degrade(msg);
        dead = true;
    }
}