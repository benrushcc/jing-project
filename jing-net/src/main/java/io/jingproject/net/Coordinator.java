package io.jingproject.net;

import io.jingproject.bindings.net.Mux;
import io.jingproject.common.LifeCycle;

import java.util.List;

public final class Coordinator implements LifeCycle {
    private final NetConfig config;
    private final List<CoordinatorData> data;
    private final Thread coordinatorThread;

    record CoordinatorData() {

    }

    public Coordinator() {
        this(NetConfig.create());
    }

    public Coordinator(NetConfig config) {
        this.config = config;
        this.data = createCoordinatorData();
        this.coordinatorThread = createCoordinatorThread();
    }

    private List<CoordinatorData> createCoordinatorData() {
        // TODO
        return List.of();
    }

    private Thread createCoordinatorThread() {
        return Thread.ofPlatform().unstarted(() -> {

        });
    }

    public NetConfig config() {
        return config;
    }

    public void publishResource() {

    }

    public void publishServer() {

    }

    @Override
    public void start() {
        coordinatorThread.start();
    }

    @Override
    public void stop() {

    }
}
