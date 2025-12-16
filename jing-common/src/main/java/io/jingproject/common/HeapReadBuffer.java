package io.jingproject.common;

import java.lang.foreign.MemorySegment;
import java.nio.ByteOrder;
import java.util.Arrays;

public final class HeapReadBuffer implements ReadBuffer {

    private final byte[] buffer;
    private int position;

    public HeapReadBuffer(byte[] buffer) {
        this.buffer = buffer;
    }

    @Override
    public byte readByte() {
        int newPosition = Math.addExact(position, Byte.BYTES);
        if(newPosition > buffer.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        byte r = buffer[newPosition];
        position = newPosition;
        return r;
    }

    @Override
    public byte[] readBytes(int len) {
        int newPosition = Math.addExact(position, len);
        if(newPosition > buffer.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        byte[] r = Arrays.copyOfRange(buffer, position, len);
        position = newPosition;
        return r;
    }

    @Override
    public MemorySegment readSegment(long len) {
        int intLen = Math.toIntExact(len);
        int newPosition = Math.addExact(position, intLen);
        if(newPosition > buffer.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        byte[] r = Arrays.copyOfRange(buffer, position, intLen);
        position = newPosition;
        return MemorySegment.ofArray(r);
    }

    @Override
    public short readShort(ByteOrder byteOrder) {
        int newPosition = Math.addExact(position, Short.BYTES);
        if(newPosition > buffer.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        short r = ArrayAccess.getShort(buffer, position);
        position = newPosition;
        return r;
    }

    @Override
    public char readChar(ByteOrder byteOrder) {
        int newPosition = Math.addExact(position, Character.BYTES);
        if(newPosition > buffer.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        char r = ArrayAccess.getChar(buffer, position);
        position = newPosition;
        return r;
    }

    @Override
    public int readInt(ByteOrder byteOrder) {
        int newPosition = Math.addExact(position, Integer.BYTES);
        if(newPosition > buffer.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        int r = ArrayAccess.getInt(buffer, position);
        position = newPosition;
        return r;
    }

    @Override
    public long readLong(ByteOrder byteOrder) {
        int newPosition = Math.addExact(position, Long.BYTES);
        if(newPosition > buffer.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        long r = ArrayAccess.getLong(buffer, position);
        position = newPosition;
        return r;
    }

    @Override
    public float readFloat(ByteOrder byteOrder) {
        int newPosition = Math.addExact(position, Float.BYTES);
        if(newPosition > buffer.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        float r = ArrayAccess.getFloat(buffer, position);
        position = newPosition;
        return r;
    }

    @Override
    public double readDouble(ByteOrder byteOrder) {
        int newPosition = Math.addExact(position, Double.BYTES);
        if(newPosition > buffer.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        double r = ArrayAccess.getDouble(buffer, position);
        position = newPosition;
        return r;
    }

    @Override
    public int intIndex() {
        return position;
    }

    @Override
    public long longIndex() {
        return position;
    }

    @Override
    public int intLength() {
        return buffer.length;
    }

    @Override
    public long longLength() {
        return buffer.length;
    }
}
