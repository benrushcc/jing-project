#include "jing_wepoll.h"

#if defined(JING_OS_WINDOWS)
#include "wepoll.h"
#include <winsock2.h>
#include <windows.h>

int jing_wepoll_in(void) {
	return EPOLLIN;
}

int jing_wepoll_out(void) {
	return EPOLLOUT;
}

int jing_wepoll_err(void) {
	return EPOLLERR;
}

int jing_wepoll_hup(void) {
	return EPOLLHUP;
}

int jing_wepoll_ctl_add(void) {
	return EPOLL_CTL_ADD;
}

int jing_wepoll_ctl_mod(void) {
	return EPOLL_CTL_MOD;
}

int jing_wepoll_ctl_del(void) {
	return EPOLL_CTL_DEL;
}

HANDLE
jing_wepoll_create() {
	HANDLE ptr = epoll_create1(0);
	if (JING_UNLIKELY(ptr == NULL)) {
		int err = GetLastError();
		return jing_make_error_ptr(err);
	} else {
		return ptr;
	}
}

int jing_wepoll_ctl(HANDLE epfd, SOCKET socket, int op, uint32_t event_types,
                    uint32_t data) {
	int v;
	if (op == EPOLL_CTL_DEL) {
		v = epoll_ctl(epfd, op, socket, NULL);
	} else {
		struct epoll_event event;
		memset(&event, 0, sizeof(event));
		event.events   = event_types;
		event.data.u32 = data;
		v              = epoll_ctl(epfd, op, socket, &event);
	}
	if (JING_UNLIKELY(v == -1)) {
		int err = GetLastError();
		return err;
	} else {
		return 0;
	}
}

int jing_wepoll_wait(HANDLE epfd, struct epoll_event* events, int maxevents,
                     int timeout) {
	int v = epoll_wait(epfd, events, maxevents, timeout);
	if (JING_UNLIKELY(v == -1)) {
		int err = GetLastError();
		return -err;
	} else {
		return v;
	}
}

int jing_wepoll_close(HANDLE epfd) {
	int v = epoll_close(epfd);
	if (JING_UNLIKELY(v == -1)) {
		int err = GetLastError();
		return err;
	} else {
		return 0;
	}
}
#endif