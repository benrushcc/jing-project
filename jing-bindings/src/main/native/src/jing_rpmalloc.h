#ifndef JING_RPMALLOC_H
#define JING_RPMALLOC_H

#include "jing_common.h"
#if defined(JING_USE_RPMALLOC) && JING_USE_RPMALLOC
typedef struct {
    void**  ptrs;
    int     count;
    int     capacity;
} jing_rp_alloc_ctx;

JING_EXPORT_SYMBOL jing_rp_alloc_ctx* jing_rp_new_alloc_ctx(void);

JING_EXPORT_SYMBOL size_t jing_rp_max_align(void);

JING_EXPORT_SYMBOL void*  jing_rp_alloc(jing_rp_alloc_ctx* ctx, size_t size);

JING_EXPORT_SYMBOL void*  jing_rp_aligned_alloc(jing_rp_alloc_ctx* ctx, size_t size, size_t alignment);

JING_EXPORT_SYMBOL void   jing_rp_dealloc(jing_rp_alloc_ctx* ctx);

#endif

#endif