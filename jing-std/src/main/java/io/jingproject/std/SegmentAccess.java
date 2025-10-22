package io.jingproject.std;

import io.jingproject.ffm.ForeignException;
import io.jingproject.ffm.SharedLibs;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.ByteOrder;

public final class SegmentAccess {
    private SegmentAccess() {
        throw new UnsupportedOperationException("utility class");
    }

    /**
     *   Address is unsigned, so we can not represent raw address larger than Long.MAX_VALUE,
     *   However, user-land address space is usually 48-bit on most operating system, so we are all good here.
     */
    private static final MemorySegment ZERO = MemorySegment.ofAddress(0L).reinterpret(Long.MAX_VALUE);

    private static final ByteOrder OPPOSITE_BYTE_ORDER = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;

    private static final ValueLayout.OfShort JAVA_SHORT_UNALIGNED_OPPOSITE = ValueLayout.JAVA_SHORT_UNALIGNED.withOrder(OPPOSITE_BYTE_ORDER);
    private static final ValueLayout.OfChar JAVA_CHAR_UNALIGNED_OPPOSITE = ValueLayout.JAVA_CHAR_UNALIGNED.withOrder(OPPOSITE_BYTE_ORDER);
    private static final ValueLayout.OfInt JAVA_INT_UNALIGNED_OPPOSITE = ValueLayout.JAVA_INT_UNALIGNED.withOrder(OPPOSITE_BYTE_ORDER);
    private static final ValueLayout.OfLong JAVA_LONG_UNALIGNED_OPPOSITE = ValueLayout.JAVA_LONG_UNALIGNED.withOrder(OPPOSITE_BYTE_ORDER);
    private static final ValueLayout.OfFloat JAVA_FLOAT_UNALIGNED_OPPOSITE = ValueLayout.JAVA_FLOAT_UNALIGNED.withOrder(OPPOSITE_BYTE_ORDER);
    private static final ValueLayout.OfDouble JAVA_DOUBLE_UNALIGNED_OPPOSITE = ValueLayout.JAVA_DOUBLE_UNALIGNED.withOrder(OPPOSITE_BYTE_ORDER);

    public static byte getByte(long address) {
        return ZERO.get(ValueLayout.JAVA_BYTE, address);
    }

    public static void setByte(long address, byte b) {
        ZERO.set(ValueLayout.JAVA_BYTE, address, b);
    }

    public static short getShort(long address) {
        return getShort(address, ByteOrder.nativeOrder());
    }

    public static void setShort(long address, short s) {
        setShort(address, s, ByteOrder.nativeOrder());
    }

    public static short getShort(long address, ByteOrder order) {
        if(order == ByteOrder.nativeOrder()) {
            return ZERO.get(ValueLayout.JAVA_SHORT_UNALIGNED, address);
        } else {
            return ZERO.get(JAVA_SHORT_UNALIGNED_OPPOSITE, address);
        }
    }

    public static void setShort(long address, short s, ByteOrder order) {
        if (order == ByteOrder.nativeOrder()) {
            ZERO.set(ValueLayout.JAVA_SHORT_UNALIGNED, address, s);
        } else {
            ZERO.set(JAVA_SHORT_UNALIGNED_OPPOSITE, address, s);
        }
    }

    public static char getChar(long address) {
        return getChar(address, ByteOrder.nativeOrder());
    }

    public static void setChar(long address, char c) {
        setChar(address, c, ByteOrder.nativeOrder());
    }

    public static char getChar(long address, ByteOrder order) {
        if (order == ByteOrder.nativeOrder()) {
            return ZERO.get(ValueLayout.JAVA_CHAR_UNALIGNED, address);
        } else {
            return ZERO.get(JAVA_CHAR_UNALIGNED_OPPOSITE, address);
        }
    }

