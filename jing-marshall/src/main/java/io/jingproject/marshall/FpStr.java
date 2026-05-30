package io.jingproject.marshall;

public record FpStr (
        boolean negative,
        boolean trunc,
        long d,
        int frac,
        int p,
        int len
) {
}
