package io.jingproject.marshall;

public record FpFormat(
        boolean negative,
        long d,
        int dLen,
        int frac,
        int p,
        int pLen,
        int len
) {
}
