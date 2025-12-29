#include "jing_kqueue.h"

#if defined(JING_OS_MACOS)
#include <sys/types.h>
#include <sys/event.h>
#include <sys/time.h>
#include <errno.h>

int jing_kqueue_in(void) {
	return EVFILT_READ;
}

int jing_kqueue_out(void) {
	return EVFILT_WRITE;
}

// int jing_kqueue_add() {
// 	return EV_ADD;
// }

// int jing_kqueue_del() {
// 	return EV_DELETE;
// }

int jing_kqueue(void) {
	int v = kqueue();
	if (JING_UNLIKELY(v == -1)) {
		int err = errno;
		return -err;
	} else {
		return v;
	}
}

int jing_kevent_ctl(int kqfd, int socket, int mod_read, int mod_write,
                    void* udata) {
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
	if (n == 0) {
		return 0;
	}
	if (kevent(kqfd, events, n, NULL, 0, NULL) == -1) {
		int err = errno;
		return err;
	}
	return 0;
}

int jing_kevent_wait(int kqfd, struct kevent* events, int nevents,
                     int timeout) {
	struct timespec* tp = NULL;
	if (timeout >= 0) {
		struct timespec t;
		memset(&t, 0, sizeof(t));
		t.tv_sec  = timeout / 1000;
		t.tv_nsec = (timeout % 1000) * 1000000;
		tp        = &t;
	}
	int v = kevent(kqfd, NULL, 0, events, nevents, tp);
	if (v == -1) {
		int err = errno;
		return -err;
	}
	return v;
}
#endif