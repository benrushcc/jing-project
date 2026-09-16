package io.jingproject.bindings;

import io.jingproject.common.Os;
import io.jingproject.ffm.Downcall;
import io.jingproject.ffm.FFM;

@FFM(libraryName = "jing_bindings", supportedOS = {Os.LINUX, Os.MACOS})
public interface PosixBindings {
    @Downcall(methodName = "jing_posix_max_align", critical = true)
    long posixMaxAlign();

    @Downcall(methodName = "jing_posix_memalign", critical = true)
    long posixMemAlign(long alignment, long size);

    @Downcall(methodName = "jing_posix_batch_free")
    void posixBatchFree(long ptrs, long len, long freeAddr);

    @Downcall(methodName = "jing_posix_page_size", constant = true, critical = true)
    long posixPageSize();

    @Downcall(methodName = "jing_posix_prot_read", constant = true, critical = true)
    int posixProtRead();

    @Downcall(methodName = "jing_posix_prot_write", constant = true, critical = true)
    int posixProtWrite();

    @Downcall(methodName = "jing_posix_madv_free", constant = true, critical = true)
    int posixMadvFree();

    @Downcall(methodName = "jing_posix_map_private", constant = true, critical = true)
    int posixMapPrivate();

    @Downcall(methodName = "jing_posix_map_anonymous", constant = true, critical = true)
    int posixMapAnonymous();

    @Downcall(methodName = "jing_posix_mmap", critical = true)
    long posixMmap(long addr, long size, int prot, int flags, int fd, long offset);

    @Downcall(methodName = "jing_posix_mprotect", critical = true)
    int posixMprotect(long addr, long size, int prot);

    @Downcall(methodName = "jing_posix_madvise", critical = true)
    int posixMadvise(long addr, long size, int advice);

    @Downcall(methodName = "jing_posix_munmap", critical = true)
    int posixMunmap(long addr, long size);

    @Downcall(methodName = "jing_posix_close", critical = true)
    int posixClose(int fd);
}
