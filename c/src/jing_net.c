#include "jing_net.h"

#if defined(JING_OS_WINDOWS)
#include <winsock2.h>
#include <windows.h>
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
