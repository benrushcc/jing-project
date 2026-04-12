package io.jingproject.marshall;

import io.jingproject.common.anno.ProcessorApi;

@ProcessorApi
public interface MarshallSchema {

    default boolean getBoolean() {
        throw new UnsupportedOperationException();
    }

    default void setBoolean(boolean value) {
        throw new UnsupportedOperationException();
    }

    default byte getByte(int offset) {
        throw new UnsupportedOperationException();
    }

    default void setByte(int offset, byte value) {
        throw new UnsupportedOperationException();
    }

    default short getShort(int offset) {
        throw new UnsupportedOperationException();
    }

    default void setShort(int offset, short value) {
        throw new UnsupportedOperationException();
    }

    default char getChar(int offset) {
        throw new UnsupportedOperationException();
    }

    default void setChar(int offset, char value) {
        throw new UnsupportedOperationException();
    }

    default int getInt(int offset) {
        throw new UnsupportedOperationException();
    }

    default void setInt(int offset, int value) {
        throw new UnsupportedOperationException();
    }

    default long getLong(int offset) {
        throw new UnsupportedOperationException();
    }

    default void setLong(int offset, long value) {
        throw new UnsupportedOperationException();
    }

    default float getFloat(int offset) {
        throw new UnsupportedOperationException();
    }

    default void setFloat(int offset, float value) {
        throw new UnsupportedOperationException();
    }

    default double getDouble(int offset) {
        throw new UnsupportedOperationException();
    }

    default void setDouble(int offset, double value) {
        throw new UnsupportedOperationException();
    }

    default Object getObject(int offset) {
        throw new UnsupportedOperationException();
    }

    default void setObject(int offset, Object value) {
        throw new UnsupportedOperationException();
    }

    default String getEnum() {
        throw new UnsupportedOperationException();
    }

    default void setEnum(String enumValue) {
        throw new UnsupportedOperationException();
    }
}
