package io.jingproject.bindings.alloc;

import io.jingproject.bindings.SysBindings;
import io.jingproject.ffm.NativeSegmentAccess;
import io.jingproject.ffm.SharedLibs;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * An implementation of {@link Allocator} using system malloc for memory allocation.
 * <p>
 * This allocator uses a contiguous array to track allocated pointers, enabling efficient
 * batch deallocation through native batch free operations. Compared to JDK's
 * {@code Arena.ofConfined} which uses linked-list management, this design provides
 * better cache locality and reduces allocation overhead.
 * <p>
 * For aligned allocations: on Linux/macOS uses C11's {@code aligned_alloc}, on Windows
 * uses MSVC's {@code _aligned_malloc}. The pointer's highest bit is used to mark
 * aligned allocations for proper deallocation.
 */
public final class MallocAllocator implements Allocator {
    /**
     * Initial length of the pointer tracking array (in number of pointers).
     * <p>
     * This value can be adjusted based on performance requirements and typical
     * allocation patterns. A larger value reduces array resizing frequency but
     * consumes more memory upfront.
     */
    private static final long MALLOC_ARRAY_DEFAULT_CAPACITY = 4L;

    /** System bindings for native memory operations. */
    private static final SysBindings SYS_BINDINGS = SharedLibs.getImpl(SysBindings.class);

    /**
     * Array tracking allocated pointers for batch deallocation.
     * <p>
     * State transitions:
     * - Initially: {@code MemorySegment.NULL} (no allocations yet)
     * - After first allocation: Becomes a valid malloc-allocated segment
     * - After {@link #close()}: Set to {@code null} (allocator closed)
     * <p>
     * Checking if this field is {@code null} can determine if the allocator
     * has been closed.
     */
    private MemorySegment addressArray = MemorySegment.NULL;

    /** Current index in the address array, measured in bytes. */
    private long addressIndex = 0L;

    /**
     * Allocates memory with the specified size and alignment.
     * <p>
     * Platform-specific behavior:
     * - For alignments ≤ maxAlign: uses standard malloc/free
     * - For larger alignments:
     *   • Linux/macOS: uses C11 aligned_alloc/free
     *   • Windows: uses _aligned_malloc/_aligned_free
     * <p>
     * The pointer's highest bit is set for aligned allocations, allowing
     * batchFree to distinguish between malloc and aligned_alloc pointers.
     *
     * @param byteSize the size of memory to allocate in bytes
     * @param byteAlignment the alignment requirement (must be a power of two)
     * @return the allocated memory segment
     * @throws IllegalStateException if the allocator has been closed
     * @throws OutOfMemoryError if allocation fails
     */
    @Override
    public MemorySegment allocate(long byteSize, long byteAlignment) {
        assert Long.bitCount(byteAlignment) == 1 && ValueLayout.JAVA_LONG.byteSize() == ValueLayout.ADDRESS.byteSize();
        if(addressArray == null) {
            throw new IllegalStateException("MallocBumper already closed");
        }
        MemorySegment ptr; long storedAddr;
        if(byteAlignment <= SYS_BINDINGS.maxAlign()) {
            ptr = Mem.malloc(byteSize);
            storedAddr = ptr.address();
        } else {
            MemorySegment p = SYS_BINDINGS.alignedAlloc(byteSize, byteAlignment);
            if(p.address() == 0L) {
                throw new OutOfMemoryError();
            }
            ptr = NativeSegmentAccess.reinterpret(p, byteSize);
            // Mark pointer's highest bit to indicate aligned allocation
            storedAddr = SYS_BINDINGS.ptrErrFlag() | ptr.address();
        }
        if(addressArray.address() == 0L) {
            addressArray = Mem.malloc(Math.multiplyExact(MALLOC_ARRAY_DEFAULT_CAPACITY, ValueLayout.JAVA_LONG.byteSize()));
        }
        if(addressIndex == addressArray.byteSize()) {
            long newSize = Math.multiplyExact(addressArray.byteSize(), 2L);
            addressArray = Mem.realloc(addressArray, newSize);
        }
        NativeSegmentAccess.setLong(addressArray, addressIndex, storedAddr);
        addressIndex = Math.addExact(addressIndex, ValueLayout.JAVA_LONG.byteSize());
        return ptr;
    }

    /**
     * Closes the allocator and releases all tracked memory.
     * <p>
     * Uses platform-specific batch free:
     * - Standard malloc pointers: freed normally
     * - Marked aligned pointers (highest bit set): platform-specific free function
     *   • Linux/macOS: standard free (aligned_alloc uses free)
     *   • Windows: _aligned_free (marked pointers need special handling)
     * <p>
     * After successful execution:
     * - {@code addressArray} is set to {@code null} indicating closure
     * - {@code addressIndex} is set to {@code Long.MIN_VALUE} as a sentinel value
     * - All tracked memory segments are freed
     *
     * @throws IllegalStateException if the allocator has already been closed
     */
    @Override
    public void close() {
        if(addressArray == null) {
            throw new IllegalStateException("MallocBumper already closed");
        }
        long count = Math.divideExact(addressIndex, ValueLayout.JAVA_LONG.byteSize());
        if(count > 0L) {
            SYS_BINDINGS.batchFree(addressArray, count);
            addressIndex = Long.MIN_VALUE;
        }
    }
}
