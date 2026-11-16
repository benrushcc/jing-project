#include "jing_rpmalloc.h"
#if defined(JING_USE_RPMALLOC) && JING_USE_RPMALLOC
#include <rpmalloc.h>
#include <stdint.h>
#include <limits.h>

#define ALLOC_INITIAL_CAPACITY 8

jing_rp_alloc_ctx* jing_rp_new_alloc_ctx(void) {
    jing_rp_alloc_ctx* r = rpmalloc(sizeof(jing_rp_alloc_ctx));
    if(JING_UNLIKELY(r == NULL)) {
        return NULL;
    }
    void** p = rpmalloc(ALLOC_INITIAL_CAPACITY * sizeof(void*));
    if(JING_UNLIKELY(p == NULL)) {
        return NULL;
    }
    r->ptrs = p;
    r->count = 0;
    r->capacity = ALLOC_INITIAL_CAPACITY;
    return r;
}

size_t jing_rp_max_align(void) {
    return 16;
}

void* jing_rp_alloc(jing_rp_alloc_ctx* ctx, size_t size) {
    void* r = rpmalloc(size);
    if(JING_UNLIKELY(r == NULL)) {
        return NULL;
    }
    void** ptrs     = ctx->ptrs;
    int    count    = ctx->count;
    int    capacity = ctx->capacity;
    if(count == capacity) {
        if(JING_UNLIKELY(capacity > INT_MAX / 2)) {
            return NULL;
        }
        capacity *= 2;
        void** new_ptrs = rprealloc(ptrs, (size_t)capacity * sizeof(void*));
        if(JING_UNLIKELY(new_ptrs == NULL)) {
            return NULL;
        }
        ptrs = new_ptrs;
        ctx->ptrs     = ptrs;
        ctx->capacity = capacity;
    }
    ptrs[count] = r;
    ctx->count  = count + 1;
    return r;
}

void* jing_rp_aligned_alloc(jing_rp_alloc_ctx* ctx, size_t size, size_t alignment) {
    void* p = rpaligned_alloc(alignment, size);
    if(JING_UNLIKELY(p == NULL)) {
        return NULL;
    }
    void** ptrs     = ctx->ptrs;
    int    count    = ctx->count;
    int    capacity = ctx->capacity;
    if(count == capacity) {
        if(JING_UNLIKELY(capacity > INT_MAX / 2)) {
            return NULL;
        }
        capacity *= 2;
        void** new_ptrs = rprealloc(ptrs, (size_t)capacity * sizeof(void*));
        if(JING_UNLIKELY(new_ptrs == NULL)) {
            return NULL;
        }
        ptrs = new_ptrs;
        ctx->ptrs     = ptrs;
        ctx->capacity = capacity;
    }
    ptrs[count] = p;
    ctx->count  = count + 1;
    return p;
}

void jing_rp_dealloc(jing_rp_alloc_ctx* ctx) {
    int count = ctx->count;
    for(int i = 0; i < count; i++) {
        rpfree(ctx->ptrs[i]);         
    }
    rpfree(ctx->ptrs);
    rpfree(ctx);
}
#endif