#ifndef JING_COMMON_H
#define JING_COMMON_H

#if defined(__cplusplus)
#if __cplusplus < 201103L
#error "Requires C++11 or later"
#endif
#else
#if !defined(__STDC_VERSION__) || __STDC_VERSION__ < 201112L
#error "Requires C11 or later"
#endif
#endif

#if defined(_WIN32)
#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN // Exclude rarely used APIs from <Windows.h> to
                            // speed up build and reduce namespace pollution
#endif
#endif

#if defined(_WIN32)
#if !defined(_MSC_VER) || defined(__clang__)
#error "Only Microsoft Visual C++ (cl.exe) is supported on Windows."
#endif
#endif

#if defined(_WIN32)
#define JING_OS_WINDOWS 1
#elif defined(__APPLE__) && defined(__MACH__)
#define JING_OS_MACOS 1
#elif defined(__linux__)
#define JING_OS_LINUX 1
#else
#error "Unknown compiler/platform"
#endif

#if defined(__cplusplus)
#include <cstddef>
#include <cstdint>
#include <cstring>
#else
#include <stddef.h>
#include <stdint.h>
#include <string.h>
#endif

static_assert(sizeof(int) == 4, "integer size mismatch");
static_assert(sizeof(size_t) == 8, "size_t size mismatch");
static_assert(sizeof(void *) == 8, "pointer size mismatch");

static const uintptr_t JING_PTR_ERR_FLAG = (1ULL << 63);

static inline void *jing_make_error_ptr(int err) {
  return (void *)((uintptr_t)err | JING_PTR_ERR_FLAG);
}

#define JING_UNUSED(x) (void)(x)

#if defined(__GNUC__) || defined(__clang__)
#define JING_LIKELY(x) __builtin_expect(!!(x), 1)
#define JING_UNLIKELY(x) __builtin_expect(!!(x), 0)
#else
#define JING_LIKELY(x) (x)
#define JING_UNLIKELY(x) (x)
#endif

#if defined(_WIN32)
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
  struct {
    int32_t err_code;
    int32_t err_flag;
  } err_val;
} jing_data;

typedef struct {
  size_t len;
  jing_data data;
} jing_result;

static_assert(sizeof(jing_result) == 16, "jing_result size mismatch");

#define JING_SYSTEM_ERROR_FLAG 0

static inline void jing_err_result(jing_result *r, int err) {
  memset(r, 0, sizeof(jing_result));
  r->len = 0;
  r->data.err_val.err_code = err;
  r->data.err_val.err_flag = JING_SYSTEM_ERROR_FLAG;
}

static inline void jing_err_result_with_flag(jing_result *r, int err,
                                             int flag) {
  memset(r, 0, sizeof(jing_result));
  r->len = 0;
  r->data.err_val.err_code = err;
  r->data.err_val.err_flag = flag;
}

static inline void jing_byte_result(jing_result *r, int8_t value) {
  memset(r, 0, sizeof(jing_result));
  r->len = SIZE_MAX;
  r->data.byte_val = value;
}

static inline void jing_short_result(jing_result *r, int16_t value) {
  memset(r, 0, sizeof(jing_result));
  r->len = SIZE_MAX;
  r->data.short_val = value;
}

static inline void jing_int_result(jing_result *r, int32_t value) {
  memset(r, 0, sizeof(jing_result));
  r->len = SIZE_MAX;
  r->data.int_val = value;
}

static inline void jing_long_result(jing_result *r, int64_t value) {
  memset(r, 0, sizeof(jing_result));
  r->len = SIZE_MAX;
  r->data.long_val = value;
}

static inline void jing_float_result(jing_result *r, float value) {
  memset(r, 0, sizeof(jing_result));
  r->len = SIZE_MAX;
  r->data.float_val = value;
}

static inline void jing_double_result(jing_result *r, double value) {
  memset(r, 0, sizeof(jing_result));
  r->len = SIZE_MAX;
  r->data.double_val = value;
}

static inline void jing_ptr_result(jing_result *r, void *value, size_t len) {
  memset(r, 0, sizeof(jing_result));
  r->len = len;
  r->data.ptr_val = value;
}

#endif