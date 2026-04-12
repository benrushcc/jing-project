package io.jingproject.marshalltest.entity;

import io.jingproject.marshall.MarshallSchema;

import java.time.LocalDateTime;

public record BeanEntityMarshallSchema (
        BeanEntityMarshallFacade facade,
        BeanEntity instance
) implements MarshallSchema {

    @Override
    public void setInt(int offset, int value) {
        switch (offset) {
            case 0 -> facade.marshallInfoByIndex(0).vh().set(instance, value);
            default -> throw new UnsupportedOperationException();
        }
    }

    @Override
    public void setLong(int offset, long value) {
        switch (offset) {
            case 1 -> facade.marshallInfoByIndex(1).vh().set(instance, value);
            default -> throw new UnsupportedOperationException();
        }
    }

    @Override
    public void setObject(int offset, Object value) {
        switch (offset) {
            case 2 -> facade.marshallInfoByIndex(2).vh().set(instance, (String) value);
            case 3 -> facade.marshallInfoByIndex(3).vh().set(instance, (LocalDateTime) value);
            default -> throw new UnsupportedOperationException();
        }
    }
}
