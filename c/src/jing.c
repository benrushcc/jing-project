#include "jing.h"
#include <errno.h>

static const int MAJOR_VERSION     = 0;
static const int MINOR_VERSION     = 0;
static const int PATCH_VERSION     = 1;
static const char VERSION_STRING[] = "0.0.1";

const char* jing_version(void) {
	return VERSION_STRING;
}

int jing_major_version(void) {
	return MAJOR_VERSION;
}

int jing_minor_version(void) {
	return MINOR_VERSION;
}

int jing_patch_version(void) {
	return PATCH_VERSION;
}

#if defined(JING_OS_WINDOWS)
#include "wepoll.h"
#include <ws2tcpip.h>
#include <winsock2.h>
#include <windows.h>
#else
#include <sys/mman.h>
#include <sys/socket.h>
#include <unistd.h>
#endif

// memory allocation methods

size_t jing_allocate_granularity(void) {
#ifdef JING_OS_WINDOWS
	SYSTEM_INFO sys_info;
	GetSystemInfo(&sys_info);
	return sys_info.dwAllocationGranularity;
#else
	return sysconf(_SC_PAGESIZE);
#endif
}

void jing_mmap(size_t size, jing_result* r) {
#if defined(JING_OS_WINDOWS)
	void* ptr =
	    VirtualAlloc(NULL, size, MEM_RESERVE | MEM_COMMIT, PAGE_READWRITE);
	if (JING_UNLIKELY(ptr == NULL)) {
		int err = GetLastError();
		jing_err_result(r, err);
	} else {
		jing_ptr_result(r, ptr, size);
	}
#else
	void* ptr =
	    mmap(NULL, size, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANON, -1, 0);
	if (JING_UNLIKELY(ptr == MAP_FAILED)) {
		int err = errno;
		jing_err_result(r, err);
	} else {
		jing_ptr_result(r, ptr, size);
	}
#endif
}

void jing_munmap(void* addr, size_t size, jing_result* r) {
#if defined(JING_OS_WINDOWS)
	JING_UNUSED(size);
	int v = VirtualFree(addr, 0, MEM_RELEASE);
	if (JING_UNLIKELY(v == 0)) {
		jing_err_result(r, GetLastError());
	} else {
		jing_int_result(r, 0);
	}
#else
	int v = munmap(addr, size);
	if (JING_UNLIKELY(v == -1)) {
		int err = errno;
		jing_err_result(r, err);
	} else {
		jing_int_result(r, 0);
	}
#endif
}

// windows specific log writing API

#if defined(JING_OS_WINDOWS)
int jing_win_ansi_support(void) {
	HANDLE stdoutHandle = GetStdHandle(STD_OUTPUT_HANDLE);
	DWORD mode;
	if (JING_UNLIKELY(stdoutHandle == INVALID_HANDLE_VALUE)) {
		return -1;
	}
	if (JING_UNLIKELY(GetConsoleMode(stdoutHandle, &mode) == 0)) {
		return -1;
	}
	if ((mode & ENABLE_PROCESSED_OUTPUT) &&
	    (mode & ENABLE_VIRTUAL_TERMINAL_PROCESSING)) {
		return 0;
	}
	mode |= (ENABLE_PROCESSED_OUTPUT | ENABLE_VIRTUAL_TERMINAL_PROCESSING);
	if (JING_UNLIKELY(SetConsoleMode(stdoutHandle, mode) == 0)) {
		return -1;
	}
	return 0;
}

DWORD jing_std_output_dword(void) {
	return STD_OUTPUT_HANDLE;
}

DWORD jing_std_error_dword(void) {
	return STD_ERROR_HANDLE;
}

void jing_get_std_handle(DWORD d, jing_result* r) {
	HANDLE v = GetStdHandle(d);
	if (JING_UNLIKELY(v == INVALID_HANDLE_VALUE)) {
		int err = GetLastError();
		jing_err_result(r, err);
	} else {
		jing_ptr_result(r, v, SIZE_MAX);
	}
}

