package io.jingproject.ffm;

import io.jingproject.common.Os;

import java.util.List;
import java.util.function.Supplier;

public interface SharedLib {
    Class<?> target();

    List<Os> supportedOS();

    String libName();

    List<String> methodNames();

    Supplier<?> supplier();
}
