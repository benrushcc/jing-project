#ifndef JING_DEMO_H
#define JING_DEMO_H

#include "jing_common.h"
#include <stdlib.h>
#include <string.h>

JING_EXPORT_SYMBOL int demo_single_int(void);

JING_EXPORT_SYMBOL int demo_compute_add(int a, int b);

JING_EXPORT_SYMBOL int demo_compute_pointer(int* a, int* b);

JING_EXPORT_SYMBOL int64_t demo_str_to_int64(const char* str);

JING_EXPORT_SYMBOL double demo_str_to_double(const char* str);

JING_EXPORT_SYMBOL int demo_int64_to_str(int64_t val, char* buf, int len);

JING_EXPORT_SYMBOL int demo_double_to_str(double val, char* buf, int len);

#endif