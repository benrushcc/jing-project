package io.jingproject.log;

import io.jingproject.bindings.PosixLogIoBindings;
import io.jingproject.bindings.WinLogIoBindings;
import io.jingproject.bindings.alloc.Allocator;
import io.jingproject.common.ConfigurationFactory;
import io.jingproject.common.Os;
import io.jingproject.ffm.Libs;
import io.jingproject.ffm.NativeSegmentAccess;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

// file log handler: buffers each batch in a single off-heap region and issues
// one append-write per batch (plus the flush policy calls). rotation comes in
// three flavours: off (single growing file), size (archives base.N.ext, shifted
// by an iterative loop), time (the active file carries the current period key,
// so a roll is just flush+close+open of the next period file). the json format
// is hand-built UTF-8 json with a fixed schema, zero dependencies.
public final class FileLogEventHandler implements LogEventHandler, AutoCloseable {
    private static final long REGION_SIZE = 1024 * 1024;
    private static final String DEFAULT_PATH = "logs/jing.log";
    private static final long DEFAULT_ROTATE_SIZE = 10485760;
    private static final int DEFAULT_MAX_BACKUPS = 7;
    private static final int DEFAULT_MAX_AGE = 30;
    private static final long DEFAULT_FLUSH_INTERVAL = 1000;
    private static final DateTimeFormatter DAY_KEY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HOUR_KEY = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");
    private static final String[] CTRL_ESCAPE = new String[32];

    static {
        for (int i = 0; i < 32; i++) {
            CTRL_ESCAPE[i] = String.format(Locale.ROOT, "\\u%04x", i);
        }
    }

    public enum Format {
        LOG,
        JSON
    }

    public enum Rotation {
        OFF,
        SIZE,
        TIME
    }

    public enum Period {
        DAILY,
        HOURLY
    }

    public enum FlushMode {
        OFF,
        INTERVAL,
        IMMEDIATE
    }

    // immutable handler settings, resolvable without side effects so tests can
    // build one instance and assert the derived behaviour
    public record FileSettings(
            Path path,
            String pattern,
            Format format,
            Rotation rotation,
            long rotateSize,
            Period period,
            int maxBackups,
            long maxAge,
            FlushMode flushMode,
            long flushInterval,
            ZoneId zone) {

    }

    private static final boolean IS_WINDOWS = Os.current() == Os.WINDOWS;
    private static final WinLogIoBindings WIN_IO = IS_WINDOWS ? Libs.impl(WinLogIoBindings.class) : null;
    private static final PosixLogIoBindings POSIX_IO = IS_WINDOWS ? null : Libs.impl(PosixLogIoBindings.class);

    private final FileSettings settings;
    private final FormatPattern fpattern;
    private final Clock clock;
    private final Path parent;
    private final String stem;
    private final String ext;
    private long winHandle;
    private int posixFd = -1;
    private boolean opened;
    private long fileSize;
    private long lastFlushMillis;
    private String lastPeriodKey;
    private volatile boolean dead;
    private volatile boolean degraded;

    public FileLogEventHandler() {
        this(fromConfiguration(), Clock.systemDefaultZone());
    }

    public FileLogEventHandler(FileSettings settings) {
        this(settings, settings == null ? null
                : (settings.zone() == null ? Clock.systemDefaultZone() : Clock.system(settings.zone())));
    }

    public FileLogEventHandler(FileSettings settings, Clock clock) {
        if (settings == null) {
            throw new IllegalArgumentException("settings must not be null");
        }
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        this.settings = settings;
        this.clock = clock;
        Path p = settings.path();
        if (p == null) {
            throw new IllegalArgumentException("jing.log.file.path must not be null");
        }
        String name = p.getFileName() == null ? "" : p.getFileName().toString();
        if (settings.format() == Format.JSON && !name.contains(".")) {
            p = p.resolveSibling(name + ".json");
            name = p.getFileName().toString();
        }
        Path par = p.getParent();
        this.parent = par == null ? Path.of(".") : par;
        int dot = name.lastIndexOf('.');
        this.stem = dot > 0 ? name.substring(0, dot) : name;
        this.ext = dot > 0 ? name.substring(dot + 1) : "";
        if (settings.format() == Format.JSON) {
            this.fpattern = null;
        } else {
            String pattern = settings.pattern() == null ? FormatPattern.DEFAULT_PATTERN : settings.pattern();
            int w = FormatPattern.levelWidth(pattern);
            int width = w < 0 ? -w : w;
            String[] levelTexts = styledPlainLevelTexts(width, w < 0);
            this.fpattern = FormatPattern.compile(pattern, settings.zone(), levelTexts);
        }
    }

