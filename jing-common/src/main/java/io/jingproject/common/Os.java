package io.jingproject.common;

import java.lang.foreign.ValueLayout;

/**
 * Simple enum to represent the operating system type.
 * Helps detect whether the app is running on Windows, Linux, or macOS.
 */
public enum Os {
    WINDOWS,

    LINUX,

    MACOS;

    static {
        long byteSize = ValueLayout.JAVA_BYTE.byteSize();
        if(byteSize != 1L) {
            throw new UnsupportedOperationException("Unsupported byte size: " + byteSize);
        }
        long shortSize = ValueLayout.JAVA_SHORT.byteSize();
        if(shortSize != 2L) {
            throw new UnsupportedOperationException("Unsupported short size: " + shortSize);
        }
        long charSize = ValueLayout.JAVA_CHAR.byteSize();
        if(charSize != 2L) {
            throw new UnsupportedOperationException("Unsupported short size: " + charSize);
        }
        long intSize = ValueLayout.JAVA_INT.byteSize();
        if(intSize != 4L) {
            throw new ExceptionInInitializerError("Unsupported int layout size: " + intSize);
        }
        long longSize = ValueLayout.JAVA_LONG.byteSize();
        if(longSize != 8L) {
            throw new ExceptionInInitializerError("Unsupported long layout size: " + longSize);
        }
        long floatSize = ValueLayout.JAVA_FLOAT.byteSize();
        if(floatSize != 4L) {
            throw new ExceptionInInitializerError("Unsupported float layout size: " + floatSize);
        }
        long doubleSize = ValueLayout.JAVA_DOUBLE.byteSize();
        if(doubleSize != 8L) {
            throw new ExceptionInInitializerError("Unsupported double layout size: " + doubleSize);
        }
        long addressSize = ValueLayout.ADDRESS.byteSize();
        if(addressSize != 8L) {
            throw new ExceptionInInitializerError("Unsupported address layout size: " + addressSize);
        }
    }

    /**
     * For now, to keep things simple, we only support Windows, Linux, and macOS.
     * Other platforms? We'll leave that fun challenge to whoever’s destined to do it :)
     */
    private static final Os CURRENT = detectCurrentOsType();

    /**
     * Detects which OS the program is running on.
     * Throws an error if the OS is not recognized.
     */
    private static Os detectCurrentOsType() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("windows")) {
            return Os.WINDOWS;
        } else if (osName.contains("linux")) {
            return Os.LINUX;
        } else if (osName.contains("mac") && osName.contains("os")) {
            return Os.MACOS;
        } else {
            throw new AssertionError("Unsupported OS detected: " + osName);
        }
    }

    /**
     * Return the current operating system
     */
    public static Os current() {
        return CURRENT;
    }

}
