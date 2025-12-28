package io.jingproject.bindings.alloc;

import io.jingproject.bindings.SysPosixBindings;
import io.jingproject.ffm.ForeignException;
import io.jingproject.ffm.NativeSegmentAccess;
import io.jingproject.ffm.SharedLibs;

import java.lang.foreign.MemorySegment;

/**
 * POSIX-compliant implementation of the {@link Mmap} interface for Linux and macOS.
 * <p>
 * This class provides memory mapping operations using POSIX mmap/munmap APIs
 * for Linux and macOS operating systems.
 * <p>
 * Key differences between Linux and macOS:
 * - Linux typically uses 4KB page size
 * - macOS typically uses 16KB page size
 * - Both follow similar POSIX semantics but with system-specific optimizations
 */
public final class PosixMmap implements Mmap {
    /** Native bindings for POSIX memory management APIs. */
    private static final SysPosixBindings SYS_POSIX_BINDINGS = SharedLibs.getImpl(SysPosixBindings.class);

    /**
     * Returns the allocation granularity for POSIX systems.
     * <p>
     * On POSIX systems (Linux/macOS), there is no separate granularity concept
     * like Windows. This method returns the same value as {@link #pageSize()}.
     *
     * @return the system page size in bytes
     */
    @Override
    public long granularity() {
        return SYS_POSIX_BINDINGS.posixPageSize();
    }

    /**
     * Returns the system page size for the current platform.
     * <p>
     * Typical values:
     * - Linux: Usually 4KB (4096 bytes)
     * - macOS: Usually 16KB (16384 bytes)
     *
     * @return the system page size in bytes
     */
    @Override
    public long pageSize() {
        return SYS_POSIX_BINDINGS.posixPageSize();
    }

    /**
     * Reserves a region of virtual address space using POSIX mmap.
     *
     * @param size the size of the region to reserve (should be aligned to page size)
     * @return a MemorySegment representing the reserved virtual address space
     * @throws ForeignException if the reservation fails
     */
    @Override
    public MemorySegment reserve(long size) {
        int protNone = SYS_POSIX_BINDINGS.posixProtNone();
        int mapPrivate = SYS_POSIX_BINDINGS.posixMapPrivate();
        int mapAnonymous = SYS_POSIX_BINDINGS.posixMapAnonymous();
        MemorySegment mem = SYS_POSIX_BINDINGS.posixMmap(MemorySegment.NULL, size, protNone, mapPrivate | mapAnonymous, -1, 0L);
        if(NativeSegmentAccess.isErrPtr(mem)) {
            int err = NativeSegmentAccess.errCode(mem);
            throw new ForeignException("Failed to reserve memory, err : " + err);
        }
        return NativeSegmentAccess.reinterpret(mem, size);
    }

    /**
     * Commits physical memory to a previously reserved region using mprotect.
     *
     * @param mem the MemorySegment obtained from {@link #reserve(long)}
     * @throws ForeignException if the commit operation fails
     */
    @Override
    public void commit(MemorySegment mem) {
        int protRead = SYS_POSIX_BINDINGS.posixProtRead();
        int protWrite = SYS_POSIX_BINDINGS.posixProtWrite();
        int v = SYS_POSIX_BINDINGS.posixMprotect(mem, mem.byteSize(), protRead | protWrite);
        if(v > 0) {
            throw new ForeignException("Failed to commit memory, err : " + v);
        }
    }

    /**
     * Decommits physical memory from a region on POSIX systems.
     * <p>
     * Performs two operations:
     * 1. Changes protection to PROT_NONE to prevent access
     * 2. Uses madvise with MADV_DONTNEED to return physical memory to OS
     * <p>
     * Note: MADV_DONTNEED behavior differs between Linux and macOS.
     * On Linux, it immediately returns memory to OS; on macOS, it's a hint.
     *
     * @param mem the MemorySegment to decommit
     * @throws ForeignException if the decommit operation fails
     */
    @Override
    public void uncommit(MemorySegment mem) {
        int protNone = SYS_POSIX_BINDINGS.posixProtNone();
        int v = SYS_POSIX_BINDINGS.posixMprotect(mem, mem.byteSize(), protNone);
        if(v > 0) {
            throw new ForeignException("Failed to uncommit memory, err : " + v);
        }
        int madvDontNeed = SYS_POSIX_BINDINGS.posixMadvDontNeed();
        v = SYS_POSIX_BINDINGS.posixMadvise(mem, mem.byteSize(), madvDontNeed);
        if(v  > 0) {
            throw new ForeignException("Failed to return memory to OS, err : " + v);
        }
    }

    /**
     * Releases both physical memory and virtual address space using munmap.
     * <p>
     * Completely unmaps the memory region, making the MemorySegment invalid.
     *
     * @param mem the MemorySegment to release
     * @throws ForeignException if the release operation fails
     */
    @Override
    public void release(MemorySegment mem) {
        int v = SYS_POSIX_BINDINGS.posixMunmap(mem, mem.byteSize());
        if(v > 0) {
            throw new ForeignException("Failed to release memory, err : " + v);
        }
    }
}
