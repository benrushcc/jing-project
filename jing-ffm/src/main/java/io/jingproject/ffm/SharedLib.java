package io.jingproject.ffm;

import java.util.List;
import java.util.function.Supplier;

public interface SharedLib {
    Class<?> target();

    String libName();

    List<String> methodNames();

    Supplier<?> supplier();
}
