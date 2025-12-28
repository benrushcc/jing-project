package io.jingproject.bindings.alloc;

import io.jingproject.ffm.ForeignException;
import io.jingproject.ffm.NativeSegmentAccess;

import java.lang.foreign.MemorySegment;
import java.time.Duration;
import java.util.Arrays;

/**
 * This class manages a reserved virtual address space and commits physical memory
 * on-demand as allocations are made. It implements smart decommitment strategies
 * to release unused physical memory back to the OS while keeping the virtual
 * address space reserved.
 * <p>
 * The allocation follows a bump-pointer strategy where memory is allocated
 * sequentially from the start of the segment. Scale/unScale operations allow
 * for checkpointing and rollback of the allocation state.
 */
public final class MmapSegment implements AutoCloseable {
    /**
     * Number of pages to decommit when conditions are met.
     * <p>
     * This value can be adjusted based on performance requirements:
     * - Larger values reduce system call frequency but increase memory waste
     * - Smaller values conserve memory but may cause more frequent system calls
     */
    private static final long MMAP_UNCOMMITTED_PAGE_COUNT = 32L;

    /**
     * Threshold for triggering decommitment.
     * <p>
     * When the difference between committed and current write address exceeds
     * this many pages, decommitment may occur.
     */
    private static final long MMAP_UNCOMMITTED_PAGE_THREHOLD = Math.multiplyExact(MMAP_UNCOMMITTED_PAGE_COUNT, 2);

    /**
     * Minimum time between decommit operations.
     * <p>
     * This value can be adjusted based on application patterns:
     * - Larger values reduce decommit frequency but may cause memory retention
     *   during scaling operations
     * - Smaller values allow more frequent memory return but increase system call overhead
     */
    private static final Duration MMAP_UNCOMMIT_TIME_THRESHOLD = Duration.ofMillis(200);

    /**
     * Default capacity for the scale array that tracks allocation checkpoints.
     * <p>
     * This value can be adjusted based on expected nesting depth:
     * - Larger values reduce array resizing frequency but increase memory overhead
     * - Smaller values conserve memory but may cause more frequent array copies
     */
    private static final int MMAP_SCALE_ARRAY_DEFAULT_CAPACITY = 4;

    /**
     * Platform-specific memory mapping implementation.
     */
    private static final Mmap MMAP = Mmap.getInstance();

    /** Total size of the reserved memory region (aligned to granularity). */
    private final long memSize;

    /**
     * The memory segment representing the reserved virtual address space.
     * <p>
     * State transitions:
     * - MemorySegment.NULL: Not yet initialized (no virtual address space reserved)
     * - Valid MemorySegment: Initialized and in use (virtual address space reserved)
     * - null: Closed/released (resources freed)
     * <p>
     * Checking if this field is null determines if the segment has been closed.
     */
    private MemorySegment mem = MemorySegment.NULL;

    /**
     * The address up to which physical memory has been committed.
     * <p>
     * This advances as allocations require more committed memory.
     */
    private long commitAddress = 0L;

    /**
     * The current write position (allocation pointer).
     * <p>
     * New allocations start from this address.
     */
    private long writeAddress = 0L;

    /**
     * Array tracking allocation checkpoints (scale positions).
     * <p>
     * Each entry stores a writeAddress value at the time of scale().
     */
    private long[] scaleArray = null;

    /** Current index in the scale array. */
    private int scaleArrayIndex = 0;

    /**
     * Timestamp of the last decommit operation.
     * <p>
     * Measured in JVM's monotonic nanoseconds (System.nanoTime()).
     * Used to enforce minimum intervals between decommit operations.
     */
    private long lastUncommitTime = 0L;

    /**
     * Creates a new MmapSegment with the specified size.
     * The actual reserved size will be aligned up to the system's granularity.
     *
     * @param size the desired size in bytes
     */
    public MmapSegment(long size) {
        this.memSize = align(size, MMAP.granularity());
    }

    /**
     * Ensures the memory segment is initialized.
     * <p>
     * If the segment hasn't been initialized yet, reserves the virtual address space
     * @throws IllegalStateException if mmapSegment has already been closed
     * @throws ForeignException if mmap reserve failed
     */
    private void checkInitialized() {
        if(mem == null) {
            throw new IllegalStateException("MmapSegment already closed");
        }
        if(mem.address() == 0L) {
            mem = MMAP.reserve(memSize);
            commitAddress = mem.address();
            writeAddress = mem.address();
            lastUncommitTime = System.nanoTime();
        }
    }

    /**
     * Creates a checkpoint of the current allocation state.
     * <p>
     * Saves the current write address to allow rollback via {@link #unScale(int)}.
     * This enables stack-like allocation patterns.
     *
     * @return an index that can be used to roll back to this checkpoint
     * @throws IllegalStateException if mmapSegment has already been closed
     */
    public int scale() {
        checkInitialized();
        if(scaleArray == null) {
            scaleArray = new long[MMAP_SCALE_ARRAY_DEFAULT_CAPACITY];
        }
        int currentScaleIndex = scaleArrayIndex;
        if(currentScaleIndex == scaleArray.length) {
            scaleArray = Arrays.copyOf(scaleArray, Math.multiplyExact(currentScaleIndex, 2));
        }
        scaleArray[currentScaleIndex] = writeAddress;
        scaleArrayIndex = Math.addExact(currentScaleIndex, 1);
        return currentScaleIndex;
    }

