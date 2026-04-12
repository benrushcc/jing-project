package io.jingproject.marshalltest.entity;

import java.time.Duration;

public final class ExtendEntity extends BeanEntity {
    private Duration durationValue;

    public Duration durationValue() {
        return durationValue;
    }

    public void setDurationValue(Duration durationValue) {
        this.durationValue = durationValue;
    }
}
