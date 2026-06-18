package io.jingproject.common;

import java.lang.foreign.MemorySegment;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public sealed interface WriteBuffer permits HeapWriteBuffer, SegmentWriteBuffer {
    int intPosition();

    void setPosition(int newPosition);

    long longPosition();

    void setPosition(long newPosition);

    void ensureCapacity(int capacity);

    void ensureCapacity(long capacity);

    void writeByte(byte b);

    void writeBytes(byte b1, byte b2);

    void writeBytes(byte b1, byte b2, byte b3);

    void writeBytes(byte b1, byte b2, byte b3, byte b4);

    void writeBytes(byte[] bytes, int offset, int length);

    default void writeBytes(byte[] bytes) {
        writeBytes(bytes, 0, bytes.length);
    }

    void writeRepeated(byte b, int count);

    void writeSegment(MemorySegment segment, long offset, long length);

    default void writeSegment(MemorySegment segment) {
        writeSegment(segment, 0L, segment.byteSize());
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

    byte[] toByteArray();

    default void writeString(String str, Charset charset) {
        byte[] bytes = str.getBytes(charset);
        writeBytes(bytes, 0, bytes.length);
    }

    default void writeString(String str) {
        writeString(str, StandardCharsets.UTF_8);
    }

    default void writeCodePointInUtf8(int codePoint) {
        if (!Character.isValidCodePoint(codePoint)) {
            throw new IllegalArgumentException("not a valid code point: " + codePoint);
        }
        if (codePoint < 0x80) {
            writeByte((byte) codePoint);
        } else if (codePoint < 0x800) {
            writeBytes((byte) (0xC0 | (codePoint >> 6)), (byte) (0x80 | (codePoint & 0x3F)));
        } else if (codePoint < 0x10000) {
            writeBytes((byte) (0xE0 | (codePoint >> 12)), (byte) (0x80 | ((codePoint >> 6) & 0x3F)), (byte) (0x80 | (codePoint & 0x3F)));
        } else {
            writeBytes((byte) (0xF0 | (codePoint >> 18)), (byte) (0x80 | ((codePoint >> 12) & 0x3F)), (byte) (0x80 | ((codePoint >> 6) & 0x3F)), (byte) (0x80 | (codePoint & 0x3F)));
        }
    }

}
