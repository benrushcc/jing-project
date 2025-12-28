package io.jingproject.bindings.alloc;

import io.jingproject.common.Os;

import java.lang.foreign.MemorySegment;

/**
 * Platform-independent interface for memory-mapped file operations.
 * Provides methods for reserving, committing, and managing memory-mapped regions
 * with OS-specific implementations.
 * <p>
 * The implementation supports different memory mapping strategies based on the
 * underlying operating system:
 * - Windows: Uses {@link WinMmap} with Windows-specific memory APIs
 * - Linux/MacOS: Uses {@link PosixMmap} with POSIX mmap API
 * <p>
 * This interface abstracts OS-specific details while providing consistent
 * memory mapping semantics across platforms.
 */
public sealed interface Mmap permits WinMmap, PosixMmap {

    /**
     * Returns the allocation granularity for memory reservations.
     * <p>
     * On Windows: This represents the minimum alignment boundary for reserved
     * virtual address space (typically 64KB). Windows requires memory reservations
     * to be aligned to this granularity.
     * <p>
     * On POSIX systems (Linux/MacOS): This returns the same value as {@link #pageSize()},
     * as POSIX mmap has no separate granularity concept.
     *
     * @return the allocation granularity in bytes
     */
    long granularity();

    /**
     * Returns the system page size for the current platform.
     * This is the minimum unit of memory that can be independently managed
     * by the operating system's memory management and is used for commit operations.
     *
     * @return the system page size in bytes
     */
    long pageSize();

    /**
     * Reserves a region of virtual address space of the specified size.
     * The reserved region is not backed by physical memory or disk storage initially.
     * <p>
     * The actual reserved size will be aligned up to the system's granularity
     * (on Windows) or page size (on POSIX).
     *
     * @param size the desired size of the region in bytes
     * @return a {@link MemorySegment} representing the reserved virtual address space
     * @throws io.jingproject.ffm.ForeignException if the reservation fails (e.g., insufficient address space)
     */
    MemorySegment reserve(long size);

    /**
     * Commits physical memory to a portion of a previously reserved region.
     * The commit operation starts from the MemorySegment's base address and affects the entire
     * byteSize of the segment.
     * <p>
     * After this call, the specified portion of memory can be safely accessed without causing
     * page faults (for physical memory).
     *
     * @param mem the {@link MemorySegment} to commit. The operation affects the entire segment
     *            from its address to address + byteSize.
     * @throws io.jingproject.ffm.ForeignException if the commit operation fails
     */
    void commit(MemorySegment mem);

    /**
     * Uncommits (deallocates) physical memory or disk-backed storage from a portion of a region,
     * while keeping the virtual address space reserved. The operation starts from the
     * MemorySegment's base address and affects the entire byteSize of the segment.
     * The region becomes inaccessible until {@link #commit(MemorySegment)} is called again.
     *
     * @param mem the {@link MemorySegment} to uncommit. The operation affects the entire segment
     *            from its address to address + byteSize.
     * @throws io.jingproject.ffm.ForeignException if the uncommit operation fails
     */
    void uncommit(MemorySegment mem);

    /**
     * Releases both the physical memory/disk backing and the virtual address space
     * reservation for the entire MemorySegment. The operation affects the entire segment
     * from its base address to address + byteSize.
     * <p>
     * After this call, the {@link MemorySegment} becomes invalid and should not be used.
     *
     * @param mem the {@link MemorySegment} to release. The operation affects the entire segment
     *            from its address to address + byteSize.
     * @throws io.jingproject.ffm.ForeignException if the release operation fails
     */
    void release(MemorySegment mem);

    /**
     * Returns the platform-specific singleton instance of {@link Mmap}.
     * The implementation is selected based on the current operating system:
     * - Windows: {@link WinMmap} (uses VirtualAlloc/VirtualFree API)
     * - Linux/MacOS: {@link PosixMmap} (uses mmap/munmap API)
     * <p>
     * @return the platform-specific {@link Mmap} instance
     */
    static Mmap getInstance() {
        class Holder {
            static final Mmap INSTANCE = createMmapInstance();

            static Mmap createMmapInstance() {
                return switch (Os.current()) {
                    case WINDOWS -> new WinMmap();
                    case LINUX, MACOS -> new PosixMmap();
                };
            }
        }
        return Holder.INSTANCE;
    }
}
