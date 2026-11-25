package io.jingproject.net;

import io.jingproject.common.anno.ProcessorApi;

@ProcessorApi
public interface NetHandler extends AutoCloseable {
    void init();

    @Override
    void close();
}
