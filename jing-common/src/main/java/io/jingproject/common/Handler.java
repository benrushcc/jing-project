package io.jingproject.common;

public interface Handler<T extends Transmittable<T>> {
    void onFailed(Channel<T> channel);

    void onConnected(Channel<T> channel);

    void onReceived(Channel<T> channel, T data);

    void onShutdown(Channel<T> channel);

    void onRemoved(Channel<T> channel);
}
