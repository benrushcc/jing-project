package io.jingproject.common;

import java.util.function.Consumer;

public interface Transmittable<T> {

    void enc(WriteBuffer writeBuffer, T t);

    void dec(ReadBuffer readBuffer, Consumer<T> consumer); // TODO 不能用stream，要用一个单独的封装抽象

}
