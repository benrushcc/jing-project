package io.jingproject.bindings;

import io.jingproject.common.Os;
import io.jingproject.ffm.Downcall;
import io.jingproject.ffm.FFM;

@FFM(libraryName = "jing_bindings", supportedOS = {Os.LINUX, Os.MACOS})
public interface PosixBindings {
    @Downcall(methodName = "jing_posix_max_align", critical = true)
    long posixMaxAlign();

    @Downcall(methodName = "jing_posix_memalign", critical = true)
    long posixMemAlign(long alignment, long size);

    @Downcall(methodName = "jing_posix_batch_free")
    void posixBatchFree(long ptrs, long len, long freeAddr);

    @Downcall(methodName = "jing_posix_page_size", constant = true, critical = true)
    long posixPageSize();

    @Downcall(methodName = "jing_posix_prot_read", constant = true, critical = true)
    int posixProtRead();

    @Downcall(methodName = "jing_posix_prot_write", constant = true, critical = true)
    int posixProtWrite();

    @Downcall(methodName = "jing_posix_madv_free", constant = true, critical = true)
    int posixMadvFree();

    @Downcall(methodName = "jing_posix_map_private", constant = true, critical = true)
    int posixMapPrivate();

    @Downcall(methodName = "jing_posix_map_anonymous", constant = true, critical = true)
    int posixMapAnonymous();

    @Downcall(methodName = "jing_posix_mmap", critical = true)
    long posixMmap(long addr, long size, int prot, int flags, int fd, long offset);

    @Downcall(methodName = "jing_posix_mprotect", critical = true)
    int posixMprotect(long addr, long size, int prot);

    @Downcall(methodName = "jing_posix_madvise", critical = true)
    int posixMadvise(long addr, long size, int advice);

    @Downcall(methodName = "jing_posix_munmap", critical = true)
    int posixMunmap(long addr, long size);

    @Downcall(methodName = "jing_posix_close", critical = true)
    int posixClose(int fd);

    @Downcall(methodName = "jing_posix_connect_block_code", constant = true, critical = true)
    int connectBlockCode();

    @Downcall(methodName = "jing_posix_send_block_code", constant = true, critical = true)
    int sendBlockCode();

    @Downcall(methodName = "jing_posix_af_inet_code", constant = true, critical = true)
    int afInetCode();

    @Downcall(methodName = "jing_posix_af_inet6_code", constant = true, critical = true)
    int afInet6Code();

    @Downcall(methodName = "jing_posix_af_unix_code", constant = true, critical = true)
    int afUnixCode();

    @Downcall(methodName = "jing_posix_sol_socket", constant = true, critical = true)
    int solSocket();

    @Downcall(methodName = "jing_posix_tcp_type_code", constant = true, critical = true)
    int tcpTypeCode();

    @Downcall(methodName = "jing_posix_udp_type_code", constant = true, critical = true)
    int udpTypeCode();

    @Downcall(methodName = "jing_posix_tcp_protocol_code", constant = true, critical = true)
    int tcpProtocolCode();

    @Downcall(methodName = "jing_posix_udp_protocol_code", constant = true, critical = true)
    int udpProtocolCode();

    @Downcall(methodName = "jing_posix_ipproto_ipv6_code", constant = true, critical = true)
    int ipprotoIpv6Code();

    @Downcall(methodName = "jing_posix_so_reuseaddr", constant = true, critical = true)
    int soReuseaddr();

    @Downcall(methodName = "jing_posix_tcp_nodelay", constant = true, critical = true)
    int tcpNodelay();

    @Downcall(methodName = "jing_posix_ipv6_v6only", constant = true, critical = true)
    int ipv6V6only();

    @Downcall(methodName = "jing_posix_socket", critical = true)
    int socket(int domain, int type, int protocol);

    @Downcall(methodName = "jing_posix_set_sock_opt", critical = true)
    int setSockOpt(int socket, int level, int opt, long val, int len);

    @Downcall(methodName = "jing_posix_get_err_opt", critical = true)
    int getErrOpt(int socket);

    @Downcall(methodName = "jing_posix_set_nonblocking", critical = true)
    int setNonblocking(int socket);

    @Downcall(methodName = "jing_posix_bind", critical = true)
    int bind(int socket, int type, long addr, int port);

    @Downcall(methodName = "jing_posix_listen", critical = true)
    int listen(int socket, int backlog);

    @Downcall(methodName = "jing_posix_connect", critical = true)
    int connect(int socket, int type, long addr, int port);

    @Downcall(methodName = "jing_posix_accept", critical = true)
    int accept(int socket, long buf);

    @Downcall(methodName = "jing_posix_send", critical = true)
    int send(int socket, long buf, int len);

    @Downcall(methodName = "jing_posix_recv", critical = true)
    int recv(int socket, long buf, int len);

    @Downcall(methodName = "jing_posix_sendto", critical = true)
    int sendto(int socket, int type, long addr, int port, long buf, int len);

    @Downcall(methodName = "jing_posix_recvfrom", critical = true)
    int recvfrom(int socket, long data, int len, long buf);

    @Downcall(methodName = "jing_posix_read", critical = true)
    int read(int fd, long buf, int len);

    @Downcall(methodName = "jing_posix_write", critical = true)
    int write(int fd, long buf, int len);

    @Downcall(methodName = "jing_posix_socketpair", critical = true)
    long socketpair(int domain, int type);

    @Downcall(methodName = "jing_posix_read_int", critical = true)
    int readInt(int socket);

    @Downcall(methodName = "jing_posix_write_int", critical = true)
    int writeInt(int socket, int value);

    @Downcall(methodName = "jing_posix_uds_bind")
    int udsBind(int socket, long addr, long len);

    @Downcall(methodName = "jing_posix_uds_connect")
    int udsConnect(int socket, long addr, long len);

    @Downcall(methodName = "jing_posix_uds_accept")
    int udsAccept(int socket);
}
