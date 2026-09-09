package io.jingproject.bench.entity;

import java.time.LocalDateTime;

public record RecordEntity(
        int intValue,
        long longValue,
        String strValue,
        LocalDateTime timeValue
) {
}
