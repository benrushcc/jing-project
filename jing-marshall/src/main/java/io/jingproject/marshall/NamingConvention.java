package io.jingproject.marshall;

import io.jingproject.common.anno.ProcessorApi;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * this class provides strict conversion between naming conventions (camel, snake, kebab, pascal, upper_snake, upper_kebab, original).
 * unlike spring's flexible strategy, it enforces exact format compliance: each word must have at least 2 letters, only ascii letters allowed
 * casting any ambiguous input (e.g., "HTTPTest") would throws an IllegalArgumentException.
 */
@ProcessorApi
public enum NamingConvention {
    ORIGINAL,

    CAMEL_CASE, // helloWorld

    SNAKE_CASE, // hello_world

    KEBAB_CASE, // hello-world

    PASCAL_CASE, // HelloWorld

    UPPER_SNAKE_CASE, // HELLO_WORLD

    UPPER_KEBAB_CASE; // HELLO-WORLD

    private static final byte BYTE_DIFF = (byte) 'a' - (byte) 'A';

    private static byte[] normalize(String str, byte[] asciiBytes, NamingConvention from) {

        return switch (from) {
            case ORIGINAL -> throw new UnsupportedOperationException();
            case CAMEL_CASE -> {
                byte[] r = new byte[Math.addExact(asciiBytes.length, asciiBytes.length)];
                int sep = -1;
                int index = 0;
                for (byte b : asciiBytes) {
                    if (b >= (byte) 'a' && b <= (byte) 'z') {
                        r[index++] = b;
                    } else if (b >= (byte) 'A' && b <= (byte) 'Z') {
                        if (index - sep <= 2) {
                            throw new IllegalArgumentException("illegal word : " + str);
                        }
                        sep = index;
                        r[index++] = (byte) '\u0000';
                        r[index++] = (byte) (b + BYTE_DIFF);
                    } else {
                        throw new IllegalArgumentException("invalid byte: " + b);
                    }
                }
                if (sep == -1) {
                    throw new IllegalArgumentException("sep not found");
                }
                if (index - sep <= 2) {
                    throw new IllegalArgumentException("illegal word : " + str);
                }
                r[index++] = (byte) '\u0000';
                yield Arrays.copyOf(r, index);
            }
            case SNAKE_CASE -> {
                byte[] r = new byte[Math.incrementExact(asciiBytes.length)];
                int sep = -1;
                int index = 0;
                for (byte b : asciiBytes) {
                    if (b >= (byte) 'a' && b <= (byte) 'z') {
                        r[index++] = b;
                    } else if (b == (byte) '_') {
                        if (index - sep <= 2) {
                            throw new IllegalArgumentException("illegal word : " + str);
                        }
                        sep = index;
                        r[index++] = (byte) '\u0000';
                    } else {
                        throw new IllegalArgumentException("invalid byte: " + b);
                    }
                }
                if (sep == -1) {
                    throw new IllegalArgumentException("sep not found");
                }
                if (index - sep <= 2) {
                    throw new IllegalArgumentException("illegal word : " + str);
                }
                r[index] = (byte) '\u0000';
                yield r;
            }
            case KEBAB_CASE -> {
                byte[] r = new byte[Math.incrementExact(asciiBytes.length)];
                int sep = -1;
                int index = 0;
                for (byte b : asciiBytes) {
                    if (b >= (byte) 'a' && b <= (byte) 'z') {
                        r[index++] = b;
                    } else if (b == (byte) '-') {
                        if (index - sep <= 2) {
                            throw new IllegalArgumentException("illegal word : " + str);
                        }
                        sep = index;
                        r[index++] = (byte) '\u0000';
                    } else {
                        throw new IllegalArgumentException("invalid byte: " + b);
                    }
                }
                if (sep == -1) {
                    throw new IllegalArgumentException("sep not found");
                }
                if (index - sep <= 2) {
                    throw new IllegalArgumentException("illegal word : " + str);
                }
                r[index] = (byte) '\u0000';
                yield r;
            }
            case PASCAL_CASE -> {
                if (asciiBytes[0] < (byte) 'A' || asciiBytes[0] > (byte) 'Z') {
                    throw new IllegalArgumentException("illegal first byte : " + asciiBytes[0]);
                }
                byte[] r = new byte[Math.addExact(asciiBytes.length, asciiBytes.length)];
                int sep = -1;
                int index = 0;
                for (int i = 0; i < asciiBytes.length; i++) {
                    byte b = asciiBytes[i];
                    if (b >= (byte) 'a' && b <= (byte) 'z') {
                        r[index++] = b;
                    } else if (b >= (byte) 'A' && b <= (byte) 'Z') {
                        if (i != 0) {
                            if (index - sep <= 2) {
                                throw new IllegalArgumentException("illegal word : " + str);
                            }
                            sep = index;
                            r[index++] = (byte) '\u0000';
                        }
                        r[index++] = (byte) (b + BYTE_DIFF);
                    } else {
                        throw new IllegalArgumentException("invalid byte: " + b);
                    }
                }
                if (sep == -1) {
                    throw new IllegalArgumentException("sep not found");
                }
                if (index - sep <= 2) {
                    throw new IllegalArgumentException("illegal word : " + str);
                }
                r[index++] = (byte) '\u0000';
                yield Arrays.copyOf(r, index);
            }
            case UPPER_SNAKE_CASE -> {
                byte[] r = new byte[Math.incrementExact(asciiBytes.length)];
                int sep = -1;
                int index = 0;
                for (byte b : asciiBytes) {
                    if (b >= (byte) 'A' && b <= (byte) 'Z') {
                        r[index++] = (byte) (b + BYTE_DIFF);
                    } else if (b == (byte) '_') {
                        if (index - sep <= 2) {
                            throw new IllegalArgumentException("illegal word : " + str);
                        }
                        sep = index;
                        r[index++] = (byte) '\u0000';
                    } else {
                        throw new IllegalArgumentException("invalid byte: " + b);
                    }
                }
                if (sep == -1) {
                    throw new IllegalArgumentException("sep not found");
                }
                if (index - sep <= 2) {
                    throw new IllegalArgumentException("illegal word : " + str);
                }
                r[index] = (byte) '\u0000';
                yield r;
            }
            case UPPER_KEBAB_CASE -> {
                byte[] r = new byte[Math.incrementExact(asciiBytes.length)];
                int sep = -1;
                int index = 0;
                for (byte b : asciiBytes) {
                    if (b >= (byte) 'A' && b <= (byte) 'Z') {
                        r[index++] = (byte) (b + BYTE_DIFF);
                    } else if (b == (byte) '-') {
                        if (index - sep <= 2) {
                            throw new IllegalArgumentException("illegal word : " + str);
                        }
                        sep = index;
                        r[index++] = (byte) '\u0000';
                    } else {
                        throw new IllegalArgumentException("invalid byte: " + b);
                    }
                }
                if (sep == -1) {
                    throw new IllegalArgumentException("sep not found");
                }
                if (index - sep <= 2) {
                    throw new IllegalArgumentException("illegal word : " + str);
                }
                r[index] = (byte) '\u0000';
                yield r;
            }
        };
    }

