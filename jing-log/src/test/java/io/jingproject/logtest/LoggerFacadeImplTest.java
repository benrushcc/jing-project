package io.jingproject.logtest;

import io.jingproject.common.log.LogLevel;
import io.jingproject.common.log.Logger;
import io.jingproject.log.LogEvent;
import io.jingproject.log.LogEventHandler;
import io.jingproject.log.LoggerFacadeImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

// end-to-end facade tests: the async consumer must pick queued events up, the
// level gate must filter on the producer side, and stop() must drain cleanly
public class LoggerFacadeImplTest {
    private static final long TIMEOUT_MILLIS = 5000;

    private static void awaitCount(CaptureLogEventHandler capture, int expected) {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(TIMEOUT_MILLIS);
        while (System.nanoTime() < deadline) {
            if (capture.eventCount() >= expected) {
                return;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
        }
        Assertions.fail("expected " + expected + " events, actual : " + capture.eventCount());
    }

    // a handler that always fails delivery so the facade must disable it and
    // release its close() resource exactly once
    private static class FailingHandler implements LogEventHandler, AutoCloseable {
        private static final AtomicInteger CLOSES = new AtomicInteger();

        @Override
        public void handle(List<LogEvent> events) {
            throw new IllegalStateException("boom");
        }

        @Override
        public String id() {
            return "failing";
        }

        @Override
        public void close() {
            CLOSES.incrementAndGet();
        }
    }

    @Test
    public void testDisabledHandlerIsClosed() throws Exception {
        FailingHandler.CLOSES.set(0);
        LoggerFacadeImpl facade = new LoggerFacadeImpl(List.of(new FailingHandler()), 16, 4096);
        facade.start();
        try {
            Logger logger = facade.getLogger(LoggerFacadeImplTest.class);
            logger.info("will fail");
        } finally {
            facade.stop();
        }
        Assertions.assertEquals(1, FailingHandler.CLOSES.get(),
                "the disabled handler must be closed exactly once");
    }

    @Test
    public void testDeliverBasicEvent() throws Exception {
        LoggerFacadeImpl facade = new LoggerFacadeImpl();
        facade.start();
        try {
            CaptureLogEventHandler capture = CaptureLogEventHandler.latest();
            Assertions.assertNotNull(capture);
            Logger logger = facade.getLogger(LoggerFacadeImplTest.class);
            String producerThread = Thread.currentThread().getName();
            logger.info("hello jing-log");
            awaitCount(capture, 1);
            List<LogEvent> events = capture.allEvents();
            LogEvent e = events.get(0);
            Assertions.assertEquals(LogLevel.INFO, e.level());
            Assertions.assertEquals("hello jing-log", e.msg());
            Assertions.assertEquals(LoggerFacadeImplTest.class.getName(), e.className());
            Assertions.assertEquals(producerThread, e.threadName());
            Assertions.assertNotNull(e.instant());
            Assertions.assertTrue(e.logContext().isEmpty());
        } finally {
            facade.stop();
        }
    }

    @Test
    public void testLevelGateOnProducerSide() throws Exception {
        LoggerFacadeImpl facade = new LoggerFacadeImpl();
        facade.start();
        try {
            CaptureLogEventHandler capture = CaptureLogEventHandler.latest();
            Logger logger = facade.getLogger(LoggerFacadeImplTest.class);
            logger.log(LogLevel.DEBUG, "below threshold", null);
            logger.info("kept", null);
            awaitCount(capture, 1);
            Assertions.assertEquals("kept", capture.allEvents().get(0).msg());

            logger.setLevel(LogLevel.ERROR);
            logger.info("gated now", null);
            logger.error("after gate", null);
            awaitCount(capture, 2);
            for (LogEvent e : capture.allEvents()) {
                Assertions.assertTrue(e.msg().equals("kept") || e.msg().equals("after gate"),
                        "gated event must never reach the queue, actual : " + e.msg());
            }
        } finally {
            facade.stop();
        }
    }

    @Test
    public void testContextPropagation() throws Exception {
        LoggerFacadeImpl facade = new LoggerFacadeImpl();
        facade.start();
        try {
            CaptureLogEventHandler capture = CaptureLogEventHandler.latest();
            Logger logger = facade.getLogger(LoggerFacadeImplTest.class);
            ScopedValue.where(LoggerFacadeImpl.LOGGING_CONTEXT, Map.of("user", "alice")).run(() -> {
                logger.info("ctx hello");
            });
            awaitCount(capture, 1);
            LogEvent e = capture.allEvents().get(0);
            Assertions.assertEquals(Map.of("user", "alice"), e.logContext());
        } finally {
            facade.stop();
        }
    }

    @Test
    public void testStopDrainsAndIsIdempotent() throws Exception {
        LoggerFacadeImpl facade = new LoggerFacadeImpl();
        facade.start();
        CaptureLogEventHandler capture = CaptureLogEventHandler.latest();
        Logger logger = facade.getLogger(LoggerFacadeImplTest.class);
        logger.info("before stop");
        awaitCount(capture, 1);
        facade.stop();
        facade.stop();
        Assertions.assertEquals(1, capture.eventCount());
    }

    @Test
    public void testStartTwiceRejected() throws Exception {
        LoggerFacadeImpl facade = new LoggerFacadeImpl();
        facade.start();
        try {
            Assertions.assertThrows(IllegalStateException.class, facade::start);
        } finally {
            facade.stop();
        }
    }

    @Test
    public void testMultiProducerDelivery() throws Exception {
        LoggerFacadeImpl facade = new LoggerFacadeImpl();
        facade.start();
        try {
            CaptureLogEventHandler capture = CaptureLogEventHandler.latest();
            Logger logger = facade.getLogger(LoggerFacadeImplTest.class);
            int threads = 4;
            int perThread = 100;
            CountDownLatch go = new CountDownLatch(1);
            List<Thread> producers = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                int id = i;
                Thread t = new Thread(() -> {
                    try {
                        go.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    for (int j = 0; j < perThread; j++) {
                        logger.info("t" + id + "-" + j);
                    }
                });
                producers.add(t);
                t.start();
            }
            go.countDown();
            for (Thread t : producers) {
                t.join();
            }
            awaitCount(capture, threads * perThread);
            Assertions.assertEquals(threads * perThread, capture.eventCount());
        } finally {
            facade.stop();
        }
    }

    @Test
    public void testSetLevelRejectsNull() throws Exception {
        LoggerFacadeImpl facade = new LoggerFacadeImpl();
        facade.start();
        try {
            Logger logger = facade.getLogger(LoggerFacadeImplTest.class);
            Assertions.assertThrows(IllegalArgumentException.class, () -> logger.setLevel(null));
            Assertions.assertThrows(IllegalArgumentException.class, () -> logger.log(null, "x", null));
            Assertions.assertThrows(IllegalArgumentException.class, () -> logger.enabled(null));
        } finally {
            facade.stop();
        }
    }
}