#include "jing_win_net.h"

#if defined(JING_OS_WINDOWS)
#include <winsock2.h>
#include <windows.h>

SOCKET jing_win_socket(int af, int type, int protocol) {
	SOCKET v = socket(af, type, protocol);
	if (JING_UNLIKELY(v == INVALID_SOCKET)) {
		int err = WSAGetLastError();
		return (SOCKET) jing_make_error_ptr(err);
	} else {
		return v;
	}
}

#endif
