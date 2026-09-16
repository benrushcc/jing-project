package io.jingproject.bindings.alloc;

import java.lang.foreign.SegmentAllocator;

// a sealed allocator interface for the jing project's memory management.
//
// it targets single-threaded allocation patterns and is cheaper than the
// JDK arena allocator. when an mmap arena is bound to MMAP_SCOPE, allocations
// go through ArenaAllocator; otherwise they use MallocAllocator.
//
// implementations are restricted to MallocAllocator and ArenaAllocator
// for controlled extension and type safety.
public sealed interface Allocator extends SegmentAllocator, AutoCloseable permits MallocAllocator, ArenaAllocator {
    // scoped value holding a thread-local mmap arena that newAlloc picks up.
    ScopedValue<ArenaBase> MMAP_SCOPE = ScopedValue.newInstance();

    // returns an ArenaAllocator over the arena bound to MMAP_SCOPE,
    // or a MallocAllocator when no arena is bound.
    static Allocator newAlloc() {
        if (MMAP_SCOPE.isBound()) {
            return new ArenaAllocator(MMAP_SCOPE.get());
        }
        return new MallocAllocator();
    }

    @Override
    void close();
}
