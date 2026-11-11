package io.jingproject.net;

public interface ChannelHandler<T extends NetEvent> {
    void initialize();

    void onEvent(T event);

    void destroy();
}
