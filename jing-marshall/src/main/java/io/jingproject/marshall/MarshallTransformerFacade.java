package io.jingproject.marshall;

import io.jingproject.common.anno.ProcessorApi;

@ProcessorApi
public interface MarshallTransformerFacade {
    Class<?> customType();

    Class<?> builtinType();

    MarshallTransformer<?, ?> transformer();
}
