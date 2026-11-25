package io.jingproject.net;

import io.jingproject.common.anno.ProcessorApi;

@ProcessorApi
@SuppressWarnings("unused")
public interface TcpOutboundHandler extends NetHandler {
    default void onUpdate(TcpChannel channel, Class<?> type, Object data) {
        throw new UnsupportedOperationException();
    }

    default void onBlocked(TcpChannel channel) {
        throw new UnsupportedOperationException();
    }

    default void onMsg(TcpChannel channel, Object msg) {
        throw new UnsupportedOperationException();
    }

    default void onClosure(TcpChannel channel) {
        throw new UnsupportedOperationException();
    }
}
