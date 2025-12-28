#ifndef JING_H
#define JING_H

#include <errno.h>
#include <stddef.h>
#include <stdint.h>
#include <string.h>

#if !defined(__STDC_VERSION__) || __STDC_VERSION__ < 201112L
#error "Requires C11 or later"
#endif

#if defined(_WIN32) || defined(_WIN64)
#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#endif

#if defined(_WIN32) || defined(_WIN64)
#define JING_OS_WINDOWS 1
#elif defined(__APPLE__) && defined(__MACH__)
#define JING_OS_MACOS 1
#elif defined(__linux__)
#define JING_OS_LINUX 1
#else
#error "Unknown compiler/platform"
#endif

static_assert(sizeof(int) == 4, "integer size mismatch");
static_assert(sizeof(size_t) == 8, "size_t size mismatch");
static_assert(sizeof(void*) == 8, "pointer size mismatch");

#define JING_UNUSED(x) (void) (x)

#if defined(__GNUC__) || defined(__clang__)
#define JING_LIKELY(x) __builtin_expect(!!(x), 1)
#define JING_UNLIKELY(x) __builtin_expect(!!(x), 0)
#else
#define JING_LIKELY(x) (x)
#define JING_UNLIKELY(x) (x)
#endif

#if defined(_WIN32) || defined(_WIN64)
#define JING_EXPORT_SYMBOL __declspec(dllexport)
#define JING_HIDDEN_SYMBOL
#else
#define JING_EXPORT_SYMBOL __attribute__((visibility("default")))
#define JING_HIDDEN_SYMBOL __attribute__((visibility("hidden")))
#endif

typedef union {
	int8_t byte_val;
	int16_t short_val;
	uint16_t char_val;
	int32_t int_val;
	int64_t long_val;
	float float_val;
	double double_val;
	void* ptr_val;
	struct {
		int32_t err_code;
		int32_t err_flag;
	} err_val;
} jing_data;

typedef struct {
	size_t len;
	jing_data data;
} jing_result;

static_assert(sizeof(jing_result) == 16, "jing_result size mismatch");

#define JING_SYSTEM_ERROR_FLAG 0
#define JING_SSL_ERROR_FLAG 1

static inline void jing_err_result(jing_result* r, int err) {
	memset(r, 0, sizeof(jing_result));
	r->len                   = 0;
	r->data.err_val.err_code = err;
	r->data.err_val.err_flag = JING_SYSTEM_ERROR_FLAG;
}

static inline void jing_err_result_with_flag(jing_result* r, int err,
                                             int flag) {
	memset(r, 0, sizeof(jing_result));
	r->len                   = 0;
	r->data.err_val.err_code = err;
	r->data.err_val.err_flag = flag;
}

static inline void jing_byte_result(jing_result* r, int8_t value) {
	memset(r, 0, sizeof(jing_result));
	r->len           = SIZE_MAX;
	r->data.byte_val = value;
}

static inline void jing_short_result(jing_result* r, int16_t value) {
	memset(r, 0, sizeof(jing_result));
	r->len            = SIZE_MAX;
	r->data.short_val = value;
}

static inline void jing_int_result(jing_result* r, int32_t value) {
	memset(r, 0, sizeof(jing_result));
	r->len          = SIZE_MAX;
	r->data.int_val = value;
}

static inline void jing_long_result(jing_result* r, int64_t value) {
	memset(r, 0, sizeof(jing_result));
	r->len           = SIZE_MAX;
	r->data.long_val = value;
}

static inline void jing_float_result(jing_result* r, float value) {
	memset(r, 0, sizeof(jing_result));
	r->len            = SIZE_MAX;
	r->data.float_val = value;
}

static inline void jing_double_result(jing_result* r, double value) {
	memset(r, 0, sizeof(jing_result));
	r->len             = SIZE_MAX;
	r->data.double_val = value;
}

static inline void jing_ptr_result(jing_result* r, void* value, size_t len) {
	memset(r, 0, sizeof(jing_result));
	r->len          = len;
	r->data.ptr_val = value;
}

#if defined(JING_OS_WINDOWS)
#include "wepoll.h"
#include <winsock2.h>
#include <windows.h>
#else
#include <sys/socket.h>
#endif

JING_EXPORT_SYMBOL const char* jing_version(void);

JING_EXPORT_SYMBOL int jing_major_version(void);

JING_EXPORT_SYMBOL int jing_minor_version(void);

JING_EXPORT_SYMBOL int jing_patch_version(void);

JING_EXPORT_SYMBOL size_t jing_allocate_granularity(void);

JING_EXPORT_SYMBOL void jing_mmap(size_t size, jing_result* r);

JING_EXPORT_SYMBOL void jing_munmap(void* addr, size_t size, jing_result* r);

