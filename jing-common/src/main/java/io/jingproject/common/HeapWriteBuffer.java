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

    @Override
    public void writeByte(byte b) {
        int nextPosition = Math.addExact(position, 1);
        if (nextPosition > buffer.length) {
            buffer = Arrays.copyOf(buffer, buffer.length << 1);
        }
        buffer[position] = b;
        position = nextPosition;
    }

    @Override
    public void writeBytes(byte b1, byte b2) {
        int nextPosition = Math.addExact(position, 2);
        if (nextPosition > buffer.length) {
            buffer = Arrays.copyOf(buffer, buffer.length << 1);
        }
        buffer[position] = b1;
        buffer[position + 1] = b2;
        position = nextPosition;
    }

    @Override
    public void writeBytes(byte b1, byte b2, byte b3) {
        int nextPosition = Math.addExact(position, 3);
        if (nextPosition > buffer.length) {
            buffer = Arrays.copyOf(buffer, buffer.length << 1);
        }
        buffer[position] = b1;
        buffer[position + 1] = b2;
        buffer[position + 2] = b3;
        position = nextPosition;
    }

    @Override
    public void writeBytes(byte b1, byte b2, byte b3, byte b4) {
        int nextPosition = Math.addExact(position, 4);
        if (nextPosition > buffer.length) {
            buffer = Arrays.copyOf(buffer, buffer.length << 1);
        }
        buffer[position] = b1;
        buffer[position + 1] = b2;
        buffer[position + 2] = b3;
        buffer[position + 3] = b4;
        position = nextPosition;
    }

    @Override
    public void writeBytes(byte[] bytes, int off, int len) {
        if (Math.addExact(position, len) > buffer.length) {
            buffer = Arrays.copyOf(buffer, buffer.length << 1);
        }
        System.arraycopy(bytes, off, buffer, position, len);
    }

    @Override
    public void writeShort(short s, ByteOrder byteOrder) {
        int nextPosition = Math.addExact(position, 2);
        if (nextPosition > buffer.length) {
            buffer = Arrays.copyOf(buffer, buffer.length << 1);
        }
        ArrayAccess.setShort(buffer, position, s, byteOrder);
        position = nextPosition;
    }

    @Override
    public void writeChar(char c, ByteOrder byteOrder) {
        int nextPosition = Math.addExact(position, 2);
        if (nextPosition > buffer.length) {
            buffer = Arrays.copyOf(buffer, buffer.length << 1);
        }
        ArrayAccess.setChar(buffer, position, c, byteOrder);
        position = nextPosition;
    }

    @Override
    public void writeInt(int i, ByteOrder byteOrder) {
        int nextPosition = Math.addExact(position, 4);
        if (nextPosition > buffer.length) {
            buffer = Arrays.copyOf(buffer, buffer.length << 1);
        }
        ArrayAccess.setInt(buffer, position, i, byteOrder);
        position = nextPosition;
    }

    @Override
    public void writeLong(long l, ByteOrder byteOrder) {
        int nextPosition = Math.addExact(position, 8);
        if (nextPosition > buffer.length) {
            buffer = Arrays.copyOf(buffer, buffer.length << 1);
        }
        ArrayAccess.setLong(buffer, position, l, byteOrder);
        position = nextPosition;
    }

    @Override
    public void writeFloat(float f, ByteOrder byteOrder) {
        int nextPosition = Math.addExact(position, 4);
        if (nextPosition > buffer.length) {
            buffer = Arrays.copyOf(buffer, buffer.length << 1);
        }
        ArrayAccess.setFloat(buffer, position, f, byteOrder);
        position = nextPosition;
    }

    @Override
    public void writeDouble(double d, ByteOrder byteOrder) {
        int nextPosition = Math.addExact(position, 8);
        if (nextPosition > buffer.length) {
            buffer = Arrays.copyOf(buffer, buffer.length << 1);
        }
        ArrayAccess.setDouble(buffer, position, d, byteOrder);
        position = nextPosition;
    }

    @Override
    public long position() {
        return position;
    }

    public byte[] rawBuffer() {
        return buffer;
    }

    public byte[] buffer() {
        if (position == buffer.length) {
            return buffer;
        }
        return Arrays.copyOf(buffer, position);
    }
}
