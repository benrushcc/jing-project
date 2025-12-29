#ifndef JING_SYS_H
#define JING_SYS_H

#include "jing_common.h"

JING_EXPORT_SYMBOL const char* jing_version_string(void);

JING_EXPORT_SYMBOL int jing_major_version(void);

JING_EXPORT_SYMBOL int jing_minor_version(void);

JING_EXPORT_SYMBOL int jing_patch_version(void);

JING_EXPORT_SYMBOL uintptr_t jing_ptr_err_flag(void);

JING_EXPORT_SYMBOL size_t jing_max_align(void);

JING_EXPORT_SYMBOL void* jing_aligned_alloc(size_t size, size_t alignment);

JING_EXPORT_SYMBOL void jing_aligned_free(void* mem);

JING_EXPORT_SYMBOL void jing_batch_free(void** ptrs, size_t count,
                                        void (*free_func_t)(void*));

#if defined(JING_OS_WINDOWS)
#include <windows.h>
JING_EXPORT_SYMBOL size_t jing_win_page_size(void);

JING_EXPORT_SYMBOL size_t jing_win_allocate_granularity(void);

JING_EXPORT_SYMBOL DWORD jing_win_mem_reserve(void);

JING_EXPORT_SYMBOL DWORD jing_win_mem_commit(void);

JING_EXPORT_SYMBOL DWORD jing_win_mem_decommit(void);

JING_EXPORT_SYMBOL DWORD jing_win_mem_release(void);

JING_EXPORT_SYMBOL DWORD jing_win_page_read_write(void);

JING_EXPORT_SYMBOL void* jing_win_virtual_alloc(void* addr, size_t size,
                                                DWORD type, DWORD prot);

JING_EXPORT_SYMBOL int jing_win_virtual_free(void* addr, size_t size,
                                             DWORD type);
#else
#include <sys/types.h>
JING_EXPORT_SYMBOL long jing_posix_page_size();

JING_EXPORT_SYMBOL int jing_posix_prot_read(void);

JING_EXPORT_SYMBOL int jing_posix_prot_write(void);

JING_EXPORT_SYMBOL int jing_posix_prot_none(void);

JING_EXPORT_SYMBOL int jing_posix_madv_dontneed(void);

JING_EXPORT_SYMBOL int jing_posix_map_private(void);

JING_EXPORT_SYMBOL int jing_posix_map_anonymous(void);

JING_EXPORT_SYMBOL void* jing_posix_mmap(void* addr, size_t size, int prot,
                                         int flags, int fd, off_t offset);

JING_EXPORT_SYMBOL int jing_posix_mprotect(void* addr, size_t size, int prot);

JING_EXPORT_SYMBOL int jing_posix_madvise(void* addr, size_t size, int advice);

JING_EXPORT_SYMBOL int jing_posix_munmap(void* addr, size_t size);

#endif

#endif