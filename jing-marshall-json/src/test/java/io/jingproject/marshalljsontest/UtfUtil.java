package io.jingproject.marshalljsontest;

import java.util.concurrent.ThreadLocalRandom;

public final class UtfUtil {
    private static final char[] ESCAPED = new char[]{'\t', '\r', '\n', '\f', '\"'};
    private UtfUtil() {
        throw new UnsupportedOperationException("utility class");
    }

    public static void appendAscii(StringBuilder sb) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if(random.nextBoolean()) {
            sb.append((char) random.nextInt('a', 'z' + 1));
        } else {
            sb.append((char) random.nextInt('A', 'Z' + 1));
        }
    }

    public static void appendEscape(StringBuilder sb) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        sb.append(ESCAPED[random.nextInt(ESCAPED.length)]);
    }

    public static void appendUtf(StringBuilder sb) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        char c;
        if(random.nextBoolean()) {
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

    public static String randString(int size) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(size);
        while (sb.length() < size) {
            switch (random.nextInt(10)) {
                case 0 -> appendSurrogate(sb);
                case 1, 2, 3 -> appendUtf(sb);
                case 4, 5, 6, 7, 8 -> appendAscii(sb);
                case 9 -> appendEscape(sb);
            }
        }
        return sb.toString();
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
        StringBuilder sb = new StringBuilder(size);
        while (sb.length() < size) {
            int i = random.nextInt(10);
            if(i == 0) {
                appendUtf(sb);
            } else {
                appendAscii(sb);
            }
        }
        return sb.toString();
    }

    public static void countUtf8Bytes(byte[] utf8Bytes) {
        int len = utf8Bytes.length;
        int count1 = 0, count2 = 0, count3 = 0, count4 = 0, countEscape = 0;
        int i = 0;
        while (i < len) {
            int b = utf8Bytes[i] & 0xFF;
            if (b < 0x80) {
                count1++;
                if(b == (byte) '\\') {
                    countEscape++;
                }
                i++;
            } else if (b >= 0xC0 && b <= 0xDF) {
                count2++;
                i += 2;
            } else if (b >= 0xE0 && b <= 0xEF) {
                count3++;
                i += 3;
            } else if (b >= 0xF0 && b <= 0xF7) {
                count4++;
                i += 4;
            } else {
                throw new AssertionError("invalid utf-8 input");
            }
        }
        System.out.println("UTF-8 byte sequence statistics:");
        System.out.println("1-byte characters: " + count1);
        System.out.println("escape characters: " + countEscape);
        System.out.println("2-byte characters: " + count2);
        System.out.println("3-byte characters: " + count3);
        System.out.println("4-byte characters: " + count4);
        int total = count1 + count2 + count3 + count4;
        System.out.println("Total characters: " + total);
        System.out.println("Total bytes: " + len);
        if (total > 0) {
            System.out.printf("1-byte: %.2f%%\n", count1 * 100.0 / total);
            System.out.printf("escape: %.2f%%\n", countEscape * 100.0 / total);
            System.out.printf("2-byte: %.2f%%\n", count2 * 100.0 / total);
            System.out.printf("3-byte: %.2f%%\n", count3 * 100.0 / total);
            System.out.printf("4-byte: %.2f%%\n", count4 * 100.0 / total);
        }
    }
}
