package io.jingproject.bindings.alloc;

import io.jingproject.bindings.SysWinBindings;
import io.jingproject.ffm.ForeignException;
import io.jingproject.ffm.NativeSegmentAccess;
import io.jingproject.ffm.SharedLibs;

import java.lang.foreign.MemorySegment;

/**
 * Windows-specific implementation of the {@link Mmap} interface.
 * <p>
 * This class provides memory mapping operations using Windows Virtual Memory API
 * (VirtualAlloc/VirtualFree) for Windows operating systems.
 * <p>
 * Windows memory management differs from POSIX systems in several aspects:
 * - Uses allocation granularity separate from page size
 * - Different flags and constants for memory operations
 * - Specific error handling patterns
 */
public final class WinMmap implements Mmap {
    /** Native bindings for Windows memory management APIs. */
    private static final SysWinBindings SYS_WIN_BINDINGS = SharedLibs.getImpl(SysWinBindings.class);

    /**
     * Returns the allocation granularity for Windows memory operations.
     * <p>
     * On Windows, this is typically 64KB, which is the minimum alignment
     * boundary for reserved virtual address space.
     *
     * @return the allocation granularity in bytes (typically 65536)
     */
    @Override
    public long granularity() {
        return SYS_WIN_BINDINGS.winAllocateGranularity();
    }

    /**
     * Returns the system page size for Windows.
     * <p>
     * This is the minimum unit for committing physical memory to reserved
     * virtual address space.
     *
     * @return the system page size in bytes
     */
    @Override
    public long pageSize() {
        return SYS_WIN_BINDINGS.winPageSize();
    }

    /**
     * Reserves a region of virtual address space on Windows.
     * <p>
     * Uses VirtualAlloc with MEM_RESERVE flag to reserve address space
     * without committing physical memory or disk backing.
     *
     * @param size the size of the region to reserve (should be aligned to granularity)
     * @return a MemorySegment representing the reserved virtual address space
     * @throws ForeignException if the reservation fails
     */
    @Override
    public MemorySegment reserve(long size) {
        int memReserve = SYS_WIN_BINDINGS.winMemReserve();
        int pageReadWrite = SYS_WIN_BINDINGS.winPageReadWrite();
        MemorySegment reserved = SYS_WIN_BINDINGS.winVirtualAlloc(MemorySegment.NULL, size, memReserve, pageReadWrite);
        if(NativeSegmentAccess.isErrPtr(reserved)) {
            int err = NativeSegmentAccess.errCode(reserved);
            throw new ForeignException("Failed to reserve memory, err : " + err);
        }
        return NativeSegmentAccess.reinterpret(reserved, size);
    }

    /**
     * Commits physical memory to a previously reserved region on Windows.
     * <p>
     * Uses VirtualAlloc with MEM_COMMIT flag to allocate physical memory
     * to the specified virtual address range.
     *
     * @param mem the MemorySegment to commit
     * @throws ForeignException if the commit operation fails
     */
    @Override
    public void commit(MemorySegment mem) {
        int memCommit = SYS_WIN_BINDINGS.winMemCommit();
        int pageReadWrite = SYS_WIN_BINDINGS.winPageReadWrite();
        MemorySegment committed = SYS_WIN_BINDINGS.winVirtualAlloc(mem, mem.byteSize(), memCommit, pageReadWrite);
        if(NativeSegmentAccess.isErrPtr(committed)) {
            int err = NativeSegmentAccess.errCode(committed);
            throw new ForeignException("Failed to commit memory, err : " + err);
        }
    }

    /**
     * Decommits physical memory from a region on Windows.
     * <p>
     * Uses VirtualFree with MEM_DECOMMIT flag to release physical memory
     * while keeping the virtual address space reserved.
     *
     * @param mem the MemorySegment to uncommit
     * @throws ForeignException if the decommit operation fails
     */
    @Override
    public void uncommit(MemorySegment mem) {
        int memDecommit = SYS_WIN_BINDINGS.winMemDecommit();
        int v = SYS_WIN_BINDINGS.winVirtualFree(mem, mem.byteSize(), memDecommit);
        if(v > 0) {
            throw new ForeignException("Failed to decommit memory, err : " + v);
        }
    }

    /**
     * Releases both physical memory and virtual address space reservation on Windows.
     * <p>
     * Uses VirtualFree with MEM_RELEASE flag to completely free the memory region.
     * After this operation, the MemorySegment becomes invalid.
     *
     * @param mem the MemorySegment to release
     * @throws ForeignException if the release operation fails
     */
    @Override
    public void release(MemorySegment mem) {
        int memRelease = SYS_WIN_BINDINGS.winMemRelease();
        int v = SYS_WIN_BINDINGS.winVirtualFree(mem, mem.byteSize(), memRelease);
        if(v > 0) {
            throw new ForeignException("Failed to release memory, err : " + v);
        }
    }
}
