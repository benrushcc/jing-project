package io.jingproject.marshall;

import io.jingproject.common.anno.ProcessorApi;

@ProcessorApi
public interface MarshallWriter {
    default void setBoolean(boolean value) {
        throw new UnsupportedOperationException();
    }

    default void setByte(int offset, byte value) {
        throw new UnsupportedOperationException();
    }

    default void setShort(int offset, short value) {
        throw new UnsupportedOperationException();
    }

    default void setChar(int offset, char value) {
        throw new UnsupportedOperationException();
    }

    default void setInt(int offset, int value) {
        throw new UnsupportedOperationException();
    }

    default void setLong(int offset, long value) {
        throw new UnsupportedOperationException();
    }

    default void setFloat(int offset, float value) {
        throw new UnsupportedOperationException();
    }

    default void setDouble(int offset, double value) {
        throw new UnsupportedOperationException();
    }

    default void setObject(int offset, Object value) {
        throw new UnsupportedOperationException();
    }

    default void setEnum(String enumValue) {
        throw new UnsupportedOperationException();
    }
}
