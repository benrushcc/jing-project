#ifndef JING_POSIX_H
#define JING_POSIX_H

#include "jing_common.h"
#if defined(JING_OS_LINUX) || defined(JING_OS_MACOS)
#include <sys/types.h>
static_assert(sizeof(off_t) == 8, "off_t size mismatch");

// mmap related
JING_EXPORT_SYMBOL long jing_posix_page_size();

JING_EXPORT_SYMBOL int jing_posix_prot_read(void);

JING_EXPORT_SYMBOL int jing_posix_prot_write(void);

JING_EXPORT_SYMBOL int jing_posix_prot_none(void);

JING_EXPORT_SYMBOL int jing_posix_madv_dontneed(void);

JING_EXPORT_SYMBOL int jing_posix_map_private(void);

JING_EXPORT_SYMBOL int jing_posix_map_anonymous(void);

JING_EXPORT_SYMBOL void* jing_posix_mmap(void* addr, size_t size, int prot,
                                         int flags, int fd, off_t offset);

JING_EXPORT_SYMBOL int jing_posix_mprotect(void* addr, size_t size, int prot);

JING_EXPORT_SYMBOL int jing_posix_madvise(void* addr, size_t size, int advice);

JING_EXPORT_SYMBOL int jing_posix_munmap(void* addr, size_t size);

// fs related
JING_EXPORT_SYMBOL int jing_stdout_fileno(void);

JING_EXPORT_SYMBOL int jing_stderr_fileno(void);

JING_EXPORT_SYMBOL void jing_open_fd(char* filename, jing_result* r);

JING_EXPORT_SYMBOL void jing_write_fd(int fd, char* buf, size_t len,
                                      jing_result* r);

JING_EXPORT_SYMBOL void jing_sync_fd(int fd, jing_result* r);

// network related
JING_EXPORT_SYMBOL int jing_posix_af_inet_code(void);

JING_EXPORT_SYMBOL int jing_posix_af_inet6_code(void);

JING_EXPORT_SYMBOL int jing_posix_af_unix_code(void);

JING_EXPORT_SYMBOL int jing_posix_tcp_type_code(void);

JING_EXPORT_SYMBOL int jing_posix_udp_type_code(void);

JING_EXPORT_SYMBOL int jing_posix_tcp_protocol_code(void);

JING_EXPORT_SYMBOL int jing_posix_udp_protocol_code(void);
#endif

#endif