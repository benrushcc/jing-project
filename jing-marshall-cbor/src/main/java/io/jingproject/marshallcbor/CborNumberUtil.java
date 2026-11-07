package io.jingproject.marshallcbor;

// CBOR (RFC 8949) number utilities: major-type/ai decomposition and
// half-precision floating-point conversion.
public final class CborNumberUtil {

    // major type of an unsigned integer (major 0)
    public static final int TYPE_UNSIGNED = 0;

    // major type of a negative integer (major 1)
    public static final int TYPE_NEGATIVE = 1;

    // major type of a byte string (major 2)
    public static final int TYPE_BYTES = 2;

    // major type of a text string (major 3)
    public static final int TYPE_TEXT = 3;

    // major type of an array (major 4)
    public static final int TYPE_ARRAY = 4;

    // major type of a map (major 5)
    public static final int TYPE_MAP = 5;

    // major type of a tag (major 6)
    public static final int TYPE_TAG = 6;

    // major type of simple values and floating-point numbers (major 7)
    public static final int TYPE_SIMPLE = 7;

    private CborNumberUtil() {
        throw new UnsupportedOperationException("utility class");
    }

    // extract the 3-bit major type from an initial byte
    public static int majorOf(byte initialByte) {
        return (initialByte & 0xFF) >>> 5;
    }

    // extract the 5-bit additional information from an initial byte
    public static int aiOf(byte initialByte) {
        return initialByte & 0x1F;
    }

    // convert a half-precision bit pattern (IEEE 754 binary16) to a float.
    // handles NaN/Infinity/±0 and subnormals correctly.
    public static float halfToFloat(short halfBits) {
        int h = halfBits & 0xFFFF;
        int sign = (h & 0x8000) << 16;
        int exp = (h >>> 10) & 0x1F;
        int mant = h & 0x3FF;
        if (exp == 0x1F) {
            return Float.intBitsToFloat(sign | 0x7F800000 | (mant << 13));
        }
        if (exp == 0) {
            if (mant == 0) {
                return Float.intBitsToFloat(sign);
            }
            int shift = 0;
            while ((mant & 0x400) == 0) {
                mant <<= 1;
                shift++;
            }
            mant &= 0x3FF;
            return Float.intBitsToFloat(sign | ((113 - shift) << 23) | (mant << 13));
        }
        return Float.intBitsToFloat(sign | ((exp + 112) << 23) | (mant << 13));
    }

    // convert a float to its nearest half-precision bit pattern using
    // round-to-nearest-even. handles NaN/Infinity/±0 and subnormals correctly.
    public static short floatToHalf(float value) {
        int bits = Float.floatToRawIntBits(value);
        int sign = (bits >>> 16) & 0x8000;
        int exp = (bits >>> 23) & 0xFF;
        int mant = bits & 0x7FFFFF;
        if (exp == 0xFF) {
            if (mant == 0) {
                return (short) (sign | 0x7C00);
            }
            int nanMant = mant >>> 13;
            if (nanMant == 0) {
                nanMant = 1;
            }
            return (short) (sign | 0x7C00 | nanMant);
        }
        int e15 = exp - 112;
        if (e15 >= 31) {
            return (short) (sign | 0x7C00);
        }
        if (e15 >= 1) {
            int significand = (mant >>> 13) | 0x400;
            int dropped = mant & 0x1FFF;
            if (dropped > 0x1000 || (dropped == 0x1000 && (significand & 1) != 0)) {
                significand++;
            }
            if (significand == 0x800) {
                e15++;
                if (e15 == 31) {
                    return (short) (sign | 0x7C00);
                }
                significand = 0x400;
            }
            return (short) (sign | (e15 << 10) | (significand & 0x3FF));
        }
        // subnormal half result or underflow to zero
        if (exp == 0 || exp < 102) {
            return (short) sign;
        }
        int significand = (mant | 0x800000) >>> (126 - exp);
        int dropped = (mant | 0x800000) & ((1 << (126 - exp)) - 1);
        int threshold = 1 << (125 - exp);
        if (dropped > threshold || (dropped == threshold && (significand & 1) != 0)) {
            significand++;
        }
        if (significand == 0x400) {
            return (short) (sign | 0x0400);
        }
        return (short) (sign | significand);
    }
}