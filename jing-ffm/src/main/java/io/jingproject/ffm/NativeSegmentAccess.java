package io.jingproject.ffm;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.ByteOrder;

/**
 * Utility class for accessing native memorySegment and VM native functions TODO 绝大部分方法都是过度设计，可以直接砍掉
 */
public final class NativeSegmentAccess {

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     */
    private NativeSegmentAccess() {
        throw new UnsupportedOperationException("utility class");
    }

    private static final Boolean JING_REMOVE_BOUNDARY_CHECKING = Boolean.getBoolean("JING_REMOVE_BOUNDARY_CHECKING");

    /**
     * Address is unsigned, so we can not represent raw address larger than Long.MAX_VALUE,
     * However, user-land address space is usually 48-bit on most operating system, so we are all good here.
     */
    private static final MemorySegment ZERO = MemorySegment.NULL.reinterpret(Long.MAX_VALUE);

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
        if (JING_REMOVE_BOUNDARY_CHECKING) {
            return ZERO.get(ValueLayout.JAVA_BYTE, Math.addExact(segment.address(), offset));
        } else {
            return segment.get(ValueLayout.JAVA_BYTE, offset);
        }
    }

    public static void setByte(MemorySegment segment, long offset, byte b) {
        assert segment.isNative();
        if (JING_REMOVE_BOUNDARY_CHECKING) {
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
        if (JING_REMOVE_BOUNDARY_CHECKING) {
            long address = Math.addExact(segment.address(), offset);
            if (order == ByteOrder.nativeOrder()) {
                return ZERO.get(ValueLayout.JAVA_SHORT_UNALIGNED, address);
            } else {
                return ZERO.get(JAVA_SHORT_UNALIGNED_OPPOSITE, address);
            }
        } else {
            if (order == ByteOrder.nativeOrder()) {
                return segment.get(ValueLayout.JAVA_SHORT_UNALIGNED, offset);
            } else {
                return segment.get(JAVA_SHORT_UNALIGNED_OPPOSITE, offset);
            }
        }
    }

    public static void setShort(MemorySegment segment, long offset, short s, ByteOrder order) {
        assert segment.isNative();
        if (JING_REMOVE_BOUNDARY_CHECKING) {
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
        if (JING_REMOVE_BOUNDARY_CHECKING) {
            long address = Math.addExact(segment.address(), offset);
            if (order == ByteOrder.nativeOrder()) {
                return ZERO.get(ValueLayout.JAVA_CHAR_UNALIGNED, address);
            } else {
                return ZERO.get(JAVA_CHAR_UNALIGNED_OPPOSITE, address);
            }
        } else {
            if (order == ByteOrder.nativeOrder()) {
                return segment.get(ValueLayout.JAVA_CHAR_UNALIGNED, offset);
            } else {
                return segment.get(JAVA_CHAR_UNALIGNED_OPPOSITE, offset);
            }
        }
    }

    public static void setChar(MemorySegment segment, long offset, char c, ByteOrder order) {
        assert segment.isNative();
        if (JING_REMOVE_BOUNDARY_CHECKING) {
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
        if (JING_REMOVE_BOUNDARY_CHECKING) {
            long address = Math.addExact(segment.address(), offset);
            if (order == ByteOrder.nativeOrder()) {
                return ZERO.get(ValueLayout.JAVA_INT_UNALIGNED, address);
            } else {
                return ZERO.get(JAVA_INT_UNALIGNED_OPPOSITE, address);
            }
        } else {
            if (order == ByteOrder.nativeOrder()) {
                return segment.get(ValueLayout.JAVA_INT_UNALIGNED, offset);
            } else {
                return segment.get(JAVA_INT_UNALIGNED_OPPOSITE, offset);
            }
        }
    }

    public static void setInt(MemorySegment segment, long offset, int i, ByteOrder order) {
        assert segment.isNative();
        if (JING_REMOVE_BOUNDARY_CHECKING) {
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
        if (JING_REMOVE_BOUNDARY_CHECKING) {
            long address = Math.addExact(segment.address(), offset);
            if (order == ByteOrder.nativeOrder()) {
                return ZERO.get(ValueLayout.JAVA_LONG_UNALIGNED, address);
            } else {
                return ZERO.get(JAVA_LONG_UNALIGNED_OPPOSITE, address);
            }
        } else {
            if (order == ByteOrder.nativeOrder()) {
                return segment.get(ValueLayout.JAVA_LONG_UNALIGNED, offset);
            } else {
                return segment.get(JAVA_LONG_UNALIGNED_OPPOSITE, offset);
            }
        }
    }

    public static void setLong(MemorySegment segment, long offset, long l, ByteOrder order) {
        assert segment.isNative();
        if (JING_REMOVE_BOUNDARY_CHECKING) {
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
        if (JING_REMOVE_BOUNDARY_CHECKING) {
            long address = Math.addExact(segment.address(), offset);
            if (order == ByteOrder.nativeOrder()) {
                return ZERO.get(ValueLayout.JAVA_FLOAT_UNALIGNED, address);
            } else {
                return ZERO.get(JAVA_FLOAT_UNALIGNED_OPPOSITE, address);
            }
        } else {
            if (order == ByteOrder.nativeOrder()) {
                return segment.get(ValueLayout.JAVA_FLOAT_UNALIGNED, offset);
            } else {
                return segment.get(JAVA_FLOAT_UNALIGNED_OPPOSITE, offset);
            }
        }
    }

    public static void setFloat(MemorySegment segment, long offset, float f, ByteOrder order) {
        assert segment.isNative();
        if (JING_REMOVE_BOUNDARY_CHECKING) {
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
        if (JING_REMOVE_BOUNDARY_CHECKING) {
            long address = Math.addExact(segment.address(), offset);
            if (order == ByteOrder.nativeOrder()) {
                return ZERO.get(ValueLayout.JAVA_DOUBLE_UNALIGNED, address);
            } else {
                return ZERO.get(JAVA_DOUBLE_UNALIGNED_OPPOSITE, address);
            }
        } else {
            if (order == ByteOrder.nativeOrder()) {
                return segment.get(ValueLayout.JAVA_DOUBLE_UNALIGNED, offset);
            } else {
                return segment.get(JAVA_DOUBLE_UNALIGNED_OPPOSITE, offset);
            }
        }
    }

    public static void setDouble(MemorySegment segment, long offset, double d, ByteOrder order) {
        assert segment.isNative();
        if (JING_REMOVE_BOUNDARY_CHECKING) {
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
        if (JING_REMOVE_BOUNDARY_CHECKING) {
            long address = Math.addExact(segment.address(), offset);
            if (order == ByteOrder.nativeOrder()) {
                return ZERO.get(ValueLayout.ADDRESS_UNALIGNED, address);
            } else {
                return ZERO.get(JAVA_ADDRESS_OPPOSITE, address);
            }
        } else {
            if (order == ByteOrder.nativeOrder()) {
                return segment.get(ValueLayout.ADDRESS_UNALIGNED, offset);
            } else {
                return segment.get(JAVA_ADDRESS_OPPOSITE, offset);
            }
        }
    }

    public static void setAddress(MemorySegment segment, long offset, MemorySegment m, ByteOrder order) {
        assert segment.isNative();
        if (JING_REMOVE_BOUNDARY_CHECKING) {
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

    // jing_result related methods
    private static final MemoryLayout SIZE_T_LAYOUT = Linker.nativeLinker().canonicalLayouts().get("size_t");
    private static final MemoryLayout JING_ERR_VAL_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("err_code"),
            ValueLayout.JAVA_INT.withName("err_flag")
    );
    private static final long JING_ERR_CODE_OFFSET = JING_ERR_VAL_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("err_code"));
    private static final long JING_ERR_FLAG_OFFSET = JING_ERR_VAL_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("err_flag"));


    private static final MemoryLayout JING_DATA_LAYOUT = MemoryLayout.unionLayout(
            ValueLayout.JAVA_BYTE.withName("byte_val"),
            ValueLayout.JAVA_SHORT.withName("short_val"),
            ValueLayout.JAVA_CHAR.withName("char_val"),
            ValueLayout.JAVA_INT.withName("int_val"),
            ValueLayout.JAVA_LONG.withName("long_val"),
            ValueLayout.JAVA_FLOAT.withName("float_val"),
            ValueLayout.JAVA_DOUBLE.withName("double_val"),
            ValueLayout.ADDRESS.withName("ptr_val"),
            JING_ERR_VAL_LAYOUT.withName("err_val")
    );

    private static final long JING_DATA_BYTE_VAL_OFFSET = JING_DATA_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("byte_val"));
    private static final long JING_DATA_SHORT_VAL_OFFSET = JING_DATA_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("short_val"));
    private static final long JING_DATA_CHAR_VAL_OFFSET = JING_DATA_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("char_val"));
    private static final long JING_DATA_INT_VAL_OFFSET = JING_DATA_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("int_val"));
    private static final long JING_DATA_LONG_VAL_OFFSET = JING_DATA_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("long_val"));
    private static final long JING_DATA_FLOAT_VAL_OFFSET = JING_DATA_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("float_val"));
    private static final long JING_DATA_DOUBLE_VAL_OFFSET = JING_DATA_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("double_val"));
    private static final long JING_DATA_PTR_VAL_OFFSET = JING_DATA_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("ptr_val"));
    private static final long JING_DATA_ERR_VAL_OFFSET = JING_DATA_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("err_val"));

    private static final MemoryLayout JING_RESULT_LAYOUT = MemoryLayout.structLayout(
            SIZE_T_LAYOUT.withName("len"),
            JING_DATA_LAYOUT.withName("data")
    );

    private static final long JING_LEN_OFFSET = JING_RESULT_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("len"));
    private static final long JING_DATA_OFFSET = JING_RESULT_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("data"));

    static {
        if (SIZE_T_LAYOUT.byteSize() != 8 || JING_DATA_LAYOUT.byteSize() != 8 || JING_RESULT_LAYOUT.byteSize() != 16) {
            throw new ExceptionInInitializerError("Layout byteSize mismatch, might not be 64-bits operating system");
        }
    }

    public static MemoryLayout rLayout() {
        return JING_RESULT_LAYOUT;
    }

    public static byte rByte(MemorySegment r) {
        return getByte(r, JING_DATA_OFFSET + JING_DATA_BYTE_VAL_OFFSET);
    }

    public static short rShort(MemorySegment r) {
        return getShort(r, JING_DATA_OFFSET + JING_DATA_SHORT_VAL_OFFSET);
    }

    public static char rChar(MemorySegment r) {
        return getChar(r, JING_DATA_OFFSET + JING_DATA_CHAR_VAL_OFFSET);
    }

    public static int rInt(MemorySegment r) {
        return getInt(r, JING_DATA_OFFSET + JING_DATA_INT_VAL_OFFSET);
    }

    public static long rLong(MemorySegment r) {
        return getLong(r, JING_DATA_OFFSET + JING_DATA_LONG_VAL_OFFSET);
    }

    public static float rFloat(MemorySegment r) {
        return getFloat(r, JING_DATA_OFFSET + JING_DATA_FLOAT_VAL_OFFSET);
    }

    public static double rDouble(MemorySegment r) {
        return getDouble(r, JING_DATA_OFFSET + JING_DATA_DOUBLE_VAL_OFFSET);
    }

    public static MemorySegment rAddress(MemorySegment r) {
        return getAddress(r, JING_DATA_OFFSET + JING_DATA_PTR_VAL_OFFSET);
    }

    public static long rLen(MemorySegment r) {
        return getLong(r, JING_LEN_OFFSET);
    }

    public static int rErrCode(MemorySegment r) {
        return getInt(r, JING_DATA_OFFSET + JING_DATA_ERR_VAL_OFFSET + JING_ERR_CODE_OFFSET);
    }

    public static int rErrFlag(MemorySegment r) {
        return getInt(r, JING_DATA_OFFSET + JING_DATA_ERR_VAL_OFFSET + JING_ERR_FLAG_OFFSET);
    }

    private static final long JING_POINTER_ERR_FLAG = 0x8000000000000000L;

    public static boolean isErrPtr(MemorySegment seg) {
        return (seg.address() & JING_POINTER_ERR_FLAG) != 0;
    }

    public static int errCode(MemorySegment seg) {
        return (int) seg.address();
    }

    public static MemorySegment reinterpret(MemorySegment seg, long newSize) {
        assert seg.isNative();
        return seg.reinterpret(newSize);
    }

}
