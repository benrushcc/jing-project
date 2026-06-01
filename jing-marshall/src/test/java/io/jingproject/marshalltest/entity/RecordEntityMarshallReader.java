package io.jingproject.marshalltest.entity;

import io.jingproject.marshall.MarshallReader;

public record RecordEntityMarshallReader (
        RecordEntity instance
) implements MarshallReader {

    @Override
    public int getInt(int offset) {
        return switch (offset) {
            case 0 -> instance.intValue();
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public long getLong(int offset) {
        return switch (offset) {
            case 0 -> instance.longValue();
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public Object getObject(int offset) {
        return switch (offset) {
            case 2 -> instance.strValue();
            case 3 -> instance.timeValue();
            default -> throw new UnsupportedOperationException();
        };
    }
}
