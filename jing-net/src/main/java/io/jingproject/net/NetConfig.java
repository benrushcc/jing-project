package io.jingproject.net;

import io.jingproject.common.ConfigurationFactory;

public record NetConfig (
        String coordinatorThreadName,
        String reactorThreadNamePrefix,
        String workerThreadNamePrefix,
        int workers,
        int coordinatorArenaSize,
        int reactorArenaSize,
        int reactorMaxEvents,
        int reactorBufferSize,
        int shutdownTimeout,
        int workerQueueSize
) {

    public static NetConfig create() {
        int workers = ConfigurationFactory.confAsInt("jing.net.workers", 4);
        if(workers <= 0) {
            throw new NetException("illegal workers : " + workers);
        }
        // TODO
        return null;
    }

}
