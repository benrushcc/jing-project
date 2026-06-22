package io.jingproject.marshalltest.entity;

import io.jingproject.marshall.MarshallTransformer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// example implementation
public final class TimeTransformer implements MarshallTransformer<LocalDateTime, String> {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String toBuiltin(LocalDateTime ct) {
        return formatter.format(ct);
    }

    @Override
    public LocalDateTime toCustom(String bt) {
        return formatter.parse(bt, LocalDateTime::from);
    }
}
