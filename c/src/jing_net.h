#ifndef JING_NET_H
#define JING_NET_H

#include "jing_common.h"

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

#endif