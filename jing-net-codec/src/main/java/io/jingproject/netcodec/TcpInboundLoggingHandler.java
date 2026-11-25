package io.jingproject.netcodec;

import io.jingproject.common.log.Logger;
import io.jingproject.common.log.LoggerFactory;
import io.jingproject.net.TcpChannel;
import io.jingproject.net.TcpInboundHandler;

public final class TcpInboundLoggingHandler implements TcpInboundHandler {
    private static final Logger log = LoggerFactory.getLogger(TcpInboundLoggingHandler.class);

    @Override
    public void init() {

    }

    @Override
    public void onUpdate(TcpChannel channel, Class<?> type, Object data) {
        TcpInboundHandler.super.onUpdate(channel, type, data);
    }

    @Override
    public void onMsg(TcpChannel channel, Object msg) {
        TcpInboundHandler.super.onMsg(channel, msg);
    }

    @Override
    public void onTimeout(TcpChannel channel, Object msg) {
        TcpInboundHandler.super.onTimeout(channel, msg);
    }

    @Override
    public void onClosure(TcpChannel channel) {
        TcpInboundHandler.super.onClosure(channel);
    }

    @Override
    public void close() {

    }
}
