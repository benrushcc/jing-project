package io.jingproject.common;

@FunctionalInterface
public interface Enc<T> {
    void enc(WriteBuffer writeBuffer, T t);
}
