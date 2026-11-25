package io.jingproject.bindingstest;

import io.jingproject.bindings.alloc.Allocator;
import io.jingproject.bindings.alloc.ArenaBase;
import io.jingproject.ffm.ForeignException;
import io.jingproject.ffm.NativeSegmentAccess;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("require-native-library")
public class AllocTest {
    private static final long ARENA_SIZE = 4L * 1024L * 1024L;

    @Test
    public void testMallocAllocator() {
        try (Allocator allocator = Allocator.newAllocator()) {
            long addr = allocator.alignedAlloc(32L, 8L);
            Assertions.assertNotEquals(0L, addr);
            Assertions.assertEquals(0L, addr % 8L);
            NativeSegmentAccess.setLong(addr, 0L, 0x0123456789abcdefL);
            Assertions.assertEquals(0x0123456789abcdefL, NativeSegmentAccess.getLong(addr, 0L));
            NativeSegmentAccess.setInt(addr, 24L, 42);
            Assertions.assertEquals(42, NativeSegmentAccess.getInt(addr, 24L));
        }
        try (Allocator allocator = Allocator.newAllocator()) {
            long addr = allocator.alignedAlloc(4096L, 4096L);
            Assertions.assertEquals(0L, addr % 4096L);
            NativeSegmentAccess.setLong(addr, 4096L - 8L, 0x0123456789abcdefL);
            Assertions.assertEquals(0x0123456789abcdefL, NativeSegmentAccess.getLong(addr, 4096L - 8L));
        }
        try (Allocator allocator = Allocator.newAllocator()) {
            for (int i = 0; i < 1000; i++) {
                long addr = allocator.alloc(1L);
                Assertions.assertNotEquals(0L, addr);
            }
        }
    }

    @Test
    public void testMallocAllocatorDefaultAlignment() {
        try (Allocator allocator = Allocator.newAllocator()) {
            long maxAlign = allocator.maxAlign();
            for (int i = 0; i < 100; i++) {
                long addr = allocator.alloc(1L + i);
                Assertions.assertEquals(0L, addr % maxAlign);
            }
        }
    }

    @Test
    public void testArenaAllocatorBasicAllocation() {
        try (ArenaBase arenaBase = ArenaBase.newInstance(ARENA_SIZE, 4, 4)) {
            try (Allocator allocator = Allocator.newArenaAllocator(arenaBase)) {
                long addr = allocator.alignedAlloc(128L, 8L);
                Assertions.assertNotEquals(0L, addr);
                Assertions.assertEquals(0L, addr % 8L);
            }
            try (Allocator allocator = Allocator.newArenaAllocator(arenaBase)) {
                long addr = allocator.alignedAlloc(4096L, 4096L);
                Assertions.assertEquals(0L, addr % 4096L);
            }
            try (Allocator allocator = Allocator.newArenaAllocator(arenaBase)) {
                long maxAlign = allocator.maxAlign();
                for (int i = 0; i < 100; i++) {
                    long addr = allocator.alloc(1L + i);
                    Assertions.assertEquals(0L, addr % maxAlign);
                }
            }
            try (Allocator allocator = Allocator.newArenaAllocator(arenaBase)) {
                for (int i = 0; i < 1000; i++) {
                    long addr = allocator.alloc(1L);
                    Assertions.assertNotEquals(0L, addr);
                }
            }
        }
    }

    @Test
    public void testArenaAllocatorCommitGrowth() {
        try (ArenaBase arenaBase = ArenaBase.newInstance(ARENA_SIZE, 4, 1)) {
            try (Allocator allocator = Allocator.newArenaAllocator(arenaBase)) {
                int count = 8;
                long[] addrs = new long[count];
                for (int i = 0; i < count; i++) {
                    long addr = allocator.alignedAlloc(64L * 1024L, 8L);
                    NativeSegmentAccess.setLong(addr, 64L * 1024L - 8L, i);
                    addrs[i] = addr;
                }
                for (int i = 0; i < count; i++) {
                    Assertions.assertEquals(i, NativeSegmentAccess.getLong(addrs[i], 64L * 1024L - 8L));
                }
            }
        }
    }

    @Test
    public void testArenaAllocatorShrinkGrowth() {
        try (ArenaBase arenaBase = ArenaBase.newInstance(ARENA_SIZE, 1, 1)) {
            try (Allocator allocator = Allocator.newArenaAllocator(arenaBase)) {
                long addr = allocator.alignedAlloc(64L * 1024L, 4096L);
                Assertions.assertEquals(0L, addr % 4096L);
            }
            Assertions.assertTrue(arenaBase.used() >= 64L * 1024L);
            try (Allocator allocator = Allocator.newArenaAllocator(arenaBase)) {
                long addr = allocator.alloc(1L);
                Assertions.assertNotEquals(0L, addr);
            }
            Assertions.assertTrue(arenaBase.used() < 64L * 1024L);
        }
    }

    @Test
    public void testArenaAllocatorOutOfRange() {
        try (ArenaBase arenaBase = ArenaBase.newInstance(ARENA_SIZE, 4, 4)) {
            try (Allocator allocator = Allocator.newArenaAllocator(arenaBase)) {
                Assertions.assertThrows(ForeignException.class, () -> allocator.alignedAlloc(ARENA_SIZE * 2L, 8L));
            }
        }
    }
}