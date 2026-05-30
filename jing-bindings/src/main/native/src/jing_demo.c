#include "jing_demo.h"
#include <stdlib.h>
#include <stdio.h>

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
	return snprintf(buf, len, "%lld", val);
}

int demo_double_to_str(double val, char* buf, int len) {
	return snprintf(buf, len, "%g", val);
}