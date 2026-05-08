package io.jingproject.common;

import java.nio.ByteOrder;

public final class Utils {
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    private Utils() {
        throw new AssertionError();
    }

    public static Object[] emptyObjectArray() {
        return EMPTY_OBJECT_ARRAY;
    }

    public static byte[] emptyByteArray() {
        return EMPTY_BYTE_ARRAY;
    }

    /**
     * Generate annotation processor generated class name.
     * The naming strategy is arbitrarily defined, as long as it ensures proper loading and distinguishes from regular class names.
     * This strategy may change in future versions, but the Jing library will aim to maintain this approach as much as possible, unless there is a compelling need for change.
     */
    public static String generateClassName(String base, String tag) {
        return "_" + base + "$$" + tag;
    }


    public static short compact(byte b0, byte b1) {
        return switch (ByteOrder.nativeOrder()) {
            case LITTLE_ENDIAN -> (short) (((b1 & 0xFF) << 8) | (b0 & 0xFF));
            case BIG_ENDIAN -> (short) (((b0 & 0xFF) << 8) | (b1 & 0xFF));
        };
    }

    public static int compact(short s0, short s1) {
        return switch (ByteOrder.nativeOrder()) {
            case LITTLE_ENDIAN -> ((s1 & 0xFFFF) << 16) | (s0 & 0xFFFF);
            case BIG_ENDIAN -> ((s0 & 0xFFFF) << 16) | (s1 & 0xFFFF);
        };
    }

    public static long compact(int i0, int i1) {
        return switch (ByteOrder.nativeOrder()) {
            case LITTLE_ENDIAN -> ((i1 & 0xFFFFFFFFL) << 32) | (i0 & 0xFFFFFFFFL);
            case BIG_ENDIAN -> ((i0 & 0xFFFFFFFFL) << 32) | (i1 & 0xFFFFFFFFL);
        };
    }
}
