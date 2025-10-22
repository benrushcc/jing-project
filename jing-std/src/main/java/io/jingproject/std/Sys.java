package io.jingproject.std;

import io.jingproject.ffm.Downcall;
import io.jingproject.ffm.FFM;

@FFM(libraryName = "jing")
public interface Sys {
    @Downcall(methodName = "jing_allocate_granularity", constant = true, critical = true)
    long jingAllocateGranularity();

    @Downcall(methodName = "jing_mmap")
    void jingMmap(long size, long result);

    @Downcall(methodName = "jing_munmap")
    void jingMunmap(long addr, long size);
}
