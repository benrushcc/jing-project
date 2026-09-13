#include "jing_demo.h"
#include <stdlib.h>
#include <stdio.h>
#include <inttypes.h>
#include <float.h>

int demo_single_int(void) {
	return 7355608;
}

int demo_compute_add(int a, int b) {
	return a + b;
}

int demo_compute_pointer(int* a, int* b) {
	return *a - *b;
}

int64_t demo_str_to_int64(const char* str) {
	return (int64_t) strtoll(str, NULL, 10);
}

double demo_str_to_double(const char* str) {
	return strtod(str, NULL);
}

int demo_int64_to_str(int64_t val, char* buf, int len) {
	return snprintf(buf, len, "%" PRId64, val);
}

int demo_double_to_str(double val, char* buf, int len) {
	// DBL_DECIMAL_DIG digits guarantee a bit-exact round trip through
	// strtod/parseDouble on any conforming implementation
	return snprintf(buf, len, "%.*g", DBL_DECIMAL_DIG, val);
}

#if defined(JING_OS_WINDOWS)
// windows: 'long' is 4 bytes, real semantics live in demo_long_win_add
long long demo_long_add(long long a, long long b) {
	(void) a;
	(void) b;
	return 0;
}

long demo_long_win_add(long a, long b) {
	return a + b;
}
#else
// lp64: 'long' is 8 bytes, this is the real long demo
long demo_long_add(long a, long b) {
	return a + b;
}

// 4-byte stub, width matches Java 'int'
int demo_long_win_add(int a, int b) {
	(void) a;
	(void) b;
	return 0;
}
#endif

size_t demo_size_t_add(size_t a, size_t b) {
	return a + b;
}

unsigned int demo_unsigned_int_add(unsigned int a, unsigned int b) {
	return a + b;
}

#if defined(JING_OS_WINDOWS)
// windows: 'unsigned long' is 4 bytes, real semantics live in
// demo_unsigned_long_win_add
unsigned long long demo_unsigned_long_add(unsigned long long a,
                                          unsigned long long b) {
	(void) a;
	(void) b;
	return 0;
}

unsigned long demo_unsigned_long_win_add(unsigned long a, unsigned long b) {
	return a + b;
}
#else
// lp64: 'unsigned long' is 8 bytes, this is the real unsigned long demo
unsigned long demo_unsigned_long_add(unsigned long a, unsigned long b) {
	return a + b;
}

// 4-byte stub, width matches Java 'int'
unsigned int demo_unsigned_long_win_add(unsigned int a, unsigned int b) {
	(void) a;
	(void) b;
	return 0;
}
#endif

size_t demo_str_len(const char* s) {
	return strlen(s);
}

void* demo_void_ptr_identity(void* p) {
	return p;
}

int demo_sizeof_int(void) {
	return (int) sizeof(int);
}

int demo_sizeof_long(void) {
	return (int) sizeof(long);
}

int demo_sizeof_size_t(void) {
	return (int) sizeof(size_t);
}

int demo_sizeof_pointer(void) {
	return (int) sizeof(void*);
}

bool demo_bool_not(bool v) {
	return !v;
}

bool demo_bool_and(bool a, bool b) {
	return a && b;
}

bool demo_bool_true(void) {
	return true;
}

bool demo_bool_false(void) {
	return false;
}

bool demo_int_to_bool(int v) {
	return v != 0;
}

int demo_bool_to_int(bool v) {
	return v ? 1 : 0;
}

int8_t demo_byte_add(int8_t a, int8_t b) {
	return (int8_t) (a + b);
}

int16_t demo_short_add(int16_t a, int16_t b) {
	return (int16_t) (a + b);
}

uint16_t demo_char_upper(uint16_t c) {
	if (c >= 'a' && c <= 'z') {
		return (uint16_t) (c - ('a' - 'A'));
	} else {
		return c;
	}
}

float demo_float_add(float a, float b) {
	return a + b;
}

void demo_void_noop(void) {
}

int* demo_pointer_identity(int* p) {
	return p;
}