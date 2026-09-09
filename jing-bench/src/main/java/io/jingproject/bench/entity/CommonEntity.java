package io.jingproject.bench.entity;

import io.jingproject.marshall.Marshallable;

@Marshallable
public record CommonEntity(
        int a,
        long b,
        float c,
        double d,
        String str
) {
}
