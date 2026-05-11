#ifndef JING_LINUX
#define JING_LINUX

#if defined(JING_OS_LINUX)
#include "jing_common.h"
JING_EXPORT_SYMBOL int jing_linux_epoll_in(void);

JING_EXPORT_SYMBOL int jing_linux_epoll_out(void);

JING_EXPORT_SYMBOL int jing_linux_epoll_default(void);

JING_EXPORT_SYMBOL int jing_linux_epoll_ctl_add(void);

JING_EXPORT_SYMBOL int jing_linux_epoll_ctl_mod(void);

JING_EXPORT_SYMBOL int jing_linux_epoll_ctl_del(void);

JING_EXPORT_SYMBOL int jing_linux_epoll_cloexec(void);

JING_EXPORT_SYMBOL void jing_linux_epoll_create(jing_result* r);

JING_EXPORT_SYMBOL void jing_linux_epoll_ctl(int epfd, int socket, int op,
                                             uint32_t events, uint32_t data);

JING_EXPORT_SYMBOL void jing_linux_epoll_wait(int epfd,
                                              struct epoll_event* events,
                                              int maxevents, int timeout);
#endif

#endif