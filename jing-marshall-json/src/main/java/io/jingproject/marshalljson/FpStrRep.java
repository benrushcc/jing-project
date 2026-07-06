package io.jingproject.marshalljson;

public record FpStrRep(
        boolean negative,
        boolean trunc,
        long d,
        int frac,
        int p,
        int len
) {
}
