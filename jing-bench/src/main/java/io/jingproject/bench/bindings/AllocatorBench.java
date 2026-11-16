package io.jingproject.bench.bindings;

import io.jingproject.bench.AbstractBench;
import io.jingproject.bindings.alloc.Allocator;
import io.jingproject.bindings.alloc.ArenaBase;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Warmup(iterations = 1, time = 2000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 1, time = 4000, timeUnit = TimeUnit.MILLISECONDS)
public class AllocatorBench extends AbstractBench {
    private static final int ROUNDS = 10;
    private static final int ALLOCATIONS_PER_ROUND = 1024;
    private static final int OPERATIONS = ROUNDS * ALLOCATIONS_PER_ROUND;
    private static final long ARENA_SIZE = 16L * 1024L * 1024L;

    private int[] sizes;

    @Setup(Level.Trial)
    public void setup() {
        sizes = new int[ALLOCATIONS_PER_ROUND];
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < ALLOCATIONS_PER_ROUND; i++) {
            sizes[i] = random.nextInt(16, 1024);
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        sizes = null;
    }

    @Benchmark
    @OperationsPerInvocation(OPERATIONS)
    public void mallocAlloc(Blackhole blackhole) {
        for (int r = 0; r < ROUNDS; r++) {
            try (Allocator allocator = Allocator.newAllocator()) {
                for (int i = 0; i < ALLOCATIONS_PER_ROUND; i++) {
                    long addr = allocator.alloc(sizes[i]);
                    blackhole.consume(addr);
                }
            }
        }
    }

    @Benchmark
    @OperationsPerInvocation(OPERATIONS)
    @Fork(value = FORK_COUNT, jvmArgsAppend = {
            "-Djing.bindings.alloc.type=rpmalloc"
    })
    public void rpAlloc(Blackhole blackhole) {
        for (int r = 0; r < ROUNDS; r++) {
            try (Allocator allocator = Allocator.newAllocator()) {
                for (int i = 0; i < ALLOCATIONS_PER_ROUND; i++) {
                    long addr = allocator.alloc(sizes[i]);
                    blackhole.consume(addr);
                }
            }
        }
    }

    @Benchmark
    @OperationsPerInvocation(OPERATIONS)
    public void arenaAlloc(Blackhole blackhole) {
        try(ArenaBase arenaBase = ArenaBase.newInstance(ARENA_SIZE, 4, 4)) {
            ScopedValue.where(Allocator.MMAP_SCOPE, arenaBase).run(() -> {
                for (int r = 0; r < ROUNDS; r++) {
                    try (Allocator allocator = Allocator.newAllocator()) {
                        for (int i = 0; i < ALLOCATIONS_PER_ROUND; i++) {
                            long addr = allocator.alloc(sizes[i]);
                            blackhole.consume(addr);
                        }
                    }
                }
            });
        }
    }

    @Benchmark
    @OperationsPerInvocation(OPERATIONS)
    public void jdkArenaAlloc(Blackhole blackhole) {
        for (int r = 0; r < ROUNDS; r++) {
            try (Arena arena = Arena.ofConfined()) {
                for (int i = 0; i < ALLOCATIONS_PER_ROUND; i++) {
                    MemorySegment segment = arena.allocate(sizes[i]);
                    blackhole.consume(segment.address());
                }
            }
        }
    }
}