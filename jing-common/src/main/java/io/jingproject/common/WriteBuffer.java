package io.jingproject.common;

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public interface WriteBuffer {
    void writeByte(byte b);

    void writeBytes(byte b1, byte b2);

    void writeBytes(byte b1, byte b2, byte b3);

    void writeBytes(byte[] bytes, int off, int len);

    default void writeBytes(byte[] bytes) {
        writeBytes(bytes, 0, bytes.length);
    }

    void writeShort(short s, ByteOrder byteOrder);

    default void writeShort(short s) {
        writeShort(s, ByteOrder.nativeOrder());
    }

    void writeChar(char c, ByteOrder byteOrder);

    default void writeChar(char c) {
        writeChar(c, ByteOrder.nativeOrder());
    }

    void writeInt(int i, ByteOrder byteOrder);

    default void writeInt(int i) {
        writeInt(i, ByteOrder.nativeOrder());
    }

    void writeLong(long l, ByteOrder byteOrder);

    default void writeLong(long l) {
        writeLong(l, ByteOrder.nativeOrder());
    }

    void writeFloat(float f, ByteOrder byteOrder);

    default void writeFloat(float f) {
        writeFloat(f, ByteOrder.nativeOrder());
    }

    void writeDouble(double d, ByteOrder byteOrder);

    default void writeDouble(double d) {
        writeDouble(d, ByteOrder.nativeOrder());
    }

    default void writeString(String str, Charset charset) {
        byte[] bytes = str.getBytes(charset);
        writeBytes(bytes, 0, bytes.length);
    }

    default void writeString(String str) {
        writeString(str, StandardCharsets.UTF_8);
    }

    default void writeUtf8CodePoint(int codePoint) {
        if (codePoint < 0x80) {
            writeByte((byte) codePoint);
        } else if (codePoint < 0x800) {
            writeBytes((byte) (0xC0 | (codePoint >> 6)), (byte) (0x80 | (codePoint & 0x3F)));
        } else {
            writeBytes((byte) (0xE0 | (codePoint >> 12)), (byte) (0x80 | ((codePoint >> 6) & 0x3F)), (byte) (0x80 | (codePoint & 0x3F)));
        }
    }

    long position();
}
