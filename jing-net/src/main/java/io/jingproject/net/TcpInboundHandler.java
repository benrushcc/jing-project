package io.jingproject.net;

public interface TcpInboundHandler extends ChannelHandler {

    void onFailed(TcpChannel tcpChannel);

    void onConnected(TcpChannel tcpChannel);

    void onReceived(TcpChannel tcpChannel, Object data);

    void onShutdown(TcpChannel tcpChannel);

    void onRemoved(TcpChannel tcpChannel);

}
