package io.jingproject.log;

import io.jingproject.common.log.LogLevel;

import java.io.PrintStream;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// strict pattern language engine (see doc/Logging.md D6).
//
// the pattern is parsed once at compile time into a small node array; every
// literal and constant byte sequence (including the per-level level texts) is
// pre-encoded, dynamic nodes write directly into the region through RegionOut.
// the runtime is a plain iterative loop over the nodes, never recursive.
//
// level texts are supplied by the caller fully prepared (console pads by the
// pattern width and wraps in ansi, file pads plainly): the engine is ansi
// agnostic and writes levelTexts[level.ordinal()] verbatim. aligning decisions
// therefore live in the handlers, the engine only validates the width syntax.
public final class FormatPattern {
    // the default pattern shared by console and file (log format)
    public static final String DEFAULT_PATTERN = "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n";

    // date presets matched case-insensitively before falling back to a user pattern
    private static final Map<String, DateTimeFormatter> DATE_PRESETS = Map.ofEntries(
            Map.entry("iso_local_date", DateTimeFormatter.ISO_LOCAL_DATE),
            Map.entry("iso_offset_date", DateTimeFormatter.ISO_OFFSET_DATE),
            Map.entry("iso_date", DateTimeFormatter.ISO_DATE),
            Map.entry("iso_local_time", DateTimeFormatter.ISO_LOCAL_TIME),
            Map.entry("iso_offset_time", DateTimeFormatter.ISO_OFFSET_TIME),
            Map.entry("iso_time", DateTimeFormatter.ISO_TIME),
            Map.entry("iso_local_date_time", DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            Map.entry("iso_offset_date_time", DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            Map.entry("iso_zoned_date_time", DateTimeFormatter.ISO_ZONED_DATE_TIME),
            Map.entry("iso_date_time", DateTimeFormatter.ISO_DATE_TIME),
            Map.entry("iso_instant", DateTimeFormatter.ISO_INSTANT),
            Map.entry("basic_iso_date", DateTimeFormatter.BASIC_ISO_DATE),
            Map.entry("rfc_1123_date_time", DateTimeFormatter.RFC_1123_DATE_TIME));

    private static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";

    private final Node[] nodes;

    private FormatPattern(Node[] nodes) {
        this.nodes = nodes;
    }

    // compiles the pattern; every syntax error surfaces here as an
    // IllegalArgumentException whose message carries the offending position
    public static FormatPattern compile(String pattern, ZoneId zone, String[] levelTexts) {
        if (pattern == null) {
            throw new IllegalArgumentException("pattern must not be null");
        }
        if (zone == null) {
            throw new IllegalArgumentException("zone must not be null");
        }
        if (levelTexts == null) {
            throw new IllegalArgumentException("levelTexts must not be null");
        }
        LogLevel[] levels = LogLevel.values();
        if (levelTexts.length != levels.length) {
            throw new IllegalArgumentException("levelTexts must hold one entry per log level, actual : "
                    + levelTexts.length);
        }
        byte[][] levelBytes = new byte[levels.length][];
        for (int i = 0; i < levels.length; i++) {
            if (levelTexts[i] == null) {
                throw new IllegalArgumentException("levelTexts[" + i + "] must not be null");
            }
            levelBytes[i] = levelTexts[i].getBytes(StandardCharsets.UTF_8);
        }
        List<Node> parsed = new ArrayList<>();
        StringBuilder literal = new StringBuilder();
        int i = 0;
        int len = pattern.length();
        while (i < len) {
            char c = pattern.charAt(i);
            if (c != '%') {
                literal.append(c);
                i++;
                continue;
            }
            if (literal.length() > 0) {
                parsed.add(new LiteralNode(literal.toString().getBytes(StandardCharsets.UTF_8)));
                literal.setLength(0);
            }
            if (i + 1 >= len) {
                throw error(pattern, i, "dangling '%' at the end of the pattern");
            }
            char next = pattern.charAt(i + 1);
            if (next == '%') {
                literal.append('%');
                i += 2;
                continue;
            }
            int j = i + 1;
            int width = 0;
            boolean leftAlign = false;
            if (next == '-') {
                leftAlign = true;
                j++;
                int digitStart = j;
                while (j < len && isDigit(pattern.charAt(j))) {
                    j++;
                }
                if (j == digitStart) {
                    throw error(pattern, i + 1, "width requires digits after '-'");
                }
                width = parseWidth(pattern, digitStart, j);
            } else if (isDigit(next)) {
                int digitStart = j;
                while (j < len && isDigit(pattern.charAt(j))) {
                    j++;
                }
                width = parseWidth(pattern, digitStart, j);
            }
            int nameStart = j;
            while (j < len && isAsciiLetter(pattern.charAt(j))) {
                j++;
            }
            if (j == nameStart) {
                throw error(pattern, j, "unknown conversion, expected a specifier letter");
            }
            String name = pattern.substring(nameStart, j);
            String options = null;
            if (j < len && pattern.charAt(j) == '{') {
                int close = pattern.indexOf('}', j + 1);
                if (close < 0) {
                    throw error(pattern, j, "unclosed '{'");
                }
                options = pattern.substring(j + 1, close);
                j = close + 1;
            }
            boolean hasWidth = width != 0;
            switch (name) {
                case "date", "d" -> {
                    if (hasWidth) {
                        throw error(pattern, nameStart, "specifier '%" + name + "' does not support a width");
                    }
                    DateTimeFormatter formatter = buildDateFormatter(options, pattern, nameStart);
                    parsed.add(new DateNode(formatter, zone));
                }
                case "thread", "t" -> {
                    if (hasWidth) {
                        throw error(pattern, nameStart, "specifier '%" + name + "' does not support a width");
                    }
                    if (options != null) {
                        throw error(pattern, j, "specifier '%" + name + "' does not support options");
                    }
                    parsed.add(ThreadNode.INSTANCE);
                }
                case "level", "p" -> {
                    if (options != null) {
                        throw error(pattern, j, "specifier '%" + name
                                + "' does not support options, use the width prefix");
                    }
                    parsed.add(new LevelNode(levelBytes));
                }
                case "logger", "c" -> {
                    if (hasWidth) {
                        throw error(pattern, nameStart, "specifier '%" + name + "' does not support a width");
                    }
                    int budget = -1;
                    if (options != null) {
                        budget = parseBudget(options, pattern, j);
                    }
                    parsed.add(new LoggerNode(budget));
                }
                case "msg", "m" -> {
                    if (hasWidth) {
                        throw error(pattern, nameStart, "specifier '%" + name + "' does not support a width");
                    }
                    if (options != null) {
                        throw error(pattern, j, "specifier '%" + name + "' does not support options");
                    }
                    parsed.add(MsgNode.INSTANCE);
                }
                case "ex", "throwable" -> {
                    if (hasWidth) {
                        throw error(pattern, nameStart, "specifier '%" + name + "' does not support a width");
                    }
                    if (options != null) {
                        throw error(pattern, j, "specifier '%" + name + "' does not support options");
                    }
                    parsed.add(ExNode.INSTANCE);
                }
                case "n" -> {
                    if (hasWidth) {
                        throw error(pattern, nameStart, "specifier '%" + name + "' does not support a width");
                    }
                    if (options != null) {
                        throw error(pattern, j, "specifier '%" + name + "' does not support options");
                    }
                    parsed.add(NewlineNode.INSTANCE);
                }
                case "X", "context" -> {
                    if (hasWidth) {
                        throw error(pattern, nameStart, "specifier '%" + name + "' does not support a width");
                    }
                    if (options == null || options.isEmpty()) {
                        throw error(pattern, j, "specifier '%" + name + "' requires a non-empty {key}");
                    }
                    parsed.add(new ContextNode(options));
                }
                default -> throw error(pattern, nameStart, "unknown specifier '%" + name + "'");
            }
            i = j;
        }
        if (literal.length() > 0) {
            parsed.add(new LiteralNode(literal.toString().getBytes(StandardCharsets.UTF_8)));
        }
        return new FormatPattern(parsed.toArray(new Node[0]));
    }

    // writes one event into the region starting at offset; returns the new end
    // offset on success or -1 when the remaining capacity of the region cannot
    // hold the event (rolling back is inherent: the caller re-formats at the
    // start of a fresh region). "%ex" is the only overflow tolerant node.
    public long fill(MemorySegment region, long offset, LogEvent event) {
        if (region == null) {
            throw new IllegalArgumentException("region must not be null");
        }
        long cap = region.byteSize() - offset;
        if (cap < 0) {
            return -1;
        }
        RegionOut out = new RegionOut(region.asSlice(offset, cap));
        for (Node node : nodes) {
            node.write(out, event);
            if (out.overflowed()) {
                return -1;
            }
        }
        return offset + out.written();
    }

    // scans the pattern for the first %level/%p conversion and returns its
    // width, negative for left alignment (%-5), zero when absent. handlers use
    // this to pre-compute the padded level texts handed to compile.
    public static int levelWidth(String pattern) {
        int i = 0;
        int len = pattern == null ? 0 : pattern.length();
        while (i < len) {
            char c = pattern.charAt(i);
            if (c != '%') {
                i++;
                continue;
            }
            if (i + 1 >= len) {
                return 0;
            }
            char next = pattern.charAt(i + 1);
            if (next == '%') {
                i += 2;
                continue;
            }
            int j = i + 1;
            int width = 0;
            boolean leftAlign = false;
            if (next == '-') {
                leftAlign = true;
                j++;
                int digitStart = j;
                while (j < len && isDigit(pattern.charAt(j))) {
                    j++;
                }
                if (j > digitStart) {
                    width = Integer.parseInt(pattern.substring(digitStart, j));
                }
            } else if (isDigit(next)) {
                int digitStart = j;
                while (j < len && isDigit(pattern.charAt(j))) {
                    j++;
                }
                if (j > digitStart) {
                    width = Integer.parseInt(pattern.substring(digitStart, j));
                }
            }
            int nameStart = j;
            while (j < len && isAsciiLetter(pattern.charAt(j))) {
                j++;
            }
            String name = pattern.substring(nameStart, j);
            if (name.equals("level") || name.equals("p")) {
                return leftAlign ? -width : width;
            }
            if (j < len && pattern.charAt(j) == '{') {
                int close = pattern.indexOf('}', j + 1);
                if (close < 0) {
                    return 0;
                }
                j = close + 1;
            }
            i = j;
        }
        return 0;
    }

    private static DateTimeFormatter buildDateFormatter(String options, String pattern, int nameStart) {
        if (options == null) {
            return new DateTimeFormatterBuilder().appendPattern(DEFAULT_DATE_PATTERN).toFormatter();
        }
        if (options.isBlank()) {
            throw error(pattern, nameStart, "specifier '%d' requires options");
        }
        DateTimeFormatter preset = DATE_PRESETS.get(options.toLowerCase(Locale.ROOT));
        if (preset != null) {
            return preset;
        }
        try {
            return new DateTimeFormatterBuilder().appendPattern(options).toFormatter();
        } catch (IllegalArgumentException e) {
            throw error(pattern, nameStart, "invalid date pattern, reason : " + e.getMessage());
        }
    }

    private static int parseBudget(String options, String pattern, int braceIndex) {
        int len = options.length();
        if (len == 0) {
            throw error(pattern, braceIndex, "logger budget {N} must be a non-negative integer");
        }
        long v = 0;
        for (int i = 0; i < len; i++) {
            char c = options.charAt(i);
            if (!isDigit(c)) {
                throw error(pattern, braceIndex, "logger budget {N} must be a non-negative integer");
            }
            v = v * 10 + (c - '0');
            if (v > Integer.MAX_VALUE) {
                throw error(pattern, braceIndex, "logger budget {N} is too large");
            }
        }
        return (int) v;
    }

    private static int parseWidth(String pattern, int start, int end) {
        long v = 0;
        for (int i = start; i < end; i++) {
            v = v * 10 + (pattern.charAt(i) - '0');
            if (v > Integer.MAX_VALUE) {
                throw error(pattern, start, "width is too large");
            }
        }
        return (int) v;
    }

    private static IllegalArgumentException error(String pattern, int index, String msg) {
        return new IllegalArgumentException("invalid pattern '" + pattern + "' : " + msg + " at index " + index);
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isAsciiLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    // abbreviates a fully qualified class name to the given budget, iterative.
    // rule 1: full name fits -> as-is. rule 2: keep the last segment whole and
    // take "first letter + dot" from the front segments until the budget runs
    // out (prefer shorter over truncating the class name). rule 3: a single
    // overly long segment is truncated to the budget.
    private static String abbreviate(String name, int budget) {
        if (name == null) {
            return "";
        }
        if (budget <= 0 || name.length() <= budget) {
            return name;
        }
        int lastDot = name.lastIndexOf('.');
        if (lastDot < 0) {
            return name.substring(0, budget);
        }
        String clazz = name.substring(lastDot + 1);
        int remaining = budget - clazz.length();
        if (remaining <= 0) {
            return clazz;
        }
        StringBuilder sb = new StringBuilder();
        int segStart = 0;
        while (remaining > 0 && segStart < lastDot) {
            int dot = name.indexOf('.', segStart);
            if (dot < 0 || dot > lastDot) {
                break;
            }
            String seg = name.substring(segStart, dot);
            if (seg.isEmpty()) {
                break;
            }
            if (remaining < 2) {
                break;
            }
            sb.append(seg.charAt(0)).append('.');
            remaining -= 2;
            segStart = dot + 1;
        }
        sb.append(clazz);
        return sb.toString();
    }

    private interface Node {
        // writes the node; on capacity shortage RegionOut marks overflow and
        // the fill loop turns that into a -1 result, except for "%ex" which
        // clears the flag after appending its truncation marker
        void write(RegionOut out, LogEvent event);
    }

    private record LiteralNode(byte[] bytes) implements Node {
        @Override
        public void write(RegionOut out, LogEvent event) {
            out.write(bytes, 0, bytes.length);
        }
    }

    private record LevelNode(byte[][] texts) implements Node {
        @Override
        public void write(RegionOut out, LogEvent event) {
            byte[] bytes = texts[event.level().ordinal()];
            out.write(bytes, 0, bytes.length);
        }
    }

    private enum ThreadNode implements Node {
        INSTANCE;

        @Override
        public void write(RegionOut out, LogEvent event) {
            String t = event.threadName();
            out.writeUtf8(t == null ? "" : t);
        }
    }

    private record LoggerNode(int budget) implements Node {
        @Override
        public void write(RegionOut out, LogEvent event) {
            String name = event.className();
            out.writeUtf8(abbreviate(name, budget));
        }
    }

    private enum MsgNode implements Node {
        INSTANCE;

        @Override
        public void write(RegionOut out, LogEvent event) {
            String msg = event.msg();
            out.writeUtf8(msg == null ? "null" : msg);
        }
    }

    private enum NewlineNode implements Node {
        INSTANCE;

        @Override
        public void write(RegionOut out, LogEvent event) {
            // always a bare 0x0A, no platform translation
            out.write(0x0A);
        }
    }

    private record DateNode(DateTimeFormatter formatter, ZoneId zone) implements Node {
        @Override
        public void write(RegionOut out, LogEvent event) {
            ZonedDateTime when = event.instant().atZone(zone);
            formatter.formatTo(when, out);
        }
    }

    private record ContextNode(String key) implements Node {
        @Override
        public void write(RegionOut out, LogEvent event) {
            Map<String, Object> ctx = event.logContext();
            if (ctx == null || ctx.isEmpty()) {
                return;
            }
            Object value = ctx.get(key);
            if (value == null) {
                return;
            }
            out.writeUtf8(String.valueOf(value));
        }
    }

    private enum ExNode implements Node {
        INSTANCE;

        @Override
        public void write(RegionOut out, LogEvent event) {
            Throwable t = event.throwable();
            if (t == null) {
                return;
            }
            PrintStream ps = new PrintStream(out, false, StandardCharsets.UTF_8);
            t.printStackTrace(ps);
            if (out.overflowed()) {
                // the stack trace cannot be replayed, truncate and mark the
                // event as complete instead of failing the whole fill
                out.clearOverflow();
                out.writeUtf8("...");
                out.clearOverflow();
            }
        }
    }
}