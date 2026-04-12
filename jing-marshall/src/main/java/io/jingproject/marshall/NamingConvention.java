package io.jingproject.marshall;

import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.common.anno.ProcessorApi;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Function;

@ProcessorApi
public enum NamingConvention {
    ORIGINAL,

    CAMEL_CASE, // helloWorld

    SNAKE_CASE, // hello_world

    KEBAB_CASE, // hello-world

    PASCAL_CASE, // HelloWorld

    UPPER_SNAKE_CASE, // HELLO_WORLD

    UPPER_KEBAB_CASE; // HELLO-WORLD

    private static final byte BYTE_a = (byte) 'a';
    private static final byte BYTE_z = (byte) 'z';
    private static final byte BYTE_A = (byte) 'A';
    private static final byte BYTE_Z = (byte) 'Z';
    private static final byte BYTE_NULL = (byte) '\u0000';
    private static final byte BYTE_DIFF = BYTE_a - BYTE_A;
    private static final byte BYTE_UNDERSCORE = (byte) '_';
    private static final byte BYTE_MINUS = (byte) '-';

    private static Function<byte[], HeapWriteBuffer> toBufferFunc(NamingConvention namingConvention) {
        return switch (namingConvention) {
            case ORIGINAL -> throw new UnsupportedOperationException();
            case CAMEL_CASE -> bytes -> {
                HeapWriteBuffer buffer = new HeapWriteBuffer(Math.addExact(bytes.length, bytes.length));
                for (byte b : bytes) {
                    if(b >= BYTE_a && b <= BYTE_z) {
                        buffer.writeByte(b);
                    } else if(b >= BYTE_A && b <= BYTE_Z) {
                        buffer.writeByte(BYTE_NULL);
                        buffer.writeByte((byte) (b + BYTE_DIFF));
                    } else {
                        throw new IllegalArgumentException("Invalid byte: " + b);
                    }
                }
                buffer.writeByte(BYTE_NULL);
                return buffer;
            };
            case SNAKE_CASE -> bytes -> {
                if(bytes[0] == BYTE_UNDERSCORE || bytes[Math.decrementExact(bytes.length)] == BYTE_UNDERSCORE) {
                    throw new IllegalArgumentException("Invalid snake case input");
                }
                HeapWriteBuffer buffer = new HeapWriteBuffer(Math.incrementExact(bytes.length));
                for (byte b : bytes) {
                    if(b >= BYTE_a && b <= BYTE_z) {
                        buffer.writeByte(b);
                    } else if(b == BYTE_UNDERSCORE) {
                        buffer.writeByte(BYTE_NULL);
                    } else {
                        throw new IllegalArgumentException("Invalid byte: " + b);
                    }
                }
                buffer.writeByte(BYTE_NULL);
                return buffer;
            };
            case KEBAB_CASE -> bytes -> {
                if(bytes[0] == BYTE_MINUS || bytes[Math.decrementExact(bytes.length)] == BYTE_MINUS) {
                    throw new IllegalArgumentException("Invalid kebab case input");
                }
                HeapWriteBuffer buffer = new HeapWriteBuffer(Math.incrementExact(bytes.length));
                for (byte b : bytes) {
                    if(b >= BYTE_a && b <= BYTE_z) {
                        buffer.writeByte(b);
                    } else if(b == BYTE_MINUS) {
                        buffer.writeByte(BYTE_NULL);
                    } else {
                        throw new IllegalArgumentException("Invalid byte: " + b);
                    }
                }
                buffer.writeByte(BYTE_NULL);
                return buffer;
            };
            case PASCAL_CASE -> bytes -> {
                HeapWriteBuffer buffer = new HeapWriteBuffer(Math.addExact(bytes.length, bytes.length));
                for(int i = 0; i < bytes.length; i++) {
                    byte b = bytes[i];
                    if(b >= BYTE_a && b <= BYTE_z) {
                        buffer.writeByte(b);
                    } else if(b >= BYTE_A && b <= BYTE_Z) {
                        if(i != 0) {
                            buffer.writeByte(BYTE_NULL);
                        }
                        buffer.writeByte((byte) (b + BYTE_DIFF));
                    } else {
                        throw new IllegalArgumentException("Invalid byte: " + b);
                    }
                }
                buffer.writeByte(BYTE_NULL);
                return buffer;
            };
            case UPPER_SNAKE_CASE -> bytes -> {
                HeapWriteBuffer buffer = new HeapWriteBuffer(Math.incrementExact(bytes.length));
                for (byte b : bytes) {
                    if(b >= BYTE_A && b <= BYTE_Z) {
                        buffer.writeByte((byte) (b + BYTE_DIFF));
                    } else if(b == BYTE_UNDERSCORE) {
                        buffer.writeByte(BYTE_NULL);
                    } else {
                        throw new IllegalArgumentException("Invalid byte: " + b);
                    }
                }
                buffer.writeByte(BYTE_NULL);
                return buffer;
            };
            case UPPER_KEBAB_CASE -> bytes -> {
                HeapWriteBuffer buffer = new HeapWriteBuffer(Math.incrementExact(bytes.length));
                for (byte b : bytes) {
                    if(b >= BYTE_A && b <= BYTE_Z) {
                        buffer.writeByte((byte) (b + BYTE_DIFF));
                    } else if(b == BYTE_MINUS) {
                        buffer.writeByte(BYTE_NULL);
                    } else {
                        throw new IllegalArgumentException("Invalid byte: " + b);
                    }
                }
                buffer.writeByte(BYTE_NULL);
                return buffer;
            };
        };
    }

