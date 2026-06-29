package io.jingproject.marshalljson;

public final class JsonSerializerContext {
    private static final int MIN_SIZE = 16;
    private char[] buffer;

    public JsonSerializerContext(int size) {
        if(size < MIN_SIZE) {
            throw new IllegalArgumentException("buffer size too small");
        }
        this.buffer = new char[size];
    }

    public char[] buffer(int required) {
        if(required > buffer.length) {
            int newLen = 1 << (32 - Integer.numberOfLeadingZeros(required));
            if(newLen < 0) {
                throw new JsonSerializerException("buffer overflow");
            }
            buffer = new char[newLen];
        }
        return buffer;
    }
}
