package io.jingproject.bindings.alloc;

import io.jingproject.bindings.VmBindings;
import io.jingproject.ffm.Libs;
import io.jingproject.ffm.NativeSegmentAccess;

import java.lang.foreign.MemorySegment;

public final class Mem {
    private static final VmBindings VM_BINDINGS = Libs.getImpl(VmBindings.class);

    static {
        if (VM_BINDINGS == null) {
            throw new ExceptionInInitializerError("cannot initialize vm bindings");
        }
    }

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     */
    private Mem() {
        throw new UnsupportedOperationException("utility class");
    }

    public static MemorySegment malloc(long byteSize) {

        MemorySegment r = VM_BINDINGS.malloc(byteSize);
        if (r.address() == 0L) {
            throw new OutOfMemoryError();
        }
        return NativeSegmentAccess.resize(r, byteSize);
    }

    public static MemorySegment realloc(MemorySegment segment, long newSize) {

        MemorySegment r = VM_BINDINGS.realloc(segment, newSize);
        if (r.address() == 0L) {
            free(segment);
            throw new OutOfMemoryError();
        }
        return NativeSegmentAccess.resize(r, newSize);
    }

    public static void free(MemorySegment segment) {

        VM_BINDINGS.free(segment);
    }

    public static int memcmp(MemorySegment dest, MemorySegment src, long size) {

        return VM_BINDINGS.memcmp(dest, src, size);
    }

    public static void memcpy(MemorySegment dest, MemorySegment src, long size) {


        MemorySegment _ = VM_BINDINGS.memcpy(dest, src, size);
    }

    public static void memmove(MemorySegment dest, MemorySegment src, long size) {

        MemorySegment _ = VM_BINDINGS.memmove(dest, src, size);
    }

    public static MemorySegment memchr(MemorySegment src, byte ch, long size) {

        return VM_BINDINGS.memchr(src, Byte.toUnsignedInt(ch), size);
    }

    public static void memset(MemorySegment src, byte ch, long count) {

        MemorySegment _ = VM_BINDINGS.memset(src, Byte.toUnsignedInt(ch), count);
    }
}
