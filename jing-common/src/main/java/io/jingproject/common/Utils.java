package io.jingproject.common;

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

    // factor : 2
    public static int grow(int currentCapacity) {
        return Math.addExact(currentCapacity, currentCapacity);
    }

    // factor : 2
    public static long grow(long currentCapacity) {
        return Math.addExact(currentCapacity, currentCapacity);
    }

    // factor : 1.5
    public static int grow(int currentCapacity, int defaultCapacity) {
        if(defaultCapacity < 4) {
            throw new IllegalArgumentException("defaultCapacity must be at least 4");
        }
        if(currentCapacity < defaultCapacity) {
            return defaultCapacity;
        }
        int half = currentCapacity >> 1;
        return Math.addExact(currentCapacity, half);
    }

    // factor : 1.5
    public static long grow(long currentCapacity, long defaultCapacity) {
        if(defaultCapacity < 4) {
            throw new IllegalArgumentException("defaultCapacity must be at least 4");
        }
        if(currentCapacity < defaultCapacity) {
            return defaultCapacity;
        }
        long half = currentCapacity >> 1;
        return Math.addExact(currentCapacity, half);
    }

    /**
     *   Generate annotation processor generated class name.
     *   The naming strategy is arbitrarily defined, as long as it ensures proper loading and distinguishes from regular class names.
     *   This strategy may change in future versions, but the Jing library will aim to maintain this approach as much as possible, unless there is a compelling need for change.
     */
    public static String generateClassName(String base, String tag) {
        return "_" + base + "$$" + tag;
    }
}
