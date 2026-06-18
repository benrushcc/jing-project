#include "jing_common.h"

#if defined(JING_OS_WINDOWS)
#include "jing_win.h"
#include <windows.h>
#include <winsock2.h>
#include <ws2tcpip.h>

size_t jing_win_page_size(void) {
	SYSTEM_INFO sys_info;
	memset(&sys_info, 0, sizeof(sys_info));
	GetSystemInfo(&sys_info);
	return sys_info.dwPageSize;
}

size_t jing_win_allocate_granularity(void) {
	SYSTEM_INFO sys_info;
	memset(&sys_info, 0, sizeof(sys_info));
	GetSystemInfo(&sys_info);
	return sys_info.dwAllocationGranularity;
}

DWORD
jing_win_mem_reserve(void) {
	return MEM_RESERVE;
}

DWORD
jing_win_mem_commit(void) {
	return MEM_COMMIT;
}

DWORD
jing_win_mem_decommit(void) {
	return MEM_DECOMMIT;
}

DWORD
jing_win_mem_release(void) {
	return MEM_RELEASE;
}

DWORD
jing_win_page_read_write(void) {
	return PAGE_READWRITE;
}

void* jing_win_virtual_alloc(void* addr, size_t size, DWORD type, DWORD prot) {
	void* ptr = VirtualAlloc(addr, size, type, prot);
	if (JING_UNLIKELY(ptr == NULL)) {
		int err = GetLastError();
		return jing_make_error_ptr(err);
	} else {
		return ptr;
	}
}

int jing_win_virtual_free(void* addr, size_t size, DWORD type) {
	int v = VirtualFree(addr, size, type);
	if (JING_UNLIKELY(v == 0)) {
		int err = GetLastError();
		return err;
	} else {
		return 0;
	}
}

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

// network related
int jing_win_connect_blocked_errcode(void) {
	return WSAEWOULDBLOCK;
}

int jing_win_send_blocked_errcode(void) {
	return WSAEWOULDBLOCK;
}

int jing_win_interrupt_errcode(void) {
	return WSAEINTR;
}

int jing_win_af_inet_code(void) {
	return AF_INET;
}

int jing_win_af_inet6_code(void) {
	return AF_INET6;
}

int jing_win_af_unix_code(void) {
	return AF_UNIX;
}

int jing_win_tcp_type_code(void) {
	return SOCK_STREAM;
}

int jing_win_udp_type_code(void) {
	return SOCK_DGRAM;
}

int jing_win_tcp_protocol_code(void) {
	return IPPROTO_TCP;
}

int jing_win_udp_protocol_code(void) {
	return IPPROTO_UDP;
}

SOCKET jing_win_socket(int af, int type, int protocol) {
	SOCKET v = socket(af, type, protocol);
	if (JING_UNLIKELY(v == INVALID_SOCKET)) {
		int err = WSAGetLastError();
		return (SOCKET) jing_make_error_ptr(err);
	} else {
		return v;
	}
}

#ifdef JING_USE_WEPOLL
#include "wepoll.h"
int jing_win_wepoll_in(void) {
	return EPOLLIN;
}

int jing_win_wepoll_out(void) {
	return EPOLLOUT;
}

int jing_win_wepoll_err(void) {
	return EPOLLERR;
}

int jing_win_wepoll_hup(void) {
	return EPOLLHUP;
}

int jing_win_wepoll_ctl_add(void) {
	return EPOLL_CTL_ADD;
}

int jing_win_wepoll_ctl_mod(void) {
	return EPOLL_CTL_MOD;
}

int jing_win_wepoll_ctl_del(void) {
	return EPOLL_CTL_DEL;
}

HANDLE
jing_win_wepoll_create() {
	HANDLE ptr = epoll_create1(0);
	if (JING_UNLIKELY(ptr == NULL)) {
		int err = GetLastError();
		return -err;
	} else {
		return ptr;
	}
}

int jing_win_wepoll_ctl(HANDLE epfd, SOCKET socket, int op,
                        uint32_t event_types, uint32_t data) {
	int v;
	if (op == EPOLL_CTL_DEL) {
		v = epoll_ctl(epfd, op, socket, NULL);
	} else {
		struct epoll_event event;
		memset(&event, 0, sizeof(event));
		event.events   = event_types;
		event.data.u32 = data;
		v              = epoll_ctl(epfd, op, socket, &event);
	}
	if (JING_UNLIKELY(v == -1)) {
		int err = GetLastError();
		return -err;
	} else {
		return 0;
	}
}

int jing_win_wepoll_wait(HANDLE epfd, struct epoll_event* events, int maxevents,
                         int timeout) {
	int v = epoll_wait(epfd, events, maxevents, timeout);
	if (JING_UNLIKELY(v == -1)) {
		int err = GetLastError();
		return -err;
	} else {
		return v;
	}
}

int jing_win_wepoll_close(HANDLE epfd) {
	int v = epoll_close(epfd);
	if (JING_UNLIKELY(v == -1)) {
		int err = GetLastError();
		return -err;
	} else {
		return 0;
	}
}
#endif

#endif