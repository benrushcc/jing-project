package io.jingproject.bindingstest.test;

import io.jingproject.bindings.alloc.Allocator;
import io.jingproject.bindings.alloc.ArenaAllocator;
import io.jingproject.bindings.alloc.ArenaBase;
import io.jingproject.bindings.alloc.MallocAllocator;
import io.jingproject.ffm.ForeignException;
import io.jingproject.ffm.NativeSegmentAccess;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;

// verifies that the Allocator interface performs off-heap allocations through
// both of its implementations: MallocAllocator and ArenaAllocator; the native
// library is required, hence the require-native-library tag.
@Tag("require-native-library")
public class AllocTest {
    // arena size of 4 MiB, a multiple of the 64 KiB allocation granularity
    // used on Windows, so the reserved region never gets rounded up unexpectedly.
    private static final long ARENA_SIZE = 4L * 1024L * 1024L;

    @Test
    public void testMallocAllocator() {
        try (Allocator allocator = MallocAllocator.newInstance()) {
            MemorySegment segment = allocator.allocate(32L, 8L);
            Assertions.assertNotEquals(0L, segment.address());
            Assertions.assertEquals(32L, segment.byteSize());
            NativeSegmentAccess.setLong(segment, 0L, 0x0123456789abcdefL);
            Assertions.assertEquals(0x0123456789abcdefL, NativeSegmentAccess.getLong(segment, 0L));
            NativeSegmentAccess.setInt(segment, 24L, 42);
            Assertions.assertEquals(42, NativeSegmentAccess.getInt(segment, 24L));
        }
        try (Allocator allocator = MallocAllocator.newInstance()) {
            MemorySegment segment = allocator.allocate(4096L, 4096L);
            Assertions.assertEquals(0L, segment.address() % 4096L);
            NativeSegmentAccess.setLong(segment, 4096L - 8L, 0x0123456789abcdefL);
            Assertions.assertEquals(0x0123456789abcdefL, NativeSegmentAccess.getLong(segment, 4096L - 8L));
        }
        try (Allocator allocator = MallocAllocator.newInstance()) {
            for(int i = 0; i < 1000; i++) {
                MemorySegment segment = allocator.allocate(1L, 1L);
                Assertions.assertEquals(1L, segment.byteSize());
            }
        }
    }

    @Test
    public void testMallocAllocatorInvalidArguments() {
        try (Allocator allocator = MallocAllocator.newInstance()) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> allocator.allocate(0L, 8L));
            Assertions.assertThrows(IllegalArgumentException.class, () -> allocator.allocate(-1L, 8L));
            Assertions.assertThrows(IllegalArgumentException.class, () -> allocator.allocate(8L, 0L));
            Assertions.assertThrows(IllegalArgumentException.class, () -> allocator.allocate(8L, -1L));
            Assertions.assertThrows(IllegalArgumentException.class, () -> allocator.allocate(8L, 3L));
            Assertions.assertThrows(IllegalArgumentException.class, () -> allocator.allocate(8L, 6L));
        }
    }

    @Test
    public void testMallocAllocatorCloseSemantics() {
        Allocator allocator = MallocAllocator.newInstance();
        allocator.allocate(32L, 8L);
        allocator.close();
        Assertions.assertThrows(IllegalStateException.class, () -> allocator.allocate(32L, 8L));
        Assertions.assertThrows(IllegalStateException.class, allocator::close);

        Allocator allocator2 = MallocAllocator.newInstance();
        allocator2.close();
        Assertions.assertThrows(IllegalStateException.class, allocator2::close);
    }

    @Test
    public void testArenaAllocatorBasicAllocation() {
        try(ArenaBase arenaBase = ArenaBase.newInstance(ARENA_SIZE, 4, 4)) {
            try(Allocator allocator = new ArenaAllocator(arenaBase)) {
                MemorySegment segment = allocator.allocate(128L, 8L);
                Assertions.assertEquals(128L, segment.byteSize());
            }
            try(Allocator allocator = new ArenaAllocator(arenaBase)) {
                MemorySegment segment = allocator.allocate(4096L, 4096L);
                Assertions.assertEquals(0L, segment.address() % 4096L);
            }
            //noinspection EmptyTryBlock
            try(Allocator _ = new ArenaAllocator(arenaBase)) {
                // no op
            }
            try(Allocator allocator = new ArenaAllocator(arenaBase)) {
                for(int i = 0; i < 1000; i++) {
                    MemorySegment segment = allocator.allocate(1L, 1L);
                    Assertions.assertEquals(1L, segment.byteSize());
                }
            }
        }
    }

    @Test
    public void testArenaAllocatorCommitGrowth() {
        try (ArenaBase arenaBase = ArenaBase.newInstance(ARENA_SIZE, 4, 1)) {
            try (Allocator allocator = new ArenaAllocator(arenaBase)) {
                int count = 8;
                MemorySegment[] segments = new MemorySegment[count];
                for (int i = 0; i < count; i++) {
                    MemorySegment segment = allocator.allocate(64L * 1024L, 8L);
                    // writing into the tail of each block exercises the
                    // lazily grown committed region of the arena
                    NativeSegmentAccess.setLong(segment, 64L * 1024L - 8L, i);
                    segments[i] = segment;
                }
                for (int i = 0; i < count; i++) {
                    Assertions.assertEquals(i, NativeSegmentAccess.getLong(segments[i], 64L * 1024L - 8L));
                }
            }
        }
    }

    @Test
    public void testArenaAllocatorShrinkGrowth() {
        try (ArenaBase arenaBase = ArenaBase.newInstance(ARENA_SIZE, 1, 1)) {
            try(Allocator allocator = new ArenaAllocator(arenaBase)) {
                MemorySegment segment = allocator.allocate(64L * 1024L, 4096L);
                Assertions.assertEquals(0L, segment.address() % 4096L);
            }
            Assertions.assertTrue(arenaBase.used() >= 64L * 1024L);
            try(Allocator allocator = new ArenaAllocator(arenaBase)) {
                MemorySegment segment = allocator.allocate(1L, 1L);
                Assertions.assertEquals(1L, segment.byteSize());
            }
            Assertions.assertTrue(arenaBase.used() < 64L * 1024L);
        }
    }

    @Test
    public void testArenaAllocatorOutOfRange() {
        try (ArenaBase arenaBase = ArenaBase.newInstance(ARENA_SIZE, 4, 4)) {
            try (Allocator allocator = new ArenaAllocator(arenaBase)) {
                Assertions.assertThrows(ForeignException.class, () -> allocator.allocate(ARENA_SIZE * 2L, 8L));
            }
        }
    }

    @Test
    public void testArenaAllocatorInvalidArguments() {
        try (ArenaBase arenaBase = ArenaBase.newInstance(ARENA_SIZE, 4, 4)) {
            try (Allocator allocator = new ArenaAllocator(arenaBase)) {
                Assertions.assertThrows(IllegalArgumentException.class, () -> allocator.allocate(0L, 8L));
                Assertions.assertThrows(IllegalArgumentException.class, () -> allocator.allocate(-1L, 8L));
                Assertions.assertThrows(IllegalArgumentException.class, () -> allocator.allocate(8L, 3L));
            }
        }
    }
}