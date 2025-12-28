package io.jingproject.bindings;

import io.jingproject.ffm.Downcall;
import io.jingproject.ffm.FFM;

import java.lang.foreign.MemorySegment;

@FFM
public interface VmBindings {
    @Downcall(methodName = "malloc", critical = true)
    MemorySegment malloc(long byteSize);

    @Downcall(methodName = "realloc", critical = true)
    MemorySegment realloc(MemorySegment m, long newByteSize);

    @Downcall(methodName = "free", critical = true)
    void free(MemorySegment m);

    @Downcall(methodName = "memcmp", critical = true)
    int memcmp(MemorySegment dest, MemorySegment src, long size);

    @Downcall(methodName = "memcpy", critical = true)
    MemorySegment memcpy(MemorySegment dest, MemorySegment src, long size);

    @Downcall(methodName = "memmove", critical = true)
    MemorySegment memmove(MemorySegment dest, MemorySegment src, long size);

    @Downcall(methodName = "memchr", critical = true)
    MemorySegment memchr(MemorySegment src, int ch, long size);

    @Downcall(methodName = "memset", critical = true)
    MemorySegment memset(MemorySegment src, int ch, long count);
}
