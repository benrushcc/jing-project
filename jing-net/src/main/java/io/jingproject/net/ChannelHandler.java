package io.jingproject.net;

public interface ChannelHandler {
    void initialize();

    void onEvent(Object event);

    void destroy();
}
