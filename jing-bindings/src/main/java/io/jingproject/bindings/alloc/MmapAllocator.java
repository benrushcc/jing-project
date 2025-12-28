package io.jingproject.bindings.alloc;

import java.lang.foreign.MemorySegment;

/**
 * An implementation of {@link Allocator} using memory-mapped segments for allocation.
 * <p>
 * This allocator utilizes a {@link MmapSegment} to provide stack-like memory allocation
 * with efficient deallocation through index resetting. The allocation strategy follows
 * a linear progression within the memory-mapped region, similar to a bump pointer allocator.
 */
public final class MmapAllocator implements Allocator {
    /** Sentinel value indicating the allocator hasn't performed any allocation yet. */
    private static final int SCALE_GUARD = -2;

    /** Sentinel value indicating the allocator has been closed. */
    private static final int CLOSE_GUARD = -1;

    /** The underlying memory-mapped segment used for allocations. */
    private final MmapSegment mmapSegment;

    /**
     * Tracks the allocation state:
     * - {@code SCALE_GUARD} (-2): No allocations made yet (initial state)
     * - Non-negative value: Current allocation index within the mmap segment
     * - {@code CLOSE_GUARD} (-1): Allocator has been closed
     */
    private int scaleIndex = SCALE_GUARD;

    /**
     * Creates a new MmapAllocator backed by the specified memory-mapped segment.
     *
     * @param mmapSegment the underlying memory-mapped segment to allocate from
     */
    public MmapAllocator(MmapSegment mmapSegment) {
        this.mmapSegment = mmapSegment;
    }

    /**
     * Allocates memory with the specified size and alignment from the mmap segment.
     * <p>
     * On the first allocation, this method captures the current write index
     * of the mmap segment using {@link MmapSegment#scale()}. Subsequent allocations
     * continue from where the previous allocation left off.
     * <p>
     * The allocation follows a stack-like pattern where memory is carved out
     * sequentially from the underlying segment.
     *
     * @param byteSize the size of memory to allocate in bytes
     * @param byteAlignment the alignment requirement (must be a power of two)
     * @return a slice of the memory-mapped segment
     * @throws IllegalStateException if the allocator has been closed
     * @throws io.jingproject.ffm.ForeignException if mmap operations failed
     */
    @Override
    public MemorySegment allocate(long byteSize, long byteAlignment) {
        if(scaleIndex == CLOSE_GUARD) {
            throw new IllegalStateException("MmapAllocator already closed");
        }
        if(scaleIndex == SCALE_GUARD) {
            scaleIndex = mmapSegment.scale();
        }
        return mmapSegment.slice(byteSize, byteAlignment);
    }

    /**
     * Closes the allocator and resets the allocation state.
     * <p>
     * When closed, if any allocations were made, the mmap segment's write index
     * is reset to the position saved during the first allocation using
     * {@link MmapSegment#unScale(int)}. This effectively deallocates all
     * memory allocated through this allocator in a single operation.
     *
     * @throws IllegalStateException if the allocator has already been closed
     */
    @Override
    public void close() {
        if(scaleIndex == CLOSE_GUARD) {
            throw new IllegalStateException("MmapAllocator already closed");
        }
        if(scaleIndex != SCALE_GUARD) {
            mmapSegment.unScale(scaleIndex);
            scaleIndex = CLOSE_GUARD;
        }
    }
}
