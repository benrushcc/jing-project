package io.jingproject.common;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.nio.ByteOrder;

public final class SegmentWriteBuffer implements WriteBuffer {
    static {
        try {
            Class<Os> _ = MethodHandles.lookup().ensureInitialized(Os.class);
        } catch (IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final long MIN_INITIAL_SIZE = 4L;
    private static final long MIN_LIMIT = 256L;

    private final SegmentAllocator alloc;
    private final long limit;
    private MemorySegment seg;
    private long position;

    public SegmentWriteBuffer(SegmentAllocator allocator, long initialSize) {
        this(allocator, initialSize, Integer.MAX_VALUE);
    }

    public SegmentWriteBuffer(SegmentAllocator allocator, long initialSize, long limit) {
        if(allocator == null) {
            throw new IllegalArgumentException("allocator cannot be null");
        }
        if(initialSize < MIN_INITIAL_SIZE) {
            throw new IllegalArgumentException("initial size must be at least " + MIN_INITIAL_SIZE);
        }
        if(limit < MIN_LIMIT) {
            throw new IllegalArgumentException("limit must be at least " + MIN_LIMIT);
        }
        this.alloc = allocator;
        this.seg = allocator.allocate(initialSize);
        
        this.position = 0L;
        this.limit = limit;
    }

    private void growBufferIfNeeded(long requiredCapacity) {
        
        long currentCapacity = seg.byteSize();
        if (currentCapacity < requiredCapacity) {
            long growedCapacity = Math.addExact(seg.byteSize(), seg.byteSize());
            long newLength = Math.max(growedCapacity, requiredCapacity);
            if(newLength > limit) {
                throw new SizeLimitExceededException(newLength, limit);
            }
            MemorySegment newSegment = alloc.allocate(newLength);
            MemorySegment.copy(seg, 0L, newSegment, 0L, position);
            seg = newSegment;
        }
    }

    @Override
    public int intPosition() {
        return Math.toIntExact(position);
    }

    @Override
    public void setPosition(int newPosition) {
        
        position = newPosition;
    }

    @Override
    public long longPosition() {
        return position;
    }

    @Override
    public void setPosition(long newPosition) {
        
        position = newPosition;
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
        growBufferIfNeeded(Math.addExact(position, capacity));
    }

    @Override
    public void writeByte(byte b) {
        long newPosition = Math.incrementExact(position);
        growBufferIfNeeded(newPosition);
        SegmentAccess.setByte(seg, position, b);
        position = newPosition;
    }

    @Override
    public void writeBytes(byte b1, byte b2) {
        long newPosition = Math.addExact(position, 2);
        growBufferIfNeeded(newPosition);
        SegmentAccess.setByte(seg, position, b1);
        SegmentAccess.setByte(seg, position + 1, b2);
        position = newPosition;
    }

    @Override
    public void writeBytes(byte b1, byte b2, byte b3) {
        long newPosition = Math.addExact(position, 3);
        growBufferIfNeeded(newPosition);
        SegmentAccess.setByte(seg, position, b1);
        SegmentAccess.setByte(seg, position + 1, b2);
        SegmentAccess.setByte(seg, position + 2, b3);
        position = newPosition;
    }

    @Override
    public void writeBytes(byte b1, byte b2, byte b3, byte b4) {
        long newPosition = Math.addExact(position, 4);
        growBufferIfNeeded(newPosition);
        SegmentAccess.setByte(seg, position, b1);
        SegmentAccess.setByte(seg, position + 1, b2);
        SegmentAccess.setByte(seg, position + 2, b3);
        SegmentAccess.setByte(seg, position + 3, b4);
        position = newPosition;
    }

    @Override
    public void writeBytes(byte b1, byte b2, byte b3, byte b4, byte b5) {
        long newPosition = Math.addExact(position, 5);
        growBufferIfNeeded(newPosition);
        SegmentAccess.setByte(seg, position, b1);
        SegmentAccess.setByte(seg, position + 1, b2);
        SegmentAccess.setByte(seg, position + 2, b3);
        SegmentAccess.setByte(seg, position + 3, b4);
        SegmentAccess.setByte(seg, position + 4, b5);
        position = newPosition;
    }

    @Override
    public void writeBytes(byte b1, byte b2, byte b3, byte b4, byte b5, byte b6) {
        long newPosition = Math.addExact(position, 6);
        growBufferIfNeeded(newPosition);
        SegmentAccess.setByte(seg, position, b1);
        SegmentAccess.setByte(seg, position + 1, b2);
        SegmentAccess.setByte(seg, position + 2, b3);
        SegmentAccess.setByte(seg, position + 3, b4);
        SegmentAccess.setByte(seg, position + 4, b5);
        SegmentAccess.setByte(seg, position + 5, b6);
        position = newPosition;
    }

    @Override
    public void writeBytes(byte[] bytes, int offset, int length) {
        
        long newPosition = Math.addExact(position, length);
        growBufferIfNeeded(newPosition);
        MemorySegment.copy(bytes, offset, seg, ValueLayout.JAVA_BYTE, position, length);
        position = newPosition;
    }

    @Override
    public void writeRepeated(byte b, int count) {
        
        long newPosition = Math.addExact(position, count);
        growBufferIfNeeded(newPosition);
        seg.asSlice(position, count).fill(b);
        position = newPosition;
    }

    @Override
    public void writeSegment(MemorySegment segment, long offset, long length) {
        
        long newPosition = Math.addExact(position, length);
        growBufferIfNeeded(newPosition);
        MemorySegment.copy(segment, offset, seg, position, length);
        position = newPosition;
    }

    @Override
    public void writeShort(short s, ByteOrder byteOrder) {
        long newPosition = Math.addExact(position, 2);
        growBufferIfNeeded(newPosition);
        SegmentAccess.setShort(seg, position, s, byteOrder);
        position = newPosition;
    }

    @Override
    public void writeChar(char c, ByteOrder byteOrder) {
        long newPosition = Math.addExact(position, 2);
        growBufferIfNeeded(newPosition);
        SegmentAccess.setChar(seg, position, c, byteOrder);
        position = newPosition;
    }

    @Override
    public void writeInt(int i, ByteOrder byteOrder) {
        long newPosition = Math.addExact(position, 4);
        growBufferIfNeeded(newPosition);
        SegmentAccess.setInt(seg, position, i, byteOrder);
        position = newPosition;
    }

    @Override
    public void writeLong(long l, ByteOrder byteOrder) {
        long newPosition = Math.addExact(position, 8);
        growBufferIfNeeded(newPosition);
        SegmentAccess.setLong(seg, position, l, byteOrder);
        position = newPosition;
    }

    @Override
    public void writeFloat(float f, ByteOrder byteOrder) {
        long newPosition = Math.addExact(position, 4);
        growBufferIfNeeded(newPosition);
        SegmentAccess.setFloat(seg, position, f, byteOrder);
        position = newPosition;
    }

    @Override
    public void writeDouble(double d, ByteOrder byteOrder) {
        long newPosition = Math.addExact(position, 8);
        growBufferIfNeeded(newPosition);
        SegmentAccess.setDouble(seg, position, d, byteOrder);
        position = newPosition;
    }

    public MemorySegment rawSegment() {
        return seg;
    }

    @Override
    public byte[] toByteArray() {
        if (position == 0L) {
            return Utils.emptyByteArray();
        }
        return seg.asSlice(0L, position).toArray(ValueLayout.JAVA_BYTE);
    }

    @Override
    public void reset() {
        position = 0L;
    }
}
