#include "sys.h"

#ifdef JING_OS_WINDOWS
#include <windows.h>
#else
#include <sys/mman.h>
#include <unistd.h>
#endif

size_t jing_allocate_granularity(void) {
#ifdef JING_OS_WINDOWS
  SYSTEM_INFO sys_info;
  GetSystemInfo(&sys_info);
  return sys_info.dwAllocationGranularity;
#else
  return sysconf(_SC_PAGESIZE);
#endif
}

void jing_mmap(size_t size, jing_result *r) {
#ifdef JING_OS_WINDOWS
  void *ptr =
      VirtualAlloc(NULL, size, MEM_RESERVE | MEM_COMMIT, PAGE_READWRITE);
  if (JING_UNLIKELY(ptr == NULL)) {
    jing_err_result(r, GetLastError());
  } else {
    jing_ptr_result(r, ptr, size);
  }
#else
  void *ptr =
      mmap(NULL, size, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANON, -1, 0);
  if (JING_UNLIKELY(ptr == MAP_FAILED)) {
    jing_err_result(r, errno);
  } else {
    jing_ptr_result(r, ptr, size);
  }
#endif
}

void jing_munmap(void *addr, size_t size, jing_result *r) {
#ifdef JING_OS_WINDOWS
  JING_UNUSED(size);
  int v = VirtualFree(addr, 0, MEM_RELEASE);
  if (JING_UNLIKELY(v == 0)) {
    jing_err_result(r, GetLastError());
  } else {
    jing_int_result(r, 0);
  }
#else
  int v = munmap(addr, size);
  if (JING_UNLIKELY(v == -1)) {
    jing_err_result(r, errno);
  } else {
    jing_int_result(r, 0);
  }
#endif
}

void jing_write() {}