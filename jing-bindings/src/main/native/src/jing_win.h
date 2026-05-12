#ifndef JING_WIN
#define JING_WIN

#if defined(JING_OS_WINDOWS)
#include "jing_common.h"
#include <windows.h>
#include <winsock2.h>
#include <ws2tcpip.h>

static_assert(sizeof(DWORD) == 4, "DWORD size mismatch");
static_assert(sizeof(SOCKET) == 8, "SOCKET size mismatch");

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

// network related
JING_EXPORT_SYMBOL int jing_win_connect_blocked_errcode(void);

JING_EXPORT_SYMBOL int jing_win_send_blocked_errcode(void);

JING_EXPORT_SYMBOL int jing_win_interrupt_errcode(void);

JING_EXPORT_SYMBOL int jing_win_af_inet_code(void);

JING_EXPORT_SYMBOL int jing_win_af_inet6_code(void);

JING_EXPORT_SYMBOL int jing_win_af_unix_code(void);

JING_EXPORT_SYMBOL int jing_win_tcp_type_code(void);

JING_EXPORT_SYMBOL int jing_win_udp_type_code(void);

JING_EXPORT_SYMBOL int jing_win_tcp_protocol_code(void);

JING_EXPORT_SYMBOL int jing_win_udp_protocol_code(void);

JING_EXPORT_SYMBOL SOCKET jing_win_socket(int af, int type, int protocol);
#ifdef JING_USE_WEPOLL
#include "wepoll.h"
JING_EXPORT_SYMBOL int jing_win_wepoll_in(void);

JING_EXPORT_SYMBOL int jing_win_wepoll_out(void);

JING_EXPORT_SYMBOL int jing_win_wepoll_err(void);

JING_EXPORT_SYMBOL int jing_win_wepoll_hup(void);

JING_EXPORT_SYMBOL int jing_win_wepoll_ctl_add(void);

JING_EXPORT_SYMBOL int jing_win_wepoll_ctl_mod(void);

JING_EXPORT_SYMBOL int jing_win_wepoll_ctl_del(void);

JING_EXPORT_SYMBOL HANDLE jing_win_wepoll_create();

JING_EXPORT_SYMBOL int jing_win_wepoll_ctl(HANDLE epfd, SOCKET socket, int op,
                                           uint32_t event_types, uint32_t data);

JING_EXPORT_SYMBOL int jing_win_wepoll_wait(HANDLE epfd,
                                            struct epoll_event* events,
                                            int maxevents, int timeout);

JING_EXPORT_SYMBOL int jing_win_wepoll_close(HANDLE epfd);
#endif
#endif

#endif