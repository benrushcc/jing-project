package io.jingproject.marshall;

import io.jingproject.common.anno.ProcessorApi;

@ProcessorApi
public interface MarshallTransformerFacade {
    Class<?> fromClass();

    Class<?> toClass();

    MarshallTransformer<?, ?> transformer();
}
