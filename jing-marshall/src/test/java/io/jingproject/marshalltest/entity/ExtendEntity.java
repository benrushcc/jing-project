package io.jingproject.marshalltest.entity;

import java.time.Duration;
import java.util.Map;

public final class ExtendEntity extends BeanEntity {
    private Duration durationValue;
    private Map<Integer, String> mapValue;

    public Duration durationValue() {
        return durationValue;
    }

    public void setDurationValue(Duration durationValue) {
        this.durationValue = durationValue;
    }

    public Map<Integer, String> mapValue() {
        return mapValue;
    }

    public void setMapValue(Map<Integer, String> mapValue) {
        this.mapValue = mapValue;
    }
}
