package io.jingproject.ffm;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.ByteOrder;

/**
 *   Utility class for accessing native memorySegment and VM native functions
 */
public final class NativeSegmentAccess {

    private NativeSegmentAccess() {
        throw new UnsupportedOperationException("utility class");
    }

    private static final Boolean JING_REMOVE_BOUNDARY_CHECKING = Boolean.getBoolean("JING_REMOVE_BOUNDARY_CHECKING");

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
    private static final AddressLayout JAVA_ADDRESS_OPPOSITE = ValueLayout.ADDRESS_UNALIGNED.withOrder(OPPOSITE_BYTE_ORDER);

    public static byte getByte(MemorySegment segment, long offset) {
        assert segment.isNative();
        if(JING_REMOVE_BOUNDARY_CHECKING) {
            return ZERO.get(ValueLayout.JAVA_BYTE, Math.addExact(segment.address(), offset));
        } else {
            return segment.get(ValueLayout.JAVA_BYTE, offset);
        }
    }

    public static void setByte(MemorySegment segment, long offset, byte b) {
        assert segment.isNative();
        if(JING_REMOVE_BOUNDARY_CHECKING) {
            ZERO.set(ValueLayout.JAVA_BYTE, Math.addExact(segment.address(), offset), b);
        } else {
            segment.set(ValueLayout.JAVA_BYTE, offset, b);
        }
    }

    public static short getShort(MemorySegment segment, long offset) {
        assert segment.isNative();
        return getShort(segment, offset, ByteOrder.nativeOrder());
    }

    public static void setShort(MemorySegment segment, long offset, short s) {
        assert segment.isNative();
        setShort(segment, offset, s, ByteOrder.nativeOrder());
    }

    public static short getShort(MemorySegment segment, long offset, ByteOrder order) {
        assert segment.isNative();
        if(JING_REMOVE_BOUNDARY_CHECKING) {
            long address = Math.addExact(segment.address(), offset);
            if(order == ByteOrder.nativeOrder()) {
                return ZERO.get(ValueLayout.JAVA_SHORT_UNALIGNED, address);
            } else {
                return ZERO.get(JAVA_SHORT_UNALIGNED_OPPOSITE, address);
            }
        } else {
            if(order == ByteOrder.nativeOrder()) {
                return segment.get(ValueLayout.JAVA_SHORT_UNALIGNED, offset);
            } else {
                return segment.get(JAVA_SHORT_UNALIGNED_OPPOSITE, offset);
            }
        }
    }

    public static void setShort(MemorySegment segment, long offset, short s, ByteOrder order) {
        assert segment.isNative();
        if(JING_REMOVE_BOUNDARY_CHECKING) {
            long address = Math.addExact(segment.address(), offset);
            if (order == ByteOrder.nativeOrder()) {
                ZERO.set(ValueLayout.JAVA_SHORT_UNALIGNED, address, s);
            } else {
                ZERO.set(JAVA_SHORT_UNALIGNED_OPPOSITE, address, s);
            }
        } else {
            if (order == ByteOrder.nativeOrder()) {
                segment.set(ValueLayout.JAVA_SHORT_UNALIGNED, offset, s);
            } else {
                segment.set(JAVA_SHORT_UNALIGNED_OPPOSITE, offset, s);
            }
        }
    }

    public static char getChar(MemorySegment segment, long offset) {
        assert segment.isNative();
        return getChar(segment, offset, ByteOrder.nativeOrder());
    }

    public static void setChar(MemorySegment segment, long offset, char c) {
        assert segment.isNative();
        setChar(segment, offset, c, ByteOrder.nativeOrder());
    }

    public static char getChar(MemorySegment segment, long offset, ByteOrder order) {
        assert segment.isNative();
        if(JING_REMOVE_BOUNDARY_CHECKING) {
            long address = Math.addExact(segment.address(), offset);
            if(order == ByteOrder.nativeOrder()) {
                return ZERO.get(ValueLayout.JAVA_CHAR_UNALIGNED, address);
            } else {
                return ZERO.get(JAVA_CHAR_UNALIGNED_OPPOSITE, address);
            }
        } else {
            if(order == ByteOrder.nativeOrder()) {
                return segment.get(ValueLayout.JAVA_CHAR_UNALIGNED, offset);
            } else {
                return segment.get(JAVA_CHAR_UNALIGNED_OPPOSITE, offset);
            }
        }
    }

