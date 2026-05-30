package io.jingproject.marshall;

public record FpSpec(
        int mantBits,
        int expBits,
        int bias,
        int minExp,
        int maxDecExp,
        int minDecExp
) {
}
