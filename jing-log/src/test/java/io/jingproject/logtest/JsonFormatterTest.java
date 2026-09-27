package io.jingproject.logtest;

import io.jingproject.bindings.alloc.Allocator;
import io.jingproject.common.log.LogLevel;
import io.jingproject.log.FileLogEventHandler;
import io.jingproject.log.FormatPattern;
import io.jingproject.log.LogEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

// golden tests for the hand-built json schema: fixed field order, optional ctx
// and ex fields, explicit escaping of " \ and control characters
public class JsonFormatterTest {
    private static final Instant NOW = Instant.parse("2026-09-19T03:04:05.123Z");
    private static final ZoneId UTC = ZoneId.of("UTC");

    private static String json(LogEvent e, ZoneId zone) {
        try (Allocator alloc = Allocator.newAlloc()) {
            MemorySegment region = alloc.allocate(65536);
            long end = FileLogEventHandler.writeJson(region, 0, e, zone);
            Assertions.assertTrue(end >= 0, "writeJson must succeed");
            byte[] bytes = new byte[(int) end];
            MemorySegment.copy(region, ValueLayout.JAVA_BYTE, 0, bytes, 0, bytes.length);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    // mirrors the escaping rule for building expected values
    private static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    @Test
    public void testGolden() {
        LogEvent e = new LogEvent(LogLevel.INFO, NOW, "io.jingproject.logtest.JsonFormatterTest", "main", "hi", null,
                Map.of("k1", "v1", "k2", "v2"));
        Assertions.assertEquals(
                "{\"ts\":\"2026-09-19T03:04:05.123Z\",\"level\":\"INFO\",\"thread\":\"main\","
                        + "\"logger\":\"io.jingproject.logtest.JsonFormatterTest\",\"msg\":\"hi\","
                        + "\"ctx\":{\"k1\":\"v1\",\"k2\":\"v2\"}}\n",
                json(e, UTC));
    }

    @Test
    public void testOffsetDateTimeZone() {
        LogEvent e = new LogEvent(LogLevel.INFO, NOW, "io.jingproject.logtest.JsonFormatterTest", "main", "hi", null,
                Map.of());
        String out = json(e, ZoneId.of("+05:30"));
        Assertions.assertTrue(out.startsWith("{\"ts\":\"2026-09-19T08:34:05.123+05:30\""), "actual : " + out);
    }

    @Test
    public void testEscaping() {
        String raw = "a\"b\\c\nd\t\b\u0001";
        LogEvent e = new LogEvent(LogLevel.INFO, NOW, "io.jingproject.logtest.JsonFormatterTest", "main", raw, null,
                Map.of());
        String expected = "{\"ts\":\"2026-09-19T03:04:05.123Z\",\"level\":\"INFO\",\"thread\":\"main\","
                + "\"logger\":\"io.jingproject.logtest.JsonFormatterTest\",\"msg\":\"" + escape(raw) + "\"}\n";
        Assertions.assertEquals(expected, json(e, UTC));
    }

    @Test
    public void testCtxValueTypes() {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("num", 42);
        ctx.put("flag", true);
        ctx.put("nil", null);
        ctx.put("text", "x");
        LogEvent e = new LogEvent(LogLevel.INFO, NOW, "io.jingproject.logtest.JsonFormatterTest", "main", "m", null,
                ctx);
        String out = json(e, UTC);
        Assertions.assertTrue(out.contains("\"ctx\":{\"flag\":true,\"nil\":null,\"num\":42,\"text\":\"x\"}"),
                "actual : " + out);
    }

    @Test
    public void testCtxOmittedWhenEmpty() {
        LogEvent empty = new LogEvent(LogLevel.INFO, NOW, "io.jingproject.logtest.JsonFormatterTest", "main", "m",
                null, Map.of());
        Assertions.assertFalse(json(empty, UTC).contains("ctx"));
        LogEvent nullCtx = new LogEvent(LogLevel.INFO, NOW, "io.jingproject.logtest.JsonFormatterTest", "main", "m",
                null, null);
        Assertions.assertFalse(json(nullCtx, UTC).contains("ctx"));
    }

    @Test
    public void testExField() {
        Throwable t = new RuntimeException("boom");
        LogEvent e = new LogEvent(LogLevel.INFO, NOW, "io.jingproject.logtest.JsonFormatterTest", "main", "m", t,
                Map.of());
        String out = json(e, UTC);
        Assertions.assertTrue(out.startsWith("{\"ts\":\"2026-09-19T03:04:05.123Z\",\"level\":\"INFO\","
                + "\"thread\":\"main\",\"logger\":\"io.jingproject.logtest.JsonFormatterTest\",\"msg\":\"m\","),
                "actual : " + out);
        Assertions.assertTrue(out.contains(",\"ex\":\"" + escape(FileLogEventHandler.renderThrowable(t)) + "\"}\n"),
                "actual : " + out);
    }

    @Test
    public void testNullThrowableOmitted() {
        LogEvent e = new LogEvent(LogLevel.INFO, NOW, "io.jingproject.logtest.JsonFormatterTest", "main", "m", null,
                Map.of());
        Assertions.assertFalse(json(e, UTC).contains("ex"));
    }

    @Test
    public void testNullMsgBecomesEmptyString() {
        LogEvent e = new LogEvent(LogLevel.INFO, NOW, "io.jingproject.logtest.JsonFormatterTest", "main", null, null,
                Map.of());
        Assertions.assertTrue(json(e, UTC).contains("\"msg\":\"\""), "actual : " + json(e, UTC));
    }

    @Test
    public void testOverflowReturnsMinusOne() {
        LogEvent e = new LogEvent(LogLevel.INFO, NOW, "io.jingproject.logtest.JsonFormatterTest", "main", "hi", null,
                Map.of());
        try (Allocator alloc = Allocator.newAlloc()) {
            MemorySegment region = alloc.allocate(8);
            Assertions.assertEquals(-1, FileLogEventHandler.writeJson(region, 0, e, UTC));
        }
    }

    @Test
    public void testOffsetWrites() {
        LogEvent e = new LogEvent(LogLevel.INFO, NOW, "io.jingproject.logtest.JsonFormatterTest", "main", "hi", null,
                Map.of());
        try (Allocator alloc = Allocator.newAlloc()) {
            MemorySegment region = alloc.allocate(8192);
            long once = FileLogEventHandler.writeJson(region, 0, e, UTC);
            long twice = FileLogEventHandler.writeJson(region, once, e, UTC);
            Assertions.assertEquals(twice, once * 2);
        }
    }

    @Test
    public void testArgumentValidation() {
        LogEvent e = new LogEvent(LogLevel.INFO, NOW, "io.jingproject.logtest.JsonFormatterTest", "main", "hi", null,
                Map.of());
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> FileLogEventHandler.writeJson(null, 0, e, UTC));
        try (Allocator alloc = Allocator.newAlloc()) {
            MemorySegment region = alloc.allocate(64);
            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> FileLogEventHandler.writeJson(region, 0, null, UTC));
            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> FileLogEventHandler.writeJson(region, 0, e, null));
        }
    }

    @Test
    public void testRenderThrowableCauseChain() {
        IllegalStateException root = new IllegalStateException("root");
        RuntimeException outer = new RuntimeException("outer", root);
        String rendered = FileLogEventHandler.renderThrowable(outer);
        Assertions.assertTrue(rendered.startsWith("java.lang.RuntimeException: outer" + System.lineSeparator()));
        Assertions.assertTrue(rendered.contains("Caused by: java.lang.IllegalStateException: root"));
        Assertions.assertTrue(rendered.contains("\tat "));
    }

    @Test
    public void testRenderThrowableMatchesExEngine() {
        IllegalStateException suppressed = new IllegalStateException("suppressed");
        RuntimeException outer = new RuntimeException("outer", new IllegalStateException("root"));
        outer.addSuppressed(suppressed);
        String rendered = FileLogEventHandler.renderThrowable(outer);
        Assertions.assertTrue(rendered.contains("Suppressed:"), "actual : " + rendered);
        Assertions.assertTrue(rendered.contains("Caused by: java.lang.IllegalStateException: root"),
                "actual : " + rendered);
        Assertions.assertEquals(renderEx(outer), rendered,
                "the json ex field must match the %ex engine output");
    }

    // renders a throwable through the "%ex" pattern node, the engine under test
    private static String renderEx(Throwable t) {
        LogEvent e = new LogEvent(LogLevel.INFO, NOW, "io.jingproject.logtest.JsonFormatterTest", "main", "m", t,
                Map.of());
        FormatPattern fp = FormatPattern.compile("%ex", UTC, new String[]{"TRACE", "DEBUG", "INFO", "WARN", "ERROR"});
        try (Allocator alloc = Allocator.newAlloc()) {
            MemorySegment region = alloc.allocate(65536);
            long end = fp.fill(region, 0, e);
            Assertions.assertTrue(end >= 0, "fill must succeed, actual : " + end);
            byte[] bytes = new byte[(int) end];
            MemorySegment.copy(region, ValueLayout.JAVA_BYTE, 0, bytes, 0, bytes.length);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }
}