    // reads the file handler settings from the central configuration
    public static FileSettings fromConfiguration() {
        Path path = Path.of(ConfigurationFactory.conf("jing.log.file.path", DEFAULT_PATH));
        String pattern = ConfigurationFactory.conf("jing.log.file.pattern", FormatPattern.DEFAULT_PATTERN);
        Format format = parseEnum("jing.log.file.format", Format.values(), Format.LOG);
        Rotation rotation = parseEnum("jing.log.file.rotation", Rotation.values(), Rotation.SIZE);
        long rotateSize = parseLong("jing.log.file.rotate.size", DEFAULT_ROTATE_SIZE, 1, Long.MAX_VALUE);
        Period period = parseEnum("jing.log.file.rotate.period", Period.values(), Period.DAILY);
        int maxBackups = parseInt("jing.log.file.maxBackups", DEFAULT_MAX_BACKUPS, 0, Integer.MAX_VALUE);
        int maxAge = parseInt("jing.log.file.maxAge", DEFAULT_MAX_AGE, 0, Integer.MAX_VALUE);
        FlushMode flushMode = parseEnum("jing.log.file.flush", FlushMode.values(), FlushMode.INTERVAL);
        long flushInterval = parseLong("jing.log.file.flush.interval", DEFAULT_FLUSH_INTERVAL, 1, Long.MAX_VALUE);
        ZoneId zone = zoneConfig();
        return new FileSettings(path, pattern, format, rotation, rotateSize, period, maxBackups, maxAge,
                flushMode, flushInterval, zone);
    }

    @Override
    public String id() {
        return "file";
    }

    @Override
    public void handle(List<LogEvent> events) {
        if (dead || events == null || events.isEmpty()) {
            return;
        }
        try {
            if (!opened) {
                open();
            }
            checkPeriodRoll();
            writeBatch(events);
        } catch (Throwable t) {
            disable("jing-log: file handler failed, disabled, reason : " + t);
        }
    }

    @Override
    public void flush() {
        try {
            flushNative();
            lastFlushMillis = clock.millis();
        } catch (Throwable t) {
            degrade("jing-log: file flush failed, reason : " + t);
        }
    }

    @Override
    public void close() {
        try {
            if (opened) {
                flushNative();
                closeNative();
            }
        } catch (Throwable t) {
            degrade("jing-log: file close failed, reason : " + t);
        }
    }

    // rolls the active file when enough bytes would cross the size threshold.
    // the size mode check runs per event so the batch is never allowed to grow
    // the file past one threshold crossing; a single oversized event is still
    // written into a fresh file instead of being dropped.
    private void writeBatch(List<LogEvent> events) throws IOException {
        Allocator alloc = Allocator.newAlloc();
        MemorySegment region = alloc.allocate(REGION_SIZE);
        long cursor = 0;
        try {
            for (LogEvent e : events) {
                long start = cursor;
                long end = fill(region, start, e);
                if (end == -1) {
                    // the region filled up before this event: write the prefix
                    if (cursor > 0) {
                        writeNative(region, cursor);
                        cursor = 0;
                    }
                    alloc.close();
                    alloc = Allocator.newAlloc();
                    region = alloc.allocate(REGION_SIZE);
                    end = fill(region, 0, e);
                    if (end == -1) {
                        degrade("jing-log: dropping a file event larger than the write region");
                        continue;
                    }
                    start = 0;
                }
                if (settings.rotation() == Rotation.SIZE && fileSize > 0
                        && fileSize + end > settings.rotateSize()) {
                    // the old file already holds batch bytes [0, start) from
                    // earlier events; those go to the old file, the current
                    // event is re-formatted into a fresh region for the new
                    // file once the roll completed
                    if (start > 0) {
                        writeNative(region, start);
                    }
                    rotate();
                    alloc.close();
                    alloc = Allocator.newAlloc();
                    region = alloc.allocate(REGION_SIZE);
                    end = fill(region, 0, e);
                    if (end == -1) {
                        cursor = 0;
                        degrade("jing-log: dropping a file event larger than the write region");
                        continue;
                    }
                }
                cursor = end;
            }
            if (cursor > 0) {
                writeNative(region, cursor);
            }
        } finally {
            alloc.close();
        }
        flushIfNeeded();
    }

