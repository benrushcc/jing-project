#include "jing_epoll.h"

#if defined(JING_OS_LINUX)
#include <errno.h>
#include <sys/epoll.h>

int jing_epoll_in(void) {
	return EPOLLIN;
}

int jing_epoll_out(void) {
	return EPOLLOUT;
}

int jing_epoll_default(void) {
	return EPOLLERR | EPOLLHUP;
}

int jing_epoll_ctl_add(void) {
	return EPOLL_CTL_ADD;
}

int jing_epoll_ctl_mod(void) {
	return EPOLL_CTL_MOD;
}

int jing_epoll_ctl_del(void) {
	return EPOLL_CTL_DEL;
}

int jing_epoll_cloexec(void) {
	return EPOLL_CLOEXEC;
}

int jing_epoll_create(jing_result* r) {
	int epfd = epoll_create1(0);
	if (JING_UNLIKELY(epfd == -1)) {
		int err = errno;
		return -err;
	} else {
		return epfd;
	}
}

void jing_epoll_ctl(int epfd, int socket, int op, uint32_t events,
                    uint32_t data, jing_result* r) {
	struct epoll_event event;
	memset(&event, 0, sizeof(event));
	event.events   = events;
	event.data.u32 = data;
	int v          = epoll_ctl(epfd, op, socket, &event);
	if (JING_UNLIKELY(v == -1)) {
		int err = errno;
		jing_err_result(r, err);
	} else {
		jing_int_result(r, 0);
	}
}

void jing_epoll_wait(int epfd, struct epoll_event* events, int maxevents,
                     int timeout, jing_result* r) {
	int v = epoll_wait(epfd, events, maxevents, timeout);
	if (JING_UNLIKELY(v == -1)) {
		int err = errno;
		jing_err_result(r, err);
	} else {
		jing_int_result(r, 0);
	}
}
#endif