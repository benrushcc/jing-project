package io.jingproject.common;

public interface Handler<T> {
    void onFailed(Channel<T> channel);

    void onConnected(Channel<T> channel);

    // TODO 待定接受的逻辑
    void onReceived(Channel<T> channel, T data);

    void onShutdown(Channel<T> channel);

    void onRemoved(Channel<T> channel);
}
