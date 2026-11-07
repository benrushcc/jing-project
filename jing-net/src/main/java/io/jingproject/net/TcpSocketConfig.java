package io.jingproject.net;

public final class TcpSocketConfig {
    private boolean reuseAddr = true;
    private boolean tcpNoDelay = true;
    private boolean ipv6Only = false;

    public boolean reuseAddr() {
        return reuseAddr;
    }

    public void setReuseAddr(boolean reuseAddr) {
        this.reuseAddr = reuseAddr;
    }

    public boolean tcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    public boolean ipv6Only() {
        return ipv6Only;
    }

    public void setIpv6Only(boolean ipv6Only) {
        this.ipv6Only = ipv6Only;
    }
}
