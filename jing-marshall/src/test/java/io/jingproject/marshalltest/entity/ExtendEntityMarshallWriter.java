package io.jingproject.marshalltest.entity;

import io.jingproject.marshall.MarshallWriter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

public record ExtendEntityMarshallWriter(
        ExtendEntity instance
) implements MarshallWriter {
    @Override
    public void setInt(int offset, int value) {
        switch (offset) {
            case 0 -> ExtendEntityMarshallFacade.vh(0).set(instance, value);
            default -> throw new UnsupportedOperationException();
        }
    }

    @Override
    public void setLong(int offset, long value) {
        switch (offset) {
            case 1 -> ExtendEntityMarshallFacade.vh(1).set(instance, value);
            default -> throw new UnsupportedOperationException();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setObject(int offset, Object value) {
        switch (offset) {
            case 2 -> ExtendEntityMarshallFacade.vh(2).set(instance, (String) value);
            case 3 -> ExtendEntityMarshallFacade.vh(3).set(instance, (LocalDateTime) value);
            case 4 -> ExtendEntityMarshallFacade.vh(4).set(instance, (Duration) value);
            case 5 -> ExtendEntityMarshallFacade.vh(5).set(instance, (Map<Integer, String>) value);
            default -> throw new UnsupportedOperationException();
        }
    }
}
