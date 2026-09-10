package io.jingproject.bench.entity;

import io.jingproject.marshall.Marshallable;

import java.time.LocalDateTime;

@Marshallable
public record RecordEntity(
        int intValue,
        long longValue,
        String strValue,
        LocalDateTime timeValue
) {
}
