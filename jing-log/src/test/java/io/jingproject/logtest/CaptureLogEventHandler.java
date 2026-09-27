package io.jingproject.logtest;

import io.jingproject.log.LogEvent;
import io.jingproject.log.LogEventHandler;

import java.util.ArrayList;
import java.util.List;

// test-double log event handler that records every batch it receives,
// allowing asynchronous tests to assert on delivered events. every instance
// registers itself here so tests can grab the instance bound to their own
// facade (the facade creates its providers through ServiceLoader).
public final class CaptureLogEventHandler implements LogEventHandler {
    private static final List<CaptureLogEventHandler> CREATED = new ArrayList<>();

    private final List<List<LogEvent>> batches = new ArrayList<>();

    public CaptureLogEventHandler() {
        synchronized (CREATED) {
            CREATED.add(this);
        }
    }

    @Override
    public String id() {
        return "capture";
    }

    @Override
    public void handle(List<LogEvent> events) {
        synchronized (batches) {
            batches.add(List.copyOf(events));
        }
    }

    @Override
    public void flush() {
    }

    // returns the most recently created instance
    public static CaptureLogEventHandler latest() {
        synchronized (CREATED) {
            return CREATED.isEmpty() ? null : CREATED.get(CREATED.size() - 1);
        }
    }

    // returns all events delivered so far across every batch
    public int eventCount() {
        synchronized (batches) {
            return batches.stream().mapToInt(List::size).sum();
        }
    }

    // returns a copy of all delivered events in delivery order
    public List<LogEvent> allEvents() {
        synchronized (batches) {
            List<LogEvent> r = new ArrayList<>();
            for (List<LogEvent> batch : batches) {
                r.addAll(batch);
            }
            return r;
        }
    }
}