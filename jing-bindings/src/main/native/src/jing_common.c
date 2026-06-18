#include "jing_common.h"
#if defined(JING_OS_WINDOWS)
#include <malloc.h>
#endif

static const int MAJOR_VERSION     = 0;
static const int MINOR_VERSION     = 0;
static const int PATCH_VERSION     = 1;
static const char VERSION_STRING[] = "0.0.1";

const char* jing_version_string(void) {
	return VERSION_STRING;
}

int jing_major_version(void) {
	return MAJOR_VERSION;
}

int jing_minor_version(void) {
	return MINOR_VERSION;
}

int jing_patch_version(void) {
	return PATCH_VERSION;
}

uintptr_t jing_ptr_err_flag(void) {
	return JING_PTR_ERR_FLAG;
}

#if defined(JING_OS_WINDOWS)   // Fix MSVC align support
typedef struct {
	long long _max_align_ll;
	long double _max_align_ld;
} max_align_t;

#define alignof(t) __alignof(t)
#endif

size_t jing_max_align(void) {
	return alignof(max_align_t);
}

void* jing_aligned_alloc(size_t size, size_t alignment) {
#if defined(JING_OS_WINDOWS)
	return _aligned_malloc(size, alignment);
#else
	return aligned_alloc(alignment, size);
#endif
}

void jing_aligned_free(void* mem) {
#if defined(JING_OS_WINDOWS)
	_aligned_free(mem);
#else
	free(mem);
#endif
}

void jing_batch_free(void** ptrs, size_t count, void (*free_func_t)(void*)) {
	for (size_t i = 0; i < count; ++i) {
		uintptr_t addr = (uintptr_t) ptrs[i];
		if (addr & JING_PTR_ERR_FLAG) {
			addr &= ~(JING_PTR_ERR_FLAG);
			jing_aligned_free((void*) addr);
		} else {
			free_func_t((void*) addr);
		}
	}
	free(ptrs);
}