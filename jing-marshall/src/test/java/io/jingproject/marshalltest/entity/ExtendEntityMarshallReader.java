package io.jingproject.marshalltest.entity;

import io.jingproject.marshall.MarshallReader;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

public record ExtendEntityMarshallReader(
        ExtendEntity instance
) implements MarshallReader {
    @Override
    public int getInt(int offset) {
        return switch (offset) {
            case 0 -> (int) ExtendEntityMarshallFacade.vh(0).get(instance);
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public long getLong(int offset) {
        return switch (offset) {
            case 1 -> (long) ExtendEntityMarshallFacade.vh(1).get(instance);
            default -> throw new UnsupportedOperationException();
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object getObject(int offset) {
        return switch (offset) {
            case 2 -> (String) BeanEntityMarshallFacade.vh(2).get(instance);
            case 3 -> (LocalDateTime) BeanEntityMarshallFacade.vh(3).get(instance);
            case 4 -> (Duration) BeanEntityMarshallFacade.vh(4).get(instance);
            case 5 -> (Map<Integer, String>) BeanEntityMarshallFacade.vh(5).get(instance);
            default -> throw new UnsupportedOperationException();
        };
    }
}
