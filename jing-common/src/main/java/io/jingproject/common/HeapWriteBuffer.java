package io.jingproject.common;

import java.lang.invoke.MethodHandles;
import java.nio.ByteOrder;
import java.util.Arrays;

public final class HeapWriteBuffer implements WriteBuffer {

    static {
        try {
            Class<Os> _ = MethodHandles.lookup().ensureInitialized(Os.class);
        } catch (IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private byte[] buffer;
    private int position;

    public HeapWriteBuffer(int size) {
        buffer = new byte[size];
        position = 0;
    }

    @Override
    public void writeByte(byte b) {
        int newPosition = Math.incrementExact(position);
        if (newPosition > buffer.length) {
            int newLength = Utils.grow(buffer.length);
            buffer = Arrays.copyOf(buffer, newLength);
        }
        buffer[position] = b;
        position = newPosition;
    }

    @Override
    public void writeBytes(byte b1, byte b2) {
        int newPosition = Math.addExact(position, 2);
        if (newPosition > buffer.length) {
            int newLength = Utils.grow(buffer.length);
            buffer = Arrays.copyOf(buffer, newLength);
        }
        buffer[position] = b1;
        buffer[position + 1] = b2;
        position = newPosition;
    }

    @Override
    public void writeBytes(byte b1, byte b2, byte b3) {
        int newPosition = Math.addExact(position, 3);
        if (newPosition > buffer.length) {
            int newLength = Utils.grow(buffer.length);
            buffer = Arrays.copyOf(buffer, newLength);
        }
        buffer[position] = b1;
        buffer[position + 1] = b2;
        buffer[position + 2] = b3;
        position = newPosition;
    }

    @Override
    public void writeBytes(byte b1, byte b2, byte b3, byte b4) {
        int newPosition = Math.addExact(position, 4);
        if (newPosition > buffer.length) {
            int newLength = Utils.grow(buffer.length);
            buffer = Arrays.copyOf(buffer, newLength);
        }
        buffer[position] = b1;
        buffer[position + 1] = b2;
        buffer[position + 2] = b3;
        buffer[position + 3] = b4;
        position = newPosition;
    }

    @Override
    public void writeBytes(byte[] bytes, int off, int len) {
        int newPosition = Math.addExact(position, len);
        if (newPosition > buffer.length) {
            int newLength = Utils.grow(buffer.length);
            buffer = Arrays.copyOf(buffer, newLength);
        }
        System.arraycopy(bytes, off, buffer, position, len);
        position = newPosition;
    }

    @Override
    public void writeShort(short s, ByteOrder byteOrder) {
        int newPosition = Math.addExact(position, 2);
        if (newPosition > buffer.length) {
            int newLength = Utils.grow(buffer.length);
            buffer = Arrays.copyOf(buffer, newLength);
        }
        ArrayAccess.setShort(buffer, position, s, byteOrder);
        position = newPosition;
    }

    @Override
    public void writeChar(char c, ByteOrder byteOrder) {
        int newPosition = Math.addExact(position, 2);
        if (newPosition > buffer.length) {
            int newLength = Utils.grow(buffer.length);
            buffer = Arrays.copyOf(buffer, newLength);
        }
        ArrayAccess.setChar(buffer, position, c, byteOrder);
        position = newPosition;
    }

    @Override
    public void writeInt(int i, ByteOrder byteOrder) {
        int newPosition = Math.addExact(position, 4);
        if (newPosition > buffer.length) {
            int newLength = Utils.grow(buffer.length);
            buffer = Arrays.copyOf(buffer, newLength);
        }
        ArrayAccess.setInt(buffer, position, i, byteOrder);
        position = newPosition;
    }

    @Override
    public void writeLong(long l, ByteOrder byteOrder) {
        int newPosition = Math.addExact(position, 8);
        if (newPosition > buffer.length) {
            int newLength = Utils.grow(buffer.length);
            buffer = Arrays.copyOf(buffer, newLength);
        }
        ArrayAccess.setLong(buffer, position, l, byteOrder);
        position = newPosition;
    }

    @Override
    public void writeFloat(float f, ByteOrder byteOrder) {
        int newPosition = Math.addExact(position, 4);
        if (newPosition > buffer.length) {
            int newLength = Utils.grow(buffer.length);
            buffer = Arrays.copyOf(buffer, newLength);
        }
        ArrayAccess.setFloat(buffer, position, f, byteOrder);
        position = newPosition;
    }

    @Override
    public void writeDouble(double d, ByteOrder byteOrder) {
        int newPosition = Math.addExact(position, 8);
        if (newPosition > buffer.length) {
            int newLength = Utils.grow(buffer.length);
            buffer = Arrays.copyOf(buffer, newLength);
        }
        ArrayAccess.setDouble(buffer, position, d, byteOrder);
        position = newPosition;
    }

    @Override
    public int intPosition() {
        return position;
    }

    @Override
    public long longPosition() {
        return position;
    }

    public int length() {
        return buffer.length;
    }

    public byte[] rawByteArray() {
        return buffer;
    }

    public byte[] toByteArray() {
        if (position == buffer.length) {
            return buffer;
        }
        return Arrays.copyOf(buffer, position);
    }
}
