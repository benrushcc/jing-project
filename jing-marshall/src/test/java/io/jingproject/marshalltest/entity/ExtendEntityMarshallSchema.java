package io.jingproject.marshalltest.entity;

import io.jingproject.marshall.MarshallSchema;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

public record ExtendEntityMarshallSchema (
        ExtendEntityMarshallFacade facade,
        ExtendEntity instance
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
    @SuppressWarnings("unchecked")
    public void setObject(int offset, Object value) {
        switch (offset) {
            case 2 -> facade.marshallInfoByIndex(2).vh().set(instance, (String) value);
            case 3 -> facade.marshallInfoByIndex(3).vh().set(instance, (LocalDateTime) value);
            case 4 -> facade.marshallInfoByIndex(4).vh().set(instance, (Duration) value);
            case 5 -> facade.marshallInfoByIndex(5).vh().set(instance, (Map<Integer, String>) value);
            default -> throw new UnsupportedOperationException();
        }
    }
}
