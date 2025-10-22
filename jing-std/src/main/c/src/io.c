#include "io.h"

#ifdef JING_OS_WINDOWS

#include "wepoll.h"
#include <WS2tcpip.h>
#include <windows.h>


int jing_connect_blocked_errcode(void) { return WSAEWOULDBLOCK; }

int jing_send_blocked_errcode(void) { return WSAEWOULDBLOCK; }

int jing_interrupt_errcode(void) { return WSAEINTR; }

int jing_af_inet_code(void) { return AF_INET; }

int jing_af_inet6_code(void) { return AF_INET6; }

int jing_af_unix_code(void) { return AF_UNIX; }

int jing_tcp_type_code(void) { return SOCK_STREAM; }

int jing_udp_type_code(void) { return SOCK_DGRAM; }

int jing_tcp_protocol_code(void) { return IPPROTO_TCP; }

int jing_udp_protocol_code(void) { return IPPROTO_UDP; }

int jing_wepoll_in(void) { return EPOLLIN; }

int jing_wepoll_out(void) { return EPOLLOUT; }

int jing_wepoll_default(void) { return EPOLLERR | EPOLLHUP; }

void jing_wepoll_create(jing_result *r) {
  HANDLE ptr = epoll_create1(0);
  if (JING_UNLIKELY(ptr == NULL)) {
    int err = GetLastError();
    jing_err_result(r, err);
  } else {
    jing_ptr_result(r, ptr, SIZE_MAX);
  }
}

void jing_wepoll_ctl(HANDLE epfd, SOCKET socket, int from, int to, int data,
                     jing_result *r) {
  int v;
  if (from == 0) {
    struct epoll_event event = {.events = to, .data.fd = data};
    v = epoll_ctl(epfd, EPOLL_CTL_ADD, socket, &event);
  } else if (to == 0) {
    v = epoll_ctl(epfd, EPOLL_CTL_DEL, socket, NULL);
  } else {
    struct epoll_event event = {.events = to, .data.fd = data};
    v = epoll_ctl(epfd, EPOLL_CTL_MOD, socket, &event);
  }

  if (JING_UNLIKELY(v == -1)) {
    int err = GetLastError();
    jing_err_result(r, err);
  } else {
    jing_int_result(r, 0);
  }
}

void jing_wepoll_wait(HANDLE epfd, struct epoll_event *events, int maxevents,
                      int timeout, jing_result *r) {
  int v = epoll_wait(epfd, events, maxevents, timeout);
  if (JING_UNLIKELY(v == -1)) {
    int err = GetLastError();
    jing_err_result(r, err);
  } else {
    jing_int_result(r, 0);
  }
}

void jing_wepoll_close(HANDLE epfd, jing_result *r) {
  int v = epoll_close(epfd);
  if (JING_UNLIKELY(v == -1)) {
    int err = GetLastError();
    jing_err_result(r, err);
  } else {
    jing_int_result(r, 0);
  }
}

void jing_socket(int af, int type, int protocol, jing_result *r) {
  SOCKET v = socket(af, type, protocol);
  if (JING_UNLIKELY(v == INVALID_SOCKET)) {
    int err = WSAGetLastError();
    jing_err_result(r, err);
  } else {
    jing_ptr_result(r, v, SIZE_MAX);
  }
}

#endif
