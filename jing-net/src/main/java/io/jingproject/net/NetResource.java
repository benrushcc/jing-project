package io.jingproject.net;

import io.jingproject.common.anno.ProcessorApi;

@ProcessorApi
public interface NetResource extends AutoCloseable {

    NetResourceScope scope();

    void init();

    @Override
    void close();

    enum NetResourceScope {
        COORDINATOR,
        WORKER,
        CHANNEL
    }
}
