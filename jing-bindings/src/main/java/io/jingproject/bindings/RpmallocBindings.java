package io.jingproject.bindings;

import io.jingproject.ffm.Downcall;
import io.jingproject.ffm.FFM;

@FFM(libraryName = "jing_bindings")
public interface RpmallocBindings {
    @Downcall(methodName = "jing_rp_new_alloc_ctx", critical = true)
    long rpAllocCtx();

    @Downcall(methodName = "jing_rp_max_align", critical = true)
    long rpMaxAlign();

    @Downcall(methodName = "jing_rp_alloc", critical = true)
    long rpAlloc(long ctx, long size);

    @Downcall(methodName = "jing_rp_aligned_alloc", critical = true)
    long rpAlignedAlloc(long ctx, long size, long alignment);

    @Downcall(methodName = "jing_rp_dealloc")
    void rpDealloc(long ctx);
}
