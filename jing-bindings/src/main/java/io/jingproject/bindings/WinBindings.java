package io.jingproject.bindings;

import io.jingproject.common.Os;
import io.jingproject.ffm.Downcall;
import io.jingproject.ffm.FFM;

import java.lang.foreign.MemorySegment;

@FFM(libraryName = "jing_bindings", supportedOS = {Os.WINDOWS})
public interface WinBindings {
    @Downcall(methodName = "jing_win_max_align", critical = true)
    long winMaxAlign();

    @Downcall(methodName = "jing_win_aligned_alloc", critical = true)
    long winAlignedAlloc(long size, long alignment);

    @Downcall(methodName = "jing_win_batch_free")
    void winBatchFree(long ptrs, long len, long freeAddr);

    @Downcall(methodName = "jing_win_page_size",critical = true)
    long winPageSize();

    @Downcall(methodName = "jing_win_allocate_granularity",critical = true)
    long winAllocateGranularity();

    @Downcall(methodName = "jing_win_mem_reserve",critical = true)
    int winMemReserve();

    @Downcall(methodName = "jing_win_mem_commit",critical = true)
    int winMemCommit();

    @Downcall(methodName = "jing_win_mem_decommit",critical = true)
    int winMemDecommit();

    @Downcall(methodName = "jing_win_mem_release",critical = true)
    int winMemRelease();

    @Downcall(methodName = "jing_win_page_read_write",critical = true)
    int winPageReadWrite();

    @Downcall(methodName = "jing_win_virtual_alloc", critical = true)
    long winVirtualAlloc(long addr, long size, int type, int prot);

    @Downcall(methodName = "jing_win_virtual_free", critical = true)
    int winVirtualFree(long addr, long size, int type);

    // log related
    @Downcall(methodName = "jing_win_ansi_support",critical = true)
    int winAnsiSupport();

    @Downcall(methodName = "jing_std_output_dword",critical = true)
    int stdOutputDword();

    @Downcall(methodName = "jing_std_error_dword",critical = true)
    int stdErrorDword();

    @Downcall(methodName = "jing_get_std_handle", critical = true)
    void getStdHandle(int handle, MemorySegment jingResult);

    @Downcall(methodName = "jing_create_file")
    void createFile(MemorySegment fileName, MemorySegment jingResult);

    @Downcall(methodName = "jing_write_file")
    void writeFile(MemorySegment handle, MemorySegment buf, int len, MemorySegment jingResult);

    @Downcall(methodName = "jing_flush_file")
    void flushFile(MemorySegment handle, MemorySegment jingResult);

    // network related
    @Downcall(methodName = "jing_win_wsa_startup", critical = true)
    int wsaStartup();

    @Downcall(methodName = "jing_win_connect_block_code",critical = true)
    int connectBlockCode();

    @Downcall(methodName = "jing_win_send_block_code",critical = true)
    int sendBlockCode();

    @Downcall(methodName = "jing_win_af_inet_code",critical = true)
    int afInetCode();

    @Downcall(methodName = "jing_win_af_inet6_code",critical = true)
    int afInet6Code();

    @Downcall(methodName = "jing_win_af_unix_code",critical = true)
    int afUnixCode();

    @Downcall(methodName = "jing_win_sol_socket",critical = true)
    int solSocket();

    @Downcall(methodName = "jing_win_sock_stream_code",critical = true)
    int sockStreamCode();

    @Downcall(methodName = "jing_win_sock_dgram_code",critical = true)
    int sockDgramCode();

    @Downcall(methodName = "jing_win_ipproto_tcp_code",critical = true)
    int ipprotoTcpCode();

    @Downcall(methodName = "jing_win_ipproto_udp_code",critical = true)
    int ipprotoUdpCode();

    @Downcall(methodName = "jing_win_ipproto_ipv6_code",critical = true)
    int ipprotoIpv6Code();

    @Downcall(methodName = "jing_win_so_reuseaddr",critical = true)
    int soReuseaddr();

    @Downcall(methodName = "jing_win_tcp_nodelay",critical = true)
    int tcpNodelay();

    @Downcall(methodName = "jing_win_ipv6_v6only",critical = true)
    int ipv6V6only();

    @Downcall(methodName = "jing_win_socket", critical = true)
    int socket(int domain, int type, int protocol);

    @Downcall(methodName = "jing_win_closesocket", critical = true)
    int closeSocket(int socket);

    @Downcall(methodName = "jing_win_set_sock_opt", critical = true)
    int setSockOpt(int socket, int level, int opt, long val, int len);

    @Downcall(methodName = "jing_win_get_err_opt", critical = true)
    int getErrOpt(int socket);

    @Downcall(methodName = "jing_win_set_nonblocking", critical = true)
    int setNonblocking(int socket);

    @Downcall(methodName = "jing_win_bind", critical = true)
    int bind(int socket, int type, long addr, int port);

    @Downcall(methodName = "jing_win_listen", critical = true)
    int listen(int socket, int backlog);

    @Downcall(methodName = "jing_win_accept", critical = true)
    int accept(int socket, long buf);

    @Downcall(methodName = "jing_win_connect", critical = true)
    int connect(int socket, int type, long addr, int port);

    @Downcall(methodName = "jing_win_recv", critical = true)
    int recv(int socket, long buf, int len);

    @Downcall(methodName = "jing_win_send", critical = true)
    int send(int socket, long buf, int len);

    @Downcall(methodName = "jing_win_read_int", critical = true)
    int readInt(int socket);

    @Downcall(methodName = "jing_win_write_int", critical = true)
    int writeInt(int socket, int value);

    @Downcall(methodName = "jing_win_uds_bind")
    int udsBind(int socket, long addr, long len);

    @Downcall(methodName = "jing_win_uds_connect")
    int udsConnect(int socket, long addr, long len);

    @Downcall(methodName = "jing_win_uds_accept")
    int udsAccept(int socket);

    // wepoll related functions
    @Downcall(methodName = "jing_win_wepoll_in",critical = true)
    int wepollIn();

    @Downcall(methodName = "jing_win_wepoll_out",critical = true)
    int wepollOut();

    @Downcall(methodName = "jing_win_wepoll_rdhup",critical = true)
    int wepollRdhup();

    @Downcall(methodName = "jing_win_wepoll_err",critical = true)
    int wepollErr();

    @Downcall(methodName = "jing_win_wepoll_hup",critical = true)
    int wepollHup();

    @Downcall(methodName = "jing_win_wepoll_ctl_add",critical = true)
    int wepollAdd();

    @Downcall(methodName = "jing_win_wepoll_ctl_mod",critical = true)
    int wepollMod();

    @Downcall(methodName = "jing_win_wepoll_ctl_del",critical = true)
    int wepollDel();

    @Downcall(methodName = "jing_win_wepoll_create", critical = true)
    long wepollCreate();

    @Downcall(methodName = "jing_win_wepoll_ctl")
    int wepollCtl(long epfd, int socket, int op, int eventTypes, int data);

    @Downcall(methodName = "jing_win_wepoll_wait")
    int wepollWait(long epfd, long events, int maxEvents, int timeout);

    @Downcall(methodName = "jing_win_wepoll_close", critical = true)
    int wepollClose(long epfd);
}
