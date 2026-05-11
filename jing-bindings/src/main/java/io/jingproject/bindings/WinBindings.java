package io.jingproject.bindings;

import io.jingproject.common.Os;
import io.jingproject.ffm.Downcall;
import io.jingproject.ffm.FFM;

import java.lang.foreign.MemorySegment;

@FFM(libraryName = "jing_bindings", supportedOS = {Os.WINDOWS})
public interface WinBindings {
    @Downcall(methodName = "jing_win_page_size", constant = true, critical = true)
    long winPageSize();

    @Downcall(methodName = "jing_win_allocate_granularity", constant = true, critical = true)
    long winAllocateGranularity();

    @Downcall(methodName = "jing_win_mem_reserve", constant = true, critical = true)
    int winMemReserve();

    @Downcall(methodName = "jing_win_mem_commit", constant = true, critical = true)
    int winMemCommit();

    @Downcall(methodName = "jing_win_mem_decommit", constant = true, critical = true)
    int winMemDecommit();

    @Downcall(methodName = "jing_win_mem_release", constant = true, critical = true)
    int winMemRelease();

    @Downcall(methodName = "jing_win_page_read_write", constant = true, critical = true)
    int winPageReadWrite();

    @Downcall(methodName = "jing_win_virtual_alloc", critical = true)
    MemorySegment winVirtualAlloc(MemorySegment addr, long size, int type, int prot);

    @Downcall(methodName = "jing_win_virtual_free", critical = true)
    int winVirtualFree(MemorySegment addr, long size, int type);

    @Downcall(methodName = "jing_win_connect_blocked_errcode", constant = true, critical = true)
    int connectBlockErrCode();

    @Downcall(methodName = "jing_win_send_blocked_errcode", constant = true, critical = true)
    int sendBlockErrCode();

    @Downcall(methodName = "jing_win_interrupt_errcode", constant = true, critical = true)
    int interruptErrCode();

    @Downcall(methodName = "jing_win_af_inet_code", constant = true, critical = true)
    int afInetCode();

    @Downcall(methodName = "jing_win_af_inet6_code", constant = true, critical = true)
    int afInet6Code();

    @Downcall(methodName = "jing_win_af_unix_code", constant = true, critical = true)
    int afUnixCode();

    @Downcall(methodName = "jing_win_tcp_type_code", constant = true, critical = true)
    int tcpTypeCode();

    @Downcall(methodName = "jing_win_udp_type_code", constant = true, critical = true)
    int udpTypeCode();

    @Downcall(methodName = "jing_win_tcp_protocol_code", constant = true, critical = true)
    int tcpProtocolCode();

    @Downcall(methodName = "jing_win_udp_protocol_code", constant = true, critical = true)
    int udpProtocolCode();

    @Downcall(methodName = "jing_socket", critical = true)
    int createSocket(int af, int type, int protocol);
}
