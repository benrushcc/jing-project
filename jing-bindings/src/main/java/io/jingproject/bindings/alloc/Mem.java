package io.jingproject.bindings.alloc;

import io.jingproject.bindings.VmBindings;
import io.jingproject.ffm.NativeSegmentAccess;
import io.jingproject.ffm.SharedLibs;

import java.lang.foreign.MemorySegment;

public final class Mem {
    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     */
    private Mem() {
        throw new UnsupportedOperationException("utility class");
    }

    private static final VmBindings VM_BINDINGS = SharedLibs.getImpl(VmBindings.class);

    public static MemorySegment malloc(long byteSize) {
        assert byteSize > 0L;
        MemorySegment r = VM_BINDINGS.malloc(byteSize);
        if(r.address() == 0L) {
            throw new OutOfMemoryError();
        }
        return NativeSegmentAccess.reinterpret(r, byteSize);
    }

    public static MemorySegment realloc(MemorySegment segment, long newSize) {
        assert segment.isNative() && segment.address() != 0L && newSize > 0L;
        MemorySegment r = VM_BINDINGS.realloc(segment, newSize);
        if(r.address() == 0L) {
            free(segment);
            throw new OutOfMemoryError();
        }
        return NativeSegmentAccess.reinterpret(r, newSize);
    }

    public static void free(MemorySegment segment) {
        assert segment.isNative() && segment.address() != 0L && segment.byteSize() > 0L;
        VM_BINDINGS.free(segment);
    }

    public static int memcmp(MemorySegment dest, MemorySegment src, long size) {
        assert dest.isNative() && dest.address() != 0L && src.isNative() && src.address() != 0L && size > 0L && dest.byteSize() >= size && src.byteSize() >= size;
        return VM_BINDINGS.memcmp(dest, src, size);
    }

    public static void memcpy(MemorySegment dest, MemorySegment src, long size) {
        assert dest.isNative() && dest.address() != 0L && src.isNative() && src.address() != 0L && size > 0L && dest.byteSize() >= size && src.byteSize() >= size;
        assert dest.asOverlappingSlice(src).isEmpty();
        MemorySegment _ = VM_BINDINGS.memcpy(dest, src, size);
    }

    public static void memmove(MemorySegment dest, MemorySegment src, long size) {
        assert dest.isNative() && dest.address() != 0L && src.isNative() && src.address() != 0L && size > 0L && dest.byteSize() >= size && src.byteSize() >= size;
        MemorySegment _ = VM_BINDINGS.memmove(dest, src, size);
    }

    public static MemorySegment memchr(MemorySegment src, byte ch, long size) {
        assert src.isNative() && src.address() != 0L && size > 0L && src.byteSize() >= size;
        return VM_BINDINGS.memchr(src, Byte.toUnsignedInt(ch), size);
    }

    public static void memset(MemorySegment src, byte ch, long count) {
        assert src.isNative() && src.address() != 0L && count > 0L && src.byteSize() >= count;
        MemorySegment _  = VM_BINDINGS.memset(src, Byte.toUnsignedInt(ch), count);
    }
}
