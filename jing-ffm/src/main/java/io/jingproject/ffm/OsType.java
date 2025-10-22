package io.jingproject.ffm;

public enum OsType {
    Windows(0x1),
    Linux(0x2),
    MacOS(0x4),
    All(0xFFFFFFFF);

    private static final OsType current = detectCurrentOsType();

    private static OsType detectCurrentOsType() {
        String osName = System.getProperty("os.name").toLowerCase();
        if(osName.contains("windows")) {
            return OsType.Windows;
        }else if(osName.contains("linux")) {
            return OsType.Linux;
        }else if(osName.contains("mac") && osName.contains("os")) {
            return OsType.MacOS;
        }else {
            throw new ForeignException("Unsupported OS: " + osName);
        }
    }

    private final int flag;

    OsType(int flag) {
        this.flag = flag;
    }

    public int flag() {
        return flag;
    }

    public static OsType current() {
        return current;
    }

    public static boolean enabled(OsType osType) {
        return (osType.flag() & current.flag()) != 0;
    }

}
