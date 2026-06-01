package io.jingproject.marshalltest.entity;

import io.jingproject.marshall.MarshallWriter;

import java.time.LocalDateTime;

public final class RecordEntityMarshallWriter implements MarshallWriter {
    private int intValue;
    private long longValue;
    private String strValue;
    private LocalDateTime timeValue;

    @Override
    public void setInt(int offset, int value) {
        switch (offset) {
            case 0 -> this.intValue = value;
            default -> throw new UnsupportedOperationException();
        }
    }

    @Override
    public void setLong(int offset, long value) {
        switch (offset) {
            case 1 -> this.longValue = value;
            default -> throw new UnsupportedOperationException();
        }
    }

    @Override
    public void setObject(int offset, Object value) {
        switch (offset) {
            case 2 -> this.strValue = (String) value;
            case 3 -> this.timeValue = (LocalDateTime) value;
            default -> throw new UnsupportedOperationException();
        }
    }

    RecordEntity build() {
        return new RecordEntity(intValue, longValue, strValue, timeValue);
    }
}
