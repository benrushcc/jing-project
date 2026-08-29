package io.jingproject.common;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

public final class ArrayAccess {
    private static final VarHandle SHORT_LE = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle SHORT_BE = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle CHAR_LE = MethodHandles.byteArrayViewVarHandle(char[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle CHAR_BE = MethodHandles.byteArrayViewVarHandle(char[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle INT_LE = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle INT_BE = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle LONG_LE = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle LONG_BE = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle FLOAT_LE = MethodHandles.byteArrayViewVarHandle(float[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle FLOAT_BE = MethodHandles.byteArrayViewVarHandle(float[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle DOUBLE_LE = MethodHandles.byteArrayViewVarHandle(double[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle DOUBLE_BE = MethodHandles.byteArrayViewVarHandle(double[].class, ByteOrder.BIG_ENDIAN);

    static {
        try {
            Class<Os> _ = MethodHandles.lookup().ensureInitialized(Os.class);
        } catch (IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private ArrayAccess() {
        throw new UnsupportedOperationException("utility class");
    }

    public static short getShort(byte[] array, int offset) {
        return getShort(array, offset, ByteOrder.nativeOrder());
    }

    public static short getShort(byte[] array, int offset, ByteOrder byteOrder) {
        return switch (byteOrder) {
            case LITTLE_ENDIAN -> (short) SHORT_LE.get(array, offset);
            case BIG_ENDIAN -> (short) SHORT_BE.get(array, offset);
        };
    }

    public static void setShort(byte[] array, int offset, short value) {
        setShort(array, offset, value, ByteOrder.nativeOrder());
    }

    public static void setShort(byte[] array, int offset, short value, ByteOrder byteOrder) {
        switch (byteOrder) {
            case LITTLE_ENDIAN -> SHORT_LE.set(array, offset, value);
            case BIG_ENDIAN -> SHORT_BE.set(array, offset, value);
        }
    }

    public static char getChar(byte[] array, int offset) {
        return getChar(array, offset, ByteOrder.nativeOrder());
    }

    public static char getChar(byte[] array, int offset, ByteOrder byteOrder) {
        return switch (byteOrder) {
            case LITTLE_ENDIAN -> (char) CHAR_LE.get(array, offset);
            case BIG_ENDIAN -> (char) CHAR_BE.get(array, offset);
        };
    }

    public static void setChar(byte[] array, int offset, char value) {
        setChar(array, offset, value, ByteOrder.nativeOrder());
    }

    public static void setChar(byte[] array, int offset, char value, ByteOrder byteOrder) {
        switch (byteOrder) {
            case LITTLE_ENDIAN -> CHAR_LE.set(array, offset, value);
            case BIG_ENDIAN -> CHAR_BE.set(array, offset, value);
        }
    }

    public static int getInt(byte[] array, int offset) {
        return getInt(array, offset, ByteOrder.nativeOrder());
    }

    public static int getInt(byte[] array, int offset, ByteOrder byteOrder) {
        return switch (byteOrder) {
            case LITTLE_ENDIAN -> (int) INT_LE.get(array, offset);
            case BIG_ENDIAN -> (int) INT_BE.get(array, offset);
        };
    }

    public static void setInt(byte[] array, int offset, int value) {
        setInt(array, offset, value, ByteOrder.nativeOrder());
    }

    public static void setInt(byte[] array, int offset, int value, ByteOrder byteOrder) {
        switch (byteOrder) {
            case LITTLE_ENDIAN -> INT_LE.set(array, offset, value);
            case BIG_ENDIAN -> INT_BE.set(array, offset, value);
        }
    }

    public static long getLong(byte[] array, int offset) {
        return getLong(array, offset, ByteOrder.nativeOrder());
    }

    public static long getLong(byte[] array, int offset, ByteOrder byteOrder) {
        return switch (byteOrder) {
            case LITTLE_ENDIAN -> (long) LONG_LE.get(array, offset);
            case BIG_ENDIAN -> (long) LONG_BE.get(array, offset);
        };
    }

    public static void setLong(byte[] array, int offset, long value) {
        setLong(array, offset, value, ByteOrder.nativeOrder());
    }

    public static void setLong(byte[] array, int offset, long value, ByteOrder byteOrder) {
        switch (byteOrder) {
            case LITTLE_ENDIAN -> LONG_LE.set(array, offset, value);
            case BIG_ENDIAN -> LONG_BE.set(array, offset, value);
        }
    }

    public static float getFloat(byte[] array, int offset) {
        return getFloat(array, offset, ByteOrder.nativeOrder());
    }

    public static float getFloat(byte[] array, int offset, ByteOrder byteOrder) {
        return switch (byteOrder) {
            case LITTLE_ENDIAN -> (float) FLOAT_LE.get(array, offset);
            case BIG_ENDIAN -> (float) FLOAT_BE.get(array, offset);
        };
    }

    public static void setFloat(byte[] array, int offset, float value) {
        setFloat(array, offset, value, ByteOrder.nativeOrder());
    }

    public static void setFloat(byte[] array, int offset, float value, ByteOrder byteOrder) {
        switch (byteOrder) {
            case LITTLE_ENDIAN -> FLOAT_LE.set(array, offset, value);
            case BIG_ENDIAN -> FLOAT_BE.set(array, offset, value);
        }
    }

    public static double getDouble(byte[] array, int offset) {
        return getDouble(array, offset, ByteOrder.nativeOrder());
    }

    public static double getDouble(byte[] array, int offset, ByteOrder byteOrder) {
        return switch (byteOrder) {
            case LITTLE_ENDIAN -> (double) DOUBLE_LE.get(array, offset);
            case BIG_ENDIAN -> (double) DOUBLE_BE.get(array, offset);
        };
    }

    public static void setDouble(byte[] array, int offset, double value) {
        setDouble(array, offset, value, ByteOrder.nativeOrder());
    }

    public static void setDouble(byte[] array, int offset, double value, ByteOrder byteOrder) {
        switch (byteOrder) {
            case LITTLE_ENDIAN -> DOUBLE_LE.set(array, offset, value);
            case BIG_ENDIAN -> DOUBLE_BE.set(array, offset, value);
        }
    }

}
