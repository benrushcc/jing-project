package io.jingproject.common;

import java.util.stream.Stream;

public interface Transmittable<T> {

    void enc(WriteBuffer writeBuffer, T t);

    void dec(ReadBuffer readBuffer, Stream<T> stream); // TODO 不能用stream，要用一个单独的封装抽象

}
