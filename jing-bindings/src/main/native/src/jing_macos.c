#include "jing_common.h"

#if defined(JING_OS_MACOS)
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
	if(JING_UNLIKELY(data < 0)) {
		return -ERANGE;
	}
	void* udata = (void *)(uintptr_t) data;
	struct kevent events[2];
	memset(events, 0, sizeof(events));
	int n = 0;
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
		return 0; // no modification needed
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
	struct timespec* tp = NULL;
	if (timeout >= 0) {
		struct timespec t;
		memset(&t, 0, sizeof(t));
		t.tv_sec  = timeout / 1000;
		t.tv_nsec = (timeout % 1000) * 1000000;
		tp        = &t;
	}
	int r = kevent(kqfd, NULL, 0, events, nevents, tp);
	if (JING_UNLIKELY(r == -1)) {
		int err = errno;
		return -err;
	}
	return r;
}
#endif