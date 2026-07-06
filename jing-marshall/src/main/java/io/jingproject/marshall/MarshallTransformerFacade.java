package io.jingproject.marshall;

import io.jingproject.common.anno.ProcessorApi;

@ProcessorApi
public interface MarshallTransformerFacade {
    Class<?> customType();

    Class<?> builtinType();

    Object toCustom(Object o);

    Object toBuiltin(Object o);
}
