package io.jingproject.bench.entity;

import io.jingproject.marshall.Marshallable;

@Marshallable
public record StringEntity(
        String s1,
        String s2
) {
}