    public static void setChar(long address, char c, ByteOrder order) {
        if (order == ByteOrder.nativeOrder()) {
            ZERO.set(ValueLayout.JAVA_CHAR_UNALIGNED, address, c);
        } else {
            ZERO.set(JAVA_CHAR_UNALIGNED_OPPOSITE, address, c);
        }
    }

    public static int getInt(long address) {
        return getInt(address, ByteOrder.nativeOrder());
    }

    public static void setInt(long address, int i) {
        setInt(address, i, ByteOrder.nativeOrder());
    }

    public static int getInt(long address, ByteOrder order) {
        if (order == ByteOrder.nativeOrder()) {
            return ZERO.get(ValueLayout.JAVA_INT_UNALIGNED, address);
        } else {
            return ZERO.get(JAVA_INT_UNALIGNED_OPPOSITE, address);
        }
    }

    public static void setInt(long address, int i, ByteOrder order) {
        if (order == ByteOrder.nativeOrder()) {
            ZERO.set(ValueLayout.JAVA_INT_UNALIGNED, address, i);
        } else {
            ZERO.set(JAVA_INT_UNALIGNED_OPPOSITE, address, i);
        }
    }

    public static long getLong(long address) {
        return getLong(address, ByteOrder.nativeOrder());
    }

    public static void setLong(long address, long l) {
        setLong(address, l, ByteOrder.nativeOrder());
    }

    public static long getLong(long address, ByteOrder order) {
        if (order == ByteOrder.nativeOrder()) {
            return ZERO.get(ValueLayout.JAVA_LONG_UNALIGNED, address);
        } else {
            return ZERO.get(JAVA_LONG_UNALIGNED_OPPOSITE, address);
        }
    }

    public static void setLong(long address, long l, ByteOrder order) {
        if (order == ByteOrder.nativeOrder()) {
            ZERO.set(ValueLayout.JAVA_LONG_UNALIGNED, address, l);
        } else {
            ZERO.set(JAVA_LONG_UNALIGNED_OPPOSITE, address, l);
        }
    }

    public static float getFloat(long address) {
        return getFloat(address, ByteOrder.nativeOrder());
    }

    public static void setFloat(long address, float f) {
        setFloat(address, f, ByteOrder.nativeOrder());
    }

    public static float getFloat(long address, ByteOrder order) {
        if (order == ByteOrder.nativeOrder()) {
            return ZERO.get(ValueLayout.JAVA_FLOAT_UNALIGNED, address);
        } else {
            return ZERO.get(JAVA_FLOAT_UNALIGNED_OPPOSITE, address);
        }
    }

    public static void setFloat(long address, float f, ByteOrder order) {
        if (order == ByteOrder.nativeOrder()) {
            ZERO.set(ValueLayout.JAVA_FLOAT_UNALIGNED, address, f);
        } else {
            ZERO.set(JAVA_FLOAT_UNALIGNED_OPPOSITE, address, f);
        }
    }

    public static double getDouble(long address) {
        return getDouble(address, ByteOrder.nativeOrder());
    }

    public static void setDouble(long address, double d) {
        setDouble(address, d, ByteOrder.nativeOrder());
    }

    public static double getDouble(long address, ByteOrder order) {
        if (order == ByteOrder.nativeOrder()) {
            return ZERO.get(ValueLayout.JAVA_DOUBLE_UNALIGNED, address);
        } else {
            return ZERO.get(JAVA_DOUBLE_UNALIGNED_OPPOSITE, address);
        }
    }

    public static void setDouble(long address, double d, ByteOrder order) {
        if (order == ByteOrder.nativeOrder()) {
            ZERO.set(ValueLayout.JAVA_DOUBLE_UNALIGNED, address, d);
        } else {
            ZERO.set(JAVA_DOUBLE_UNALIGNED_OPPOSITE, address, d);
        }
    }

    // memory related methods

