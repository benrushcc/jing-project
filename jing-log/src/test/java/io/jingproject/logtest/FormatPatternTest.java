package io.jingproject.logtest;

import io.jingproject.bindings.alloc.Allocator;
import io.jingproject.common.log.LogLevel;
import io.jingproject.log.FormatPattern;
import io.jingproject.log.LogEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

// golden tests for the pattern engine: every conversion is rendered into a
// fixed native region and read back as utf-8 text
public class FormatPatternTest {
    private static final String[] LEVELS = {"TRACE", "DEBUG", "INFO", "WARN", "ERROR"};
    private static final String[] PADDED_LEVELS = {"TRACE", "DEBUG", "INFO ", "WARN ", "ERROR"};
    private static final String[] PADDED_RIGHT = {"TRACE", "DEBUG", " INFO", " WARN", "ERROR"};
    private static final Instant NOW = Instant.parse("2026-09-19T03:04:05.123456789Z");
    private static final ZoneId UTC = ZoneId.of("UTC");

    private static LogEvent event() {
        return new LogEvent(LogLevel.INFO, NOW, "com.example.util.NameFormatter", "worker-1", "hello", null,
                Map.of());
    }

    private static String render(String pattern, LogEvent e, String[] levelTexts) {
        FormatPattern fp = FormatPattern.compile(pattern, UTC, levelTexts);
        try (Allocator alloc = Allocator.newAlloc()) {
            MemorySegment region = alloc.allocate(16384);
            long end = fp.fill(region, 0, e);
            Assertions.assertTrue(end >= 0, "fill must succeed, actual : " + end);
            byte[] bytes = new byte[(int) end];
            MemorySegment.copy(region, ValueLayout.JAVA_BYTE, 0, bytes, 0, bytes.length);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    @Test
    public void testDefaultPattern() {
        String out = render("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n", event(), LEVELS);
        Assertions.assertEquals("2026-09-19 03:04:05.123 [worker-1] INFO com.example.util.NameFormatter - hello\n",
                out);
    }

    @Test
    public void testDateDefaultWithoutBraces() {
        Assertions.assertEquals("2026-09-19 03:04:05.123", render("%d", event(), LEVELS));
    }

    @Test
    public void testCustomDatePattern() {
        Assertions.assertEquals("03:04:05.123", render("%d{HH:mm:ss.SSS}", event(), LEVELS));
    }

    @Test
    public void testDatePresets() {
        Map<String, String> cases = Map.ofEntries(
                Map.entry("iso_local_date", "2026-09-19"),
                Map.entry("iso_offset_date", "2026-09-19Z"),
                Map.entry("iso_date", "2026-09-19Z"),
                Map.entry("iso_local_time", "03:04:05.123456789"),
                Map.entry("iso_offset_time", "03:04:05.123456789Z"),
                Map.entry("iso_time", "03:04:05.123456789Z"),
                Map.entry("iso_local_date_time", "2026-09-19T03:04:05.123456789"),
                Map.entry("iso_offset_date_time", "2026-09-19T03:04:05.123456789Z"),
                Map.entry("iso_zoned_date_time", "2026-09-19T03:04:05.123456789Z[UTC]"),
                Map.entry("iso_date_time", "2026-09-19T03:04:05.123456789Z[UTC]"),
                Map.entry("iso_instant", "2026-09-19T03:04:05.123456789Z"),
                Map.entry("basic_iso_date", "20260919Z"),
                Map.entry("rfc_1123_date_time", "Sat, 19 Sep 2026 03:04:05 GMT"));
        for (Map.Entry<String, String> c : cases.entrySet()) {
            Assertions.assertEquals(c.getValue(), render("%d{" + c.getKey() + "}", event(), LEVELS),
                    "preset " + c.getKey());
        }
    }

    @Test
    public void testPresetNamesAreCaseInsensitive() {
        Assertions.assertEquals(render("%d{iso_offset_date_time}", event(), LEVELS),
                render("%d{ISO_OFFSET_DATE_TIME}", event(), LEVELS));
    }

    @Test
    public void testPercentLiteral() {
        Assertions.assertEquals("100% done", render("100%% done", event(), LEVELS));
    }

    @Test
    public void testPlainLevelTextWrittenVerbatim() {
        // the engine writes the caller-prepared level text verbatim; handlers
        // pre-pad by the width, so a pre-padded level text stays exactly as given
        Assertions.assertEquals("INFO ", render("%-5level", event(), PADDED_LEVELS));
        Assertions.assertEquals(" INFO", render("%5level", event(), PADDED_RIGHT));
    }

    @Test
    public void testLevelWidthParsing() {
        Assertions.assertEquals(-5, FormatPattern.levelWidth("%-5level"));
        Assertions.assertEquals(5, FormatPattern.levelWidth("%5p"));
        Assertions.assertEquals(0, FormatPattern.levelWidth("no level here"));
        Assertions.assertEquals(0, FormatPattern.levelWidth("%d{xx} %msg"));
        Assertions.assertEquals(-8, FormatPattern.levelWidth("a %-8level b"));
    }

    @Test
    public void testLoggerAbbreviation() {
        // multi-segment names keep the last segment whole and abbreviate the
        // front segments to their first letter plus a dot
        Assertions.assertEquals("c.e.u.NameFormatter", render("%logger{20}", event(), LEVELS));
        // whole name when it fits
        Assertions.assertEquals("com.example.util.NameFormatter", render("%logger{50}", event(), LEVELS));
        // last segment alone exceeds the budget: the class name is returned
        Assertions.assertEquals("NameFormatter", render("%logger{5}", event(), LEVELS));
        // a single overly long segment is truncated to the budget
        LogEvent single = new LogEvent(LogLevel.INFO, NOW, "SomeClass", "w", "m", null, Map.of());
        Assertions.assertEquals("Som", render("%logger{3}", single, LEVELS));
        // zero budget means no abbreviation
        Assertions.assertEquals("com.example.util.NameFormatter", render("%logger{0}", event(), LEVELS));
        Assertions.assertEquals("com.example.util.NameFormatter", render("%logger{36}", event(), LEVELS));
    }

    @Test
    public void testContextLookup() {
        LogEvent e = new LogEvent(LogLevel.INFO, NOW, "io.jingproject.logtest.Ctx", "w", "m", null,
                Map.of("user", "alice", "count", 7));
        Assertions.assertEquals("alice", render("%X{user}", e, LEVELS));
        Assertions.assertEquals("7", render("%context{count}", e, LEVELS));
        Assertions.assertEquals("", render("%X{missing}", e, LEVELS));
    }

    @Test
    public void testNewlineNode() {
        Assertions.assertEquals("a\n b", render("a%n b", event(), LEVELS));
        Assertions.assertEquals("a\n\n", render("a%n%n", event(), LEVELS));
    }

    @Test
    public void testAlternateNames() {
        Assertions.assertEquals("hello", render("%m", event(), LEVELS));
        Assertions.assertEquals("worker-1", render("%t", event(), LEVELS));
        Assertions.assertEquals("INFO", render("%p", event(), LEVELS));
        Assertions.assertEquals("com.example.util.NameFormatter", render("%c", event(), LEVELS));
    }

    @Test
    public void testExAbsentIsEmpty() {
        Assertions.assertEquals("no throwable", render("no throwable%ex", event(), LEVELS));
    }

    @Test
    public void testExPresent() {
        Throwable t = new RuntimeException("boom");
        LogEvent e = new LogEvent(LogLevel.INFO, NOW, "io.jingproject.logtest.Ex", "w", "m", t, Map.of());
        // the engine renders via printStackTrace: full frame list, tab indented
        StringBuilder expected = new StringBuilder("java.lang.RuntimeException: boom").append(System.lineSeparator());
        for (StackTraceElement frame : t.getStackTrace()) {
            expected.append("\tat ").append(frame).append(System.lineSeparator());
        }
        Assertions.assertEquals(expected.toString(), render("%ex", e, LEVELS));
    }

    @Test
    public void testExTruncationTolerated() {
        // a too-small region must not fail the whole fill; the stack is
        // truncated and a marker is appended instead
        Throwable t = new RuntimeException("boom");
        LogEvent e = new LogEvent(LogLevel.INFO, NOW, "io.jingproject.logtest.Ex", "w", "m", t, Map.of());
        FormatPattern fp = FormatPattern.compile("%ex", UTC, LEVELS);
        try (Allocator alloc = Allocator.newAlloc()) {
            MemorySegment region = alloc.allocate(60);
            long end = fp.fill(region, 0, e);
            Assertions.assertTrue(end >= 0, "truncation must be tolerated");
            Assertions.assertTrue(end <= 60);
            byte[] bytes = new byte[(int) end];
            MemorySegment.copy(region, ValueLayout.JAVA_BYTE, 0, bytes, 0, bytes.length);
            String text = new String(bytes, StandardCharsets.UTF_8);
            Assertions.assertTrue(text.endsWith("..."), "truncated output ends with '...', actual : " + text);
            Assertions.assertTrue(text.startsWith("java.lang.RuntimeException: boom"));
        }
    }

    @Test
    public void testFillReturnsMinusOneWhenRegionTooSmall() {
        LogEvent e = new LogEvent(LogLevel.INFO, NOW, "io.jingproject.logtest.Tiny", "w", "long message", null,
                Map.of());
        FormatPattern fp = FormatPattern.compile("%msg%n", UTC, LEVELS);
        try (Allocator alloc = Allocator.newAlloc()) {
            MemorySegment region = alloc.allocate(8);
            Assertions.assertEquals(-1, fp.fill(region, 0, e));
        }
    }

    @Test
    public void testExactFitSucceeds() {
        LogEvent e = new LogEvent(LogLevel.INFO, NOW, "io.jingproject.logtest.Tiny", "w", "hello", null, Map.of());
        FormatPattern fp = FormatPattern.compile("%msg%n", UTC, LEVELS);
        try (Allocator alloc = Allocator.newAlloc()) {
            MemorySegment region = alloc.allocate(6);
            Assertions.assertEquals(6, fp.fill(region, 0, e));
        }
    }

    @Test
    public void testFillAtOffset() {
        LogEvent e = new LogEvent(LogLevel.INFO, NOW, "io.jingproject.logtest.Tiny", "w", "hello", null, Map.of());
        FormatPattern fp = FormatPattern.compile("%msg%n", UTC, LEVELS);
        try (Allocator alloc = Allocator.newAlloc()) {
            MemorySegment region = alloc.allocate(32);
            Assertions.assertEquals(12, fp.fill(region, 6, e));
            Assertions.assertEquals(18, fp.fill(region, 12, e));
        }
    }
}