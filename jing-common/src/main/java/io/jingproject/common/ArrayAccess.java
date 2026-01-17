package io.jingproject.common;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

public final class ArrayAccess {
    private ArrayAccess() {
        throw new UnsupportedOperationException("utility class");
    }

    private static final ByteOrder NATIVE_ORDER = ByteOrder.nativeOrder();

    private static final ByteOrder OPPOSITE_ORDER = NATIVE_ORDER == ByteOrder.BIG_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;

    private static final VarHandle SHORT = MethodHandles.byteArrayViewVarHandle(short[].class, NATIVE_ORDER);
    private static final VarHandle SHORT_OP = MethodHandles.byteArrayViewVarHandle(short[].class, OPPOSITE_ORDER);

    private static final VarHandle CHAR = MethodHandles.byteArrayViewVarHandle(char[].class, NATIVE_ORDER);
    private static final VarHandle CHAR_OP = MethodHandles.byteArrayViewVarHandle(char[].class, OPPOSITE_ORDER);

    private static final VarHandle INT = MethodHandles.byteArrayViewVarHandle(int[].class, NATIVE_ORDER);
    private static final VarHandle INT_OP = MethodHandles.byteArrayViewVarHandle(int[].class, OPPOSITE_ORDER);

    private static final VarHandle FLOAT = MethodHandles.byteArrayViewVarHandle(float[].class, NATIVE_ORDER);
    private static final VarHandle FLOAT_OP = MethodHandles.byteArrayViewVarHandle(float[].class, OPPOSITE_ORDER);

    private static final VarHandle LONG = MethodHandles.byteArrayViewVarHandle(long[].class, NATIVE_ORDER);
    private static final VarHandle LONG_OP = MethodHandles.byteArrayViewVarHandle(long[].class, OPPOSITE_ORDER);

    private static final VarHandle DOUBLE = MethodHandles.byteArrayViewVarHandle(double[].class, NATIVE_ORDER);
    private static final VarHandle DOUBLE_OP = MethodHandles.byteArrayViewVarHandle(double[].class, OPPOSITE_ORDER);


    public static short getShort(byte[] array, int offset, ByteOrder byteOrder) {
        if (byteOrder == NATIVE_ORDER) {
            return (short) SHORT.get(array, offset);
        } else {
            return (short) SHORT_OP.get(array, offset);
        }
    }

    public static short getShort(byte[] array, int offset) {
        return getShort(array, offset, NATIVE_ORDER);
    }

    public static void setShort(byte[] array, int offset, short value, ByteOrder byteOrder) {
        if (byteOrder == NATIVE_ORDER) {
            SHORT.set(array, offset, value);
        } else {
            SHORT_OP.set(array, offset, value);
        }
    }

    public static void setShort(byte[] array, int offset, short value) {
        setShort(array, offset, value, NATIVE_ORDER);
    }

    public static char getChar(byte[] array, int offset, ByteOrder byteOrder) {
        if (byteOrder == NATIVE_ORDER) {
            return (char) CHAR.get(array, offset);
        } else {
            return (char) CHAR_OP.get(array, offset);
        }
    }

    public static char getChar(byte[] array, int offset) {
        return getChar(array, offset, NATIVE_ORDER);
    }

    public static void setChar(byte[] array, int offset, char value, ByteOrder byteOrder) {
        if (byteOrder == NATIVE_ORDER) {
            CHAR.set(array, offset, value);
        } else {
            CHAR_OP.set(array, offset, value);
        }
    }

    public static void setChar(byte[] array, int offset, char value) {
        setChar(array, offset, value, NATIVE_ORDER);
    }

    public static int getInt(byte[] array, int offset, ByteOrder byteOrder) {
        if (byteOrder == NATIVE_ORDER) {
            return (int) INT.get(array, offset);
        } else {
            return (int) INT_OP.get(array, offset);
        }
    }

    public static int getInt(byte[] array, int offset) {
        return getInt(array, offset, NATIVE_ORDER);
    }

    public static void setInt(byte[] array, int offset, int value, ByteOrder byteOrder) {
        if (byteOrder == NATIVE_ORDER) {
            INT.set(array, offset, value);
        } else {
            INT_OP.set(array, offset, value);
        }
    }

    public static void setInt(byte[] array, int offset, int value) {
        setInt(array, offset, value, NATIVE_ORDER);
    }

    public static float getFloat(byte[] array, int offset, ByteOrder byteOrder) {
        if (byteOrder == NATIVE_ORDER) {
            return (float) FLOAT.get(array, offset);
        } else {
            return (float) FLOAT_OP.get(array, offset);
        }
    }

    public static float getFloat(byte[] array, int offset) {
        return getFloat(array, offset, NATIVE_ORDER);
    }

    public static void setFloat(byte[] array, int offset, float value, ByteOrder byteOrder) {
        if (byteOrder == NATIVE_ORDER) {
            FLOAT.set(array, offset, value);
        } else {
            FLOAT_OP.set(array, offset, value);
        }
    }

    public static void setFloat(byte[] array, int offset, float value) {
        setFloat(array, offset, value, NATIVE_ORDER);
    }

    public static long getLong(byte[] array, int offset, ByteOrder byteOrder) {
        if (byteOrder == NATIVE_ORDER) {
            return (long) LONG.get(array, offset);
        } else {
            return (long) LONG_OP.get(array, offset);
        }
    }

    public static long getLong(byte[] array, int offset) {
        return getLong(array, offset, NATIVE_ORDER);
    }

    public static void setLong(byte[] array, int offset, long value, ByteOrder byteOrder) {
        if (byteOrder == NATIVE_ORDER) {
            LONG.set(array, offset, value);
        } else {
            LONG_OP.set(array, offset, value);
        }
    }

    public static void setLong(byte[] array, int offset, long value) {
        setLong(array, offset, value, NATIVE_ORDER);
    }

    public static double getDouble(byte[] array, int offset, ByteOrder byteOrder) {
        if (byteOrder == NATIVE_ORDER) {
            return (double) DOUBLE.get(array, offset);
        } else {
            return (double) DOUBLE_OP.get(array, offset);
        }
    }

    public static double getDouble(byte[] array, int offset) {
        return getDouble(array, offset, NATIVE_ORDER);
    }

    public static void setDouble(byte[] array, int offset, double value, ByteOrder byteOrder) {
        if (byteOrder == NATIVE_ORDER) {
            DOUBLE.set(array, offset, value);
        } else {
            DOUBLE_OP.set(array, offset, value);
        }
    }

    public static void setDouble(byte[] array, int offset, double value) {
        setDouble(array, offset, value, NATIVE_ORDER);
    }
}
