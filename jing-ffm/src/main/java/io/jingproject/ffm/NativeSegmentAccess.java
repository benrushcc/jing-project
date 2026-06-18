package io.jingproject.ffm;

import io.jingproject.common.Os;
import io.jingproject.common.SegmentAccess;
import io.jingproject.common.anno.Fragile;

import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * Utility class for accessing native memorySegment and VM native functions
 */
@Fragile
@SuppressWarnings("unused")
public final class NativeSegmentAccess {

    static {
        try {
            Class<Os> _ = MethodHandles.lookup().ensureInitialized(Os.class);
        } catch (IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     */
    private NativeSegmentAccess() {
        throw new UnsupportedOperationException("utility class");
    }

    /**
     * Address is unsigned, so we can not represent raw address larger than Long.MAX_VALUE,
     * However, user-land address space is usually 48-bit on most operating system, so we are all good here.
     */
    private static final MemorySegment ZERO = resize(MemorySegment.NULL, Long.MAX_VALUE);

    public static byte getByte(MemorySegment segment, long offset) {
        assert segment != null && segment.isNative() && Objects.checkIndex(offset, segment.byteSize()) >= 0L;
        return ZERO.get(ValueLayout.JAVA_BYTE, Math.addExact(segment.address(), offset));
    }

    public static void setByte(MemorySegment segment, long offset, byte b) {
        assert segment != null && segment.isNative() && Objects.checkIndex(offset, segment.byteSize()) >= 0L;
        ZERO.set(ValueLayout.JAVA_BYTE, Math.addExact(segment.address(), offset), b);
    }

    public static short getShort(MemorySegment segment, long offset) {
        return getShort(segment, offset, ByteOrder.nativeOrder());
    }

    public static void setShort(MemorySegment segment, long offset, short s) {
        setShort(segment, offset, s, ByteOrder.nativeOrder());
    }

    public static short getShort(MemorySegment segment, long offset, ByteOrder order) {
        assert segment != null && order != null && segment.isNative() && Objects.checkIndex(offset, segment.byteSize()) >= 0L;
        long address = Math.addExact(segment.address(), offset);
        return switch (order) {
            case BIG_ENDIAN -> ZERO.get(SegmentAccess.SHORT_UNALIGNED_BE, address);
            case LITTLE_ENDIAN -> ZERO.get(SegmentAccess.SHORT_UNALIGNED_LE, address);
        };
    }

    public static void setShort(MemorySegment segment, long offset, short s, ByteOrder order) {
        assert segment != null && order != null && segment.isNative() && Objects.checkIndex(offset, segment.byteSize()) >= 0L;
        long address = Math.addExact(segment.address(), offset);
        switch (order) {
            case BIG_ENDIAN -> ZERO.set(SegmentAccess.SHORT_UNALIGNED_BE, address, s);
            case LITTLE_ENDIAN -> ZERO.set(SegmentAccess.SHORT_UNALIGNED_LE, address, s);
        }
    }

    public static char getChar(MemorySegment segment, long offset) {
        return getChar(segment, offset, ByteOrder.nativeOrder());
    }

    public static void setChar(MemorySegment segment, long offset, char c) {
        setChar(segment, offset, c, ByteOrder.nativeOrder());
    }

    public static char getChar(MemorySegment segment, long offset, ByteOrder order) {
        assert segment != null && order != null && segment.isNative()
                && Objects.checkIndex(offset, segment.byteSize()) >= 0L;
        long address = Math.addExact(segment.address(), offset);
        return switch (order) {
            case BIG_ENDIAN    -> ZERO.get(SegmentAccess.CHAR_UNALIGNED_BE, address);
            case LITTLE_ENDIAN -> ZERO.get(SegmentAccess.CHAR_UNALIGNED_LE, address);
        };
    }

    public static void setChar(MemorySegment segment, long offset, char c, ByteOrder order) {
        assert segment != null && order != null && segment.isNative()
                && Objects.checkIndex(offset, segment.byteSize()) >= 0L;
        long address = Math.addExact(segment.address(), offset);
        switch (order) {
            case BIG_ENDIAN    -> ZERO.set(SegmentAccess.CHAR_UNALIGNED_BE, address, c);
            case LITTLE_ENDIAN -> ZERO.set(SegmentAccess.CHAR_UNALIGNED_LE, address, c);
        }
    }

    public static int getInt(MemorySegment segment, long offset) {
        return getInt(segment, offset, ByteOrder.nativeOrder());
    }

    public static void setInt(MemorySegment segment, long offset, int i) {
        setInt(segment, offset, i, ByteOrder.nativeOrder());
    }

    public static int getInt(MemorySegment segment, long offset, ByteOrder order) {
        assert segment != null && order != null && segment.isNative()
                && Objects.checkIndex(offset, segment.byteSize()) >= 0L;
        long address = Math.addExact(segment.address(), offset);
        return switch (order) {
            case BIG_ENDIAN    -> ZERO.get(SegmentAccess.INT_UNALIGNED_BE, address);
            case LITTLE_ENDIAN -> ZERO.get(SegmentAccess.INT_UNALIGNED_LE, address);
        };
    }

    public static void setInt(MemorySegment segment, long offset, int i, ByteOrder order) {
        assert segment != null && order != null && segment.isNative()
                && Objects.checkIndex(offset, segment.byteSize()) >= 0L;
        long address = Math.addExact(segment.address(), offset);
        switch (order) {
            case BIG_ENDIAN    -> ZERO.set(SegmentAccess.INT_UNALIGNED_BE, address, i);
            case LITTLE_ENDIAN -> ZERO.set(SegmentAccess.INT_UNALIGNED_LE, address, i);
        }
    }

    public static long getLong(MemorySegment segment, long offset) {
        return getLong(segment, offset, ByteOrder.nativeOrder());
    }

    public static void setLong(MemorySegment segment, long offset, long l) {
        setLong(segment, offset, l, ByteOrder.nativeOrder());
    }

    public static long getLong(MemorySegment segment, long offset, ByteOrder order) {
        assert segment != null && order != null && segment.isNative()
                && Objects.checkIndex(offset, segment.byteSize()) >= 0L;
        long address = Math.addExact(segment.address(), offset);
        return switch (order) {
            case BIG_ENDIAN    -> ZERO.get(SegmentAccess.LONG_UNALIGNED_BE, address);
            case LITTLE_ENDIAN -> ZERO.get(SegmentAccess.LONG_UNALIGNED_LE, address);
        };
    }

    public static void setLong(MemorySegment segment, long offset, long l, ByteOrder order) {
        assert segment != null && order != null && segment.isNative()
                && Objects.checkIndex(offset, segment.byteSize()) >= 0L;
        long address = Math.addExact(segment.address(), offset);
        switch (order) {
            case BIG_ENDIAN    -> ZERO.set(SegmentAccess.LONG_UNALIGNED_BE, address, l);
            case LITTLE_ENDIAN -> ZERO.set(SegmentAccess.LONG_UNALIGNED_LE, address, l);
        }
    }

    public static float getFloat(MemorySegment segment, long offset) {
        return getFloat(segment, offset, ByteOrder.nativeOrder());
    }

    public static void setFloat(MemorySegment segment, long offset, float f) {
        setFloat(segment, offset, f, ByteOrder.nativeOrder());
    }

    public static float getFloat(MemorySegment segment, long offset, ByteOrder order) {
        assert segment != null && order != null && segment.isNative()
                && Objects.checkIndex(offset, segment.byteSize()) >= 0L;
        long address = Math.addExact(segment.address(), offset);
        return switch (order) {
            case BIG_ENDIAN    -> ZERO.get(SegmentAccess.FLOAT_UNALIGNED_BE, address);
            case LITTLE_ENDIAN -> ZERO.get(SegmentAccess.FLOAT_UNALIGNED_LE, address);
        };
    }

    public static void setFloat(MemorySegment segment, long offset, float f, ByteOrder order) {
        assert segment != null && order != null && segment.isNative()
                && Objects.checkIndex(offset, segment.byteSize()) >= 0L;
        long address = Math.addExact(segment.address(), offset);
        switch (order) {
            case BIG_ENDIAN    -> ZERO.set(SegmentAccess.FLOAT_UNALIGNED_BE, address, f);
            case LITTLE_ENDIAN -> ZERO.set(SegmentAccess.FLOAT_UNALIGNED_LE, address, f);
        }
    }

    public static double getDouble(MemorySegment segment, long offset) {
        return getDouble(segment, offset, ByteOrder.nativeOrder());
    }

    public static void setDouble(MemorySegment segment, long offset, double d) {
        setDouble(segment, offset, d, ByteOrder.nativeOrder());
    }

    public static double getDouble(MemorySegment segment, long offset, ByteOrder order) {
        assert segment != null && order != null && segment.isNative()
                && Objects.checkIndex(offset, segment.byteSize()) >= 0L;
        long address = Math.addExact(segment.address(), offset);
        return switch (order) {
            case BIG_ENDIAN    -> ZERO.get(SegmentAccess.DOUBLE_UNALIGNED_BE, address);
            case LITTLE_ENDIAN -> ZERO.get(SegmentAccess.DOUBLE_UNALIGNED_LE, address);
        };
    }

    public static void setDouble(MemorySegment segment, long offset, double d, ByteOrder order) {
        assert segment != null && order != null && segment.isNative()
                && Objects.checkIndex(offset, segment.byteSize()) >= 0L;
        long address = Math.addExact(segment.address(), offset);
        switch (order) {
            case BIG_ENDIAN    -> ZERO.set(SegmentAccess.DOUBLE_UNALIGNED_BE, address, d);
            case LITTLE_ENDIAN -> ZERO.set(SegmentAccess.DOUBLE_UNALIGNED_LE, address, d);
        }
    }

    public static MemorySegment getAddress(MemorySegment segment, long offset) {
        return getAddress(segment, offset, ByteOrder.nativeOrder());
    }

    public static void setAddress(MemorySegment segment, long offset, MemorySegment addr) {
        setAddress(segment, offset, addr, ByteOrder.nativeOrder());
    }

    public static MemorySegment getAddress(MemorySegment segment, long offset, ByteOrder order) {
        assert segment != null && order != null && segment.isNative()
                && Objects.checkIndex(offset, segment.byteSize()) >= 0L;
        long address = Math.addExact(segment.address(), offset);
        return switch (order) {
            case BIG_ENDIAN    -> ZERO.get(SegmentAccess.ADDRESS_UNALIGNED_BE, address);
            case LITTLE_ENDIAN -> ZERO.get(SegmentAccess.ADDRESS_UNALIGNED_LE, address);
        };
    }

    public static void setAddress(MemorySegment segment, long offset, MemorySegment addr, ByteOrder order) {
        assert segment != null && addr != null && order != null && segment.isNative()
                && Objects.checkIndex(offset, segment.byteSize()) >= 0L;
        long address = Math.addExact(segment.address(), offset);
        switch (order) {
            case BIG_ENDIAN    -> ZERO.set(SegmentAccess.ADDRESS_UNALIGNED_BE, address, addr);
            case LITTLE_ENDIAN -> ZERO.set(SegmentAccess.ADDRESS_UNALIGNED_LE, address, addr);
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

    public static MemorySegment resize(MemorySegment seg, long newSize) {
        assert seg.isNative() && newSize >= 0L;
        return seg.reinterpret(newSize);
    }

}
