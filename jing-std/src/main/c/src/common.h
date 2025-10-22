#ifndef JING_COMMON_H
#define JING_COMMON_H

#include <stddef.h>
#include <stdint.h>
#include <string.h>

#if !defined(__STDC_VERSION__) || __STDC_VERSION__ < 201112L
#error "Requires C11 or later"
#endif

#if defined(_WIN32) || defined(_WIN64)
#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#endif

#if defined(_WIN32) || defined(_WIN64)
#define JING_OS_WINDOWS 1
#elif defined(__APPLE__) && defined(__MACH__)
#define JING_OS_MACOS 1
#elif defined(__linux__)
#define JING_OS_LINUX 1
#else
#error "Unknown compiler/platform"
#endif

static_assert(sizeof(size_t) == 8, "size_t size mismatch");
static_assert(sizeof(void *) == 8, "pointer size mismatch");

#define JING_UNUSED(x) (void)(x)

#if defined(__GNUC__) || defined(__clang__)
#define JING_LIKELY(x) __builtin_expect(!!(x), 1)
#define JING_UNLIKELY(x) __builtin_expect(!!(x), 0)
#else
#define JING_LIKELY(x) (x)
#define JING_UNLIKELY(x) (x)
#endif

#if defined(_WIN32) || defined(_WIN64)
#define JING_EXPORT_SYMBOL __declspec(dllexport)
#define JING_HIDDEN_SYMBOL
#else
#define JING_EXPORT_SYMBOL __attribute__((visibility("default")))
#define JING_HIDDEN_SYMBOL __attribute__((visibility("hidden")))
#endif

typedef union {
  int8_t byte_val;
  int16_t short_val;
  uint16_t char_val;
  int32_t int_val;
  int64_t long_val;
  float float_val;
  double double_val;
  void *ptr_val;
} jing_data;

typedef struct {
  jing_data data;
  size_t len;
} jing_result;

static_assert(sizeof(jing_result) == 16, "jing_result size mismatch");

static inline void jing_err_result(jing_result *r, int err) {
  memset(r, 0, sizeof(jing_result));
  r->data.int_val = err;
  r->len = 0;
}

static inline void jing_byte_result(jing_result *r, int8_t value) {
  memset(r, 0, sizeof(jing_result));
  r->data.byte_val = value;
  r->len = SIZE_MAX;
}

static inline void jing_short_result(jing_result *r, int16_t value) {
  memset(r, 0, sizeof(jing_result));
  r->data.short_val = value;
  r->len = SIZE_MAX;
}

static inline void jing_int_result(jing_result *r, int32_t value) {
  memset(r, 0, sizeof(jing_result));
  r->data.int_val = value;
  r->len = SIZE_MAX;
}

static inline void jing_long_result(jing_result *r, int64_t value) {
  memset(r, 0, sizeof(jing_result));
  r->data.long_val = value;
  r->len = SIZE_MAX;
}

static inline void jing_float_result(jing_result *r, float value) {
  memset(r, 0, sizeof(jing_result));
  r->data.float_val = value;
  r->len = SIZE_MAX;
}

static inline void jing_double_result(jing_result *r, double value) {
  memset(r, 0, sizeof(jing_result));
  r->data.double_val = value;
  r->len = SIZE_MAX;
}

static inline void jing_ptr_result(jing_result *r, void *value, size_t len) {
  memset(r, 0, sizeof(jing_result));
  r->data.ptr_val = value;
  r->len = len;
}

JING_EXPORT_SYMBOL const char *jing_version(void);

JING_EXPORT_SYMBOL int jing_major_version(void);

JING_EXPORT_SYMBOL int jing_minor_version(void);

JING_EXPORT_SYMBOL int jing_patch_version(void);

#endif