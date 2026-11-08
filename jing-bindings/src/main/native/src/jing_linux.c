#include "jing_common.h"

#if defined(JING_OS_LINUX)
#include "jing_linux.h"
#include <unistd.h>
#include <errno.h>
#include <sys/epoll.h>
#include <sys/eventfd.h>

int jing_linux_epoll_in(void) {
	return EPOLLIN;
}

int jing_linux_epoll_out(void) {
	return EPOLLOUT;
}

int jing_linux_epoll_rdhup(void) {
	return EPOLLRDHUP;
}

int jing_linux_epoll_err(void) {
	return EPOLLERR;
}

int jing_linux_epoll_hup(void) {
	return EPOLLHUP;
}

int jing_linux_epoll_ctl_add(void) {
	return EPOLL_CTL_ADD;
}

int jing_linux_epoll_ctl_mod(void) {
	return EPOLL_CTL_MOD;
}

int jing_linux_epoll_ctl_del(void) {
	return EPOLL_CTL_DEL;
}

int jing_linux_epoll_create(void) {
	int r = epoll_create1(EPOLL_CLOEXEC);
	if (JING_UNLIKELY(r == -1)) {
		int err = errno;
		return -err;
	}
	return r;
}

int jing_linux_epoll_ctl(int epfd, int socket, int op, uint32_t events,
                         uint32_t data) {
	int r;
	if (op == EPOLL_CTL_DEL) {
		r = epoll_ctl(epfd, op, socket, NULL);
	} else {
		struct epoll_event event;
		memset(&event, 0, sizeof(event));
		event.events   = events;
		event.data.u32 = data;
		r              = epoll_ctl(epfd, op, socket, &event);
	}
	if (JING_UNLIKELY(r == -1)) {
		int err = errno;
		return -err;
	}
	return r;
}

int jing_linux_epoll_wait(int epfd, struct epoll_event* events, int maxevents,
                          int timeout) {
	int r = epoll_wait(epfd, events, maxevents, timeout);
	if (JING_UNLIKELY(r == -1)) {
		int err = errno;
		return -err;
	}
	return r;
}

int jing_linux_efd_cloexec(void) {
	return EFD_CLOEXEC;
}

int jing_linux_efd_nonblock(void) {
	return EFD_NONBLOCK;
}

int jing_linux_eventfd(int flags) {
	int r = eventfd(0, flags);
	if (JING_UNLIKELY(r == -1)) {
		int err = errno;
		return -err;
	}
	return r;
}

int jing_linux_read_efd(int efd) {
	uint64_t v;
	ssize_t r = read(efd, &v, sizeof(uint64_t));
	if (JING_UNLIKELY(r == -1)) {
		int err = errno;
		return -err;
	}
	if (JING_UNLIKELY(r != (ssize_t) sizeof(uint64_t))) {
		int err = EIO;
		return -err;
	}
	return (int) v;
}

int jing_linux_write_efd(int efd, int value) {
	uint64_t v = (uint64_t) value;
	ssize_t r  = write(efd, (const char*) &v, sizeof(uint64_t));
	if (JING_UNLIKELY(r == -1)) {
		int err = errno;
		return -err;
	}
	if (JING_UNLIKELY(r != (ssize_t) sizeof(uint64_t))) {
		int err = EIO;
		return -err;
	}
	return 0;
}
#endif