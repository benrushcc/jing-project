package io.jingproject.net;

import io.jingproject.bindings.net.Mux;
import io.jingproject.common.LifeCycle;

import java.util.List;

public final class Coordinator implements LifeCycle {

    private final NetConfig config;
    private final Mux mux;
    private final List<CoordinatorData> data;
    private final Thread thread;

    record CoordinatorData() {

    }

    public Coordinator() {
        this(NetConfig.create());
    }

    public Coordinator(NetConfig config) {
        this.config = config;
        this.mux = Mux.newMux();
        this.data = createCoordinatorData();
        this.thread = createCoordinatorThread();
    }

    private List<CoordinatorData> createCoordinatorData() {
        // TODO
        return List.of();
    }

    private Thread createCoordinatorThread() {
        return Thread.ofPlatform().unstarted(() -> {

        });
    }

    public void publishResource() {

    }

    public void publishServer() {

    }

    @Override
    public void start() {
        thread.start();
    }

    @Override
    public void stop() {

    }
}