    public static void setChar(MemorySegment segment, long offset, char c, ByteOrder order) {
        assert segment.isNative();
        if(JING_REMOVE_BOUNDARY_CHECKING) {
            long address = Math.addExact(segment.address(), offset);
            if (order == ByteOrder.nativeOrder()) {
                ZERO.set(ValueLayout.JAVA_CHAR_UNALIGNED, address, c);
            } else {
                ZERO.set(JAVA_CHAR_UNALIGNED_OPPOSITE, address, c);
            }
        } else {
            if (order == ByteOrder.nativeOrder()) {
                segment.set(ValueLayout.JAVA_CHAR_UNALIGNED, offset, c);
            } else {
                segment.set(JAVA_CHAR_UNALIGNED_OPPOSITE, offset, c);
            }
        }
    }

    public static int getInt(MemorySegment segment, long offset) {
        assert segment.isNative();
        return getInt(segment, offset, ByteOrder.nativeOrder());
    }

    public static void setInt(MemorySegment segment, long offset, int i) {
        assert segment.isNative();
        setInt(segment, offset, i, ByteOrder.nativeOrder());
    }

    public static int getInt(MemorySegment segment, long offset, ByteOrder order) {
        assert segment.isNative();
        if(JING_REMOVE_BOUNDARY_CHECKING) {
            long address = Math.addExact(segment.address(), offset);
            if(order == ByteOrder.nativeOrder()) {
                return ZERO.get(ValueLayout.JAVA_INT_UNALIGNED, address);
            } else {
                return ZERO.get(JAVA_INT_UNALIGNED_OPPOSITE, address);
            }
        } else {
            if(order == ByteOrder.nativeOrder()) {
                return segment.get(ValueLayout.JAVA_INT_UNALIGNED, offset);
            } else {
                return segment.get(JAVA_INT_UNALIGNED_OPPOSITE, offset);
            }
        }
    }

    public static void setInt(MemorySegment segment, long offset, int i, ByteOrder order) {
        assert segment.isNative();
        if(JING_REMOVE_BOUNDARY_CHECKING) {
            long address = Math.addExact(segment.address(), offset);
            if (order == ByteOrder.nativeOrder()) {
                ZERO.set(ValueLayout.JAVA_INT_UNALIGNED, address, i);
            } else {
                ZERO.set(JAVA_INT_UNALIGNED_OPPOSITE, address, i);
            }
        } else {
            if (order == ByteOrder.nativeOrder()) {
                segment.set(ValueLayout.JAVA_INT_UNALIGNED, offset, i);
            } else {
                segment.set(JAVA_INT_UNALIGNED_OPPOSITE, offset, i);
            }
        }
    }

    public static long getLong(MemorySegment segment, long offset) {
        assert segment.isNative();
        return getLong(segment, offset, ByteOrder.nativeOrder());
    }

    public static void setLong(MemorySegment segment, long offset, long l) {
        assert segment.isNative();
        setLong(segment, offset, l, ByteOrder.nativeOrder());
    }

    public static long getLong(MemorySegment segment, long offset, ByteOrder order) {
        assert segment.isNative();
        if(JING_REMOVE_BOUNDARY_CHECKING) {
            long address = Math.addExact(segment.address(), offset);
            if(order == ByteOrder.nativeOrder()) {
                return ZERO.get(ValueLayout.JAVA_LONG_UNALIGNED, address);
            } else {
                return ZERO.get(JAVA_LONG_UNALIGNED_OPPOSITE, address);
            }
        } else {
            if(order == ByteOrder.nativeOrder()) {
                return segment.get(ValueLayout.JAVA_LONG_UNALIGNED, offset);
            } else {
                return segment.get(JAVA_LONG_UNALIGNED_OPPOSITE, offset);
            }
        }
    }

