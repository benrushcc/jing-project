package io.jingproject.common;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;

public final class SegmentReadBuffer implements ReadBuffer {

    private final MemorySegment segment;
    private long position;

    public SegmentReadBuffer(MemorySegment segment) {
        this.segment = segment.isReadOnly() ? segment : segment.asReadOnly();
    }

    @Override
    public byte readByte() {
        long newPosition = Math.addExact(position, ValueLayout.JAVA_BYTE.byteSize());
        if (newPosition > segment.byteSize()) {
            throw new IndexOutOfBoundsException();
        }
        byte r = segment.get(ValueLayout.JAVA_BYTE, position);
        position = newPosition;
        return r;
    }

    @Override
    public byte[] readBytes(int len) {
        long newPosition = Math.addExact(position, Math.multiplyExact(len, ValueLayout.JAVA_BYTE.byteSize()));
        if (newPosition > segment.byteSize()) {
            throw new IndexOutOfBoundsException();
        }
        byte[] r = new byte[len];
        MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, position, r, 0, len);
        position = newPosition;
        return r;
    }

    @Override
    public MemorySegment readSegment(long len) {
        byte[] r = readBytes(Math.toIntExact(len));
        return MemorySegment.ofArray(r);
    }

    @Override
    public short readShort(ByteOrder byteOrder) {
        long newPosition = Math.addExact(position, ValueLayout.JAVA_SHORT.byteSize());
        if (newPosition > segment.byteSize()) {
            throw new IndexOutOfBoundsException();
        }
        short r = segment.get(ValueLayout.JAVA_SHORT, position);
        position = newPosition;
        return r;
    }

    @Override
    public char readChar(ByteOrder byteOrder) {
        long newPosition = Math.addExact(position, ValueLayout.JAVA_CHAR.byteSize());
        if (newPosition > segment.byteSize()) {
            throw new IndexOutOfBoundsException();
        }
        char r = segment.get(ValueLayout.JAVA_CHAR, position);
        position = newPosition;
        return r;
    }

    @Override
    public int readInt(ByteOrder byteOrder) {
        long newPosition = Math.addExact(position, ValueLayout.JAVA_INT.byteSize());
        if (newPosition > segment.byteSize()) {
            throw new IndexOutOfBoundsException();
        }
        int r = segment.get(ValueLayout.JAVA_INT, position);
        position = newPosition;
        return r;
    }

    @Override
    public long readLong(ByteOrder byteOrder) {
        long newPosition = Math.addExact(position, ValueLayout.JAVA_LONG.byteSize());
        if (newPosition > segment.byteSize()) {
            throw new IndexOutOfBoundsException();
        }
        long r = segment.get(ValueLayout.JAVA_LONG, position);
        position = newPosition;
        return r;
    }

    @Override
    public float readFloat(ByteOrder byteOrder) {
        long newPosition = Math.addExact(position, ValueLayout.JAVA_FLOAT.byteSize());
        if (newPosition > segment.byteSize()) {
            throw new IndexOutOfBoundsException();
        }
        float r = segment.get(ValueLayout.JAVA_FLOAT, position);
        position = newPosition;
        return r;
    }

    @Override
    public double readDouble(ByteOrder byteOrder) {
        long newPosition = Math.addExact(position, ValueLayout.JAVA_DOUBLE.byteSize());
        if (newPosition > segment.byteSize()) {
            throw new IndexOutOfBoundsException();
        }
        double r = segment.get(ValueLayout.JAVA_DOUBLE, position);
        position = newPosition;
        return r;
    }

    @Override
    public int intIndex() {
        return Math.toIntExact(position);
    }

    @Override
    public long longIndex() {
        return position;
    }

    @Override
    public int intLength() {
        return Math.toIntExact(segment.byteSize());
    }

    @Override
    public long longLength() {
        return segment.byteSize();
    }
}
