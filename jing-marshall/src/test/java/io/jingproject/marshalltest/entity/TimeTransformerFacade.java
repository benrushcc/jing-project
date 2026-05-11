package io.jingproject.marshalltest.entity;

import io.jingproject.marshall.MarshallTransformer;
import io.jingproject.marshall.MarshallTransformerFacade;

import java.time.LocalDateTime;

public final class TimeTransformerFacade implements MarshallTransformerFacade {
    private static final MarshallTransformer<String, LocalDateTime> INSTANCE = new TimeTransformer();

    @Override
    public Class<?> fromClass() {
        return String.class;
    }

    @Override
    public Class<?> toClass() {
        return LocalDateTime.class;
    }

    @Override
    public MarshallTransformer<?, ?> transformer() {
        return INSTANCE;
    }
}
