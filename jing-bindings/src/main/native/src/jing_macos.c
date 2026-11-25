#include "jing_common.h"

#if defined(JING_OS_MACOS) && JING_OS_MACOS
#include "jing_macos.h"
#include <sys/types.h>
#include <sys/event.h>
#include <sys/time.h>
#include <errno.h>

int16_t jing_macos_evfilt_read(void) {
	return EVFILT_READ;
}

int16_t jing_macos_evfilt_write(void) {
	return EVFILT_WRITE;
}

int jing_macos_kqueue(void) {
	int r = kqueue();
	if (JING_UNLIKELY(r == -1)) {
		int err = errno;
		return -err;
	}
	return r;
}

int jing_macos_kevent_ctl(int kqfd, int socket, int mod_read, int mod_write,
                          int data) {
	void* udata             = (void*) (uintptr_t) data;
	struct kevent events[2] = {0};
	int n                   = 0;
	if (mod_read != 0) {
		EV_SET(&events[n], socket, EVFILT_READ,
		       (mod_read > 0 ? EV_ADD : EV_DELETE), 0, 0, udata);
		n++;
	}
	if (mod_write != 0) {
		EV_SET(&events[n], socket, EVFILT_WRITE,
		       (mod_write > 0 ? EV_ADD : EV_DELETE), 0, 0, udata);
		n++;
	}
	if (JING_UNLIKELY(n == 0)) {
		return 0;   // no modification needed
	}
	int r = kevent(kqfd, events, n, NULL, 0, NULL);
	if (JING_UNLIKELY(r == -1)) {
		int err = errno;
		return -err;
	}
	return r;
}

int jing_macos_kevent_wait(int kqfd, struct kevent* events, int nevents,
                           int timeout) {
	int r;
	if (JING_UNLIKELY(timeout == -1)) {
		r = kevent(kqfd, NULL, 0, events, nevents, NULL);
	} else {
		struct timespec t = {.tv_sec  = timeout / 1000,
		                     .tv_nsec = (timeout % 1000) * 1000000};
		r                 = kevent(kqfd, NULL, 0, events, nevents, &t);
	}
	if (JING_UNLIKELY(r == -1)) {
		int err = errno;
		return -err;
	}
	return r;
}
#endif