#if defined(JING_OS_WINDOWS)
JING_EXPORT_SYMBOL int jing_win_ansi_support(void);

JING_EXPORT_SYMBOL DWORD jing_std_output_dword(void);

JING_EXPORT_SYMBOL DWORD jing_std_error_dword(void);

JING_EXPORT_SYMBOL void jing_get_std_handle(DWORD d, jing_result* r);

JING_EXPORT_SYMBOL void jing_create_file(LPCWSTR filename, jing_result* r);

JING_EXPORT_SYMBOL void jing_write_file(HANDLE h, char* buffer, int len,
                                        jing_result* r);

JING_EXPORT_SYMBOL void jing_flush_file(HANDLE h, jing_result* r);
#endif

#if defined(JING_OS_WINDOWS)
JING_EXPORT_SYMBOL int jing_wepoll_in(void);

JING_EXPORT_SYMBOL int jing_wepoll_out(void);

JING_EXPORT_SYMBOL int jing_wepoll_default(void);

JING_EXPORT_SYMBOL int jing_wepoll_ctl_add(void);

JING_EXPORT_SYMBOL int jing_wepoll_ctl_mod(void);

JING_EXPORT_SYMBOL int jing_wepoll_ctl_del(void);

JING_EXPORT_SYMBOL void jing_wepoll_create(jing_result* r);

JING_EXPORT_SYMBOL void jing_wepoll_ctl(HANDLE epfd, SOCKET socket, int op,
                                        uint32_t events, int data,
                                        jing_result* r);

JING_EXPORT_SYMBOL void jing_wepoll_wait(HANDLE epfd,
                                         struct epoll_event* events,
                                         int maxevents, int timeout,
                                         jing_result* r);

JING_EXPORT_SYMBOL void jing_wepoll_close(HANDLE epfd, jing_result* r);
#endif

#if defined(JING_OS_LINUX) || defined(JING_OS_MACOS)
JING_EXPORT_SYMBOL int jing_stdout_fileno(void);

JING_EXPORT_SYMBOL int jing_stderr_fileno(void);

JING_EXPORT_SYMBOL void jing_open_fd(char* filename, jing_result* r);

JING_EXPORT_SYMBOL void jing_write_fd(int fd, char* buf, size_t len,
                                      jing_result* r);

JING_EXPORT_SYMBOL void jing_sync_fd(int fd, jing_result* r);
#endif

JING_EXPORT_SYMBOL int jing_connect_blocked_errcode(void);

JING_EXPORT_SYMBOL int jing_send_blocked_errcode(void);

JING_EXPORT_SYMBOL int jing_interrupt_errcode(void);

JING_EXPORT_SYMBOL int jing_af_inet_code(void);

JING_EXPORT_SYMBOL int jing_af_inet6_code(void);

JING_EXPORT_SYMBOL int jing_af_unix_code(void);

JING_EXPORT_SYMBOL int jing_tcp_type_code(void);

JING_EXPORT_SYMBOL int jing_udp_type_code(void);

JING_EXPORT_SYMBOL int jing_tcp_protocol_code(void);

JING_EXPORT_SYMBOL int jing_udp_protocol_code(void);

JING_EXPORT_SYMBOL void jing_socket(int af, int type, int protocol,
                                    jing_result* r);

// boringssl related methods
#if defined(JING_ENABLE_BORINGSSL)
#include <openssl/crypto.h>
#include <openssl/err.h>
#include <openssl/ssl.h>
#if !defined(OPENSSL_IS_BORINGSSL)
#error "Requires boringssl, not openssl"
#endif

JING_EXPORT_SYMBOL const char* jing_ssl_version(void);

JING_EXPORT_SYMBOL const char* jing_ssl_cflags(void);

JING_EXPORT_SYMBOL const char* jing_ssl_built_on(void);

JING_EXPORT_SYMBOL uint16_t jing_tls1_2_version(void);

JING_EXPORT_SYMBOL uint16_t jing_tls1_3_version(void);

JING_EXPORT_SYMBOL const SSL_METHOD* jing_tls_method(void);

JING_EXPORT_SYMBOL const SSL_METHOD* jing_dtls_method(void);

JING_EXPORT_SYMBOL SSL_CTX* jing_ssl_ctx_new(const SSL_METHOD* method);

JING_EXPORT_SYMBOL void jing_ssl_ctx_free(SSL_CTX* ctx);

JING_EXPORT_SYMBOL int jing_ssl_ctx_set_min_proto_version(SSL_CTX* ctx,
                                                          uint16_t version);

JING_EXPORT_SYMBOL int jing_ssl_ctx_set_max_proto_version(SSL_CTX* ctx,
                                                          uint16_t version);

JING_EXPORT_SYMBOL SSL* jing_ssl_new(SSL_CTX* ctx);
#endif

#endif