    private static boolean isSimpleWord(byte[] bytes, NamingConvention from) {
        switch (from) {
            case CAMEL_CASE, SNAKE_CASE, KEBAB_CASE -> {
                for (byte b : bytes) {
                    if (b < (byte) 'a' || b > (byte) 'z') {
                        return false;
                    }
                }
                return true;
            }
            case PASCAL_CASE -> {
                if (bytes[0] < (byte) 'A' || bytes[0] > (byte) 'Z') {
                    throw new IllegalArgumentException("illegal first byte : " + bytes[0]);
                }
                for (int i = 1; i < bytes.length; i++) {
                    if (bytes[i] < (byte) 'a' || bytes[i] > (byte) 'z') {
                        return false;
                    }
                }
                return true;
            }
            case UPPER_SNAKE_CASE, UPPER_KEBAB_CASE -> {
                for (byte b : bytes) {
                    if (b < (byte) 'A' || b > (byte) 'Z') {
                        return false;
                    }
                }
                return true;
            }
            default -> throw new UnsupportedOperationException();
        }
    }

    private static String castSimpleWord(byte[] bytes, NamingConvention to) {
        switch (to) {
            case CAMEL_CASE, SNAKE_CASE, KEBAB_CASE -> {
                for (int i = 0; i < bytes.length; i++) {
                    byte b = bytes[i];
                    if (b >= (byte) 'A' && b <= (byte) 'Z') {
                        bytes[i] = (byte) (b + BYTE_DIFF);
                    }
                }
            }
            case PASCAL_CASE -> {
                byte b = bytes[0];
                if (b >= (byte) 'a' && b <= (byte) 'z') {
                    bytes[0] = (byte) (b - BYTE_DIFF);
                }
                for (int i = 1; i < bytes.length; i++) {
                    b = bytes[i];
                    if (b >= (byte) 'A' && b <= (byte) 'Z') {
                        bytes[i] = (byte) (b + BYTE_DIFF);
                    }
                }
            }
            case UPPER_SNAKE_CASE, UPPER_KEBAB_CASE -> {
                for (int i = 0; i < bytes.length; i++) {
                    byte b = bytes[i];
                    if (b >= (byte) 'a' && b <= (byte) 'z') {
                        bytes[i] = (byte) (b - BYTE_DIFF);
                    }
                }
            }
        }
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    public static String cast(NamingConvention from, NamingConvention to, String name) {

        if (name.isBlank()) {
            throw new IllegalArgumentException("empty name: " + name);
        }
        byte[] bytes = name.getBytes(StandardCharsets.US_ASCII);
        if (isSimpleWord(bytes, from)) {
            return castSimpleWord(bytes, to);
        }
        byte[] normalized = normalize(name, bytes, from);
        byte[] r = new byte[normalized.length];
        int rIndex = 0;
        int nIndex = 0;
        for (int i = 0; i < normalized.length; i++) {
            byte b = normalized[i];
            if (b == (byte) '\u0000') {
                int len = i - nIndex;
                switch (to) {
                    case CAMEL_CASE -> {
                        System.arraycopy(normalized, nIndex, r, rIndex, len);
                        if (rIndex > 0) {
                            byte tmp = r[rIndex];
                            r[rIndex] = (byte) (tmp - BYTE_DIFF);
                        }
                    }
                    case SNAKE_CASE -> {
                        System.arraycopy(normalized, nIndex, r, rIndex, len);
                        if (i != normalized.length - 1) {
                            r[rIndex + len] = (byte) '_';
                            rIndex++;
                        }
                    }
                    case KEBAB_CASE -> {
                        System.arraycopy(normalized, nIndex, r, rIndex, len);
                        if (i != normalized.length - 1) {
                            r[rIndex + len] = (byte) '-';
                            rIndex++;
                        }
                    }
                    case PASCAL_CASE -> {
                        System.arraycopy(normalized, nIndex, r, rIndex, len);
                        byte tmp = r[rIndex];
                        r[rIndex] = (byte) (tmp - BYTE_DIFF);
                    }
                    case UPPER_SNAKE_CASE -> {
                        for (int j = 0; j < len; j++) {
                            r[rIndex + j] = (byte) (normalized[nIndex + j] - BYTE_DIFF);
                        }
                        if (i != normalized.length - 1) {
                            r[rIndex + len] = (byte) '_';
                            rIndex++;
                        }
                    }
                    case UPPER_KEBAB_CASE -> {
                        for (int j = 0; j < len; j++) {
                            r[rIndex + j] = (byte) (normalized[nIndex + j] - BYTE_DIFF);
                        }
                        if (i != normalized.length - 1) {
                            r[rIndex + len] = (byte) '-';
                            rIndex++;
                        }
                    }
                    default -> throw new UnsupportedOperationException();
                }
                rIndex += len;
                nIndex = i + 1;
            }
        }
        return new String(r, 0, rIndex, StandardCharsets.US_ASCII);
    }
}
