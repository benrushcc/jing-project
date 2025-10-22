package io.jingproject.common;

import java.util.List;

@FunctionalInterface
public interface Dec<T> {
    void dec(ReadBuffer readBuffer, List<T> entityList);
}
