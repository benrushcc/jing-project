package io.jingproject.bindings;

import io.jingproject.ffm.Downcall;
import io.jingproject.ffm.FFM;

@FFM(libraryName = "jing_bindings")
public interface StdBinding {
    @Downcall(methodName = "jing_malloc", critical = true)
    long malloc(long size);

    @Downcall(methodName = "jing_realloc", critical = true)
    long realloc(long addr, long newSize);

    @Downcall(methodName = "jing_free", critical = true)
    void free(long addr);

    @Downcall(methodName = "jing_memcmp", critical = true)
    int memcmp(long dest, long src, long size);

    @Downcall(methodName = "jing_memcpy", critical = true)
    long memcpy(long dest, long src, long size);

    @Downcall(methodName = "jing_memmove", critical = true)
    long memmove(long dest, long src, long size);

    @Downcall(methodName = "jing_memchr", critical = true)
    long memchr(long src, int ch, long size);

    @Downcall(methodName = "jing_memset", critical = true)
    long memset(long src, int ch, long count);
}
