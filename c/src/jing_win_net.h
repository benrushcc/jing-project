#ifndef JING_WIN_NET_H
#define JING_WIN_NET_H

#include "jing_common.h"

#if defined(JING_OS_WINDOWS)
#include <winsock2.h>
#include <windows.h>

static_assert(sizeof(SOCKET) == 8, "SOCKET size mismatch");

JING_EXPORT_SYMBOL SOCKET jing_win_socket(int af, int type, int protocol);

#endif

#endif