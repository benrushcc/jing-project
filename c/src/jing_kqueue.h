#ifndef JING_KQUEUE_H
#define JING_KQUEUE_H

#include "jing_common.h"
#include <sys/event.h>

#if defined(JING_OS_MACOS)
JING_EXPORT_SYMBOL int jing_kqueue_in(void);

JING_EXPORT_SYMBOL int jing_kqueue_out(void);

// int jing_kqueue_add();

// int jing_kqueue_del();

JING_EXPORT_SYMBOL int jing_kqueue(void);

JING_EXPORT_SYMBOL int jing_kevent_ctl(int kqfd, int socket, int mod_read,
                                       int mod_write, void* udata);

JING_EXPORT_SYMBOL int jing_kevent_wait(int kqfd, struct kevent* events,
                                        int nevents, int timeout);
#endif

#endif