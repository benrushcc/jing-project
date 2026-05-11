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