    public static void setLong(MemorySegment segment, long offset, long l, ByteOrder order) {
        assert segment.isNative();
        if(JING_REMOVE_BOUNDARY_CHECKING) {
            long address = Math.addExact(segment.address(), offset);
            if (order == ByteOrder.nativeOrder()) {
                ZERO.set(ValueLayout.JAVA_LONG_UNALIGNED, address, l);
            } else {
                ZERO.set(JAVA_LONG_UNALIGNED_OPPOSITE, address, l);
            }
        } else {
            if (order == ByteOrder.nativeOrder()) {
                segment.set(ValueLayout.JAVA_LONG_UNALIGNED, offset, l);
            } else {
                segment.set(JAVA_LONG_UNALIGNED_OPPOSITE, offset, l);
            }
        }
    }

    public static float getFloat(MemorySegment segment, long offset) {
        assert segment.isNative();
        return getFloat(segment, offset, ByteOrder.nativeOrder());
    }

    public static void setFloat(MemorySegment segment, long offset, float f) {
        assert segment.isNative();
        setFloat(segment, offset, f, ByteOrder.nativeOrder());
    }

    public static float getFloat(MemorySegment segment, long offset, ByteOrder order) {
        assert segment.isNative();
        if(JING_REMOVE_BOUNDARY_CHECKING) {
            long address = Math.addExact(segment.address(), offset);
            if(order == ByteOrder.nativeOrder()) {
                return ZERO.get(ValueLayout.JAVA_FLOAT_UNALIGNED, address);
            } else {
                return ZERO.get(JAVA_FLOAT_UNALIGNED_OPPOSITE, address);
            }
        } else {
            if(order == ByteOrder.nativeOrder()) {
                return segment.get(ValueLayout.JAVA_FLOAT_UNALIGNED, offset);
            } else {
                return segment.get(JAVA_FLOAT_UNALIGNED_OPPOSITE, offset);
            }
        }
    }

    public static void setFloat(MemorySegment segment, long offset, float f, ByteOrder order) {
        assert segment.isNative();
        if(JING_REMOVE_BOUNDARY_CHECKING) {
            long address = Math.addExact(segment.address(), offset);
            if (order == ByteOrder.nativeOrder()) {
                ZERO.set(ValueLayout.JAVA_FLOAT_UNALIGNED, address, f);
            } else {
                ZERO.set(JAVA_FLOAT_UNALIGNED_OPPOSITE, address, f);
            }
        } else {
            if (order == ByteOrder.nativeOrder()) {
                segment.set(ValueLayout.JAVA_FLOAT_UNALIGNED, offset, f);
            } else {
                segment.set(JAVA_FLOAT_UNALIGNED_OPPOSITE, offset, f);
            }
        }
    }

    public static double getDouble(MemorySegment segment, long offset) {
        assert segment.isNative();
        return getDouble(segment, offset, ByteOrder.nativeOrder());
    }

    public static void setDouble(MemorySegment segment, long offset, double d) {
        assert segment.isNative();
        setDouble(segment, offset, d, ByteOrder.nativeOrder());
    }

    public static double getDouble(MemorySegment segment, long offset, ByteOrder order) {
        assert segment.isNative();
        if(JING_REMOVE_BOUNDARY_CHECKING) {
            long address = Math.addExact(segment.address(), offset);
            if(order == ByteOrder.nativeOrder()) {
                return ZERO.get(ValueLayout.JAVA_DOUBLE_UNALIGNED, address);
            } else {
                return ZERO.get(JAVA_DOUBLE_UNALIGNED_OPPOSITE, address);
            }
        } else {
            if(order == ByteOrder.nativeOrder()) {
                return segment.get(ValueLayout.JAVA_DOUBLE_UNALIGNED, offset);
            } else {
                return segment.get(JAVA_DOUBLE_UNALIGNED_OPPOSITE, offset);
            }
        }
    }

