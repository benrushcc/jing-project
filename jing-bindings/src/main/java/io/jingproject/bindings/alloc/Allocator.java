package io.jingproject.bindings.alloc;

import io.jingproject.bindings.PosixBindings;
import io.jingproject.bindings.RpmallocBindings;
import io.jingproject.bindings.WinBindings;
import io.jingproject.common.Os;
import io.jingproject.common.anno.Fragile;
import io.jingproject.ffm.Libs;
import io.jingproject.ffm.NativeSegmentAccess;

import java.lang.foreign.ValueLayout;

// a sealed allocator interface for the jing project's memory management.
@Fragile
public sealed interface Allocator extends AutoCloseable
        permits Allocator.WinMallocAllocator, Allocator.PosixMallocAllocator,
        Allocator.RpMallocAllocator, Allocator.ArenaAllocator {
    boolean USE_RPMALLOC = System.getProperty("jing.bindings.alloc.type", "").equals("rpmalloc");
    ScopedValue<ArenaBase> MMAP_SCOPE = ScopedValue.newInstance();

    static Allocator newAllocator() {
        if(MMAP_SCOPE.isBound()) {
            return new ArenaAllocator(MMAP_SCOPE.get());
        }
        if(USE_RPMALLOC) {
            return new RpMallocAllocator();
        }
        if(Os.current() == Os.WINDOWS) {
            return new WinMallocAllocator();
        }
        return new PosixMallocAllocator();
    }

    static Allocator newArenaAllocator(ArenaBase arenaBase) {
        return new ArenaAllocator(arenaBase);
    }

    long maxAlign();

    long alloc(long size);

    long alignedAlloc(long size, long alignment);

    @Override
    void close();

    final class WinMallocAllocator implements Allocator {
        private static final WinBindings WIN_BINDINGS = Libs.impl(WinBindings.class);
        private static final long PROCESS_HEAP;
        private static final long MAX_ALIGN;

        static {
            if(WIN_BINDINGS == null) {
                throw new ExceptionInInitializerError("cannot initialize windows system bindings");
            }
            MAX_ALIGN = WIN_BINDINGS.winMaxAlign();
            if(MAX_ALIGN < ValueLayout.ADDRESS.byteAlignment() || Long.bitCount(MAX_ALIGN) != 1) {
                throw new ExceptionInInitializerError("illegal alignment : " + MAX_ALIGN);
            }
            long processHeap = WIN_BINDINGS.winProcessHeap();
            if(NativeSegmentAccess.isErrPtr(processHeap)) {
                int err = NativeSegmentAccess.errCode(processHeap);
                throw new ExceptionInInitializerError("failed to fetch global process heap, err : " + err);
            }
            PROCESS_HEAP = processHeap;
        }

        private final long ctx = WIN_BINDINGS.winAllocCtx(PROCESS_HEAP);

        public WinMallocAllocator() {
            if(ctx == 0L) {
                throw new OutOfMemoryError("failed to create alloc ctx for windows system");
            }
        }

        @Override
        public long maxAlign() {
            return MAX_ALIGN;
        }

        @Override
        public long alloc(long size) {
            long r = WIN_BINDINGS.winAlloc(PROCESS_HEAP, ctx, size);
            if(r == 0L) {
                throw new OutOfMemoryError("failed to perform HeapAlloc on windows system");
            }
            return r;
        }

        @Override
        public long alignedAlloc(long size, long alignment) {
            long r = WIN_BINDINGS.winAlignedAlloc(PROCESS_HEAP, ctx, size, alignment);
            if(r == 0L) {
                throw new OutOfMemoryError("failed to perform _aligned_alloc on windows system");
            }
            return r;
        }

        @Override
        public void close() {
            WIN_BINDINGS.winDealloc(PROCESS_HEAP, ctx);
        }
    }

    final class PosixMallocAllocator implements Allocator {
        private static final PosixBindings POSIX_BINDINGS = Libs.impl(PosixBindings.class);
        private static final long MAX_ALIGN;

        static {
            if(POSIX_BINDINGS == null) {
                throw new ExceptionInInitializerError("cannot initialize posix system bindings");
            }
            MAX_ALIGN = POSIX_BINDINGS.posixMaxAlign();
            if(MAX_ALIGN < ValueLayout.ADDRESS.byteAlignment() || Long.bitCount(MAX_ALIGN) != 1) {
                throw new ExceptionInInitializerError("illegal alignment : " + MAX_ALIGN);
            }
        }

        private final long ctx = POSIX_BINDINGS.posixAllocCtx();

        public PosixMallocAllocator() {
            if(ctx == 0L) {
                throw new OutOfMemoryError("failed to create alloc ctx for posix system");
            }
        }

        @Override
        public long maxAlign() {
            return MAX_ALIGN;
        }

        @Override
        public long alloc(long size) {
            long r = POSIX_BINDINGS.posixAlloc(ctx, size);
            if(r == 0L) {
                throw new OutOfMemoryError("failed to perform malloc on posix system");
            }
            return r;
        }

        @Override
        public long alignedAlloc(long size, long alignment) {
            long r = POSIX_BINDINGS.posixMemAlign(ctx, size, alignment);
            if(r == 0L) {
                throw new OutOfMemoryError("failed to perform posix_memalign on posix system");
            }
            return r;
        }

        @Override
        public void close() {
            POSIX_BINDINGS.posixDealloc(ctx);
        }
    }

    final class RpMallocAllocator implements Allocator {
        private static final RpmallocBindings RPMALLOC_BINDINGS = Libs.impl(RpmallocBindings.class);
        private static final long MAX_ALIGN;

        static {
            if(RPMALLOC_BINDINGS == null) {
                throw new ExceptionInInitializerError("cannot initialize rpmalloc bindings");
            }
            MAX_ALIGN = RPMALLOC_BINDINGS.rpMaxAlign();
            if(MAX_ALIGN < ValueLayout.ADDRESS.byteAlignment() || Long.bitCount(MAX_ALIGN) != 1) {
                throw new ExceptionInInitializerError("illegal alignment : " + MAX_ALIGN);
            }
        }

        private final long ctx = RPMALLOC_BINDINGS.rpAllocCtx();

        public RpMallocAllocator() {
            if(ctx == 0L) {
                throw new OutOfMemoryError("failed to create alloc ctx for rpmalloc");
            }
        }

        @Override
        public long maxAlign() {
            return MAX_ALIGN;
        }

        @Override
        public long alloc(long size) {
            long r = RPMALLOC_BINDINGS.rpAlloc(ctx, size);
            if(r == 0L) {
                throw new OutOfMemoryError("failed to perform malloc on posix system");
            }
            return r;
        }

        @Override
        public long alignedAlloc(long size, long alignment) {
            long r = RPMALLOC_BINDINGS.rpAlignedAlloc(ctx, size, alignment);
            if(r == 0L) {
                throw new OutOfMemoryError("failed to perform posix_memalign on posix system");
            }
            return r;
        }

        @Override
        public void close() {
            RPMALLOC_BINDINGS.rpDealloc(ctx);
        }
    }

    final class ArenaAllocator implements Allocator {
        private static final long DEFAULT_ALIGN = 16;
        private static final long MAX_ALIGN;
        private final ArenaBase arenaBase;
        private final long pos;

        static {
            long requiredAlign = Long.getLong("jing.bindings.alloc.arena.alignment", DEFAULT_ALIGN);
            MAX_ALIGN = Math.max(requiredAlign, DEFAULT_ALIGN);
            if(MAX_ALIGN < ValueLayout.ADDRESS.byteAlignment() || Long.bitCount(MAX_ALIGN) != 1) {
                throw new ExceptionInInitializerError("illegal alignment : " + MAX_ALIGN);
            }
        }

        public ArenaAllocator(ArenaBase arenaBase) {
            this.arenaBase = arenaBase;
            this.pos = arenaBase.pos();
        }

        @Override
        public long maxAlign() {
            return MAX_ALIGN;
        }

        @Override
        public long alloc(long size) {
            return arenaBase.slice(size, 16);
        }

        @Override
        public long alignedAlloc(long size, long alignment) {
            return arenaBase.slice(size, alignment);
        }

        @Override
        public void close() {
            arenaBase.shrink(pos);
        }
    }
}
