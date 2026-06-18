package io.jingproject.marshalltest.entity;

import io.jingproject.marshall.MarshallTransformer;
import io.jingproject.marshall.MarshallTransformerFacade;

import java.time.LocalDateTime;

public final class TimeTransformerFacade implements MarshallTransformerFacade {
    private static final MarshallTransformer<LocalDateTime, String> INSTANCE = new TimeTransformer();

    @Override
    public Class<?> customType() {
        return LocalDateTime.class;
    }

    @Override
    public Class<?> builtinType() {
        return String.class;
    }

    @Override
    public Object toCustom(Object o) {
        return INSTANCE.toCustom((String) o);
    }

    @Override
    public Object toBuiltin(Object o) {
        return INSTANCE.toBuiltin((LocalDateTime) o);
    }
}
