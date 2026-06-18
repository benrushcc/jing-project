package io.jingproject.marshall;

import io.jingproject.common.anno.ProcessorApi;

@ProcessorApi
public interface MarshallTransformerFacade {
    Class<?> customType();

    Class<?> builtinType();

    Object toCustom(Object o);

    Object toBuiltin(Object o);

    default Object castCustom(Object o, Class<?> builtinType) {
        return toCustom(builtinType.cast(o));
    }

    default <T> T castBuiltin(Object o, Class<T> clazz) {
        return clazz.cast(toBuiltin(o));
    }
}
