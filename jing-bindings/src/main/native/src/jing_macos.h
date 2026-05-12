#ifndef JING_MACOS_H
#define JING_MACOS_H

#include "jing_common.h"
#if defined(JING_OS_MACOS)
#include <sys/event.h>
JING_EXPORT_SYMBOL int jing_macos_kqueue_in(void);

JING_EXPORT_SYMBOL int jing_macos_kqueue_out(void);

JING_EXPORT_SYMBOL int jing_macos_kqueue(void);

JING_EXPORT_SYMBOL int jing_macos_kevent_ctl(int kqfd, int socket, int mod_read,
                                             int mod_write, void* udata);

JING_EXPORT_SYMBOL int jing_macos_kevent_wait(int kqfd, struct kevent* events,
                                              int nevents, int timeout);
#endif

#endif