package io.jingproject.bindings;

import io.jingproject.ffm.Downcall;
import io.jingproject.ffm.FFM;

import java.lang.foreign.MemorySegment;

@FFM(libraryName = "jing")
public interface SysBindings {
    @Downcall(methodName = "jing_version_string", constant = true, critical = true)
    MemorySegment versionString();

    @Downcall(methodName = "jing_major_version", constant = true, critical = true)
    int majorVersion();

    @Downcall(methodName = "jing_minor_version", constant = true, critical = true)
    int minorVersion();

    @Downcall(methodName = "jing_patch_version", constant = true, critical = true)
    int patchVersion();

    @Downcall(methodName = "jing_ptr_err_flag", constant = true, critical = true)
    long ptrErrFlag();

    default int errPtr(MemorySegment segment) {
        long addr = segment.address();
        if ((addr & ptrErrFlag()) != 0L) {
            return (int) addr;
        }
        return 0;
    }

    @Downcall(methodName = "jing_max_align", constant = true, critical = true)
    long maxAlign();

    @Downcall(methodName = "jing_aligned_alloc", critical = true)
    MemorySegment alignedAlloc(long size, long alignment);

    @Downcall(methodName = "jing_aligned_free", critical = true)
    void alignedFree(MemorySegment segment);

    @Downcall(methodName = "jing_batch_free", critical = true)
    void batchFree(MemorySegment ptrs, long count, MemorySegment freeAddr);
}
