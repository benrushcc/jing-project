package io.jingproject.marshalltest.test;

import java.time.LocalDateTime;

public record RecordEntity(
        int intValue,
        long longValue,
        String strValue,
        LocalDateTime timeValue
) {
}
