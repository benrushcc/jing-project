package io.jingproject.bindings.alloc;

import io.jingproject.bindings.VmBindings;
import io.jingproject.common.anno.Fragile;
import io.jingproject.ffm.Libs;

// provides wrappers around C standard library memory functions.
@Fragile
public final class Mem {
    private static final VmBindings VM_BINDINGS = Libs.impl(VmBindings.class);
    // function pointer for C's malloc()
    private static final long MALLOC_FUNC_ADDR = Libs.addrFromVM("malloc").address();
    // function pointer for C's free()
    private static final long FREE_FUNC_ADDR = Libs.addrFromVM("free").address();

    static {
        if (VM_BINDINGS == null) {
            throw new ExceptionInInitializerError("cannot initialize vm bindings");
        }
        if(MALLOC_FUNC_ADDR == 0L) {
            throw new ExceptionInInitializerError("cannot initialize malloc function pointer");
        }
        if(FREE_FUNC_ADDR == 0L) {
            throw new ExceptionInInitializerError("cannot initialize free function pointer");
        }
    }

    private Mem() {
        throw new UnsupportedOperationException("utility class");
    }

    // returns the function pointer for C's malloc()
    public static long mallocFuncAddr() {
        return MALLOC_FUNC_ADDR;
    }

    // returns the function pointer for C's free()
    public static long freeFuncAddr() {
        return FREE_FUNC_ADDR;
    }

    // corresponds to C's malloc()
    public static long malloc(long size) {
        return VM_BINDINGS.malloc(size);
    }

    // corresponds to C's realloc()
    public static long realloc(long addr, long newSize) {
        return VM_BINDINGS.realloc(addr, newSize);
    }

    // corresponds to C's free()
    public static void free(long addr) {
        VM_BINDINGS.free(addr);
    }

    // corresponds to C's memcmp()
    public static int memcmp(long dest, long src, long size) {
        return VM_BINDINGS.memcmp(dest, src, size);
    }

    // corresponds to C's memcpy()
    public static long memcpy(long dest, long src, long size) {
        return VM_BINDINGS.memcpy(dest, src, size);
    }

    // corresponds to C's memmove()
    public static long memmove(long dest, long src, long size) {
        return VM_BINDINGS.memmove(dest, src, size);
    }

    // corresponds to C's memchr()
    public static long memchr(long src, byte b, long size) {
        int i = b & 0xFF;
        return VM_BINDINGS.memchr(src, i, size);
    }

    // corresponds to C's memset()
    public static long memset(long src, byte ch, long count) {
        return VM_BINDINGS.memset(src, Byte.toUnsignedInt(ch), count);
    }
}