    private long fill(MemorySegment region, long offset, LogEvent e) {
        if (settings.format() == Format.JSON) {
            return writeJson(region, offset, e, settings.zone());
        }
        return fpattern.fill(region, offset, e);
    }

    private void checkPeriodRoll() throws IOException {
        if (settings.rotation() != Rotation.TIME) {
            return;
        }
        String key = currentPeriodKey();
        if (lastPeriodKey == null) {
            lastPeriodKey = key;
            return;
        }
        if (!key.equals(lastPeriodKey)) {
            rotate();
            lastPeriodKey = key;
        }
    }

    private void rotate() throws IOException {
        flushNative();
        closeNative();
        if (settings.rotation() == Rotation.SIZE) {
            if (settings.maxBackups() <= 0) {
                Files.deleteIfExists(activePath());
            } else {
                shiftArchives();
                Files.move(activePath(), archivePath(1), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        open();
    }

    // shifts base.N.ext -> base.N+1.ext descending so the oldest archive
    // (highest number) is dropped first; iterative, never recursive
    private void shiftArchives() throws IOException {
        int max = settings.maxBackups();
        for (int n = max; n >= 1; n--) {
            Path src = archivePath(n);
            if (Files.exists(src)) {
                if (n == max) {
                    Files.delete(src);
                } else {
                    Files.move(src, archivePath(n + 1), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    // scans the archive naming pattern and deletes files older than maxAge;
    // the active file is never touched. maxAge 0 disables the cleanup.
    private void cleanupArchives() {
        if (settings.maxAge() <= 0) {
            return;
        }
        long cutoff = clock.millis() - Duration.ofDays(settings.maxAge()).toMillis();
        try (Stream<Path> stream = Files.list(parent)) {
            stream.filter(this::matchesArchive).forEach(p -> {
                if (p.getFileName().toString().equals(activeName())) {
                    return;
                }
                try {
                    long modified = Files.getLastModifiedTime(p).toMillis();
                    if (modified < cutoff) {
                        Files.deleteIfExists(p);
                    }
                } catch (IOException e) {
                    // a single archive failing cleanup must not fail the batch
                }
            });
        } catch (IOException e) {
            degrade("jing-log: archive cleanup failed, reason : " + e);
        }
    }

    // true when the file matches the archive naming pattern of this handler
    // (size: stem.N.ext; time: stem.<date-key>.ext)
    private boolean matchesArchive(Path p) {
        String name = p.getFileName() == null ? "" : p.getFileName().toString();
        if (name.equals(activeName())) {
            return false;
        }
        String prefix = stem + ".";
        if (!name.startsWith(prefix)) {
            return false;
        }
        String tail = name.substring(prefix.length());
        if (!ext.isEmpty()) {
            if (!tail.endsWith("." + ext)) {
                return false;
            }
            tail = tail.substring(0, tail.length() - ext.length() - 1);
        }
        if (tail.isEmpty()) {
            return false;
        }
        if (settings.rotation() == Rotation.TIME) {
            return looksLikeDateKey(tail);
        }
        return isDigits(tail);
    }

    private static boolean looksLikeDateKey(String tail) {
        if (tail.length() != 10 && tail.length() != 13) {
            return false;
        }
        for (int i = 0; i < tail.length(); i++) {
            char c = tail.charAt(i);
            if (i == 4 || i == 7 || (tail.length() == 13 && i == 10)) {
                if (c != '-') {
                    return false;
                }
            } else if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    private static boolean isDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) < '0' || s.charAt(i) > '9') {
                return false;
            }
        }
        return true;
    }

    private void open() throws IOException {
        Path target = activePath();
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        Allocator alloc = Allocator.newAlloc();
        try {
            MemorySegment nameCStr = cString(alloc, target.toString());
            if (IS_WINDOWS) {
                if (WIN_IO == null) {
                    throw new IllegalStateException("jing-log: WinLogIoBindings unavailable, native library"
                            + " not loaded");
                }
                long handle = WIN_IO.openAppend(nameCStr);
                if (NativeSegmentAccess.isErrPtr(handle)) {
                    throw new IOException("jing-log: open failed for " + target + ", code="
                            + NativeSegmentAccess.errCode(handle));
                }
                long size = WIN_IO.fileSize(handle);
                if (size < 0) {
                    WIN_IO.close(handle);
                    throw new IOException("jing-log: file size query failed for " + target + ", err=" + -size);
                }
                winHandle = handle;
                fileSize = size;
            } else {
                if (POSIX_IO == null) {
                    throw new IllegalStateException("jing-log: PosixLogIoBindings unavailable, native library"
                            + " not loaded");
                }
                int fd = POSIX_IO.openAppend(nameCStr);
                if (fd < 0) {
                    throw new IOException("jing-log: open failed for " + target + ", err=" + -fd);
                }
                long size = POSIX_IO.fileSize(fd);
                if (size < 0) {
                    POSIX_IO.close(fd);
                    throw new IOException("jing-log: file size query failed for " + target + ", err=" + -size);
                }
                posixFd = fd;
                fileSize = size;
            }
        } finally {
            alloc.close();
        }
        opened = true;
        lastFlushMillis = clock.millis();
        cleanupArchives();
    }

    private void closeNative() {
        if (!opened) {
            return;
        }
        if (IS_WINDOWS) {
            if (winHandle != 0) {
                WIN_IO.close(winHandle);
            }
        } else if (posixFd >= 0) {
            POSIX_IO.close(posixFd);
        }
        opened = false;
    }

    private void flushNative() throws IOException {
        if (!opened) {
            return;
        }
        int rc;
        if (IS_WINDOWS) {
            rc = WIN_IO.flush(winHandle);
        } else {
            rc = POSIX_IO.flush(posixFd);
        }
        if (rc < 0) {
            throw new IOException("jing-log: flush failed, err=" + -rc);
        }
    }

    // allocates a NUL-terminated utf-8 c string for a native open call
    private static MemorySegment cString(Allocator alloc, String s) {
        byte[] encoded = s.getBytes(StandardCharsets.UTF_8);
        MemorySegment seg = alloc.allocate(encoded.length + 1L);
        MemorySegment.copy(encoded, 0, seg, ValueLayout.JAVA_BYTE, 0, encoded.length);
        NativeSegmentAccess.setByte(seg.address(), encoded.length, (byte) 0);
        return seg;
    }

    private void writeNative(MemorySegment region, long len) throws IOException {
        if (len <= 0) {
            return;
        }
        long written;
        if (IS_WINDOWS) {
            written = WIN_IO.write(winHandle, region.asSlice(0, len), len);
        } else {
            written = POSIX_IO.write(posixFd, region.asSlice(0, len), len);
        }
        if (written != len) {
            if (written < 0) {
                throw new IOException("jing-log: write failed, err=" + -written);
            }
            throw new IOException("jing-log: write failed, wrote " + written + " of " + len + " bytes");
        }
        fileSize += written;
    }

    private void flushIfNeeded() throws IOException {
        FlushMode mode = settings.flushMode();
        switch (mode) {
            case OFF -> {
                // no proactive flush
            }
            case IMMEDIATE -> flushNative();
            case INTERVAL -> {
                long now = clock.millis();
                if (now - lastFlushMillis >= settings.flushInterval()) {
                    flushNative();
                    lastFlushMillis = now;
                }
            }
        }
    }

    private Path activePath() {
        return parent.resolve(activeName());
    }

    private String activeName() {
        if (settings.rotation() == Rotation.TIME) {
            return stem + "." + currentPeriodKey() + (ext.isEmpty() ? "" : "." + ext);
        }
        return stem + (ext.isEmpty() ? "" : "." + ext);
    }

    private Path archivePath(int n) {
        return parent.resolve(stem + "." + n + (ext.isEmpty() ? "" : "." + ext));
    }

    private String currentPeriodKey() {
        DateTimeFormatter format = settings.period() == Period.HOURLY ? HOUR_KEY : DAY_KEY;
        return clock.instant().atZone(settings.zone()).format(format);
    }

    // writes one event as a fixed-schema json line into the region at offset,
    // returns the new end offset or -1 when the remaining capacity is exceeded.
    // the schema is hand-built, zero-dependency, one event per line:
    // {"ts":..,"level":..,"thread":..,"logger":..,"msg":..,"ctx":{..},"ex":..}
    public static long writeJson(MemorySegment region, long offset, LogEvent event, ZoneId zone) {
        if (region == null) {
            throw new IllegalArgumentException("region must not be null");
        }
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        if (zone == null) {
            throw new IllegalArgumentException("zone must not be null");
        }
        long cap = region.byteSize() - offset;
        if (cap < 0) {
            return -1;
        }
        RegionOut out = new RegionOut(region.asSlice(offset, cap));
        out.writeUtf8("{\"ts\":\"");
        out.writeUtf8(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(event.instant().atZone(zone)));
        out.writeUtf8("\",\"level\":\"");
        out.writeUtf8(event.level().name());
        out.writeUtf8("\",\"thread\":\"");
        appendEscaped(out, event.threadName());
        out.writeUtf8("\",\"logger\":\"");
        appendEscaped(out, event.className());
        out.writeUtf8("\",\"msg\":\"");
        appendEscaped(out, event.msg());
        out.writeUtf8("\"");
        Map<String, Object> ctx = event.logContext();
        if (ctx != null && !ctx.isEmpty()) {
            out.writeUtf8(",\"ctx\":{");
            TreeMap<String, Object> sorted = new TreeMap<>();
            sorted.putAll(ctx);
            boolean first = true;
            for (Map.Entry<String, Object> entry : sorted.entrySet()) {
                if (!first) {
                    out.writeUtf8(",");
                }
                first = false;
                out.writeUtf8("\"");
                appendEscaped(out, entry.getKey());
                out.writeUtf8("\":");
                appendJsonValue(out, entry.getValue());
            }
            out.writeUtf8("}");
        }
        if (event.throwable() != null) {
            out.writeUtf8(",\"ex\":\"");
            appendEscaped(out, renderThrowable(event.throwable()));
            out.writeUtf8("\"");
        }
        out.writeUtf8("}\n");
        if (out.overflowed()) {
            return -1;
        }
        return offset + out.written();
    }

    // renders a throwable into the standard printStackTrace layout including
    // suppressed exceptions, exactly matching the "%ex" pattern engine
    public static String renderThrowable(Throwable throwable) {
        StringWriter sw = new StringWriter(256);
        throwable.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    // appends a string to the json buffer, escaping " \ and control characters
    // per D9; other unicode bytes flow through unchanged
    private static void appendEscaped(RegionOut out, String s) {
        if (s == null) {
            return;
        }
        int runStart = 0;
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            String escape = null;
            switch (c) {
                case '"' -> escape = "\\\"";
                case '\\' -> escape = "\\\\";
                case '\n' -> escape = "\\n";
                case '\r' -> escape = "\\r";
                case '\t' -> escape = "\\t";
                case '\b' -> escape = "\\b";
                case '\f' -> escape = "\\f";
                default -> {
                    if (c < 0x20) {
                        escape = CTRL_ESCAPE[c];
                    }
                }
            }
            if (escape != null) {
                if (runStart < i) {
                    out.writeUtf8(s, runStart, i);
                }
                out.writeUtf8(escape);
                runStart = i + 1;
            }
        }
        if (runStart < len) {
            out.writeUtf8(s, runStart, len);
        }
    }

    private static void appendJsonValue(RegionOut out, Object value) {
        if (value == null) {
            out.writeUtf8("null");
            return;
        }
        if (value instanceof Boolean || value instanceof Number) {
            out.writeUtf8(value.toString());
            return;
        }
        out.writeUtf8("\"");
        appendEscaped(out, String.valueOf(value));
        out.writeUtf8("\"");
    }

    // plain level texts, width padded, no ansi color (file log format)
    private static String[] styledPlainLevelTexts(int width, boolean leftAlign) {
        String[] result = new String[io.jingproject.common.log.LogLevel.values().length];
        for (int i = 0; i < result.length; i++) {
            String name = io.jingproject.common.log.LogLevel.values()[i].name();
            int pad = width - name.length();
            if (pad <= 0) {
                result[i] = name;
            } else if (leftAlign) {
                result[i] = name + " ".repeat(pad);
            } else {
                result[i] = " ".repeat(pad) + name;
            }
        }
        return result;
    }

    private static <E extends Enum<E>> E parseEnum(String key, E[] values, E defaultValue) {
        String raw = ConfigurationFactory.conf(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        for (E value : values) {
            if (value.name().equalsIgnoreCase(raw)) {
                return value;
            }
        }
        throw new IllegalArgumentException(key + " : unsupported value '" + raw + "'");
    }

    private static int parseInt(String key, int defaultValue, int min, int max) {
        long value = parseLong(key, defaultValue, min, max);
        return (int) value;
    }

    private static long parseLong(String key, long defaultValue, long min, long max) {
        String raw = ConfigurationFactory.conf(key);
        long value;
        try {
            value = ConfigurationFactory.valueAsLong(raw, defaultValue);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(key + " : invalid integer value '" + raw + "'");
        }
        if (value < min || value > max) {
            throw new IllegalArgumentException(key + " : value out of range [" + min + ", " + max + "], actual : "
                    + value);
        }
        return value;
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