package io.jingproject.common;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.nio.ByteOrder;
import java.util.Objects;

public final class SegmentReadBuffer implements ReadBuffer {

    static {
        try {
            Class<Os> _ = MethodHandles.lookup().ensureInitialized(Os.class);
        } catch (IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final MemorySegment buffer;
    private long position;

    public SegmentReadBuffer(MemorySegment segment) {
        this.buffer = segment.isReadOnly() ? segment : segment.asReadOnly();
        this.position = 0L;
    }

    @Override
    public int intPosition() {
        return Math.toIntExact(position);
    }

    @Override
    public int intLength() {
        return Math.toIntExact(buffer.byteSize());
    }

    @Override
    public void setPosition(int newPosition) {
        assert newPosition >= 0L && newPosition <= buffer.byteSize();
        position = Math.toIntExact(newPosition);
    }

    @Override
    public long longPosition() {
        return position;
    }

    @Override
    public long longLength() {
        return buffer.byteSize();
    }

    @Override
    public void setPosition(long newPosition) {
        assert newPosition >= 0L && newPosition <= buffer.byteSize();
        position = newPosition;
    }

    @Override
    public byte readByte() {
        long newPosition = Math.incrementExact(position);
        if (newPosition > buffer.byteSize()) {
            throw new IndexOutOfBoundsException();
        }
        byte r = SegmentAccess.getByte(buffer, position);
        position = newPosition;
        return r;
    }

    @Override
    public void readBytes(byte[] bytes, int offset, int length) {
        assert bytes != null && Objects.checkFromIndexSize(offset, bytes.length, length) >= 0;
        long newPosition = Math.addExact(position, length);
        if (newPosition > buffer.byteSize()) {
            throw new IndexOutOfBoundsException();
        }
        MemorySegment.copy(buffer, ValueLayout.JAVA_BYTE, position, bytes, offset, length);
        position = newPosition;
    }

    @Override
    public void readSegment(MemorySegment segment, long offset, long length) {
        assert segment != null && Objects.checkFromIndexSize(offset, segment.byteSize(), length) >= 0;
        long newPosition = Math.addExact(position, length);
        if (newPosition > buffer.byteSize()) {
            throw new IndexOutOfBoundsException();
        }
        MemorySegment.copy(buffer, position, segment, offset, length);
        position = newPosition;
    }

    @Override
    public short readShort(ByteOrder byteOrder) {
        long newPosition = Math.addExact(position, 2);
        if (newPosition > buffer.byteSize()) {
            throw new IndexOutOfBoundsException();
        }
        short r = SegmentAccess.getShort(buffer, position, byteOrder);
        position = newPosition;
        return r;
    }

    @Override
    public char readChar(ByteOrder byteOrder) {
        long newPosition = Math.addExact(position, 2);
        if (newPosition > buffer.byteSize()) {
            throw new IndexOutOfBoundsException();
        }
        char r = SegmentAccess.getChar(buffer, position, byteOrder);
        position = newPosition;
        return r;
    }

    @Override
    public int readInt(ByteOrder byteOrder) {
        long newPosition = Math.addExact(position, 4);
        if (newPosition > buffer.byteSize()) {
            throw new IndexOutOfBoundsException();
        }
        int r = SegmentAccess.getInt(buffer, position, byteOrder);
        position = newPosition;
        return r;
    }

    @Override
    public long readLong(ByteOrder byteOrder) {
        long newPosition = Math.addExact(position, 8);
        if (newPosition > buffer.byteSize()) {
            throw new IndexOutOfBoundsException();
        }
        long r = SegmentAccess.getLong(buffer, position, byteOrder);
        position = newPosition;
        return r;
    }

    @Override
    public float readFloat(ByteOrder byteOrder) {
        long newPosition = Math.addExact(position, 4);
        if (newPosition > buffer.byteSize()) {
            throw new IndexOutOfBoundsException();
        }
        float r = SegmentAccess.getFloat(buffer, position, byteOrder);
        position = newPosition;
        return r;
    }

    @Override
    public double readDouble(ByteOrder byteOrder) {
        long newPosition = Math.addExact(position, 8);
        if (newPosition > buffer.byteSize()) {
            throw new IndexOutOfBoundsException();
        }
        double r = SegmentAccess.getDouble(buffer, position, byteOrder);
        position = newPosition;
        return r;
    }

    @Override
    public void reset() {
        position = 0L;
    }

    public MemorySegment rawSegment() {
        return buffer;
    }
}
