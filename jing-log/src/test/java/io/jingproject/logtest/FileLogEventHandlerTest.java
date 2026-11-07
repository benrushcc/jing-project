package io.jingproject.logtest;

import io.jingproject.common.log.LogLevel;
import io.jingproject.log.FileLogEventHandler;
import io.jingproject.log.FileLogEventHandler.*;
import io.jingproject.log.FormatPattern;
import io.jingproject.log.LogEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

// file handler tests against the temp directory: size archives are numbered
// and shifted iteratively, time files carry the period key, maxAge drops stale
// archives on rotation, and the json format is a single line per event
public class FileLogEventHandlerTest {
    private static final Instant NOW = Instant.parse("2026-09-19T03:04:05.123Z");
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final Clock FIXED = Clock.fixed(NOW, UTC);

    @TempDir
    Path tempDir;

    private static LogEvent event(String msg) {
        return new LogEvent(LogLevel.INFO, NOW, "io.jingproject.logtest.FileLog", "t", msg, null, Map.of());
    }

    private static FileSettings settings(Path path, Format format, Rotation rotation, long rotateSize,
                                         int maxBackups, long maxAge) {
        return new FileSettings(path, FormatPattern.DEFAULT_PATTERN, format, rotation, rotateSize, Period.DAILY,
                maxBackups, maxAge, FlushMode.IMMEDIATE, 1000, UTC);
    }

    private static class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone;

        MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        void set(Instant instant) {
            this.instant = instant;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId z) {
            return new MutableClock(instant, z);
        }
    }

    @Test
    public void testSizeRotationNumbering() throws Exception {
        Path dir = tempDir.resolve("size");
        Files.createDirectories(dir);
        FileSettings s = settings(dir.resolve("jing.log"), Format.LOG, Rotation.SIZE, 32, 2, 0);
        FileLogEventHandler handler = new FileLogEventHandler(s, FIXED);
        for (int i = 1; i <= 4; i++) {
            handler.handle(List.of(event("m" + i)));
        }
        handler.close();
        Assertions.assertTrue(Files.exists(dir.resolve("jing.log")));
        Assertions.assertTrue(Files.exists(dir.resolve("jing.1.log")));
        Assertions.assertTrue(Files.exists(dir.resolve("jing.2.log")));
        Assertions.assertFalse(Files.exists(dir.resolve("jing.3.log")));
        Assertions.assertTrue(readText(dir.resolve("jing.log")).endsWith("- m4\n"));
        Assertions.assertTrue(readText(dir.resolve("jing.1.log")).endsWith("- m3\n"));
        Assertions.assertTrue(readText(dir.resolve("jing.2.log")).endsWith("- m2\n"));
    }

    @Test
    public void testMaxBackupsZeroDeletesActive() throws Exception {
        Path dir = tempDir.resolve("max0");
        Files.createDirectories(dir);
        FileSettings s = settings(dir.resolve("jing.log"), Format.LOG, Rotation.SIZE, 16, 0, 0);
        FileLogEventHandler handler = new FileLogEventHandler(s, FIXED);
        handler.handle(List.of(event("m1")));
        handler.handle(List.of(event("m2")));
        handler.close();
        Assertions.assertTrue(Files.exists(dir.resolve("jing.log")));
        Assertions.assertFalse(Files.exists(dir.resolve("jing.1.log")));
        Assertions.assertTrue(readText(dir.resolve("jing.log")).endsWith("- m2\n"));
    }

    @Test
    public void testTimeRotation() throws Exception {
        Path dir = tempDir.resolve("time");
        Files.createDirectories(dir);
        MutableClock clock = new MutableClock(NOW, UTC);
        FileSettings s = settings(dir.resolve("jing.log"), Format.LOG, Rotation.TIME, 32, 2, 0);
        FileLogEventHandler handler = new FileLogEventHandler(s, clock);
        handler.handle(List.of(event("day1")));
        Assertions.assertTrue(Files.exists(dir.resolve("jing.2026-09-19.log")));
        clock.set(NOW.plus(Duration.ofDays(1)));
        handler.handle(List.of(event("day2")));
        handler.close();
        Assertions.assertTrue(Files.exists(dir.resolve("jing.2026-09-20.log")));
        Path day1 = dir.resolve("jing.2026-09-19.log");
        Path day2 = dir.resolve("jing.2026-09-20.log");
        Assertions.assertTrue(readText(day1).endsWith("- day1\n"));
        Assertions.assertTrue(readText(day2).endsWith("- day2\n"));
    }

    @Test
    public void testMaxAgeCleanupOnRotation() throws Exception {
        Path dir = tempDir.resolve("maxage");
        Files.createDirectories(dir);
        Path stale = dir.resolve("jing.2026-09-17.log");
        Files.writeString(stale, "old");
        Files.setLastModifiedTime(stale, FileTime.from(NOW.minus(Duration.ofDays(3))));
        Path ancient = dir.resolve("jing.2026-09-15.log");
        Files.writeString(ancient, "old");
        Files.setLastModifiedTime(ancient, FileTime.from(NOW.minus(Duration.ofDays(10))));
        // archive-looking names that must survive the cleanup: not a date key
        Path noise = dir.resolve("jing.notes.log");
        Files.writeString(noise, "n");
        Files.setLastModifiedTime(noise, FileTime.from(NOW.minus(Duration.ofDays(3))));
        Path unrelated = dir.resolve("readme.txt");
        Files.writeString(unrelated, "r");
        Files.setLastModifiedTime(unrelated, FileTime.from(NOW.minus(Duration.ofDays(3))));

        MutableClock clock = new MutableClock(NOW, UTC);
        FileSettings s = settings(dir.resolve("jing.log"), Format.LOG, Rotation.TIME, 32, 2, 1);
        FileLogEventHandler handler = new FileLogEventHandler(s, clock);
        handler.handle(List.of(event("day1")));
        Assertions.assertTrue(Files.exists(dir.resolve("jing.2026-09-19.log")));
        clock.set(NOW.plus(Duration.ofDays(1))); // rotation triggers the cleanup
        handler.handle(List.of(event("day2")));
        handler.close();
        Assertions.assertFalse(Files.exists(stale), "stale archive must be deleted");
        Assertions.assertFalse(Files.exists(ancient), "ancient archive must be deleted");
        Assertions.assertTrue(Files.exists(noise), "non-date-key files must be kept");
        Assertions.assertTrue(Files.exists(unrelated), "unrelated files must be kept");
        Assertions.assertTrue(Files.exists(dir.resolve("jing.2026-09-19.log")));
        Assertions.assertTrue(Files.exists(dir.resolve("jing.2026-09-20.log")));
    }

    @Test
    public void testJsonFormatAppendsJsonExtension() throws Exception {
        Path dir = tempDir.resolve("json");
        Files.createDirectories(dir);
        FileSettings s = settings(dir.resolve("app"), Format.JSON, Rotation.OFF, 32, 0, 0);
        FileLogEventHandler handler = new FileLogEventHandler(s, FIXED);
        handler.handle(List.of(event("hi")));
        handler.close();
        Path app = dir.resolve("app.json");
        Assertions.assertTrue(Files.exists(app), "json path without a dot must gain the .json extension");
        String content = readText(app);
        Assertions.assertTrue(content.startsWith(
                "{\"ts\":\"2026-09-19T03:04:05.123Z\",\"level\":\"INFO\",\"thread\":\"t\","
                        + "\"logger\":\"io.jingproject.logtest.FileLog\",\"msg\":\"hi\""));
        Assertions.assertTrue(content.endsWith("}\n"));
    }

    @Test
    public void testRotationOffSingleGrowingFile() throws Exception {
        Path dir = tempDir.resolve("off");
        Files.createDirectories(dir);
        FileSettings s = settings(dir.resolve("jing.log"), Format.LOG, Rotation.OFF, 8, 0, 0);
        FileLogEventHandler handler = new FileLogEventHandler(s, FIXED);
        handler.handle(List.of(event("m1")));
        handler.handle(List.of(event("m2")));
        handler.close();
        String content = readText(dir.resolve("jing.log"));
        Assertions.assertEquals(2, content.split("\n", -1).length - 1, "content : [" + content + "]");
        Assertions.assertTrue(content.contains("- m1\n") && content.endsWith("- m2\n"),
                "content : [" + content + "]");
    }

    @Test
    public void testOversizedEventDroppedFromFile() throws Exception {
        Path dir = tempDir.resolve("oversized");
        Files.createDirectories(dir);
        FileSettings s = settings(dir.resolve("jing.log"), Format.LOG, Rotation.OFF, 8, 0, 0);
        FileLogEventHandler handler = new FileLogEventHandler(s, FIXED);
        handler.handle(List.of(event("z".repeat(1_100_000)), event("small")));
        handler.close();
        String content = readText(dir.resolve("jing.log"));
        Assertions.assertEquals(1, content.split("\n", -1).length - 1,
                "an event larger than the write region must be dropped, content : [" + content + "]");
        Assertions.assertTrue(content.endsWith("- small\n"), "content : [" + content + "]");
    }

    @Test
    public void testMaxAgeCleanupRunsAtStartWithoutRotation() throws Exception {
        Path dir = tempDir.resolve("maxage-start");
        Files.createDirectories(dir);
        Path stale = dir.resolve("jing.3.log");
        Files.writeString(stale, "old");
        Files.setLastModifiedTime(stale, FileTime.from(NOW.minus(Duration.ofDays(10))));
        Path recent = dir.resolve("jing.2.log");
        Files.writeString(recent, "new");
        Files.setLastModifiedTime(recent, FileTime.from(NOW.minus(Duration.ofDays(1))));

        FileSettings s = settings(dir.resolve("jing.log"), Format.LOG, Rotation.OFF, 8, 0, 1);
        FileLogEventHandler handler = new FileLogEventHandler(s, FIXED);
        handler.handle(List.of(event("m1")));
        handler.close();
        Assertions.assertFalse(Files.exists(stale), "a stale archive must be cleaned at the first open");
        Assertions.assertTrue(Files.exists(recent), "a recent archive must be kept");
    }

    @Test
    public void testIdFlushCloseAndValidation() throws Exception {
        Path dir = tempDir.resolve("id");
        Files.createDirectories(dir);
        FileSettings s = settings(dir.resolve("jing.log"), Format.LOG, Rotation.OFF, 8, 0, 0);
        FileLogEventHandler handler = new FileLogEventHandler(s, FIXED);
        Assertions.assertEquals("file", handler.id());
        handler.flush();
        handler.close();
        handler.close();
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new FileLogEventHandler((FileSettings) null));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new FileLogEventHandler(s, (Clock) null));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new FileLogEventHandler(
                        new FileSettings(null, FormatPattern.DEFAULT_PATTERN, Format.LOG, Rotation.OFF, 8,
                        Period.DAILY, 0, 0, FlushMode.IMMEDIATE, 1000, UTC),
                        FIXED));
    }

    private static String readText(Path p) throws Exception {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }
}