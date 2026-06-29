package io.jingproject.common;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;

public final class HeapWriteBuffer implements WriteBuffer {

    static {
        try {
            Class<Os> _ = MethodHandles.lookup().ensureInitialized(Os.class);
        } catch (IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final int limit;
    private byte[] buffer;
    private int position;


    public HeapWriteBuffer(byte[] buffer) {
        this(buffer, Integer.MAX_VALUE);
    }

    public HeapWriteBuffer(int size) {
        this(new byte[size], Integer.MAX_VALUE);
    }

    public HeapWriteBuffer(int size, int limit) {
        this(new byte[size], limit);
    }

    public HeapWriteBuffer(byte[] buffer, int limit) {
        if (buffer == null || buffer.length == 0) {
            throw new IllegalArgumentException("empty buffer");
        }
        if(limit < 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        this.buffer = buffer;
        this.position = 0;
        this.limit = limit;
    }

    private void growBufferIfNeeded(int requiredCapacity) {
        assert requiredCapacity > 0;
        int currentCapacity = buffer.length;
        if (currentCapacity < requiredCapacity) {
            int growedCapacity = Math.addExact(currentCapacity, currentCapacity);
            int newLength = Math.max(growedCapacity, requiredCapacity);
            if(newLength > limit) {
                throw new SizeLimitExceededException(newLength, limit);
            }
            buffer = Arrays.copyOf(buffer, newLength);
        }
    }

    @Override
    public int intPosition() {
        return position;
    }

    @Override
    public void setPosition(int newPosition) {
        assert newPosition >= 0 && newPosition <= buffer.length;
        position = newPosition;
    }

    @Override
    public long longPosition() {
        return position;
    }

    @Override
    public void setPosition(long newPosition) {
        assert newPosition >= 0L && Math.toIntExact(newPosition) <= buffer.length;
        position = Math.toIntExact(newPosition);
    }

    @Override
    public void ensureCapacity(int capacity) {
        if(capacity < 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        growBufferIfNeeded(Math.addExact(position, capacity));
    }

    @Override
    public void ensureCapacity(long capacity) {
        if(capacity < 0L) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        growBufferIfNeeded(Math.addExact(position, Math.toIntExact(capacity)));
    }

    @Override
    public void writeByte(byte b) {
        int newPosition = Math.incrementExact(position);
        growBufferIfNeeded(newPosition);
        buffer[position] = b;
        position = newPosition;
    }

    @Override
    public void writeBytes(byte b1, byte b2) {
        int newPosition = Math.addExact(position, 2);
        growBufferIfNeeded(newPosition);
        buffer[position] = b1;
        buffer[position + 1] = b2;
        position = newPosition;
    }

    @Override
    public void writeBytes(byte b1, byte b2, byte b3) {
        int newPosition = Math.addExact(position, 3);
        growBufferIfNeeded(newPosition);
        buffer[position] = b1;
        buffer[position + 1] = b2;
        buffer[position + 2] = b3;
        position = newPosition;
    }

    @Override
    public void writeBytes(byte b1, byte b2, byte b3, byte b4) {
        int newPosition = Math.addExact(position, 4);
        growBufferIfNeeded(newPosition);
        buffer[position] = b1;
        buffer[position + 1] = b2;
        buffer[position + 2] = b3;
        buffer[position + 3] = b4;
        position = newPosition;
    }

    @Override
    public void writeBytes(byte b1, byte b2, byte b3, byte b4, byte b5) {
        int newPosition = Math.addExact(position, 5);
        growBufferIfNeeded(newPosition);
        buffer[position] = b1;
        buffer[position + 1] = b2;
        buffer[position + 2] = b3;
        buffer[position + 3] = b4;
        buffer[position + 4] = b5;
        position = newPosition;
    }

    @Override
    public void writeBytes(byte b1, byte b2, byte b3, byte b4, byte b5, byte b6) {
        int newPosition = Math.addExact(position, 6);
        growBufferIfNeeded(newPosition);
        buffer[position] = b1;
        buffer[position + 1] = b2;
        buffer[position + 2] = b3;
        buffer[position + 3] = b4;
        buffer[position + 4] = b5;
        buffer[position + 5] = b6;
        position = newPosition;
    }

    @Override
    public void writeBytes(byte[] bytes, int offset, int length) {
        assert bytes != null && Objects.checkFromIndexSize(offset, length, bytes.length) >= 0;
        int newPosition = Math.addExact(position, length);
        growBufferIfNeeded(newPosition);
        System.arraycopy(bytes, offset, buffer, position, length);
        position = newPosition;
    }

    @Override
    public void writeRepeated(byte b, int count) {
        assert count > 0;
        int newPosition = Math.addExact(position, count);
        growBufferIfNeeded(newPosition);
        Arrays.fill(buffer, position, newPosition, b);
        position = newPosition;
    }

    @Override
    public void writeSegment(MemorySegment segment, long offset, long length) {
        assert segment != null && Objects.checkFromIndexSize(offset, length, segment.byteSize()) >= 0;
        int intLength = Math.toIntExact(length);
        int newPosition = Math.addExact(position, intLength);
        growBufferIfNeeded(newPosition);
        MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, offset, buffer, position, intLength);
        position = newPosition;
    }

    @Override
    public void writeShort(short s, ByteOrder byteOrder) {
        int newPosition = Math.addExact(position, 2);
        growBufferIfNeeded(newPosition);
        ArrayAccess.setShort(buffer, position, s, byteOrder);
        position = newPosition;
    }

    @Override
    public void writeChar(char c, ByteOrder byteOrder) {
        int newPosition = Math.addExact(position, 2);
        growBufferIfNeeded(newPosition);
        ArrayAccess.setChar(buffer, position, c, byteOrder);
        position = newPosition;
    }

    @Override
    public void writeInt(int i, ByteOrder byteOrder) {
        int newPosition = Math.addExact(position, 4);
        growBufferIfNeeded(newPosition);
        ArrayAccess.setInt(buffer, position, i, byteOrder);
        position = newPosition;
    }

    @Override
    public void writeLong(long l, ByteOrder byteOrder) {
        int newPosition = Math.addExact(position, 8);
        growBufferIfNeeded(newPosition);
        ArrayAccess.setLong(buffer, position, l, byteOrder);
        position = newPosition;
    }

    @Override
    public void writeFloat(float f, ByteOrder byteOrder) {
        int newPosition = Math.addExact(position, 4);
        growBufferIfNeeded(newPosition);
        ArrayAccess.setFloat(buffer, position, f, byteOrder);
        position = newPosition;
    }

    @Override
    public void writeDouble(double d, ByteOrder byteOrder) {
        int newPosition = Math.addExact(position, 8);
        growBufferIfNeeded(newPosition);
        ArrayAccess.setDouble(buffer, position, d, byteOrder);
        position = newPosition;
    }

    public byte[] rawByteArray() {
        return buffer;
    }

    @Override
    public byte[] toByteArray() {
        if (position == buffer.length) {
            return buffer;
        }
        return Arrays.copyOf(buffer, position);
    }

    public void reset() {
        position = 0;
    }
}
