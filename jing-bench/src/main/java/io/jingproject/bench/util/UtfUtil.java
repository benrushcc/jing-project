package io.jingproject.bench.util;

import java.util.concurrent.ThreadLocalRandom;

public final class UtfUtil {
    private UtfUtil() {
        throw new UnsupportedOperationException("utility class");
    }

    public static void appendAscii(StringBuilder sb) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextBoolean()) {
            sb.append((char) random.nextInt('a', 'z' + 1));
        } else {
            sb.append((char) random.nextInt('A', 'Z' + 1));
        }
    }

    public static void appendUtf(StringBuilder sb) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        char c;
        if (random.nextInt(10) == 0) {
            c = (char) random.nextInt(0x800, 0xD800);
        } else {
            do {
                c = (char) random.nextInt(0x800, 0x10000);
            } while (Character.isSurrogate(c));
        }
        sb.append(c);
    }

    public static void appendSurrogate(StringBuilder sb) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int cp = random.nextInt(0x10000, 0x110000);
        char[] chars = Character.toChars(cp);
        for (char c : chars) {
            sb.append(c);
        }
    }

    public static String randTypedString(String type, int size) {
        return switch (type) {
            case "empty" -> "";
            case "ascii" -> UtfUtil.randAsciiString(size);
            case "utf" -> UtfUtil.randUtfString(size);
            case "surr" -> UtfUtil.randSurrogateString(size);
            case "mostAscii" -> UtfUtil.randMostlyAsciiString(size);
            default -> throw new AssertionError();
        };
    }

    public static String randAsciiString(int size) {
        StringBuilder sb = new StringBuilder(size);
        while (sb.length() < size) {
            appendAscii(sb);
        }
        return sb.toString();
    }

    public static String randUtfString(int size) {
        StringBuilder sb = new StringBuilder(size);
        while (sb.length() < size) {
            appendUtf(sb);
        }
        return sb.toString();
    }

    public static String randSurrogateString(int size) {
        StringBuilder sb = new StringBuilder(size);
        while (sb.length() < size) {
            appendSurrogate(sb);
        }
        return sb.toString();
    }

    public static String randMostlyAsciiString(int size) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if(random.nextInt(10) > 0) {
            return UtfUtil.randAsciiString(size);
        }
        StringBuilder sb = new StringBuilder(size);
        while (sb.length() < size) {
            int i = random.nextInt(100);
            if (i == 3) {
                appendSurrogate(sb);
            } else if(i < 10) {
                appendUtf(sb);
            }else {
                appendAscii(sb);
            }
        }
        return sb.toString();
    }
}
