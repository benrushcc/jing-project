#ifndef JING_SYS_H
#define JING_SYS_H

#include "common.h"

JING_EXPORT_SYMBOL size_t jing_allocate_granularity(void);

JING_EXPORT_SYMBOL void jing_mmap(size_t size, jing_result *r);

JING_EXPORT_SYMBOL void jing_munmap(void *addr, size_t size, jing_result *r);

#endif