#ifndef JING_POSIX_H
#define JING_POSIX_H

#include "jing_common.h"
#if defined(JING_OS_LINUX) || defined(JING_OS_MACOS)
#include <stdint.h>
#include <sys/types.h>
static_assert(sizeof(off_t) == 8, "off_t size mismatch");

JING_EXPORT_SYMBOL size_t jing_posix_max_align(void);

JING_EXPORT_SYMBOL void* jing_posix_memalign(size_t alignment, size_t size);

JING_EXPORT_SYMBOL void jing_posix_batch_free(void** ptrs, size_t len,
                                              void (*free_func_t)(void*));

// mmap related
JING_EXPORT_SYMBOL long jing_posix_page_size(void);

JING_EXPORT_SYMBOL int jing_posix_prot_read(void);

JING_EXPORT_SYMBOL int jing_posix_prot_write(void);

JING_EXPORT_SYMBOL int jing_posix_madv_free(void);

JING_EXPORT_SYMBOL int jing_posix_map_private(void);

JING_EXPORT_SYMBOL int jing_posix_map_anonymous(void);

JING_EXPORT_SYMBOL void* jing_posix_mmap(void* addr, size_t size, int prot,
                                         int flags, int fd, off_t offset);

JING_EXPORT_SYMBOL int jing_posix_mprotect(void* addr, size_t size, int prot);

JING_EXPORT_SYMBOL int jing_posix_madvise(void* addr, size_t size, int advice);

JING_EXPORT_SYMBOL int jing_posix_munmap(void* addr, size_t size);

JING_EXPORT_SYMBOL int jing_posix_close(int fd);

// fs related
JING_EXPORT_SYMBOL int jing_stdout_fileno(void);

JING_EXPORT_SYMBOL int jing_stderr_fileno(void);

JING_EXPORT_SYMBOL void jing_open_fd(char* filename, jing_result* r);

JING_EXPORT_SYMBOL void jing_write_fd(int fd, char* buf, size_t len,
                                      jing_result* r);

JING_EXPORT_SYMBOL void jing_sync_fd(int fd, jing_result* r);

// log io related
JING_EXPORT_SYMBOL int jing_log_posix_open_append(const char* filename);

JING_EXPORT_SYMBOL long long jing_log_posix_write(int fd, const char* buffer,
                                                  size_t len);

JING_EXPORT_SYMBOL int jing_log_posix_flush(int fd);

JING_EXPORT_SYMBOL long long jing_log_posix_file_size(int fd);

JING_EXPORT_SYMBOL int jing_log_posix_close(int fd);

// network related
JING_EXPORT_SYMBOL int jing_posix_connect_block_code(void);

JING_EXPORT_SYMBOL int jing_posix_send_block_code(void);

JING_EXPORT_SYMBOL int jing_posix_af_inet_code(void);

JING_EXPORT_SYMBOL int jing_posix_af_inet6_code(void);

JING_EXPORT_SYMBOL int jing_posix_af_unix_code(void);

JING_EXPORT_SYMBOL int jing_posix_sol_socket(void);

JING_EXPORT_SYMBOL int jing_posix_tcp_type_code(void);

JING_EXPORT_SYMBOL int jing_posix_udp_type_code(void);

JING_EXPORT_SYMBOL int jing_posix_tcp_protocol_code(void);

JING_EXPORT_SYMBOL int jing_posix_udp_protocol_code(void);

JING_EXPORT_SYMBOL int jing_posix_ipproto_ipv6_code(void);

JING_EXPORT_SYMBOL int jing_posix_so_reuseaddr(void);

JING_EXPORT_SYMBOL int jing_posix_tcp_nodelay(void);

JING_EXPORT_SYMBOL int jing_posix_ipv6_v6only(void);

JING_EXPORT_SYMBOL int jing_posix_socket(int domain, int type, int protocol);

JING_EXPORT_SYMBOL int jing_posix_set_sock_opt(int socket, int level, int opt,
                                               const void* val, int len);

JING_EXPORT_SYMBOL int jing_posix_get_err_opt(int socket);

JING_EXPORT_SYMBOL int jing_posix_set_nonblocking(int socket);

JING_EXPORT_SYMBOL int jing_posix_bind(int socket, int type, const char* addr,
                                       int port);

JING_EXPORT_SYMBOL int jing_posix_listen(int socket, int backlog);

JING_EXPORT_SYMBOL int jing_posix_accept(int socket, char* buf);

JING_EXPORT_SYMBOL int jing_posix_connect(int socket, int type,
                                          const char* addr, int port);

JING_EXPORT_SYMBOL int jing_posix_recv(int sockfd, char* buf, int len);

JING_EXPORT_SYMBOL int jing_posix_send(int sockfd, const char* buf, int len);

JING_EXPORT_SYMBOL int jing_posix_recvfrom(int socket, char* data, int len,
                                           char* buf);

JING_EXPORT_SYMBOL int jing_posix_sendto(int socket, int type, const char* addr,
                                         int port, const char* buf, int len);

JING_EXPORT_SYMBOL int jing_posix_read(int fd, char* buf, int len);

JING_EXPORT_SYMBOL int jing_posix_write(int fd, const char* buf, int len);

JING_EXPORT_SYMBOL uint64_t jing_posix_socketpair(int domain, int type);

JING_EXPORT_SYMBOL int jing_posix_read_int(int socket);

JING_EXPORT_SYMBOL int jing_posix_write_int(int socket, int value);

JING_EXPORT_SYMBOL int jing_posix_uds_bind(int socket, const char* addr,
                                           size_t len);

JING_EXPORT_SYMBOL int jing_posix_uds_connect(int socket, const char* addr,
                                              size_t len);

JING_EXPORT_SYMBOL int jing_posix_uds_accept(int socket);

#endif

#endif