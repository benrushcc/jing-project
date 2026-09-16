#include "jing_common.h"

#if defined(JING_OS_LINUX) || defined(JING_OS_MACOS)
#include "jing_posix.h"
#include <stdlib.h>
#include <stdalign.h>
#include <sys/socket.h>
#include <sys/mman.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <errno.h>
#include <unistd.h>
#include <fcntl.h>

size_t jing_posix_max_align(void) {
	return alignof(max_align_t);
}

void* jing_posix_memalign(size_t alignment, size_t size) {
	void* p = NULL;
	int r = posix_memalign(&p, alignment, size);
	if(r != 0) {
		return jing_make_error_ptr(r);
	}
	return p;
}

void jing_posix_batch_free(uintptr_t* ptrs, size_t len, void (*free_func_t)(void*)) {
	size_t count = len / sizeof(uintptr_t);
	for (size_t i = 0; i < count; ++i) {
		uintptr_t addr = (uintptr_t) ptrs[i];
		free_func_t((void*) addr);
	}
	free_func_t(ptrs);
}

// mmap related
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

int jing_posix_madv_free(void) {
	return MADV_FREE;
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
		return -err;
	} else {
		return 0;
	}
}

int jing_posix_madvise(void* addr, size_t size, int advice) {
	int v = madvise(addr, size, advice);
	if (JING_UNLIKELY(v == -1)) {
		int err = errno;
		return -err;
	} else {
		return 0;
	}
}

int jing_posix_munmap(void* addr, size_t size) {
	int v = munmap(addr, size);
	if (JING_UNLIKELY(v == -1)) {
		int err = errno;
		return -err;
	} else {
		return 0;
	}
}

int jing_posix_close(int fd) {
	int v = close(fd);
	if (JING_UNLIKELY(v == -1)) {
		int err = errno;
		return -err;
	}
	return 0;
}

// fs related
int jing_stdout_fileno(void) {
	return STDOUT_FILENO;
}

int jing_stderr_fileno(void) {
	return STDERR_FILENO;
}

void jing_open_fd(char* filename, jing_result* r) {
	int v = open(filename, O_WRONLY | O_CREAT | O_APPEND, 0644);
	if (JING_UNLIKELY(v == -1)) {
		int err = errno;
		jing_err_result(r, err);
	} else {
		jing_int_result(r, v);
	}
}

void jing_write_fd(int fd, char* buf, size_t len, jing_result* r) {
	ssize_t total = 0;
	while (total < len) {
		int v = write(fd, buf + total, len - total);
		if (JING_UNLIKELY(v == -1)) {
			int err = errno;
			jing_err_result(r, err);
			return;
		} else {
			total += v;
		}
	}
	jing_long_result(r, total);
}

void jing_sync_fd(int fd, jing_result* r) {
	int v = fsync(fd);
	if (JING_UNLIKELY(v == -1)) {
		int err = errno;
		jing_err_result(r, err);
	} else {
		jing_int_result(r, 0);
	}
}

// network related
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