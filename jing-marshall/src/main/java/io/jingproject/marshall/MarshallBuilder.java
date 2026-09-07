package io.jingproject.marshall;

import io.jingproject.common.anno.ProcessorApi;

@ProcessorApi
public interface MarshallBuilder {
    default void writeBoolean(int index, boolean value) {
        throw new UnsupportedOperationException();
    }

    default void writeByte(int index, byte value) {
        throw new UnsupportedOperationException();
    }

    default void writeShort(int index, short value) {
        throw new UnsupportedOperationException();
    }

    default void writeChar(int index, char value) {
        throw new UnsupportedOperationException();
    }

    default void writeInt(int index, int value) {
        throw new UnsupportedOperationException();
    }

    default void writeLong(int index, long value) {
        throw new UnsupportedOperationException();
    }

    default void writeFloat(int index, float value) {
        throw new UnsupportedOperationException();
    }

    default void writeDouble(int index, double value) {
        throw new UnsupportedOperationException();
    }

    default void writeObject(int index, Object value) {
        throw new UnsupportedOperationException();
    }
}