    public static long malloc(long byteSize) {
        class MallocHolder {
            static final MethodHandle MH =
                    SharedLibs.getMethodHandleFromVM("malloc", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG));
        }
        try {
            long memoryAddress = (long) MallocHolder.MH.invokeExact(byteSize);
            if(memoryAddress == 0L) {
                throw new OutOfMemoryError();
            }
            return memoryAddress;
        } catch (Throwable t) {
            throw new ForeignException("Failed to invoke malloc method", t);
        }
    }

    public static long realloc(long segmentAddress, long newByteSize) {
        class ReallocHolder {
            static final MethodHandle MH =
                    SharedLibs.getMethodHandleFromVM("realloc", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG));
        }
        try {
             long memoryAddress = (long) ReallocHolder.MH.invokeExact(segmentAddress, newByteSize);
            if(memoryAddress == 0L) {
                throw new OutOfMemoryError();
            }
            return memoryAddress;
        } catch (Throwable t) {
            throw new ForeignException("Failed to invoke malloc method", t);
        }
    }

    public static void free(long segmentAddress) {
        class FreeHolder {
            static final MethodHandle MH =
                    SharedLibs.getMethodHandleFromVM("free", FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG));
        }
        try {
            FreeHolder.MH.invokeExact(segmentAddress);
        } catch (Throwable t) {
            throw new ForeignException("Failed to invoke free method", t);
        }
    }

    public static void memcpy(long destAddress, long srcAddress, long size) {
        class MemcpyHolder {
            static final MethodHandle MH =
                    SharedLibs.getMethodHandleFromVM("memcpy", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG));
        }
        try {
            long _ = (long) MemcpyHolder.MH.invokeExact(destAddress, srcAddress, size);
        } catch (Throwable t) {
            throw new ForeignException("Failed to invoke memcpy method", t);
        }
    }

    public static void memmove(long destAddress, long srcAddress, long size) {
        class MemmoveHolder {
            static final MethodHandle MH =
                    SharedLibs.getMethodHandleFromVM("memmove", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG));
        }
        try {
            long _ = (long) MemmoveHolder.MH.invokeExact(destAddress, srcAddress, size);
        } catch (Throwable t) {
            throw new ForeignException("Failed to invoke memmove method", t);
        }
    }

    public static long memchr(long srcAddress, int ch, long size) {
        class MemchrHolder {
            static final MethodHandle MH =
                    SharedLibs.getMethodHandleFromVM("memchr", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));
        }
        try {
            return (long) MemchrHolder.MH.invokeExact(srcAddress, ch, size);
        } catch (Throwable t) {
            throw new ForeignException("Failed to invoke memchr method", t);
        }
    }

    // jing_result related methods

    private static final MemoryLayout JING_DATA_LAYOUT = MemoryLayout.unionLayout(
            ValueLayout.JAVA_BYTE.withName("byte_val"),
            ValueLayout.JAVA_SHORT.withName("short_val"),
            ValueLayout.JAVA_CHAR.withName("char_val"),
            ValueLayout.JAVA_INT.withName("int_val"),
            ValueLayout.JAVA_LONG.withName("long_val"),
            ValueLayout.JAVA_FLOAT.withName("float_val"),
            ValueLayout.JAVA_DOUBLE.withName("double_val"),
            ValueLayout.ADDRESS.withName("ptr_val")
    );

    private static final MemoryLayout JING_RESULT_LAYOUT = MemoryLayout.structLayout(
            JING_DATA_LAYOUT.withName("data"),
            Linker.nativeLinker().canonicalLayouts().get("size_t").withName("len")
    );

    private static final long JING_LEN_OFFSET = JING_RESULT_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("len"));

    static {
        if(JING_DATA_LAYOUT.byteSize() != 8 || JING_RESULT_LAYOUT.byteSize() != 16) {
            throw new ExceptionInInitializerError("JING_RESULT_LAYOUT byteSize mismatch");
        }
    }

    public static long len(long r) {
        return ZERO.get(ValueLayout.JAVA_LONG_UNALIGNED, r + JING_LEN_OFFSET);
    }

}
