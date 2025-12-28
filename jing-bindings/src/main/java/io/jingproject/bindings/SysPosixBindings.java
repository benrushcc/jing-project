package io.jingproject.bindings;

import io.jingproject.ffm.Downcall;

import java.lang.foreign.MemorySegment;

public interface SysPosixBindings {
    @Downcall(methodName = "jing_posix_page_size", constant = true, critical = true)
    long posixPageSize();

    @Downcall(methodName = "jing_posix_prot_read", constant = true, critical = true)
    int posixProtRead();

    @Downcall(methodName = "jing_posix_prot_write", constant = true, critical = true)
    int posixProtWrite();

    @Downcall(methodName = "jing_posix_prot_none", constant = true, critical = true)
    int posixProtNone();

    @Downcall(methodName = "jing_posix_madv_dontneed", constant = true, critical = true)
    int posixMadvDontNeed();

    @Downcall(methodName = "jing_posix_map_private", constant = true, critical = true)
    int posixMapPrivate();

    @Downcall(methodName = "jing_posix_map_anonymous", constant = true, critical = true)
    int posixMapAnonymous();

    @Downcall(methodName = "jing_posix_mmap", critical = true)
    MemorySegment posixMmap(MemorySegment addr, long size, int prot, int flags, int fd, long offset);

    @Downcall(methodName = "jing_posix_mprotect", critical = true)
    int posixMprotect(MemorySegment addr, long size, int prot);

    @Downcall(methodName = "jing_posix_madvise", critical = true)
    int posixMadvise(MemorySegment addr, long size, int advice);

    @Downcall(methodName = "jing_posix_munmap", critical = true)
    int posixMunmap(MemorySegment addr, long size);
}
