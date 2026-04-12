package io.jingproject.marshalltest.entity;

import java.time.LocalDateTime;

public class BeanEntity {
    private int intValue;
    private long longValue;
    private String strValue;
    private LocalDateTime timeValue;

    public int intValue() {
        return intValue;
    }

    public void setIntValue(int intValue) {
        this.intValue = intValue;
    }

    public long longValue() {
        return longValue;
    }

    public void setLongValue(long longValue) {
        this.longValue = longValue;
    }

    public String strValue() {
        return strValue;
    }

    public void setStrValue(String strValue) {
        this.strValue = strValue;
    }

    public LocalDateTime timeValue() {
        return timeValue;
    }

    public void setTimeValue(LocalDateTime timeValue) {
        this.timeValue = timeValue;
    }
}