void jing_create_file(LPCWSTR filename, jing_result* r) {
	HANDLE v = CreateFileW(filename, GENERIC_WRITE, FILE_SHARE_READ, NULL,
	                       OPEN_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
	if (JING_UNLIKELY(v == INVALID_HANDLE_VALUE)) {
		int err = GetLastError();
		jing_err_result(r, err);
	} else {
		jing_ptr_result(r, v, SIZE_MAX);
	}
}

void jing_write_file(HANDLE h, char* buffer, int len, jing_result* r) {
	DWORD written = 0, total = 0, length = (DWORD) len;
	while (total < length) {
		if (JING_LIKELY(
		        WriteFile(h, buffer + total, length - total, &written, NULL))) {
			total += written;
		} else {
			int err = GetLastError();
			jing_err_result(r, err);
			return;
		}
	}
	jing_int_result(r, total);
}

void jing_flush_file(HANDLE h, jing_result* r) {
	if (JING_LIKELY(FlushFileBuffers(h))) {
		jing_int_result(r, 0);
	} else {
		int err = GetLastError();
		jing_err_result(r, err);
	}
}

#endif

// linux and macOS specific log writing API

#if defined(JING_OS_LINUX) || defined(JING_OS_MACOS)
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
	ssize_t written = 0, total = 0;
	while (total < len) {
		int v = write(fd, buf + total, len - total);
		if (JING_UNLIKELY(v == -1)) {
			int err = errno;
			jing_err_result(r, err);
			return;
		} else {
			total += written;
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

#endif

int jing_connect_blocked_errcode(void) {
	return WSAEWOULDBLOCK;
}

int jing_send_blocked_errcode(void) {
	return WSAEWOULDBLOCK;
}

int jing_interrupt_errcode(void) {
	return WSAEINTR;
}

int jing_af_inet_code(void) {
	return AF_INET;
}

int jing_af_inet6_code(void) {
	return AF_INET6;
}

int jing_af_unix_code(void) {
	return AF_UNIX;
}

int jing_tcp_type_code(void) {
	return SOCK_STREAM;
}

int jing_udp_type_code(void) {
	return SOCK_DGRAM;
}

int jing_tcp_protocol_code(void) {
	return IPPROTO_TCP;
}

int jing_udp_protocol_code(void) {
	return IPPROTO_UDP;
}

#if defined(JING_OS_WINDOWS)
int jing_wepoll_in(void) {
	return EPOLLIN;
}

int jing_wepoll_out(void) {
	return EPOLLOUT;
}

int jing_wepoll_default(void) {
	return EPOLLERR | EPOLLHUP;
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

void jing_wepoll_create(jing_result* r) {
	HANDLE ptr = epoll_create1(0);
	if (JING_UNLIKELY(ptr == NULL)) {
		int err = GetLastError();
		jing_err_result(r, err);
	} else {
		jing_ptr_result(r, ptr, SIZE_MAX);
	}
}

void jing_wepoll_ctl(HANDLE epfd, SOCKET socket, int op, uint32_t events,
                     int data, jing_result* r) {
	struct epoll_event event = {.events = events, .data.fd = data};
	int v                    = epoll_ctl(epfd, op, socket, &event);
	if (JING_UNLIKELY(v == -1)) {
		int err = GetLastError();
		jing_err_result(r, err);
	} else {
		jing_int_result(r, 0);
	}
}

void jing_wepoll_wait(HANDLE epfd, struct epoll_event* events, int maxevents,
                      int timeout, jing_result* r) {
	int v = epoll_wait(epfd, events, maxevents, timeout);
	if (JING_UNLIKELY(v == -1)) {
		int err = GetLastError();
		jing_err_result(r, err);
	} else {
		jing_int_result(r, 0);
	}
}

void jing_wepoll_close(HANDLE epfd, jing_result* r) {
	int v = epoll_close(epfd);
	if (JING_UNLIKELY(v == -1)) {
		int err = GetLastError();
		jing_err_result(r, err);
	} else {
		jing_int_result(r, 0);
	}
}
#endif

void jing_socket(int af, int type, int protocol, jing_result* r) {
	SOCKET v = socket(af, type, protocol);
	if (JING_UNLIKELY(v == INVALID_SOCKET)) {
		int err = WSAGetLastError();
		jing_err_result(r, err);
	} else {
		jing_long_result(r, v);
	}
}

// boringssl related methods
#if defined(JING_ENABLE_BORINGSSL)
#include <openssl/crypto.h>
#include <openssl/err.h>
#include <openssl/ssl.h>

const char* jing_ssl_version(void) {
	return OpenSSL_version(OPENSSL_VERSION);
}

const char* jing_ssl_cflags(void) {
	return OpenSSL_version(OPENSSL_CFLAGS);
}

const char* jing_ssl_built_on(void) {
	return OpenSSL_version(OPENSSL_BUILT_ON);
}

uint16_t jing_tls1_2_version(void) {
	return TLS1_2_VERSION;
}

uint16_t jing_tls1_3_version(void) {
	return TLS1_3_VERSION;
}

const SSL_METHOD* jing_tls_method(void) {
	return TLS_method();
}

const SSL_METHOD* jing_dtls_method(void) {
	return DTLS_method();
}

SSL_CTX* jing_ssl_ctx_new(const SSL_METHOD* method) {
	return SSL_CTX_new(method);
}

void jing_ssl_ctx_free(SSL_CTX* ctx) {
	SSL_CTX_free(ctx);
}

int jing_ssl_ctx_set_min_proto_version(SSL_CTX* ctx, uint16_t version) {
	return SSL_CTX_set_min_proto_version(ctx, version);
}

int jing_ssl_ctx_set_max_proto_version(SSL_CTX* ctx, uint16_t version) {
	return SSL_CTX_set_max_proto_version(ctx, version);
}

SSL* jing_ssl_new(SSL_CTX* ctx) {
	return SSL_new(ctx);
}

#endif
