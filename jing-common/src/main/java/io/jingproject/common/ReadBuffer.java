package io.jingproject.common;

import java.lang.foreign.MemorySegment;
import java.nio.ByteOrder;

public sealed interface ReadBuffer permits HeapReadBuffer, SegmentReadBuffer {

    int intPosition();

    int intLength();

    void setPosition(int newPosition);

    long longPosition();

    long longLength();

    void setPosition(long newPosition);

    byte readByte();

    void readBytes(byte[] bytes, int offset, int length);

    default void readBytes(byte[] bytes) {
        readBytes(bytes, 0, bytes.length);
    }

    void readSegment(MemorySegment segment, long offset, long length);

    default void readSegment(MemorySegment segment) {
        readSegment(segment, 0L, segment.byteSize());
    }

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

    default MemorySegment readAddress(ByteOrder byteOrder) {
        long rawAddress = readLong(byteOrder);
        return MemorySegment.ofAddress(rawAddress);
    }

    default MemorySegment readAddress() {
        return readAddress(ByteOrder.nativeOrder());
    }

    void reset();
}