    public static String cast(NamingConvention from, NamingConvention to, String name) {
        if(name == null || name.isBlank()) {
            throw new IllegalArgumentException("Invalid name: " + name);
        }
        byte[] normalized = toBufferFunc(from).apply(name.getBytes(StandardCharsets.UTF_8)).toByteArray();
        HeapWriteBuffer buffer = new HeapWriteBuffer(normalized.length);
        int index = 0;
        for(int i = 0; i < normalized.length; i++) {
            byte b = normalized[i];
            if(b == BYTE_NULL) {
                switch (to) {
                    case ORIGINAL -> throw new UnsupportedOperationException();
                    case CAMEL_CASE -> {
                        if(buffer.intPosition() == 0) {
                            buffer.writeBytes(normalized, index, i - index);
                        } else {
                            byte[] temp = Arrays.copyOfRange(normalized, index, i);
                            temp[0] = (byte) (temp[0] - BYTE_DIFF);
                            buffer.writeBytes(temp);
                        }
                    }
                    case SNAKE_CASE -> {
                        buffer.writeBytes(normalized, index, i - index);
                        if(i != normalized.length - 1) {
                            buffer.writeByte(BYTE_UNDERSCORE);
                        }
                    }
                    case KEBAB_CASE -> {
                        buffer.writeBytes(normalized, index, i - index);
                        if(i != normalized.length - 1) {
                            buffer.writeByte(BYTE_MINUS);
                        }
                    }
                    case PASCAL_CASE -> {
                        byte[] temp = Arrays.copyOfRange(normalized, index, i);
                        temp[0] = (byte) (temp[0] - BYTE_DIFF);
                        buffer.writeBytes(temp);
                    }
                    case UPPER_SNAKE_CASE -> {
                        byte[] temp = Arrays.copyOfRange(normalized, index, i);
                        for (byte t : temp) {
                            buffer.writeByte((byte) (t - BYTE_DIFF));
                        }
                        if(i != normalized.length - 1) {
                            buffer.writeByte(BYTE_UNDERSCORE);
                        }
                    }
                    case UPPER_KEBAB_CASE -> {
                        byte[] temp = Arrays.copyOfRange(normalized, index, i);
                        for (byte t : temp) {
                            buffer.writeByte((byte) (t - BYTE_DIFF));
                        }
                        if(i != normalized.length - 1) {
                            buffer.writeByte(BYTE_MINUS);
                        }
                    }
                }
                index = i + 1;
            }
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }
}
