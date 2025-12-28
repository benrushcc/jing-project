#include "jing_sys.h"
#include "jing_common.h"
#include <stdlib.h>

#if defined(JING_OS_WINDOWS)
#include <memoryapi.h>
#include <windows.h>
#else
#include <errno.h>
#include <stdalign.h>
#include <sys/mman.h>
#include <unistd.h>
#endif

static const int MAJOR_VERSION = 0;
static const int MINOR_VERSION = 0;
static const int PATCH_VERSION = 1;
static const char VERSION_STRING[] = "0.0.1";

const char *jing_version_string(void) { return VERSION_STRING; }

int jing_major_version(void) { return MAJOR_VERSION; }

int jing_minor_version(void) { return MINOR_VERSION; }

int jing_patch_version(void) { return PATCH_VERSION; }

uintptr_t jing_ptr_err_flag(void) { return JING_PTR_ERR_FLAG; }

#if defined(JING_OS_WINDOWS) // Fix MSVC align support
typedef struct {
  long long _max_align_ll;
  long double _max_align_ld;
} max_align_t;

#define alignof(t) __alignof(t)
#endif

size_t jing_max_align(void) { return alignof(max_align_t); }

void *jing_aligned_alloc(size_t size, size_t alignment) {
#if defined(JING_OS_WINDOWS)
  return _aligned_malloc(size, alignment);
#else
  return aligned_alloc(alignment, size);
#endif
}

void jing_aligned_free(void *mem) {
#if defined(JING_OS_WINDOWS)
  _aligned_free(mem);
#else
  free(mem);
#endif
}

void jing_batch_free(void **ptrs, size_t count) {
  for (size_t i = 0; i < count; ++i) {
    uintptr_t addr = (uintptr_t)ptrs[i];
    if (addr & JING_PTR_ERR_FLAG) {
      addr &= ~(JING_PTR_ERR_FLAG);
      jing_aligned_free((void *)addr);
    } else {
      free((void *)addr);
    }
  }
  free(ptrs);
}

#if defined(JING_OS_WINDOWS)
size_t jing_win_page_size(void) {
  SYSTEM_INFO sys_info;
  memset(&sys_info, 0, sizeof(sys_info));
  GetSystemInfo(&sys_info);
  return sys_info.dwPageSize;
}

size_t jing_win_allocate_granularity(void) {
  SYSTEM_INFO sys_info;
  memset(&sys_info, 0, sizeof(sys_info));
  GetSystemInfo(&sys_info);
  return sys_info.dwAllocationGranularity;
}

DWORD
jing_win_mem_reserve(void) { return MEM_RESERVE; }

DWORD
jing_win_mem_commit(void) { return MEM_COMMIT; }

DWORD
jing_win_mem_decommit(void) { return MEM_DECOMMIT; }

DWORD
jing_win_mem_release(void) { return MEM_RELEASE; }

DWORD
jing_win_page_read_write(void) { return PAGE_READWRITE; }

void *jing_win_virtual_alloc(void *addr, size_t size, DWORD type, DWORD prot) {
  void *ptr = VirtualAlloc(addr, size, type, prot);
  if (JING_UNLIKELY(ptr == NULL)) {
    int err = GetLastError();
    return jing_make_error_ptr(err);
  } else {
    return ptr;
  }
}

int jing_win_virtual_free(void *addr, size_t size, DWORD type) {
  int v = VirtualFree(addr, size, type);
  if (JING_UNLIKELY(v == 0)) {
    int err = GetLastError();
    return err;
  } else {
    return 0;
  }
}

#else
#define JING_DEFAULT_PAGE_SIZE 4096
long jing_posix_page_size() {
  long v = sysconf(_SC_PAGESIZE);
  if (JING_UNLIKELY(v == -1)) {
    return JING_DEFAULT_PAGE_SIZE;
  } else {
    return v;
  }
}

int jing_posix_prot_read(void) { return PROT_READ; }

int jing_posix_prot_write(void) { return PROT_WRITE; }

int jing_posix_prot_none(void) { return PROT_NONE; }

int jing_posix_madv_dontneed(void) { return MADV_DONTNEED; }

int jing_posix_map_private(void) { return MAP_PRIVATE; }

int jing_posix_map_anonymous(void) { return MAP_ANONYMOUS; }

void *jing_posix_mmap(void *addr, size_t size, int prot, int flags, int fd,
                      off_t offset) {
  void *ptr = mmap(addr, size, prot, flags, fd, offset);
  if (JING_UNLIKELY(ptr == MAP_FAILED)) {
    int err = errno;
    return jing_make_error_ptr(err);
  } else {
    return ptr;
  }
}

int jing_posix_mprotect(void *addr, size_t size, int prot) {
  int v = mprotect(addr, size, prot);
  if (JING_UNLIKELY(v == -1)) {
    int err = errno;
    return err;
  } else {
    return 0;
  }
}

int jing_posix_madvise(void *addr, size_t size, int advice) {
  int v = madvise(addr, size, advice);
  if (JING_UNLIKELY(v == -1)) {
    int err = errno;
    return err;
  } else {
    return 0;
  }
}

int jing_posix_munmap(void *addr, size_t size) {
  int v = munmap(addr, size);
  if (JING_UNLIKELY(v == -1)) {
    int err = errno;
    return err;
  } else {
    return 0;
  }
}

#endif