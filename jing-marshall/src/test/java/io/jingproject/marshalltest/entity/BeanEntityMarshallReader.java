package io.jingproject.marshalltest.entity;

import io.jingproject.marshall.MarshallReader;

import java.time.LocalDateTime;

public record BeanEntityMarshallReader(
        BeanEntity instance
) implements MarshallReader {

    @Override
    public int getInt(int offset) {
        return switch (offset) {
            case 0 -> (int) BeanEntityMarshallFacade.vh(0).get(instance);
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public long getLong(int offset) {
        return switch (offset) {
            case 1 -> (long) BeanEntityMarshallFacade.vh(1).get(instance);
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public Object getObject(int offset) {
        return switch (offset) {
            case 2 -> (String) BeanEntityMarshallFacade.vh(2).get(instance);
            case 3 -> (LocalDateTime) BeanEntityMarshallFacade.vh(3).get(instance);
            default -> throw new UnsupportedOperationException();
        };
    }
}
