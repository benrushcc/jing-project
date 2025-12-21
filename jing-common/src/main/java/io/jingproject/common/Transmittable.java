package io.jingproject.common;

import java.util.stream.Stream;

public interface Transmittable<T> {

    void enc(WriteBuffer writeBuffer, T t);

    void dec(ReadBuffer readBuffer, Stream<T> stream);

}
