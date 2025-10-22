#ifndef JING_IO_H
#define JING_IO_H

#include "common.h"

#ifdef JING_OS_WINDOWS
#include "wepoll.h"

JING_EXPORT_SYMBOL int jing_connect_blocked_errcode(void);

JING_EXPORT_SYMBOL int jing_send_blocked_errcode(void);

JING_EXPORT_SYMBOL int jing_interrupt_errcode(void);

JING_EXPORT_SYMBOL int jing_af_inet_code(void);

JING_EXPORT_SYMBOL int jing_af_inet6_code(void);

JING_EXPORT_SYMBOL int jing_af_unix_code(void);

JING_EXPORT_SYMBOL int jing_tcp_type_code(void);

JING_EXPORT_SYMBOL int jing_udp_type_code(void);

JING_EXPORT_SYMBOL int jing_tcp_protocol_code(void);

JING_EXPORT_SYMBOL int jing_udp_protocol_code(void);

JING_EXPORT_SYMBOL int jing_wepoll_in(void);

JING_EXPORT_SYMBOL int jing_wepoll_out(void);

JING_EXPORT_SYMBOL int jing_wepoll_default(void);

JING_EXPORT_SYMBOL void jing_wepoll_create(jing_result *r);

JING_EXPORT_SYMBOL void jing_wepoll_ctl(HANDLE epfd, SOCKET socket, int from,
                                        int to, int data, jing_result *r);

JING_EXPORT_SYMBOL void jing_wepoll_wait(HANDLE epfd,
                                         struct epoll_event *events,
                                         int maxevents, int timeout,
                                         jing_result *r);

JING_EXPORT_SYMBOL void jing_wepoll_close(HANDLE epfd, jing_result *r);

JING_EXPORT_SYMBOL void jing_socket(int af, int type, int protocol,
                                    jing_result *r);

#endif

#endif