package io.jingproject.common;

public enum Os {
    WINDOWS,

    LINUX,

    MACOS;

    private static final Os CURRENT = detectCurrentOsType();

    private static Os detectCurrentOsType() {
        String osName = System.getProperty("os.name").toLowerCase();
        if(osName.contains("windows")) {
            return Os.WINDOWS;
        }else if(osName.contains("linux")) {
            return Os.LINUX;
        }else if(osName.contains("mac") && osName.contains("os")) {
            return Os.MACOS;
        }else {
            throw new RuntimeException("Unsupported OS: " + osName);
        }
    }

    /**
     *   Return the current operating system
     */
    public static Os current() {
        return CURRENT;
    }

}