    public static void setDouble(MemorySegment segment, long offset, double d, ByteOrder order) {
        assert segment.isNative();
        if(JING_REMOVE_BOUNDARY_CHECKING) {
            long address = Math.addExact(segment.address(), offset);
            if (order == ByteOrder.nativeOrder()) {
                ZERO.set(ValueLayout.JAVA_DOUBLE_UNALIGNED, address, d);
            } else {
                ZERO.set(JAVA_DOUBLE_UNALIGNED_OPPOSITE, address, d);
            }
        } else {
            if (order == ByteOrder.nativeOrder()) {
                segment.set(ValueLayout.JAVA_DOUBLE_UNALIGNED, offset, d);
            } else {
                segment.set(JAVA_DOUBLE_UNALIGNED_OPPOSITE, offset, d);
            }
        }
    }

    public static MemorySegment getAddress(MemorySegment segment, long offset) {
        assert segment.isNative();
        return getAddress(segment, offset, ByteOrder.nativeOrder());
    }

    public static void setAddress(MemorySegment segment, long offset, MemorySegment m) {
        assert segment.isNative();
        setAddress(segment, offset, m, ByteOrder.nativeOrder());
    }

    public static MemorySegment getAddress(MemorySegment segment, long offset, ByteOrder order) {
        assert segment.isNative();
        if(JING_REMOVE_BOUNDARY_CHECKING) {
            long address = Math.addExact(segment.address(), offset);
            if(order == ByteOrder.nativeOrder()) {
                return ZERO.get(ValueLayout.ADDRESS_UNALIGNED, address);
            } else {
                return ZERO.get(JAVA_ADDRESS_OPPOSITE, address);
            }
        } else {
            if(order == ByteOrder.nativeOrder()) {
                return segment.get(ValueLayout.ADDRESS_UNALIGNED, offset);
            } else {
                return segment.get(JAVA_ADDRESS_OPPOSITE, offset);
            }
        }
    }

    public static void setAddress(MemorySegment segment, long offset, MemorySegment m, ByteOrder order) {
        assert segment.isNative();
        if(JING_REMOVE_BOUNDARY_CHECKING) {
            long address = Math.addExact(segment.address(), offset);
            if (order == ByteOrder.nativeOrder()) {
                ZERO.set(ValueLayout.ADDRESS_UNALIGNED, address, m);
            } else {
                ZERO.set(JAVA_ADDRESS_OPPOSITE, address, m);
            }
        } else {
            if (order == ByteOrder.nativeOrder()) {
                segment.set(ValueLayout.ADDRESS_UNALIGNED, offset, m);
            } else {
                segment.set(JAVA_ADDRESS_OPPOSITE, offset, m);
            }
        }
    }

    // memory related methods, heap memory are not allowed by default

