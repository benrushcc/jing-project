package io.jingproject.common;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.nio.ByteOrder;
import java.util.Objects;

public final class HeapReadBuffer implements ReadBuffer {

    static {
        try {
            Class<Os> _ = MethodHandles.lookup().ensureInitialized(Os.class);
        } catch (IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final byte[] buffer;
    private final int length;
    private int position;

    public HeapReadBuffer(byte[] buffer) {
        this(buffer, 0, buffer.length);
    }

    public HeapReadBuffer(byte[] buffer, int offset, int len) {
        Objects.checkFromIndexSize(offset, len, buffer.length);
        this.buffer = buffer;
        this.position = offset;
        this.length = offset + len;
    }

    @Override
    public int intPosition() {
        return position;
    }

    @Override
    public int intLength() {
        return length;
    }

    @Override
    public void setPosition(int newPosition) {
        assert newPosition >= 0 && newPosition <= length;
        position = newPosition;
    }

    @Override
    public long longPosition() {
        return position;
    }

    @Override
    public long longLength() {
        return length;
    }

    @Override
    public void setPosition(long newPosition) {
        assert newPosition >= 0 && newPosition <= length;
        position = Math.toIntExact(newPosition);
    }

    @Override
    public byte readByte() {
        int newPosition = Math.incrementExact(position);
        if (newPosition > length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        byte r = buffer[position];
        position = newPosition;
        return r;
    }

    @Override
    public void readBytes(byte[] bytes, int offset, int length) {
        assert Objects.checkFromIndexSize(offset, length, length) >= 0;
        int newPosition = Math.addExact(position, length);
        if (newPosition > length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        System.arraycopy(buffer, position, bytes, offset, length);
        position = newPosition;
    }

    @Override
    public void readSegment(MemorySegment segment, long offset, long length) {
        int intLen = Math.toIntExact(length);
        int newPosition = Math.addExact(position, intLen);
        if (newPosition > length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        MemorySegment.copy(buffer, position, segment, ValueLayout.JAVA_BYTE, offset, intLen);
        position = newPosition;
    }

    @Override
    public short readShort(ByteOrder byteOrder) {
        int newPosition = Math.addExact(position, 2);
        if (newPosition > length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        short r = ArrayAccess.getShort(buffer, position, byteOrder);
        position = newPosition;
        return r;
    }

    @Override
    public char readChar(ByteOrder byteOrder) {
        int newPosition = Math.addExact(position, 2);
        if (newPosition > length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        char r = ArrayAccess.getChar(buffer, position, byteOrder);
        position = newPosition;
        return r;
    }

    @Override
    public int readInt(ByteOrder byteOrder) {
        int newPosition = Math.addExact(position, 4);
        if (newPosition > length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        int r = ArrayAccess.getInt(buffer, position, byteOrder);
        position = newPosition;
        return r;
    }

    @Override
    public long readLong(ByteOrder byteOrder) {
        int newPosition = Math.addExact(position, 8);
        if (newPosition > length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        long r = ArrayAccess.getLong(buffer, position, byteOrder);
        position = newPosition;
        return r;
    }

    @Override
    public float readFloat(ByteOrder byteOrder) {
        int newPosition = Math.addExact(position, 4);
        if (newPosition > length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        float r = ArrayAccess.getFloat(buffer, position, byteOrder);
        position = newPosition;
        return r;
    }

    @Override
    public double readDouble(ByteOrder byteOrder) {
        int newPosition = Math.addExact(position, 8);
        if (newPosition > length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        double r = ArrayAccess.getDouble(buffer, position, byteOrder);
        position = newPosition;
        return r;
    }

    @Override
    public void reset() {
        position = 0;
    }

    // TODO 现在是所有用到的地方的语义都要跟着变一下，因为readBuffer现在是一个切片的概念，直接操纵buffer的length是可能导致越界的，不安全
    public byte[] rawByteArray() {
        return buffer;
    }
}
