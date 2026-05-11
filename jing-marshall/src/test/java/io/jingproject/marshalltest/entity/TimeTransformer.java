package io.jingproject.marshalltest.entity;

import io.jingproject.marshall.MarshallTransformer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class TimeTransformer implements MarshallTransformer<String, LocalDateTime> {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public LocalDateTime transformTo(String source) {
        return formatter.parse(source, LocalDateTime::from);
    }

    @Override
    public String transformFrom(LocalDateTime source) {
        return formatter.format(source);
    }
}
