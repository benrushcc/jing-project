package io.jingproject.common;

/**
 *  Simple enum to represent the operating system type.
 *  Helps detect whether the app is running on Windows, Linux, or macOS.
 */
public enum Os {
    WINDOWS,

    LINUX,

    MACOS;

    /**
     *  For now, to keep things simple, we only support Windows, Linux, and macOS.
     *  Other platforms? We'll leave that fun challenge to whoever’s destined to do it :)
     */
    private static final Os CURRENT = detectCurrentOsType();

    /**
     * Detects which OS the program is running on.
     * Throws an error if the OS is not recognized.
     */
    private static Os detectCurrentOsType() {
        String osName = System.getProperty("os.name").toLowerCase();
        if(osName.contains("windows")) {
            return Os.WINDOWS;
        }else if(osName.contains("linux")) {
            return Os.LINUX;
        }else if(osName.contains("mac") && osName.contains("os")) {
            return Os.MACOS;
        }else {
            throw new AssertionError("Unsupported OS detected: " + osName);
        }
    }

    /**
     *   Return the current operating system
     */
    public static Os current() {
        return CURRENT;
    }

}
