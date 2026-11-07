#ifndef JING_WIN_H
#define JING_WIN_H

#include "jing_common.h"
#if defined(JING_OS_WINDOWS)

#include <windows.h>
#include <winsock2.h>
#include <ws2tcpip.h>

static_assert(sizeof(DWORD) == 4, "DWORD size mismatch");
static_assert(sizeof(SOCKET) == 8, "SOCKET size mismatch");

JING_EXPORT_SYMBOL size_t jing_win_max_align(void);

JING_EXPORT_SYMBOL void* jing_win_aligned_alloc(size_t size, size_t alignment);

JING_EXPORT_SYMBOL void jing_win_batch_free(void** ptrs, size_t len,
                                            void (*free_func_t)(void*));

JING_EXPORT_SYMBOL size_t jing_win_page_size(void);

JING_EXPORT_SYMBOL size_t jing_win_allocate_granularity(void);

JING_EXPORT_SYMBOL DWORD jing_win_mem_reserve(void);

JING_EXPORT_SYMBOL DWORD jing_win_mem_commit(void);

JING_EXPORT_SYMBOL DWORD jing_win_mem_decommit(void);

JING_EXPORT_SYMBOL DWORD jing_win_mem_release(void);

JING_EXPORT_SYMBOL DWORD jing_win_page_read_write(void);

JING_EXPORT_SYMBOL void* jing_win_virtual_alloc(void* addr, size_t size,
                                                DWORD type, DWORD prot);

JING_EXPORT_SYMBOL int jing_win_virtual_free(void* addr, size_t size,
                                             DWORD type);

JING_EXPORT_SYMBOL int jing_win_ansi_support(void);

JING_EXPORT_SYMBOL DWORD jing_std_output_dword(void);

JING_EXPORT_SYMBOL DWORD jing_std_error_dword(void);

JING_EXPORT_SYMBOL void jing_get_std_handle(DWORD d, jing_result* r);

JING_EXPORT_SYMBOL void jing_create_file(LPCWSTR filename, jing_result* r);

JING_EXPORT_SYMBOL void jing_write_file(HANDLE h, char* buffer, int len,
                                        jing_result* r);

JING_EXPORT_SYMBOL void jing_flush_file(HANDLE h, jing_result* r);

// log io related
JING_EXPORT_SYMBOL void* jing_log_win_stdout_handle(void);

JING_EXPORT_SYMBOL void* jing_log_win_stderr_handle(void);

JING_EXPORT_SYMBOL void* jing_log_win_open_append(const char* filename);

JING_EXPORT_SYMBOL long long jing_log_win_write(void* handle,
                                                const char* buffer, size_t len);

JING_EXPORT_SYMBOL int jing_log_win_flush(void* handle);

JING_EXPORT_SYMBOL long long jing_log_win_file_size(void* handle);

JING_EXPORT_SYMBOL int jing_log_win_close(void* handle);

// network related
JING_EXPORT_SYMBOL int jing_win_wsa_startup(void);

JING_EXPORT_SYMBOL int jing_win_connect_block_code(void);

JING_EXPORT_SYMBOL int jing_win_send_block_code(void);

JING_EXPORT_SYMBOL int jing_win_af_inet_code(void);

JING_EXPORT_SYMBOL int jing_win_af_inet6_code(void);

JING_EXPORT_SYMBOL int jing_win_af_unix_code(void);

JING_EXPORT_SYMBOL int jing_win_sol_socket(void);

JING_EXPORT_SYMBOL int jing_win_sock_stream_code(void);

JING_EXPORT_SYMBOL int jing_win_sock_dgram_code(void);

JING_EXPORT_SYMBOL int jing_win_ipproto_tcp_code(void);

JING_EXPORT_SYMBOL int jing_win_ipproto_udp_code(void);

JING_EXPORT_SYMBOL int jing_win_ipproto_ipv6_code(void);

JING_EXPORT_SYMBOL int jing_win_so_reuseaddr(void);

JING_EXPORT_SYMBOL int jing_win_tcp_nodelay(void);

JING_EXPORT_SYMBOL int jing_win_ipv6_v6only(void);

JING_EXPORT_SYMBOL int jing_win_socket(int domain, int type, int protocol);

JING_EXPORT_SYMBOL int jing_win_closesocket(int socket);

JING_EXPORT_SYMBOL int jing_win_set_sock_opt(int socket, int level, int opt,
                                             const char* val, int len);

JING_EXPORT_SYMBOL int jing_win_get_err_opt(int socket);

JING_EXPORT_SYMBOL int jing_win_set_nonblocking(int socket);

JING_EXPORT_SYMBOL int jing_win_bind(int socket, int type, const char* addr,
                                     int port);

JING_EXPORT_SYMBOL int jing_win_listen(int socket, int backlog);

JING_EXPORT_SYMBOL int jing_win_accept(int socket, char* buf);

JING_EXPORT_SYMBOL int jing_win_connect(int socket, int type, const char* addr,
                                        int port);

JING_EXPORT_SYMBOL int jing_win_recv(int socket, char* buf, int len);

JING_EXPORT_SYMBOL int jing_win_send(int socket, const char* buf, int len);

JING_EXPORT_SYMBOL int jing_win_recvfrom(int socket, char* data, int len,
                                         char* buf);

JING_EXPORT_SYMBOL int jing_win_sendto(int socket, int type, const char* addr,
                                       int port, const char* buf, int len);

JING_EXPORT_SYMBOL int jing_win_read_int(int socket);

JING_EXPORT_SYMBOL int jing_win_write_int(int socket, int value);

JING_EXPORT_SYMBOL int jing_win_uds_bind(int socket, const char* addr,
                                         size_t len);

JING_EXPORT_SYMBOL int jing_win_uds_connect(int socket, const char* addr,
                                            size_t len);

JING_EXPORT_SYMBOL int jing_win_uds_accept(int socket);

#ifdef JING_USE_WEPOLL
#include "wepoll.h"
JING_EXPORT_SYMBOL int jing_win_wepoll_in(void);

JING_EXPORT_SYMBOL int jing_win_wepoll_out(void);

JING_EXPORT_SYMBOL int jing_win_wepoll_rdhup(void);

JING_EXPORT_SYMBOL int jing_win_wepoll_err(void);

JING_EXPORT_SYMBOL int jing_win_wepoll_hup(void);

JING_EXPORT_SYMBOL int jing_win_wepoll_ctl_add(void);

JING_EXPORT_SYMBOL int jing_win_wepoll_ctl_mod(void);

JING_EXPORT_SYMBOL int jing_win_wepoll_ctl_del(void);

JING_EXPORT_SYMBOL HANDLE jing_win_wepoll_create(void);

JING_EXPORT_SYMBOL int jing_win_wepoll_ctl(HANDLE epfd, int socket, int op,
                                           uint32_t event_types, uint32_t data);

JING_EXPORT_SYMBOL int jing_win_wepoll_wait(HANDLE epfd,
                                            struct epoll_event* events,
                                            int maxevents, int timeout);

JING_EXPORT_SYMBOL int jing_win_wepoll_close(HANDLE epfd);
#endif

#endif

#endif