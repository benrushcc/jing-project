package io.jingproject.common;

public final class Utils {
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    private static final int MIN_CAPACITY = 4;
    private static final int AVG_CAPACITY = 64;
    private static final int MAX_CAPACITY = 4096;

    private Utils() {
        throw new AssertionError();
    }

    public static Object[] emptyObjectArray() {
        return EMPTY_OBJECT_ARRAY;
    }

    public static byte[] emptyByteArray() {
        return EMPTY_BYTE_ARRAY;
    }

    public static int minCapacity() {
        return MIN_CAPACITY;
    }

    public static int avgCapacity() {
        return AVG_CAPACITY;
    }

    public static int maxCapacity() {
        return MAX_CAPACITY;
    }

    public static int grow(int currentCapacity, int defaultCapacity) {
        if(defaultCapacity < MIN_CAPACITY) {
            throw new AssertionError();
        }
        if(currentCapacity <= defaultCapacity) {
            return defaultCapacity;
        }
        int half = Math.divideExact(currentCapacity, 2);
        return Math.addExact(currentCapacity, half);
    }

    public static long grow(long currentCapacity, long defaultCapacity) {
        if(defaultCapacity < MIN_CAPACITY) {
            throw new AssertionError();
        }
        if(currentCapacity <= defaultCapacity) {
            return defaultCapacity;
        }
        long half = Math.divideExact(currentCapacity, 2L);
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
