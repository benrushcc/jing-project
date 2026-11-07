package io.jingproject.marshallcbor;

// a CBOR float value stored as its exact wire bit pattern for the requested width.
// keeping the bits avoids any nan payload or signed-zero loss when a decoded value
// is written back. the factory rejects conversions that are not exact.
public value record CborFloat(long bits, CborFloatWidth width) implements CborValue {

    public CborFloat {
        if (width == null) {
            throw new IllegalArgumentException("width must not be null");
        }
        if (width == CborFloatWidth.HALF && (bits & ~0xFFFFL) != 0L) {
            throw new IllegalArgumentException("half bits out of range : " + bits);
        }
        if (width == CborFloatWidth.SINGLE && (bits & ~0xFFFFFFFFL) != 0L) {
            throw new IllegalArgumentException("single bits out of range : " + bits);
        }
    }

    public static CborFloat ofDouble(double value, CborFloatWidth width) {
        if (width == CborFloatWidth.HALF) {
            float floatValue = (float) value;
            short halfBits = CborNumberUtil.floatToHalf(floatValue);
            if (Double.doubleToRawLongBits((double) CborNumberUtil.halfToFloat(halfBits))
                    != Double.doubleToRawLongBits(value)) {
                throw new IllegalArgumentException("value not exactly representable as half : " + value);
            }
            return new CborFloat(halfBits & 0xFFFFL, CborFloatWidth.HALF);
        }
        if (width == CborFloatWidth.SINGLE) {
            float floatValue = (float) value;
            if (Double.doubleToRawLongBits((double) floatValue) != Double.doubleToRawLongBits(value)) {
                throw new IllegalArgumentException("value not exactly representable as single : " + value);
            }
            return new CborFloat(Float.floatToRawIntBits(floatValue) & 0xFFFFFFFFL, CborFloatWidth.SINGLE);
        }
        return new CborFloat(Double.doubleToRawLongBits(value), CborFloatWidth.DOUBLE);
    }

}
