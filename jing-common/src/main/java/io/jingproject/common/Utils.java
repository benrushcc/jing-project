package io.jingproject.common;

public final class Utils {
    private static final int MIN_CAPACITY = 4;
    private Utils() {
        throw new AssertionError();
    }

    public static int minCapacity() {
        return MIN_CAPACITY;
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
}
