package io.jingproject.marshalltest.entity;

import io.jingproject.marshall.MarshallWriter;

import java.time.LocalDateTime;

public record BeanEntityMarshallWriter (
        BeanEntity instance
) implements MarshallWriter {

    @Override
    public void setInt(int offset, int value) {
        switch (offset) {
            case 0 -> BeanEntityMarshallFacade.vh(0).set(instance, value);
            default -> throw new UnsupportedOperationException();
        }
    }

    @Override
    public void setLong(int offset, long value) {
        switch (offset) {
            case 1 -> BeanEntityMarshallFacade.vh(1).set(instance, value);
            default -> throw new UnsupportedOperationException();
        }
    }

    @Override
    public void setObject(int offset, Object value) {
        switch (offset) {
            case 2 -> BeanEntityMarshallFacade.vh(2).set(instance, (String) value);
            case 3 -> BeanEntityMarshallFacade.vh(3).set(instance, (LocalDateTime) value);
            default -> throw new UnsupportedOperationException();
        }
    }
}
