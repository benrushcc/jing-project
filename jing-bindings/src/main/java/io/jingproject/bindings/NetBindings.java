package io.jingproject.bindings;

import io.jingproject.ffm.Downcall;
import io.jingproject.ffm.FFM;

import java.lang.foreign.MemorySegment;

@FFM(libraryName = "jing")
public interface NetBindings {
    @Downcall(methodName = "jing_connect_blocked_errcode", constant = true, critical = true)
    int connectBlockErrCode();

    @Downcall(methodName = "jing_send_blocked_errcode", constant = true, critical = true)
    int sendBlockErrCode();

    @Downcall(methodName = "jing_interrupt_errcode", constant = true, critical = true)
    int interruptErrCode();

    @Downcall(methodName = "jing_af_inet_code", constant = true, critical = true)
    int afInetCode();

    @Downcall(methodName = "jing_af_inet6_code", constant = true, critical = true)
    int afInet6Code();

    @Downcall(methodName = "jing_af_unix_code", constant = true, critical = true)
    int afUnixCode();

    @Downcall(methodName = "jing_tcp_type_code", constant = true, critical = true)
    int tcpTypeCode();

    @Downcall(methodName = "jing_udp_type_code", constant = true, critical = true)
    int udpTypeCode();

    @Downcall(methodName = "jing_tcp_protocol_code", constant = true, critical = true)
    int tcpProtocolCode();

    @Downcall(methodName = "jing_udp_protocol_code", constant = true, critical = true)
    int udpProtocolCode();

    @Downcall(methodName = "jing_socket", critical = true)
    void createSocket(int af, int type, int protocol, MemorySegment r);
}
