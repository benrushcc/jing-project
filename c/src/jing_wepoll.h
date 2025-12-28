#ifndef JING_WEPOLL_H
#define JING_WEPOLL_H

#include "jing_common.h"

#if defined(JING_OS_WINDOWS)
#include "wepoll.h"
#include <winsock2.h>
#include <windows.h>

JING_EXPORT_SYMBOL int jing_wepoll_in(void);

JING_EXPORT_SYMBOL int jing_wepoll_out(void);

JING_EXPORT_SYMBOL int jing_wepoll_err(void);

JING_EXPORT_SYMBOL int jing_wepoll_hup(void);

JING_EXPORT_SYMBOL int jing_wepoll_ctl_add(void);

JING_EXPORT_SYMBOL int jing_wepoll_ctl_mod(void);

JING_EXPORT_SYMBOL int jing_wepoll_ctl_del(void);

JING_EXPORT_SYMBOL HANDLE jing_wepoll_create();

JING_EXPORT_SYMBOL int jing_wepoll_ctl(HANDLE epfd, SOCKET socket, int op,
                                       uint32_t event_types, uint32_t data);

JING_EXPORT_SYMBOL int jing_wepoll_wait(HANDLE epfd, struct epoll_event* events,
                                        int maxevents, int timeout);

JING_EXPORT_SYMBOL int jing_wepoll_close(HANDLE epfd);
#endif

#endif