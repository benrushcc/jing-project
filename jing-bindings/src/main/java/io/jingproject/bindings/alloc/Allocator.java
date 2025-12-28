package io.jingproject.bindings.alloc;

import java.lang.foreign.SegmentAllocator;

/**
 * A sealed allocator interface designed for the Jing project's memory management needs.
 * <p>
 * This allocator provides reasonable-performance memory allocation with reduced runtime checks
 * compared to JDK's built-in arena allocator. It is specifically designed for single-threaded
 * access patterns, making it suitable for thread-local memory management.
 * <p>
 * The design leverages thread-local storage through {@link ScopedValue} for mmap-backed
 * allocations, which is ideal for scenarios with frequent allocation and deallocation.
 * <p>
 * It is recommended to use mmap allocator in long-lived threads such as event loops,
 * where the allocator can be held for extended periods to maximize performance benefits.
 * <p>
 * Implementations are restricted to {@link MallocAllocator} and {@link MmapAllocator}
 * for controlled extension and type safety.
 */
public sealed interface Allocator extends SegmentAllocator, AutoCloseable permits MallocAllocator, MmapAllocator {
    /**
     * Scoped value for thread-local mmap segment storage.
     * <p>
     * When a mmap segment is bound to this scoped value, allocations will use
     * {@link MmapAllocator}. Otherwise, allocations default to {@link MallocAllocator}.
     */
    ScopedValue<MmapSegment> MMAP_SCOPE = ScopedValue.newInstance();

    /**
     * Creates a new allocator instance based on the current thread's context.
     * <p>
     * If the current thread has a {@link MmapSegment} bound to {@link #MMAP_SCOPE},
     * returns a {@link MmapAllocator} using that segment. Otherwise, returns a
     * {@link MallocAllocator} for heap-based allocations.
     * <p>
     * This factory method enables seamless switching between allocation strategies
     * based on the thread's execution context.
     *
     * @return an appropriate {@link Allocator} instance for the current context
     */
    static Allocator newInstance() {
        MmapSegment mmapSegment = MMAP_SCOPE.get();
        if (mmapSegment == null) {
            return new MallocAllocator();
        }
        return new MmapAllocator(mmapSegment);
    }
}
