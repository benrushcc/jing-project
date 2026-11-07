package io.jingproject.net;

import io.jingproject.common.BatchQueue;

import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;

public final class Worker {
    private static final ScopedValue<WorkerData> DATA = ScopedValue.newInstance();
    private final NetConfig config;
    private final int sequence;
    private final BatchQueue<WorkerMsg> queue;
    private final Thread thread;

    public Worker(NetConfig config, int sequence) {
        this.config = config;
        this.sequence = sequence;
        this.queue = new BatchQueue<>(config.workerQueueSize());
        this.thread = createWorkerThread();
    }

    private Thread createWorkerThread() {
        return Thread.ofPlatform().name(config.workerThreadNamePrefix() + sequence).unstarted(() -> {

        });
    }
}
