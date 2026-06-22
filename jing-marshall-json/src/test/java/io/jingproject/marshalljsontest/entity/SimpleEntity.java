package io.jingproject.marshalljsontest.entity;

import io.jingproject.marshall.Marshallable;

@Marshallable
public record SimpleEntity(
        int a,
        long b,
        float c,
        double d,
        String str
) {
}
