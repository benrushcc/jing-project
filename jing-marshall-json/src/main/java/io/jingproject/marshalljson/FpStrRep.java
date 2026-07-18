package io.jingproject.marshalljson;

/**
 * represents a parsed floating-point string representation.
 *
 * @param negative whether the number is negative
 * @param trunc    indicates whether the decimal part (d) overflowed; note that
 *                 exponent (p) exceeding 10000 is also truncated but does NOT set
 *                 trunc to true, because for FP32/FP64 parsing such exponent overflow
 *                 doesn't affect the final result
 * @param d        decimal integer part stored as unsigned 64-bit value
 * @param p        exponent part; values greater than 10000 are truncated;
 *                 leading zeros are allowed
 * @param len      length of the original floating-point string
 */
public record FpStrRep(
        boolean negative,
        boolean trunc,
        long d,
        int p,
        int len
) {
}
