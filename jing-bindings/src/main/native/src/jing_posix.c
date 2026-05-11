#if defined(JING_OS_LINUX) || defined(JING_OS_MACOS)
#include "jing_posix.h"
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <errno.h>

#define JING_DEFAULT_PAGE_SIZE 4096
long jing_posix_page_size() {
	long v = sysconf(_SC_PAGESIZE);
	if (JING_UNLIKELY(v == -1)) {
		return JING_DEFAULT_PAGE_SIZE;
	} else {
		return v;
	}
}

int jing_posix_prot_read(void) {
	return PROT_READ;
}

int jing_posix_prot_write(void) {
	return PROT_WRITE;
}

int jing_posix_prot_none(void) {
	return PROT_NONE;
}

int jing_posix_madv_dontneed(void) {
	return MADV_DONTNEED;
}

int jing_posix_map_private(void) {
	return MAP_PRIVATE;
}

int jing_posix_map_anonymous(void) {
	return MAP_ANONYMOUS;
}

void* jing_posix_mmap(void* addr, size_t size, int prot, int flags, int fd,
                      off_t offset) {
	void* ptr = mmap(addr, size, prot, flags, fd, offset);
	if (JING_UNLIKELY(ptr == MAP_FAILED)) {
		int err = errno;
		return jing_make_error_ptr(err);
	} else {
		return ptr;
	}
}

int jing_posix_mprotect(void* addr, size_t size, int prot) {
	int v = mprotect(addr, size, prot);
	if (JING_UNLIKELY(v == -1)) {
		int err = errno;
		return err;
	} else {
		return 0;
	}
}

int jing_posix_madvise(void* addr, size_t size, int advice) {
	int v = madvise(addr, size, advice);
	if (JING_UNLIKELY(v == -1)) {
		int err = errno;
		return err;
	} else {
		return 0;
	}
}

int jing_posix_munmap(void* addr, size_t size) {
	int v = munmap(addr, size);
	if (JING_UNLIKELY(v == -1)) {
		int err = errno;
		return err;
	} else {
		return 0;
	}
}

int jing_posix_close(int fd) {
	int v = close(fd);
	if (JING_UNLIKELY(v == -1)) {
		int err = errno;
		return err;
	}
	return 0;
}

int jing_posix_af_inet_code(void) {
	return AF_INET;
}

int jing_posix_af_inet6_code(void) {
	return AF_INET6;
}

int jing_posix_af_unix_code(void) {
	return AF_UNIX;
}

int jing_posix_tcp_type_code(void) {
	return SOCK_STREAM;
}

int jing_posix_udp_type_code(void) {
	return SOCK_DGRAM;
}

int jing_posix_tcp_protocol_code(void) {
	return IPPROTO_TCP;
}

int jing_posix_udp_protocol_code(void) {
	return IPPROTO_UDP;
}

#endif