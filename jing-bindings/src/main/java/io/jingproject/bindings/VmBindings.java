package io.jingproject.bindings;

import io.jingproject.ffm.Downcall;
import io.jingproject.ffm.FFM;

@FFM
public interface VmBindings {
    @Downcall(methodName = "malloc", critical = true)
    long malloc(long size);

    @Downcall(methodName = "realloc", critical = true)
    long realloc(long addr, long newSize);

    @Downcall(methodName = "free", critical = true)
    void free(long addr);

    @Downcall(methodName = "memcmp", critical = true)
    int memcmp(long dest, long src, long size);

    @Downcall(methodName = "memcpy", critical = true)
    long memcpy(long dest, long src, long size);

    @Downcall(methodName = "memmove", critical = true)
    long memmove(long dest, long src, long size);

    @Downcall(methodName = "memchr", critical = true)
    long memchr(long src, int ch, long size);

    @Downcall(methodName = "memset", critical = true)
    long memset(long src, int ch, long count);
}
