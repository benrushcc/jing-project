package io.jingproject.bindings;

import io.jingproject.ffm.Downcall;
import io.jingproject.ffm.FFM;

import java.lang.foreign.MemorySegment;

@FFM(libraryName = "jing")
public interface SysWinBindings {
    @Downcall(methodName = "jing_win_page_size", constant = true, critical = true)
    long winPageSize();

    @Downcall(methodName = "jing_win_allocate_granularity", constant = true, critical = true)
    long winAllocateGranularity();

    @Downcall(methodName = "jing_win_mem_reserve", constant = true, critical = true)
    int winMemReserve();

    @Downcall(methodName = "jing_win_mem_commit", constant = true, critical = true)
    int winMemCommit();

    @Downcall(methodName = "jing_win_mem_decommit", constant = true, critical = true)
    int winMemDecommit();

    @Downcall(methodName = "jing_win_mem_release", constant = true, critical = true)
    int winMemRelease();

    @Downcall(methodName = "jing_win_page_read_write", constant = true, critical = true)
    int winPageReadWrite();

    @Downcall(methodName = "jing_win_virtual_alloc", critical = true)
    MemorySegment winVirtualAlloc(MemorySegment addr, long size, int type, int prot);

    @Downcall(methodName = "jing_win_virtual_free", critical = true)
    int winVirtualFree(MemorySegment addr, long size, int type);
}