    public static MemorySegment malloc(long byteSize) {
        class Holder {
            static final MethodHandle MH =
                    SharedLibs.getMethodHandleFromVM("malloc", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG), true);
        }
        try {
            MemorySegment r = (MemorySegment) Holder.MH.invokeExact(byteSize);
            if(r.address() == 0L) {
                throw new OutOfMemoryError();
            }
            return r.reinterpret(byteSize);
        } catch (Throwable t) {
            throw new ForeignException("Failed to invoke malloc method", t);
        }
    }

    public static MemorySegment realloc(MemorySegment m, long newByteSize) {
        assert m.isNative() && !m.isMapped();
        class Holder {
            static final MethodHandle MH =
                    SharedLibs.getMethodHandleFromVM("realloc", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG), true);
        }
        try {
            MemorySegment r = (MemorySegment) Holder.MH.invokeExact(m, newByteSize);
            if(r.address() == 0L) {
                throw new OutOfMemoryError();
            }
            return r.reinterpret(newByteSize);
        } catch (Throwable t) {
            throw new ForeignException("Failed to invoke realloc method", t);
        }
    }

    public static void free(MemorySegment m) {
        assert m.isNative() && !m.isMapped();
        class Holder {
            static final MethodHandle MH =
                    SharedLibs.getMethodHandleFromVM("free", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS), true);
        }
        try {
            Holder.MH.invokeExact(m);
        } catch (Throwable t) {
            throw new ForeignException("Failed to invoke free method", t);
        }
    }

    public static int memcmp(MemorySegment dest, MemorySegment src, long size) {
        assert dest.isNative() && src.isNative();
        class Holder {
            static final MethodHandle MH =
                    SharedLibs.getMethodHandleFromVM("memcmp", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG), true);
        }
        try {
            return (int) Holder.MH.invokeExact(dest, src, size);
        } catch (Throwable t) {
            throw new ForeignException("Failed to invoke memcpy method", t);
        }
    }

    public static void memcpy(MemorySegment dest, MemorySegment src, long size) {
        assert dest.isNative() && src.isNative();
        class Holder {
            static final MethodHandle MH =
                    SharedLibs.getMethodHandleFromVM("memcpy", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG), true);
        }
        try {
            MemorySegment _ = (MemorySegment) Holder.MH.invokeExact(dest, src, size);
        } catch (Throwable t) {
            throw new ForeignException("Failed to invoke memcpy method", t);
        }
    }

    public static void memmove(MemorySegment dest, MemorySegment src, long size) {
        assert dest.isNative() && src.isNative();
        class Holder {
            static final MethodHandle MH =
                    SharedLibs.getMethodHandleFromVM("memmove", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG), true);
        }
        try {
            MemorySegment _ = (MemorySegment) Holder.MH.invokeExact(dest, src, size);
        } catch (Throwable t) {
            throw new ForeignException("Failed to invoke memmove method", t);
        }
    }

    public static MemorySegment memchr(MemorySegment src, int ch, long size) {
        assert src.isNative();
        class Holder {
            static final MethodHandle MH =
                    SharedLibs.getMethodHandleFromVM("memchr", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG), true);
        }
        try {
            return (MemorySegment) Holder.MH.invokeExact(src, ch, size);
        } catch (Throwable t) {
            throw new ForeignException("Failed to invoke memchr method", t);
        }
    }

    public static void memset(MemorySegment src, int ch, long count) {
        assert src.isNative();
        class Holder {
            static final MethodHandle MH =
                    SharedLibs.getMethodHandleFromVM("memset", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG), true);
        }
        try {
            MemorySegment _  = (MemorySegment) Holder.MH.invokeExact(src, ch, count);
        } catch (Throwable t) {
            throw new ForeignException("Failed to invoke memset method", t);
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
            Linker.nativeLinker().canonicalLayouts().get("size_t").withName("len"),
            JING_DATA_LAYOUT.withName("data")
    );

    private static final long JING_DATA_OFFSET = JING_RESULT_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("data"));
    private static final long JING_LEN_OFFSET = JING_RESULT_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("len"));

    static {
        if(JING_DATA_LAYOUT.byteSize() != 8 || JING_RESULT_LAYOUT.byteSize() != 16) {
            throw new ExceptionInInitializerError("JING_RESULT_LAYOUT byteSize mismatch");
        }
    }

    public static byte rByte(MemorySegment r) {
        return r.get(ValueLayout.JAVA_BYTE, JING_DATA_OFFSET);
    }

    public static short rShort(MemorySegment r) {
        return r.get(ValueLayout.JAVA_SHORT, JING_DATA_OFFSET);
    }

    public static int rInt(MemorySegment r) {
        return r.get(ValueLayout.JAVA_INT, JING_DATA_OFFSET);
    }

    public static long rLong(MemorySegment r) {
        return r.get(ValueLayout.JAVA_LONG, JING_DATA_OFFSET);
    }

    public static float rFloat(MemorySegment r) {
        return r.get(ValueLayout.JAVA_FLOAT, JING_DATA_OFFSET);
    }

    public static double rDouble(MemorySegment r) {
        return r.get(ValueLayout.JAVA_DOUBLE, JING_DATA_OFFSET);
    }

    public static MemorySegment rAddress(MemorySegment r) {
        return r.get(ValueLayout.ADDRESS, JING_DATA_OFFSET);
    }

    public static long rLen(MemorySegment r) {
        return r.get(ValueLayout.JAVA_LONG_UNALIGNED, JING_LEN_OFFSET);
    }

}
