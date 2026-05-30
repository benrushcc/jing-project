package io.jingproject.common;

import java.lang.foreign.AddressLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.nio.ByteOrder;
import java.util.Objects;

@SuppressWarnings("unused")
public final class SegmentAccess {
    static {
        try {
            Class<Os> _ = MethodHandles.lookup().ensureInitialized(Os.class);
        } catch (IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private SegmentAccess() {
        throw new UnsupportedOperationException("utility class");
    }

    public static final ValueLayout.OfShort SHORT_UNALIGNED_LE =
            ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)
                    ? ValueLayout.JAVA_SHORT_UNALIGNED
                    : ValueLayout.JAVA_SHORT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);

    public static final ValueLayout.OfChar CHAR_UNALIGNED_LE =
            ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)
                    ? ValueLayout.JAVA_CHAR_UNALIGNED
                    : ValueLayout.JAVA_CHAR_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);

    public static final ValueLayout.OfInt INT_UNALIGNED_LE =
            ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)
                    ? ValueLayout.JAVA_INT_UNALIGNED
                    : ValueLayout.JAVA_INT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);

    public static final ValueLayout.OfLong LONG_UNALIGNED_LE =
            ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)
                    ? ValueLayout.JAVA_LONG_UNALIGNED
                    : ValueLayout.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);

    public static final ValueLayout.OfFloat FLOAT_UNALIGNED_LE =
            ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)
                    ? ValueLayout.JAVA_FLOAT_UNALIGNED
                    : ValueLayout.JAVA_FLOAT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);

    public static final ValueLayout.OfDouble DOUBLE_UNALIGNED_LE =
            ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)
                    ? ValueLayout.JAVA_DOUBLE_UNALIGNED
                    : ValueLayout.JAVA_DOUBLE_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);

    public static final AddressLayout ADDRESS_UNALIGNED_LE =
            ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)
                    ? ValueLayout.ADDRESS_UNALIGNED
                    : ValueLayout.ADDRESS_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);

    public static final ValueLayout.OfShort SHORT_UNALIGNED_BE =
            ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN)
                    ? ValueLayout.JAVA_SHORT_UNALIGNED
                    : ValueLayout.JAVA_SHORT_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN);

    public static final ValueLayout.OfChar CHAR_UNALIGNED_BE =
            ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN)
                    ? ValueLayout.JAVA_CHAR_UNALIGNED
                    : ValueLayout.JAVA_CHAR_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN);

    public static final ValueLayout.OfInt INT_UNALIGNED_BE =
            ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN)
                    ? ValueLayout.JAVA_INT_UNALIGNED
                    : ValueLayout.JAVA_INT_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN);

    public static final ValueLayout.OfLong LONG_UNALIGNED_BE =
            ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN)
                    ? ValueLayout.JAVA_LONG_UNALIGNED
                    : ValueLayout.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN);

    public static final ValueLayout.OfFloat FLOAT_UNALIGNED_BE =
            ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN)
                    ? ValueLayout.JAVA_FLOAT_UNALIGNED
                    : ValueLayout.JAVA_FLOAT_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN);

    public static final ValueLayout.OfDouble DOUBLE_UNALIGNED_BE =
            ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN)
                    ? ValueLayout.JAVA_DOUBLE_UNALIGNED
                    : ValueLayout.JAVA_DOUBLE_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN);

    public static final AddressLayout ADDRESS_UNALIGNED_BE =
            ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN)
                    ? ValueLayout.ADDRESS_UNALIGNED
                    : ValueLayout.ADDRESS_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN);

    public static byte getByte(MemorySegment segment, long offset) {
        assert segment != null && Objects.checkFromIndexSize(offset, 1, segment.byteSize()) >= 0;
        return segment.get(ValueLayout.JAVA_BYTE, offset);
    }

    public static void setByte(MemorySegment segment, long offset, byte b) {
        assert segment != null && Objects.checkFromIndexSize(offset, 1, segment.byteSize()) >= 0;
        segment.set(ValueLayout.JAVA_BYTE, offset, b);
    }

    public static short getShort(MemorySegment segment, long offset) {
        return getShort(segment, offset, ByteOrder.nativeOrder());
    }

    public static short getShort(MemorySegment segment, long offset, ByteOrder byteOrder) {
        assert segment != null && Objects.checkFromIndexSize(offset, 2, segment.byteSize()) >= 0 && byteOrder != null;
        return switch (byteOrder) {
            case LITTLE_ENDIAN -> segment.get(SHORT_UNALIGNED_LE, offset);
            case BIG_ENDIAN -> segment.get(SHORT_UNALIGNED_BE, offset);
        };
    }

    public static void setShort(MemorySegment segment, long offset, short s) {
        setShort(segment, offset, s, ByteOrder.nativeOrder());
    }

    public static void setShort(MemorySegment segment, long offset, short s, ByteOrder byteOrder) {
        assert segment != null && Objects.checkFromIndexSize(offset, 2, segment.byteSize()) >= 0 && byteOrder != null;
        switch (byteOrder) {
            case LITTLE_ENDIAN -> segment.set(SHORT_UNALIGNED_LE, offset, s);
            case BIG_ENDIAN -> segment.set(SHORT_UNALIGNED_BE, offset, s);
        }
    }

    public static char getChar(MemorySegment segment, long offset) {
        return getChar(segment, offset, ByteOrder.nativeOrder());
    }

    public static char getChar(MemorySegment segment, long offset, ByteOrder byteOrder) {
        assert segment != null && Objects.checkFromIndexSize(offset, 2, segment.byteSize()) >= 0 && byteOrder != null;
        return switch (byteOrder) {
            case LITTLE_ENDIAN -> segment.get(CHAR_UNALIGNED_LE, offset);
            case BIG_ENDIAN -> segment.get(CHAR_UNALIGNED_BE, offset);
        };
    }

    public static void setChar(MemorySegment segment, long offset, char value) {
        setChar(segment, offset, value, ByteOrder.nativeOrder());
    }

    public static void setChar(MemorySegment segment, long offset, char value, ByteOrder byteOrder) {
        assert segment != null && Objects.checkFromIndexSize(offset, 2, segment.byteSize()) >= 0 && byteOrder != null;
        switch (byteOrder) {
            case LITTLE_ENDIAN -> segment.set(CHAR_UNALIGNED_LE, offset, value);
            case BIG_ENDIAN -> segment.set(CHAR_UNALIGNED_BE, offset, value);
        }
    }

    public static int getInt(MemorySegment segment, long offset) {
        return getInt(segment, offset, ByteOrder.nativeOrder());
    }

    public static int getInt(MemorySegment segment, long offset, ByteOrder byteOrder) {
        assert segment != null && Objects.checkFromIndexSize(offset, 4, segment.byteSize()) >= 0 && byteOrder != null;
        return switch (byteOrder) {
            case LITTLE_ENDIAN -> segment.get(INT_UNALIGNED_LE, offset);
            case BIG_ENDIAN -> segment.get(INT_UNALIGNED_BE, offset);
        };
    }

    public static void setInt(MemorySegment segment, long offset, int value) {
        setInt(segment, offset, value, ByteOrder.nativeOrder());
    }

    public static void setInt(MemorySegment segment, long offset, int value, ByteOrder byteOrder) {
        assert segment != null && Objects.checkFromIndexSize(offset, 4, segment.byteSize()) >= 0 && byteOrder != null;
        switch (byteOrder) {
            case LITTLE_ENDIAN -> segment.set(INT_UNALIGNED_LE, offset, value);
            case BIG_ENDIAN -> segment.set(INT_UNALIGNED_BE, offset, value);
        }
    }

    public static long getLong(MemorySegment segment, long offset) {
        return getLong(segment, offset, ByteOrder.nativeOrder());
    }

    public static long getLong(MemorySegment segment, long offset, ByteOrder byteOrder) {
        assert segment != null && Objects.checkFromIndexSize(offset, 8, segment.byteSize()) >= 0 && byteOrder != null;
        return switch (byteOrder) {
            case LITTLE_ENDIAN -> segment.get(LONG_UNALIGNED_LE, offset);
            case BIG_ENDIAN -> segment.get(LONG_UNALIGNED_BE, offset);
        };
    }

    public static void setLong(MemorySegment segment, long offset, long value) {
        setLong(segment, offset, value, ByteOrder.nativeOrder());
    }

    public static void setLong(MemorySegment segment, long offset, long value, ByteOrder byteOrder) {
        assert segment != null && Objects.checkFromIndexSize(offset, 8, segment.byteSize()) >= 0 && byteOrder != null;
        switch (byteOrder) {
            case LITTLE_ENDIAN -> segment.set(LONG_UNALIGNED_LE, offset, value);
            case BIG_ENDIAN -> segment.set(LONG_UNALIGNED_BE, offset, value);
        }
    }

    public static float getFloat(MemorySegment segment, long offset) {
        return getFloat(segment, offset, ByteOrder.nativeOrder());
    }

    public static float getFloat(MemorySegment segment, long offset, ByteOrder byteOrder) {
        assert segment != null && Objects.checkFromIndexSize(offset, 4, segment.byteSize()) >= 0 && byteOrder != null;
        return switch (byteOrder) {
            case LITTLE_ENDIAN -> segment.get(FLOAT_UNALIGNED_LE, offset);
            case BIG_ENDIAN -> segment.get(FLOAT_UNALIGNED_BE, offset);
        };
    }

    public static void setFloat(MemorySegment segment, long offset, float value) {
        setFloat(segment, offset, value, ByteOrder.nativeOrder());
    }

    public static void setFloat(MemorySegment segment, long offset, float value, ByteOrder byteOrder) {
        assert segment != null && Objects.checkFromIndexSize(offset, 4, segment.byteSize()) >= 0 && byteOrder != null;
        switch (byteOrder) {
            case LITTLE_ENDIAN -> segment.set(FLOAT_UNALIGNED_LE, offset, value);
            case BIG_ENDIAN -> segment.set(FLOAT_UNALIGNED_BE, offset, value);
        }
    }

    public static double getDouble(MemorySegment segment, long offset) {
        return getDouble(segment, offset, ByteOrder.nativeOrder());
    }

    public static double getDouble(MemorySegment segment, long offset, ByteOrder byteOrder) {
        assert segment != null && Objects.checkFromIndexSize(offset, 8, segment.byteSize()) >= 0 && byteOrder != null;
        return switch (byteOrder) {
            case LITTLE_ENDIAN -> segment.get(DOUBLE_UNALIGNED_LE, offset);
            case BIG_ENDIAN -> segment.get(DOUBLE_UNALIGNED_BE, offset);
        };
    }

    public static void setDouble(MemorySegment segment, long offset, double value) {
        setDouble(segment, offset, value, ByteOrder.nativeOrder());
    }

    public static void setDouble(MemorySegment segment, long offset, double value, ByteOrder byteOrder) {
        assert segment != null && Objects.checkFromIndexSize(offset, 8, segment.byteSize()) >= 0 && byteOrder != null;
        switch (byteOrder) {
            case LITTLE_ENDIAN -> segment.set(DOUBLE_UNALIGNED_LE, offset, value);
            case BIG_ENDIAN -> segment.set(DOUBLE_UNALIGNED_BE, offset, value);
        }
    }

    public static MemorySegment getAddress(MemorySegment segment, long offset) {
        return getAddress(segment, offset, ByteOrder.nativeOrder());
    }

    public static MemorySegment getAddress(MemorySegment segment, long offset, ByteOrder byteOrder) {
        assert segment != null && Objects.checkFromIndexSize(offset, 8, segment.byteSize()) >= 0 && byteOrder != null;
        return switch (byteOrder) {
            case LITTLE_ENDIAN -> segment.get(ADDRESS_UNALIGNED_LE, offset);
            case BIG_ENDIAN -> segment.get(ADDRESS_UNALIGNED_BE, offset);
        };
    }

    public static void setAddress(MemorySegment segment, long offset, MemorySegment address) {
        setAddress(segment, offset, address, ByteOrder.nativeOrder());
    }

    public static void setAddress(MemorySegment segment, long offset, MemorySegment address, ByteOrder byteOrder) {
        assert segment != null && Objects.checkFromIndexSize(offset, 8, segment.byteSize()) >= 0 && byteOrder != null;
        switch (byteOrder) {
            case LITTLE_ENDIAN -> segment.set(ADDRESS_UNALIGNED_LE, offset, address);
            case BIG_ENDIAN -> segment.set(ADDRESS_UNALIGNED_BE, offset, address);
        }
    }

}
