#include "jing_common.h"

#if defined(JING_OS_LINUX) || defined(JING_OS_MACOS)
#include "jing_posix.h"
#include <stdlib.h>
#include <stdalign.h>
#include <sys/socket.h>
#include <sys/mman.h>
#include <sys/un.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <arpa/inet.h>
#include <errno.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>

size_t jing_posix_max_align(void) {
	return alignof(max_align_t);
}

void* jing_posix_memalign(size_t alignment, size_t size) {
	void* p = NULL;
	int r   = posix_memalign(&p, alignment, size);
	if (r != 0) {
		return jing_make_error_ptr(r);
	}
	return p;
}

void jing_posix_batch_free(void** ptrs, size_t len,
                           void (*free_func_t)(void*)) {
	size_t count = len / sizeof(void*);
	for (size_t i = 0; i < count; ++i) {
		free_func_t(ptrs[i]);
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

// log io related
int jing_log_posix_open_append(const char* filename) {
	int v = open(filename, O_WRONLY | O_CREAT | O_APPEND, 0644);
	if (JING_UNLIKELY(v == -1)) {
		int err = errno;
		return -err;
	} else {
		return v;
	}
}

long long jing_log_posix_write(int fd, const char* buffer, size_t len) {
	ssize_t total = 0;
	while (total < (ssize_t) len) {
		ssize_t v = write(fd, buffer + total, len - (size_t) total);
		if (JING_UNLIKELY(v == -1)) {
			int err = errno;
			if (err == EINTR) {
				continue;
			}
			return -(long long) err;
		}
		total += v;
	}
	return (long long) total;
}

int jing_log_posix_flush(int fd) {
	int v = fsync(fd);
	if (JING_UNLIKELY(v == -1)) {
		int err = errno;
		return -err;
	} else {
		return 0;
	}
}

long long jing_log_posix_file_size(int fd) {
	struct stat st;
	if (JING_UNLIKELY(fstat(fd, &st) == -1)) {
		int err = errno;
		return -(long long) err;
	} else {
		return (long long) st.st_size;
	}
}

int jing_log_posix_close(int fd) {
	int v = close(fd);
	if (JING_UNLIKELY(v == -1)) {
		int err = errno;
		return -err;
	} else {
		return 0;
	}
}

// network related
int jing_posix_connect_block_code(void) {
	return EINPROGRESS;
}

int jing_posix_send_block_code(void) {
	return EAGAIN;
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

int jing_posix_sol_socket(void) {
	return SOL_SOCKET;
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

int jing_posix_ipproto_ipv6_code(void) {
	return IPPROTO_IPV6;
}

int jing_posix_so_reuseaddr(void) {
	return SO_REUSEADDR;
}

int jing_posix_tcp_nodelay(void) {
	return TCP_NODELAY;
}

int jing_posix_ipv6_v6only(void) {
	return IPV6_V6ONLY;
}

int jing_posix_socket(int domain, int type, int protocol) {
	int r = socket(domain, type, protocol);
	if (JING_UNLIKELY(r == -1)) {
		int err = errno;
		return -err;
	}
	return r;
}

int jing_posix_set_sock_opt(int socket, int level, int opt, const void* val,
                            int len) {
	int r = setsockopt(socket, level, opt, val, len);
	if (JING_UNLIKELY(r == -1)) {
		int err = errno;
		return -err;
	}
	return r;
}

int jing_posix_get_err_opt(int socket) {
	int v         = 0;
	socklen_t len = sizeof(int);
	int r         = getsockopt(socket, SOL_SOCKET, SO_ERROR, &v, &len);
	if (JING_UNLIKELY(r == -1)) {
		int err = errno;
		return -err;
	}
	return v;
}

int jing_posix_set_nonblocking(int socket) {
	int flag = fcntl(socket, F_GETFL, 0);
	if (JING_UNLIKELY(flag == -1)) {
		int err = errno;
		return -err;
	}
	int r = fcntl(socket, F_SETFL, flag | O_NONBLOCK);
	if (JING_UNLIKELY(r == -1)) {
		int err = errno;
		return -err;
	}
	return r;
}

int jing_posix_bind(int socket, int type, const char* addr, int port) {
	struct sockaddr_storage ss = {0};
	socklen_t ss_len;
	if (type == AF_INET) {
		struct sockaddr_in* ipv4 = (struct sockaddr_in*) &ss;
		ipv4->sin_family         = AF_INET;
		ipv4->sin_port           = htons((u_short) port);
		if (addr == NULL) {
			ipv4->sin_addr.s_addr = htonl(INADDR_ANY);
		} else {
			int r = inet_pton(AF_INET, addr, &ipv4->sin_addr);
			if (JING_UNLIKELY(r == -1)) {
				int err = errno;
				return -err;
			}
			if (JING_UNLIKELY(r == 0)) {
				return -EINVAL;
			}
		}
		ss_len = sizeof(*ipv4);
	} else if (type == AF_INET6) {
		struct sockaddr_in6* ipv6 = (struct sockaddr_in6*) &ss;
		ipv6->sin6_family         = AF_INET6;
		ipv6->sin6_port           = htons((uint16_t) port);
		if (addr == NULL) {
			ipv6->sin6_addr = in6addr_any;
		} else {
			int r = inet_pton(AF_INET6, addr, &ipv6->sin6_addr);
			if (JING_UNLIKELY(r == -1)) {
				int err = errno;
				return -err;
			}
			if (JING_UNLIKELY(r == 0)) {
				return -EINVAL;
			}
		}
		ss_len = sizeof(*ipv6);
	} else {
		return -EAFNOSUPPORT;
	}
	int r = bind(socket, (const struct sockaddr*) &ss, ss_len);
	if (JING_UNLIKELY(r == -1)) {
		int err = errno;
		return -err;
	}
	return r;
}

int jing_posix_listen(int socket, int backlog) {
	int r = listen(socket, backlog);
	if (JING_UNLIKELY(r == -1)) {
		int err = errno;
		return -err;
	}
	return r;
}

int jing_posix_accept(int socket, char* buf) {
	struct sockaddr_storage ss = {0};
	socklen_t ss_len           = sizeof(ss);
	int conn = accept(socket, (struct sockaddr*) &ss, &ss_len);
	if (JING_UNLIKELY(conn == -1)) {
		int err = errno;
		return -err;
	}
	int type = AF_UNSPEC;
	if (ss.ss_family == AF_INET) {
		const struct sockaddr_in* ipv4 = (const struct sockaddr_in*) &ss;
		memset(buf, 0, INET_ADDRSTRLEN + 2 * sizeof(int));
		if (inet_ntop(AF_INET, &ipv4->sin_addr, buf + 2 * sizeof(int),
		              INET_ADDRSTRLEN) != NULL) {
			int port = (int) ntohs(ipv4->sin_port);
			memcpy(buf + sizeof(int), &port, sizeof(int));
		}
		type = AF_INET;
	} else if (ss.ss_family == AF_INET6) {
		const struct sockaddr_in6* ipv6 = (const struct sockaddr_in6*) &ss;
		if (IN6_IS_ADDR_V4MAPPED(&ipv6->sin6_addr)) {
			memset(buf, 0, INET_ADDRSTRLEN + 2 * sizeof(int));
			struct in_addr v4;
			memcpy(&v4, &ipv6->sin6_addr.s6_addr[12], 4);
			if (inet_ntop(AF_INET, &v4, buf + 2 * sizeof(int),
			              INET_ADDRSTRLEN) != NULL) {
				int port = (int) ntohs(ipv6->sin6_port);
				memcpy(buf + sizeof(int), &port, sizeof(int));
			}
			type = AF_INET;
		} else {
			memset(buf, 0, INET6_ADDRSTRLEN + 2 * sizeof(int));
			if (inet_ntop(AF_INET6, &ipv6->sin6_addr, buf + 2 * sizeof(int),
			              INET6_ADDRSTRLEN) != NULL) {
				int port = (int) ntohs(ipv6->sin6_port);
				memcpy(buf + sizeof(int), &port, sizeof(int));
			}
			type = AF_INET6;
		}
	}
	memcpy(buf, &type, sizeof(int));
	return conn;
}

int jing_posix_connect(int socket, int type, const char* addr, int port) {
	struct sockaddr_storage ss = {0};
	socklen_t ss_len;
	if (type == AF_INET) {
		struct sockaddr_in* ipv4 = (struct sockaddr_in*) &ss;
		ipv4->sin_family         = AF_INET;
		ipv4->sin_port           = htons((u_short) port);
		int r                    = inet_pton(AF_INET, addr, &(ipv4->sin_addr));
		if (JING_UNLIKELY(r == -1)) {
			int err = errno;
			return -err;
		}
		if (JING_UNLIKELY(r == 0)) {
			return -EINVAL;
		}
		ss_len = sizeof(*ipv4);
	} else if (type == AF_INET6) {
		struct sockaddr_in6* ipv6 = (struct sockaddr_in6*) &ss;
		ipv6->sin6_family         = AF_INET6;
		ipv6->sin6_port           = htons((uint16_t) port);
		int r                     = inet_pton(AF_INET6, addr, &ipv6->sin6_addr);
		if (JING_UNLIKELY(r == -1)) {
			return -errno;
		}
		if (JING_UNLIKELY(r == 0)) {
			return -EINVAL;
		}
		ss_len = sizeof(*ipv6);
	} else {
		return -EAFNOSUPPORT;
	}
	int r = connect(socket, (const struct sockaddr*) &ss, ss_len);
	if (r == -1) {
		int err = errno;
		return -err;
	}
	return 0;
}

int jing_posix_recv(int sockfd, char* buf, int len) {
	ssize_t r = recv(sockfd, buf, (size_t) len, 0);
	if (JING_UNLIKELY(r == -1)) {
		int err = errno;
		return -err;
	}
	return (int) r;
}

int jing_posix_send(int sockfd, const char* buf, int len) {
	ssize_t r = send(sockfd, buf, (size_t) len, 0);
	if (JING_UNLIKELY(r == -1)) {
		int err = errno;
		return -err;
	}
	return (int) r;
}

int jing_posix_recvfrom(int socket, char* data, int len, char* buf) {
	struct sockaddr_storage ss = {0};
	socklen_t ss_len           = sizeof(ss);
	ssize_t r = recvfrom(socket, data, (size_t) len, 0, (struct sockaddr*) &ss,
	                     &ss_len);
	if (JING_UNLIKELY(r == -1)) {
		int err = errno;
		return -err;
	}
	int type = AF_UNSPEC;
	if (ss.ss_family == AF_INET) {
		const struct sockaddr_in* ipv4 = (const struct sockaddr_in*) &ss;
		memset(buf, 0, INET_ADDRSTRLEN + 2 * sizeof(int));
		if (inet_ntop(AF_INET, &ipv4->sin_addr, buf + 2 * sizeof(int),
		              INET_ADDRSTRLEN) != NULL) {
			int port = (int) ntohs(ipv4->sin_port);
			memcpy(buf + sizeof(int), &port, sizeof(int));
		}
		type = AF_INET;
	} else if (ss.ss_family == AF_INET6) {
		const struct sockaddr_in6* ipv6 = (const struct sockaddr_in6*) &ss;
		if (IN6_IS_ADDR_V4MAPPED(&ipv6->sin6_addr)) {
			memset(buf, 0, INET_ADDRSTRLEN + 2 * sizeof(int));
			struct in_addr v4;
			memcpy(&v4, &ipv6->sin6_addr.s6_addr[12], 4);
			if (inet_ntop(AF_INET, &v4, buf + 2 * sizeof(int),
			              INET_ADDRSTRLEN) != NULL) {
				int port = (int) ntohs(ipv6->sin6_port);
				memcpy(buf + sizeof(int), &port, sizeof(int));
			}
			type = AF_INET;
		} else {
			memset(buf, 0, INET6_ADDRSTRLEN + 2 * sizeof(int));
			if (inet_ntop(AF_INET6, &ipv6->sin6_addr, buf + 2 * sizeof(int),
			              INET6_ADDRSTRLEN) != NULL) {
				int port = (int) ntohs(ipv6->sin6_port);
				memcpy(buf + sizeof(int), &port, sizeof(int));
			}
			type = AF_INET6;
		}
	}
	memcpy(buf, &type, sizeof(int));
	return (int) r;
}

int jing_posix_sendto(int socket, int type, const char* addr, int port,
                      const char* buf, int len) {
	struct sockaddr_storage ss = {0};
	socklen_t ss_len;
	if (type == AF_INET) {
		struct sockaddr_in* ipv4 = (struct sockaddr_in*) &ss;
		ipv4->sin_family         = AF_INET;
		ipv4->sin_port           = htons((u_short) port);
		int r                    = inet_pton(AF_INET, addr, &(ipv4->sin_addr));
		if (JING_UNLIKELY(r == -1)) {
			int err = errno;
			return -err;
		}
		if (JING_UNLIKELY(r == 0)) {
			return -EINVAL;
		}
		ss_len = sizeof(*ipv4);
	} else if (type == AF_INET6) {
		struct sockaddr_in6* ipv6 = (struct sockaddr_in6*) &ss;
		ipv6->sin6_family         = AF_INET6;
		ipv6->sin6_port           = htons((uint16_t) port);
		int r                     = inet_pton(AF_INET6, addr, &ipv6->sin6_addr);
		if (JING_UNLIKELY(r == -1)) {
			int err = errno;
			return -err;
		}
		if (JING_UNLIKELY(r == 0)) {
			return -EINVAL;
		}
		ss_len = sizeof(*ipv6);
	} else {
		return -EAFNOSUPPORT;
	}
	ssize_t r = sendto(socket, buf, (size_t) len, 0,
	                   (const struct sockaddr*) &ss, ss_len);
	if (JING_UNLIKELY(r == -1)) {
		int err = errno;
		return -err;
	}
	return (int) r;
}

int jing_posix_read(int fd, char* buf, int len) {
	ssize_t r = read(fd, buf, (size_t) len);
	if (JING_UNLIKELY(r == -1)) {
		int err = errno;
		return -err;
	}
	return (int) r;
}

int jing_posix_write(int fd, const char* buf, int len) {
	ssize_t r = write(fd, buf, (size_t) len);
	if (JING_UNLIKELY(r == -1)) {
		int err = errno;
		return -err;
	}
	return (int) r;
}

uint64_t jing_posix_socketpair(int domain, int type) {
	int sv[2];
	int r = socketpair(domain, type, 0, sv);
	if (JING_UNLIKELY(r == -1)) {
		sv[0] = -errno;
		sv[1] = 0;
	}
	return ((uint64_t) sv[0] << 32) | (uint32_t) sv[1];
}

int jing_posix_read_int(int socket) {
	int v;
	ssize_t r = read(socket, &v, sizeof(int));
	if (JING_UNLIKELY(r == -1)) {
		int err = errno;
		return -err;
	}
	if (JING_UNLIKELY(r != (ssize_t) sizeof(int))) {
		int err = EIO;
		return -err;
	}
	return v;
}

int jing_posix_write_int(int socket, int value) {
	ssize_t r = write(socket, (const char*) &value, sizeof(value));
	if (JING_UNLIKELY(r == -1)) {
		int err = errno;
		return -err;
	}
	if (JING_UNLIKELY(r != (ssize_t) sizeof(int))) {
		int err = EIO;
		return -err;
	}
	return 0;
}

int jing_posix_uds_bind(int socket, const char* addr, size_t len) {
	struct sockaddr_un un = {0};
	if (JING_UNLIKELY(len > sizeof(un.sun_path))) {
		int err = ENAMETOOLONG;
		return -err;
	}
	un.sun_family = AF_UNIX;
	memcpy(un.sun_path, addr, len);
	socklen_t addr_len =
	    (socklen_t) (offsetof(struct sockaddr_un, sun_path) + len);
	int r = bind(socket, (const struct sockaddr*) &un, addr_len);
	if (JING_UNLIKELY(r == -1)) {
		int err = errno;
		return -err;
	}
	return r;
}

int jing_posix_uds_connect(int socket, const char* addr, size_t len) {
	struct sockaddr_un un = {0};
	if (JING_UNLIKELY(len > sizeof(un.sun_path))) {
		int err = ENAMETOOLONG;
		return -err;
	}
	un.sun_family = AF_UNIX;
	memcpy(un.sun_path, addr, len);
	socklen_t addr_len =
	    (socklen_t) (offsetof(struct sockaddr_un, sun_path) + len);
	int r = connect(socket, (const struct sockaddr*) &un, addr_len);
	if (JING_UNLIKELY(r == -1)) {
		int err = errno;
		return -err;
	}
	return r;
}

int jing_posix_uds_accept(int socket) {
	int r = accept(socket, NULL, NULL);
	if (JING_UNLIKELY(r == -1)) {
		int err = errno;
		return -err;
	}
	return r;
}

#endif