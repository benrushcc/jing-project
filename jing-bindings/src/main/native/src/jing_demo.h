#ifndef JING_DEMO_H
#define JING_DEMO_H

#include "jing_common.h"
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>

JING_EXPORT_SYMBOL int demo_single_int(void);

JING_EXPORT_SYMBOL int demo_compute_add(int a, int b);

JING_EXPORT_SYMBOL int demo_compute_pointer(int* a, int* b);

JING_EXPORT_SYMBOL int64_t demo_str_to_int64(const char* str);

JING_EXPORT_SYMBOL double demo_str_to_double(const char* str);

JING_EXPORT_SYMBOL int demo_int64_to_str(int64_t val, char* buf, int len);

JING_EXPORT_SYMBOL int demo_double_to_str(double val, char* buf, int len);

JING_EXPORT_SYMBOL long demo_long_add(long a, long b);

JING_EXPORT_SYMBOL long long demo_long_long_add(long long a, long long b);

JING_EXPORT_SYMBOL size_t demo_size_t_add(size_t a, size_t b);

JING_EXPORT_SYMBOL unsigned int demo_unsigned_int_add(unsigned int a, unsigned int b);

JING_EXPORT_SYMBOL unsigned long demo_unsigned_long_add(unsigned long a, unsigned long b);

JING_EXPORT_SYMBOL size_t demo_str_len(const char* s);

JING_EXPORT_SYMBOL void* demo_void_ptr_identity(void* p);

JING_EXPORT_SYMBOL int demo_sizeof_int(void);

JING_EXPORT_SYMBOL int demo_sizeof_long(void);

JING_EXPORT_SYMBOL int demo_sizeof_size_t(void);

JING_EXPORT_SYMBOL int demo_sizeof_pointer(void);

JING_EXPORT_SYMBOL bool demo_bool_not(bool v);

JING_EXPORT_SYMBOL bool demo_bool_and(bool a, bool b);

JING_EXPORT_SYMBOL bool demo_bool_true(void);

JING_EXPORT_SYMBOL bool demo_bool_false(void);

JING_EXPORT_SYMBOL bool demo_int_to_bool(int v);

JING_EXPORT_SYMBOL int demo_bool_to_int(bool v);

JING_EXPORT_SYMBOL int8_t demo_byte_add(int8_t a, int8_t b);

JING_EXPORT_SYMBOL int16_t demo_short_add(int16_t a, int16_t b);

JING_EXPORT_SYMBOL uint16_t demo_char_upper(uint16_t c);

JING_EXPORT_SYMBOL float demo_float_add(float a, float b);

JING_EXPORT_SYMBOL void demo_void_noop(void);

JING_EXPORT_SYMBOL int* demo_pointer_identity(int* p);

#endif