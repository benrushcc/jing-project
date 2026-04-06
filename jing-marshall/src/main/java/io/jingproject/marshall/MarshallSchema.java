package io.jingproject.marshall;

import io.jingproject.common.ArrayAccess;
import io.jingproject.common.Os;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

public record MarshallSchema (
        Object[] objectBuffer,
        byte[] primitiveBuffer,
        int objectIndex,
        int primitiveIndex,
        int objectElements,
        int primitiveElements
) {
    static {
        try {
            Class<Os> _ = MethodHandles.lookup().ensureInitialized(Os.class);
        } catch (IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public byte getByte(int offset) {
        int index = Math.addExact(primitiveIndex, offset);
        Objects.checkFromIndexSize(index, 1, primitiveBuffer.length);
        return primitiveBuffer[index];
    }

    public void setByte(int offset, byte value) {
        int index = Math.addExact(primitiveIndex, offset);
        Objects.checkFromIndexSize(index, 1, primitiveBuffer.length);
        primitiveBuffer[index] = value;
    }

    public short getShort(int offset) {
        int index = Math.addExact(primitiveIndex, offset);
        Objects.checkFromIndexSize(index, 2, primitiveBuffer.length);
        return ArrayAccess.getShort(primitiveBuffer, index);
    }

    public void setShort(int offset, short value) {
        int index = Math.addExact(primitiveIndex, offset);
        Objects.checkFromIndexSize(index, 2, primitiveBuffer.length);
        ArrayAccess.setShort(primitiveBuffer, index, value);
    }

    public char getChar(int offset) {
        int index = Math.addExact(primitiveIndex, offset);
        Objects.checkFromIndexSize(index, 2, primitiveBuffer.length);
        return ArrayAccess.getChar(primitiveBuffer, index);
    }

    public void setChar(int offset, char value) {
        int index = Math.addExact(primitiveIndex, offset);
        Objects.checkFromIndexSize(index, 2, primitiveBuffer.length);
        ArrayAccess.setChar(primitiveBuffer, index, value);
    }

    public int getInt(int offset) {
        int index = Math.addExact(primitiveIndex, offset);
        Objects.checkFromIndexSize(index, 4, primitiveBuffer.length);
        return ArrayAccess.getInt(primitiveBuffer, index);
    }

    public void setInt(int offset, int value) {
        int index = Math.addExact(primitiveIndex, offset);
        Objects.checkFromIndexSize(index, 4, primitiveBuffer.length);
        ArrayAccess.setInt(primitiveBuffer, index, value);
    }

    public long getLong(int offset) {
        int index = Math.addExact(primitiveIndex, offset);
        Objects.checkFromIndexSize(index, 8, primitiveBuffer.length);
        return ArrayAccess.getLong(primitiveBuffer, index);
    }

    public void setLong(int offset, long value) {
        int index = Math.addExact(primitiveIndex, offset);
        Objects.checkFromIndexSize(index, 8, primitiveBuffer.length);
        ArrayAccess.setLong(primitiveBuffer, index, value);
    }

    public float getFloat(int offset) {
        int index = Math.addExact(primitiveIndex, offset);
        Objects.checkFromIndexSize(index, 4, primitiveBuffer.length);
        return ArrayAccess.getFloat(primitiveBuffer, index);
    }

    public void setFloat(int offset, float value) {
        int index = Math.addExact(primitiveIndex, offset);
        Objects.checkFromIndexSize(index, 4, primitiveBuffer.length);
        ArrayAccess.setFloat(primitiveBuffer, index, value);
    }

    public double getDouble(int offset) {
        int index = Math.addExact(primitiveIndex, offset);
        Objects.checkFromIndexSize(index, 8, primitiveBuffer.length);
        return ArrayAccess.getDouble(primitiveBuffer, index);
    }

    public void setDouble(int offset, double value) {
        int index = Math.addExact(primitiveIndex, offset);
        Objects.checkFromIndexSize(index, 8, primitiveBuffer.length);
        ArrayAccess.setDouble(primitiveBuffer, index, value);
    }

    public Object getObject(int offset) {
        int index = Math.addExact(objectIndex, offset);
        Objects.checkFromIndexSize(index, 1, objectBuffer.length);
        return objectBuffer[index];
    }

    public void setObject(int offset, Object value) {
        int index = Math.addExact(objectIndex, offset);
        Objects.checkFromIndexSize(index, 1, objectBuffer.length);
        objectBuffer[index] = value;
    }

}
