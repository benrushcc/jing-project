package io.jingproject.logtest;

import io.jingproject.common.log.LogLevel;
import io.jingproject.log.ConsoleLogEventHandler;
import io.jingproject.log.ConsoleLogEventHandler.ConsoleSettings;
import io.jingproject.log.ConsoleLogEventHandler.Output;
import io.jingproject.log.LogEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// handler tests: the stdout/stderr outputs are injected so no native handles
// are acquired, the routing/color/level-text rules are pure functions, and a
// failing output must disable the handler once and for all
public class ConsoleLogEventHandlerTest {
    private static final Instant NOW = Instant.parse("2026-09-19T03:04:05.123Z");
    private static final ZoneId UTC = ZoneId.of("UTC");

    private static LogEvent event(LogLevel level, String msg) {
        return new LogEvent(level, NOW, "io.jingproject.logtest.ConsoleLogEventHandlerTest", "main", msg, null,
                Map.of());
    }

    private static Output recording(List<String> into) {
        return (segment, len) -> {
            byte[] bytes = new byte[(int) len];
            MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, 0, bytes, 0, bytes.length);
            into.add(new String(bytes, StandardCharsets.UTF_8));
            return len;
        };
    }

    @Test
    public void testRoutingBetweenStreams() {
        List<String> stdout = new ArrayList<>();
        List<String> stderr = new ArrayList<>();
        ConsoleSettings settings = new ConsoleSettings(false, "%p %msg%n", LogLevel.WARN, "green", null, UTC,
                recording(stdout), recording(stderr));
        ConsoleLogEventHandler handler = new ConsoleLogEventHandler(settings);
        handler.handle(List.of(event(LogLevel.INFO, "hi"), event(LogLevel.WARN, "warn"),
                event(LogLevel.ERROR, "boom")));
        Assertions.assertEquals(List.of("INFO hi\n"), stdout);
        Assertions.assertEquals(List.of("WARN warn\nERROR boom\n"), stderr);
    }

    @Test
    public void testErrrLevelNoneRoutesEverythingToStdout() {
        List<String> stdout = new ArrayList<>();
        List<String> stderr = new ArrayList<>();
        ConsoleSettings settings = new ConsoleSettings(false, "%p%n", null, "green", null, UTC, recording(stdout),
                recording(stderr));
        ConsoleLogEventHandler handler = new ConsoleLogEventHandler(settings);
        handler.handle(List.of(event(LogLevel.ERROR, "x")));
        Assertions.assertEquals(List.of("ERROR\n"), stdout);
        Assertions.assertTrue(stderr.isEmpty());
    }

    @Test
    public void testDisabledAfterWriteFailure() {
        int[] calls = {0};
        Output failing = (segment, len) -> {
            calls[0]++;
            return -5;
        };
        ConsoleSettings settings = new ConsoleSettings(false, "%p %msg%n", null, "green", null, UTC, failing,
                failing);
        ConsoleLogEventHandler handler = new ConsoleLogEventHandler(settings);
        handler.handle(List.of(event(LogLevel.INFO, "a")));
        Assertions.assertEquals(1, calls[0]);
        handler.handle(List.of(event(LogLevel.INFO, "b")));
        Assertions.assertEquals(1, calls[0], "a disabled handler must not write again");
    }

    @Test
    public void testRoutesToStderr() {
        Assertions.assertTrue(ConsoleLogEventHandler.routesToStderr(LogLevel.ERROR, LogLevel.WARN));
        Assertions.assertFalse(ConsoleLogEventHandler.routesToStderr(LogLevel.INFO, LogLevel.WARN));
        Assertions.assertTrue(ConsoleLogEventHandler.routesToStderr(LogLevel.WARN, LogLevel.WARN));
        Assertions.assertFalse(ConsoleLogEventHandler.routesToStderr(LogLevel.ERROR, null));
    }

    @Test
    public void testResolveColor() {
        Assertions.assertEquals("32", ConsoleLogEventHandler.resolveColor("green"));
        Assertions.assertEquals("1;31", ConsoleLogEventHandler.resolveColor("bold,red"));
        Assertions.assertEquals("31;32", ConsoleLogEventHandler.resolveColor(" red , GREEN "));
        Assertions.assertEquals("", ConsoleLogEventHandler.resolveColor(null));
        Assertions.assertEquals("", ConsoleLogEventHandler.resolveColor(""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ConsoleLogEventHandler.resolveColor("neon"));
    }

    @Test
    public void testStyleLevel() {
        Assertions.assertEquals("INFO", ConsoleLogEventHandler.styleLevel(false, "green", null, "INFO"));
        Assertions.assertEquals("\u001b[32mINFO\u001b[0m",
                ConsoleLogEventHandler.styleLevel(true, "green", null, "INFO"));
        Assertions.assertEquals("\u001b[31mINFO\u001b[0m",
                ConsoleLogEventHandler.styleLevel(true, "green", "red", "INFO"));
        Assertions.assertEquals("\u001b[32mINFO\u001b[0m",
                ConsoleLogEventHandler.styleLevel(true, "green", "", "INFO"));
        Assertions.assertEquals("INFO", ConsoleLogEventHandler.styleLevel(true, null, null, "INFO"));
    }

    @Test
    public void testStyledLevelTexts() {
        String[] levels = ConsoleLogEventHandler.styledLevelTexts(false, "green", null, 5, true);
        Assertions.assertEquals("TRACE", levels[0]);
        Assertions.assertEquals("INFO ", levels[2]);
        Assertions.assertEquals("ERROR", levels[4]);
        String[] colored = ConsoleLogEventHandler.styledLevelTexts(true, "red", null, 8, false);
        Assertions.assertEquals("\u001b[31m   TRACE\u001b[0m", colored[0]);
        Assertions.assertEquals("\u001b[31m   ERROR\u001b[0m", colored[4]);
    }

    @Test
    public void testOversizedEventDroppedWithoutStaleCursor() {
        List<String> stdout = new ArrayList<>();
        List<String> stderr = new ArrayList<>();
        ConsoleSettings settings = new ConsoleSettings(false, "%msg", null, "green", null, UTC, recording(stdout),
                recording(stderr));
        ConsoleLogEventHandler handler = new ConsoleLogEventHandler(settings);
        // a 900k event fits alone, the 1.25m event exceeds the 1m write region
        // even in a fresh region, the trailing small event must render cleanly
        // after the drop and never carry a stale prefix
        LogEvent a = event(LogLevel.INFO, "a".repeat(900_000));
        LogEvent b = event(LogLevel.INFO, "x".repeat(1_250_000));
        LogEvent c = event(LogLevel.INFO, "tail");
        handler.handle(List.of(a, b, c));
        StringBuilder joined = new StringBuilder();
        for (String s : stdout) {
            joined.append(s);
        }
        Assertions.assertEquals("a".repeat(900_000) + "tail", joined.toString(),
                "the oversized middle event must be dropped without duplicating the prefix");
        Assertions.assertTrue(stderr.isEmpty());
    }

    @Test
    public void testPartialWriteRejected() {
        int[] calls = {0};
        Output partial = (segment, len) -> {
            calls[0]++;
            return len - 1;
        };
        ConsoleSettings settings = new ConsoleSettings(false, "%p %msg%n", null, "green", null, UTC, partial,
                partial);
        ConsoleLogEventHandler handler = new ConsoleLogEventHandler(settings);
        handler.handle(List.of(event(LogLevel.INFO, "a")));
        Assertions.assertEquals(1, calls[0]);
        handler.handle(List.of(event(LogLevel.INFO, "b")));
        Assertions.assertEquals(1, calls[0], "a handler with a partial write must also be disabled");
    }

    @Test
    public void testIdAndDefaults() {
        List<String> stdout = new ArrayList<>();
        List<String> stderr = new ArrayList<>();
        ConsoleSettings settings = new ConsoleSettings(false, "%p %msg%n", null, "green", null, UTC,
                recording(stdout), recording(stderr));
        ConsoleLogEventHandler handler = new ConsoleLogEventHandler(settings);
        Assertions.assertEquals("console", handler.id());
        handler.flush();
        Assertions.assertEquals(0, stdout.size());
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ConsoleLogEventHandler(null));
    }
}