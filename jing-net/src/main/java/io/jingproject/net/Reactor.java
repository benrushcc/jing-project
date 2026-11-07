package io.jingproject.net;

import io.jingproject.common.MpscLinkedQueue;

public final class Reactor {
    private final NetConfig config;
    private final int sequence;
    private final MpscLinkedQueue<ReactorMsg> queue;
    private final Thread thread;

    public Reactor(NetConfig config, int sequence, MpscLinkedQueue<ReactorMsg> queue) {
        this.config = config;
        this.sequence = sequence;
        this.queue = queue;
        this.thread = createReactorThread();
    }

    public Thread thread() {
        return thread;
    }

    private Thread createReactorThread() {
        return Thread.ofPlatform().name(config.reactorThreadNamePrefix() + sequence).unstarted(() -> {

        });
    }
}
