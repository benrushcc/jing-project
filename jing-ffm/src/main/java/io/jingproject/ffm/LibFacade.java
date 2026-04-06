package io.jingproject.ffm;

import io.jingproject.common.Os;
import io.jingproject.common.anno.ProcessorApi;

import java.util.List;
import java.util.function.Supplier;

@ProcessorApi
public interface LibFacade {
    Class<?> target();

    List<Os> supportedOS();

    String libName();

    List<String> methodNames();

    Supplier<?> supplier();
}