    /**
     * Rolls back the allocation state to a previously saved checkpoint.
     * <p>
     * Resets the write address to the value saved at the given index.
     * May trigger decommitment of unused physical memory if conditions are met:
     * 1. Sufficient unused committed pages (exceeds threshold)
     * 2. Enough time has passed since last decommit operation
     *
     * @param index the checkpoint index returned by {@link #scale()}
     * @throws IllegalStateException if the scale state is corrupted or invalid
     * @throws ForeignException if memory uncommitment fails
     */
    public void unScale(int index) {
        if(scaleArray == null) {
            throw new IllegalStateException("MmapSegment has never been scaled");
        }
        if(scaleArrayIndex != Math.addExact(index, 1)) {
            throw new IllegalStateException("Corrupted scale index");
        }
        long lastWriteAddress = scaleArray[index];
        // Check if decommitment is warranted
        // Note: Using direct subtraction for System.nanoTime() to handle overflow correctly
        if(Math.subtractExact(commitAddress, lastWriteAddress) > Math.multiplyExact(MMAP_UNCOMMITTED_PAGE_THREHOLD, MMAP.pageSize()) && (System.nanoTime() - lastUncommitTime) > MMAP_UNCOMMIT_TIME_THRESHOLD.toNanos()) {
            long uncommitSize = Math.multiplyExact(MMAP_UNCOMMITTED_PAGE_COUNT, MMAP.pageSize());
            long shrinkedCommitAddress = Math.subtractExact(commitAddress, uncommitSize);
            try {
                MMAP.uncommit(NativeSegmentAccess.reinterpret(MemorySegment.ofAddress(shrinkedCommitAddress), uncommitSize));
            } catch (ForeignException e) {
                close();
                throw e;
            }
            commitAddress = shrinkedCommitAddress;
        }
        writeAddress = lastWriteAddress;
    }

    /**
     * Aligns an address to the specified boundary.
     *
     * @param addr the address to align
     * @param byteAlignment the alignment boundary (must be power of two)
     * @return the aligned address
     */
    private static long align(long addr, long byteAlignment) {
        assert addr > 0L && Long.bitCount(byteAlignment) == 1;
        return (addr + byteAlignment - 1) & (-byteAlignment);
    }

    /**
     * Allocates a slice of memory from the segment.
     * <p>
     * The allocation is aligned to the specified boundary and may trigger
     * commitment of additional physical memory if needed.
     *
     * @param byteSize the size of the allocation in bytes
     * @param byteAlignment the alignment requirement (must be power of two)
     * @return a MemorySegment representing the allocated slice
     * @throws IndexOutOfBoundsException if the allocation exceeds segment bounds
     * @throws ForeignException if memory commitment fails
     */
    public MemorySegment slice(long byteSize, long byteAlignment) {
        assert byteSize > 0L && Long.bitCount(byteAlignment) == 1;
        checkInitialized();
        long alignedAddress = align(writeAddress, byteAlignment);
        long newWriteAddress = Math.addExact(alignedAddress, byteSize);
        if(newWriteAddress > memSize) {
            throw new IndexOutOfBoundsException("MmapSegment write out of bounds, index : " + newWriteAddress + ", size : " + memSize);
        }
        // Commit more memory if needed
        if(newWriteAddress > commitAddress) {
            long newCommitAddress = align(newWriteAddress, MMAP.pageSize());
            // NOTE: If the OS memory mapping granularity is a multiple of the page size (which is true on almost all systems),
            // this branch will never be triggered. It is kept here purely as a defensive safeguard.
            if(newCommitAddress > memSize) {
                throw new IndexOutOfBoundsException("MmapSegment commit out of bounds, index : " + newCommitAddress + ", size : " + memSize);
            }
            try {
                MMAP.commit(NativeSegmentAccess.reinterpret(MemorySegment.ofAddress(commitAddress), Math.subtractExact(newCommitAddress, commitAddress)));
            } catch (ForeignException e) {
                close();
                throw e;
            }
            commitAddress = newCommitAddress;
        }
        writeAddress = newWriteAddress;
        return NativeSegmentAccess.reinterpret(MemorySegment.ofAddress(alignedAddress), byteSize);
    }

    /**
     * Closes the segment and releases all associated resources.
     *
     * @throws IllegalStateException if the segment has already been closed
     * @throws ForeignException if mmap release failed
     */
    @Override
    public void close() {
        if(mem == null) {
            throw new IllegalStateException("MmapSegment already closed");
        }
        if(mem.address() == 0L) {
            return ;
        }
        MMAP.release(mem);
        mem = null;
        scaleArray = null;
    }
}
