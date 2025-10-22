package io.jingproject.common;

import java.nio.ByteOrder;

public interface ReadBuffer {

    byte readByte();

    byte[] readBytes(int len);

    short readShort(ByteOrder byteOrder);

    default short readShort() {
        return readShort(ByteOrder.nativeOrder());
    }

    char readChar(ByteOrder byteOrder);

    default char readChar() {
        return readChar(ByteOrder.nativeOrder());
    }

    int readInt(ByteOrder byteOrder);

    default int readInt() {
        return readInt(ByteOrder.nativeOrder());
    }

    long readLong(ByteOrder byteOrder);

    default long readLong() {
        return readLong(ByteOrder.nativeOrder());
    }

    float readFloat(ByteOrder byteOrder);

    default float readFloat() {
        return readFloat(ByteOrder.nativeOrder());
    }

    double readDouble(ByteOrder byteOrder);

    default double readDouble() {
        return readDouble(ByteOrder.nativeOrder());
    }
}
