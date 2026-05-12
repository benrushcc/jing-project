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

    @Downcall(methodName = "jing_win_ansi_support", constant = true, critical = true)
    int winAnsiSupport();

    @Downcall(methodName = "jing_std_output_dword", constant = true, critical = true)
    int stdOutputDword();

    @Downcall(methodName = "jing_std_error_dword", constant = true, critical = true)
    int stdErrorDword();

    @Downcall(methodName = "jing_get_std_handle", critical = true)
    void getStdHandle(int handle, MemorySegment jingResult);

    @Downcall(methodName = "jing_create_file")
    void createFile(MemorySegment fileName, MemorySegment jingResult);

    @Downcall(methodName = "jing_write_file")
    void writeFile(MemorySegment handle, MemorySegment buf, int len, MemorySegment jingResult);

    @Downcall(methodName = "jing_flush_file")
    void flushFile(MemorySegment handle, MemorySegment jingResult);

    @Downcall(methodName = "jing_stdout_fileno", constant = true, critical = true)
    int stdOutputFileno();

    @Downcall(methodName = "jing_stderr_fileno", constant = true, critical = true)
    int stdErrorFileno();

    @Downcall(methodName = "jing_open_fd")
    void openFd(MemorySegment fileName, MemorySegment jingResult);

    @Downcall(methodName = "jing_write_fd")
    void writeFd(int fd, MemorySegment buf, long size, MemorySegment jingResult);

    // wepoll related functions
    @Downcall(methodName = "jing_wepoll_in", constant = true, critical = true)
    int wepollIn();

    @Downcall(methodName = "jing_wepoll_out", constant = true, critical = true)
    int wepollOut();

    @Downcall(methodName = "jing_wepoll_err", constant = true, critical = true)
    int wepollErr();

    @Downcall(methodName = "jing_wepoll_hup", constant = true, critical = true)
    int wepollHup();

    @Downcall(methodName = "jing_wepoll_ctl_add", constant = true, critical = true)
    int wepollAdd();

    @Downcall(methodName = "jing_wepoll_ctl_mod", constant = true, critical = true)
    int wepollMod();

    @Downcall(methodName = "jing_wepoll_ctl_del", constant = true, critical = true)
    int wepollDel();

    @Downcall(methodName = "jing_wepoll_create", critical = true)
    MemorySegment wepollCreate();

    @Downcall(methodName = "jing_wepoll_ctl")
    int wepollCtl(MemorySegment epfd, long socket, int op, int eventTypes, int data);

    @Downcall(methodName = "jing_wepoll_wait")
    void wepollWait(MemorySegment epfd, MemorySegment events, int maxEvents, int timeout, MemorySegment r);

    @Downcall(methodName = "jing_wepoll_close", critical = true)
    int wepollClose(MemorySegment epfd);
}
