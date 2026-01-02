#ifndef JING_POSIX_NET_H
#define JING_POSIX_NET_H

#include "jing_common.h"

#if defined(JING_OS_LINUX) || defined(JING_OS_MACOS)
#include <sys/socket.h>

JING_EXPORT_SYMBOL int jing_posix_bind(int sockfd, const struct sockaddr* addr,
                                       socklen_t addrlen);

#endif

#endif