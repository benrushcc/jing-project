package io.jingproject.common;

public final class Utils {
    private static final int MIN_CAPACITY = 4;
    private static final int AVG_CAPACITY = 16;
    private static final int MAX_CAPACITY = 1024;

    private Utils() {
        throw new AssertionError();
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

    public static String concat(String first, String second) {
        return first + second;
    }

    public static String concat(String first, String second, String third) {
        return first + second + third;
    }

    public static String concat(String first, String second, String third, String fourth) {
        return first + second + third + fourth;
    }

    public static String concat(String first, String second, String third, String fourth, String fifth) {
        return first + second + third + fourth + fifth;
    }

    public static String concat(String first, String second, String third, String fourth, String fifth, String sixth) {
        return first + second + third + fourth + fifth + sixth;
    }

    public static String concat(String... strs) {
        StringBuilder sb = new StringBuilder(Math.multiplyExact(AVG_CAPACITY, strs.length));
        for (String str : strs) {
            sb.append(str);
        }
        return sb.toString();
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
        return concat("_", base, "$$", tag);
    }
}
