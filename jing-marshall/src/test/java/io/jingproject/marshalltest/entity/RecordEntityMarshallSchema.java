package io.jingproject.marshalltest.entity;

import io.jingproject.marshall.MarshallSchema;

import java.time.LocalDateTime;

public final class RecordEntityMarshallSchema implements MarshallSchema {
    private int intValue;
    private long longValue;
    private String strValue;
    private LocalDateTime timeValue;

    @Override
    public int getInt(int offset) {
        switch (offset) {
            case 0 -> {
                return intValue;
            }
            default -> throw new UnsupportedOperationException();
        }
    }

    @Override
    public void setInt(int offset, int value) {
        switch (offset) {
            case 0 -> this.intValue = value;
            default -> throw new UnsupportedOperationException();
        }
    }

    @Override
    public long getLong(int offset) {
        switch (offset) {
            case 1 -> {
                return longValue;
            }
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
    public Object getObject(int offset) {
        switch (offset) {
            case 2 -> {
                return strValue;
            }
            case 3 -> {
                return timeValue;
            }
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
}
