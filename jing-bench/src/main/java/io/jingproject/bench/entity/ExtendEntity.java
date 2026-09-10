package io.jingproject.bench.entity;

import io.jingproject.marshall.Marshallable;

import java.time.Duration;

@Marshallable
public final class ExtendEntity extends BeanEntity {
    private Duration durationValue;

    public Duration durationValue() {
        return durationValue;
    }

    public void setDurationValue(Duration durationValue) {
        this.durationValue = durationValue;
    }
}
