package io.jingproject.marshall;

import io.jingproject.common.anno.ProcessorApi;

@ProcessorApi
public interface MarshallReader {
    default boolean getBoolean() {
        throw new UnsupportedOperationException();
    }

    default byte getByte(int offset) {
        throw new UnsupportedOperationException();
    }

    default short getShort(int offset) {
        throw new UnsupportedOperationException();
    }

    default char getChar(int offset) {
        throw new UnsupportedOperationException();
    }

    default int getInt(int offset) {
        throw new UnsupportedOperationException();
    }

    default long getLong(int offset) {
        throw new UnsupportedOperationException();
    }

    default float getFloat(int offset) {
        throw new UnsupportedOperationException();
    }

    default double getDouble(int offset) {
        throw new UnsupportedOperationException();
    }

    default Object getObject(int offset) {
        throw new UnsupportedOperationException();
    }

    default String getEnum() {
        throw new UnsupportedOperationException();
    }
}
