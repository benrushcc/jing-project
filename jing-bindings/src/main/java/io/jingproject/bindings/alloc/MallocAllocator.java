package io.jingproject.bindings.alloc;

import io.jingproject.bindings.PosixBindings;
import io.jingproject.bindings.WinBindings;
import io.jingproject.common.Os;
import io.jingproject.common.anno.Fragile;
import io.jingproject.ffm.Libs;
import io.jingproject.ffm.NativeSegmentAccess;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;

// per-thread malloc-backed allocator.
//
// every allocation is recorded in a dynamically growing tracking array.
// on close(), a single batch-free call releases all tracked payloads and
// the array itself. alignment requests below the platform threshold go
// through plain malloc (the fast path); requests above it use the
// platform-specific aligned allocator (_aligned_malloc on windows,
// posix_memalign on linux/macos). on windows, aligned allocations are
// tagged with bit 1 so batch-free can dispatch between _aligned_free
// and free. posix_memalign results are free()-safe, so no tag is needed

// not thread-safe: one instance must be used by a single thread at a time
@Fragile
public abstract sealed class MallocAllocator implements Allocator permits MallocAllocator.WinMallocAllocator, MallocAllocator.PosixMallocAllocator {
    private static final long MALLOC_ARRAY_DEFAULT_CAPACITY = 4 * Long.BYTES;

    static {
        try {
            Class<Os> _ = MethodHandles.lookup().ensureInitialized(Os.class);
        } catch (IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static MallocAllocator newInstance() {
        if(Os.current() == Os.WINDOWS) {
            return new WinMallocAllocator();
        }
        return new PosixMallocAllocator();
    }

    protected long addr = 0L;
    protected long len = 0L;
    protected long index = 0L;

    public value record Alloc(long actual, long stored) {

    }

    @Override
    public MemorySegment allocate(long byteSize, long byteAlignment) {
        if(byteSize <= 0L) {
            throw new IllegalArgumentException("byteSize must be positive : " + byteSize);
        }
        if(byteAlignment <= 0L || (byteAlignment & (byteAlignment - 1L)) != 0L) {
            throw new IllegalArgumentException("byteAlignment must be a positive power of two : " + byteAlignment);
        }
        if(addr == -1L) {
            throw new IllegalStateException("malloc allocator already closed");
        } else if(addr == 0L) {
            len = MALLOC_ARRAY_DEFAULT_CAPACITY;
            addr = Mem.malloc(MALLOC_ARRAY_DEFAULT_CAPACITY);
            if(addr == 0L) {
                throw new OutOfMemoryError();
            }
        } else if(index >= len) {
            long newLen = Math.addExact(len, len);
            long newAddr = Mem.realloc(addr, newLen);
            if(newAddr == 0L) {
                throw new OutOfMemoryError();
            }
            len = newLen;
            addr = newAddr;
        }
        Alloc alloc = doAllocate(byteSize, byteAlignment);
        NativeSegmentAccess.setLong(MemorySegment.ofAddress(addr), index, alloc.stored());
        index += Long.BYTES;
        return NativeSegmentAccess.reinterpret(MemorySegment.ofAddress(alloc.actual()), byteSize);
    }

    protected abstract Alloc doAllocate(long byteSize, long byteAlignment);

    protected abstract void doFree(long addr, long index);

    @Override
    public void close() {
        if(addr == -1L) {
            throw new IllegalStateException("malloc allocator already closed");
        }
        if(index > 0L) {
            doFree(addr, index);
        }
        addr = -1L;
    }

    static final class WinMallocAllocator extends MallocAllocator {
        private static final WinBindings WIN_BINDINGS = Libs.impl(WinBindings.class);

        static {
            if(WIN_BINDINGS == null) {
                throw new ExceptionInInitializerError("cannot initialize WIN_BINDINGS");
            }
            if(WIN_BINDINGS.winMaxAlign() < Long.BYTES) {
                throw new ExceptionInInitializerError("max alignment must be at least : " + Long.BYTES);
            }
        }

        @Override
        protected Alloc doAllocate(long byteSize, long byteAlignment) {
            if(byteAlignment <= WIN_BINDINGS.winMaxAlign()) {
                long r = Mem.malloc(Math.max(byteSize, byteAlignment));
                if(r == 0L) {
                    throw new OutOfMemoryError("failed to invoke malloc");
                }
                return new Alloc(r, r);
            }
            long r = WIN_BINDINGS.winAlignedAlloc(byteSize, byteAlignment);
            if(NativeSegmentAccess.isErrPtr(r)) {
                throw new OutOfMemoryError("failed to invoke _aligned_malloc, err : " + NativeSegmentAccess.errCode(r));
            }
            return new Alloc(r, 1L | r);
        }

        @Override
        protected void doFree(long addr, long index) {
            WIN_BINDINGS.winBatchFree(addr, index, Mem.freeFuncAddr());
        }
    }

    static final class PosixMallocAllocator extends MallocAllocator {
        private static final PosixBindings POSIX_BINDINGS = Libs.impl(PosixBindings.class);

        static {
            if(POSIX_BINDINGS == null) {
                throw new ExceptionInInitializerError("cannot initialize POSIX_BINDINGS");
            }
            if(POSIX_BINDINGS.posixMaxAlign() < Long.BYTES) {
                throw new ExceptionInInitializerError("max alignment must be at least : " + Long.BYTES);
            }
        }

        @Override
        protected Alloc doAllocate(long byteSize, long byteAlignment) {
            long r;
            if(byteAlignment <= POSIX_BINDINGS.posixMaxAlign()) {
                r = Mem.malloc(Math.max(byteSize, byteAlignment));
                if(r == 0L) {
                    throw new OutOfMemoryError("failed to invoke malloc");
                }
            } else {
                r = POSIX_BINDINGS.posixMemAlign(byteAlignment, byteSize);
                if(NativeSegmentAccess.isErrPtr(r)) {
                    throw new OutOfMemoryError("failed to invoke posix_memalign, err : " + NativeSegmentAccess.errCode(r));
                }
            }
            return new Alloc(r, r);
        }

        @Override
        protected void doFree(long addr, long index) {
            POSIX_BINDINGS.posixBatchFree(addr, index, Mem.freeFuncAddr());
        }
    }
}
