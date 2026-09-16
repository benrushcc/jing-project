package io.jingproject.bindings.alloc;

import io.jingproject.bindings.CommonBinding;
import io.jingproject.common.Os;
import io.jingproject.common.Utils;
import io.jingproject.common.anno.Fragile;
import io.jingproject.ffm.Libs;
import io.jingproject.ffm.NativeSegmentAccess;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;

// allocator backed by system malloc.
// tracks every allocated pointer in a contiguous array so close() can batch-free
// all of them in a single native call. aligned allocations are tagged with the
// lowest pointer bit so the batch free can pick the matching deallocator.
// not thread-safe: use one instance per thread or guard externally with a lock.
@Fragile
public final class MallocAllocator implements Allocator {
    private static final long MALLOC_ARRAY_DEFAULT_CAPACITY = 32L;
    private static final CommonBinding SYS_BINDINGS = Libs.impl(CommonBinding.class);

    static {
        try {
            Class<Os> _ = MethodHandles.lookup().ensureInitialized(Os.class);
        } catch (IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
        if(SYS_BINDINGS == null) {
            throw new ExceptionInInitializerError("cannot initialize SYS_BINDINGS");
        }
        if(SYS_BINDINGS.maxAlign() <= 1L) {
            throw new ExceptionInInitializerError("max alignment cannot be less than 1");
        }
    }

    private long addr = 0L;
    private long len = 0L;
    private long index = 0L;

    // allocates byteSize bytes aligned to byteAlignment. alignment within the
    // platform max goes through malloc; larger alignment uses a platform aligned
    // allocation, tagged with the lowest pointer bit for the batch free.
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
        }
        long r;
        if (byteAlignment <= SYS_BINDINGS.maxAlign()) {
            // Request at least byteAlignment bytes so malloc's guarantee covers the actual alignment.
            // Per N2293 (weak-alignment model), for sizes smaller than _Alignof(max_align_t),
            // malloc only needs to align to the largest power of two not exceeding the requested size.
            // https://www.open-std.org/jtc1/sc22/wg14/www/docs/n2293.htm
            r = Mem.malloc(Math.max(byteSize, byteAlignment));
        } else {
            // aligned_alloc size parameter requirements vary by platform:
            // - Windows: size need not be a multiple of alignment; pass byteSize as-is.
            // - Linux (glibc): requires glibc 2.38+ to allow size not a multiple of alignment;
            //   earlier versions require size to be a multiple of alignment.
            // - macOS: strictly requires size to be a multiple of alignment, otherwise returns NULL and sets errno=EINVAL.
            // Therefore, for non-Windows platforms, round size up to a multiple of alignment for cross-platform compatibility.
            r = SYS_BINDINGS.alignedAlloc(Os.current() == Os.WINDOWS ? byteSize : Utils.alignUp(byteSize, byteAlignment), byteAlignment);
        }
        if(r == 0L) {
            throw new OutOfMemoryError();
        }
        if(addr == 0L) {
            len = MALLOC_ARRAY_DEFAULT_CAPACITY;
            addr = Mem.malloc(MALLOC_ARRAY_DEFAULT_CAPACITY);
            if(addr == 0L) {
                Mem.free(r);
                throw new OutOfMemoryError();
            }
        } else if(index >= len) {
            long newLen = Math.addExact(len, len);
            long newAddr = Mem.realloc(addr, newLen);
            if(newAddr == 0L) {
                // realloc leaves the old tracking array intact on failure; keep
                // it so close() can still batch-free every tracked pointer.
                Mem.free(r);
                throw new OutOfMemoryError();
            }
            len = newLen;
            addr = newAddr;
        }
        // tag the lowest bit to mark an aligned allocation
        NativeSegmentAccess.setLong(MemorySegment.ofAddress(addr), index, byteAlignment <= SYS_BINDINGS.maxAlign() ? r : (1L | r));
        index += 8L;
        return NativeSegmentAccess.reinterpret(MemorySegment.ofAddress(r), byteSize);
    }

    // batch-frees every tracked pointer and nulls the tracking array.
    // the tag on aligned pointers makes batchFree use the right deallocator.
    @Override
    public void close() {
        if(addr == -1L) {
            throw new IllegalStateException("malloc allocator already closed");
        }
        if(index > 0L) {
            SYS_BINDINGS.batchFree(addr, index, Mem.freeFuncAddr());
        }
        addr = -1L;
    }
}
