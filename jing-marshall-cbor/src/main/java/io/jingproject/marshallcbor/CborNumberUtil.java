package io.jingproject.marshallcbor;

import io.jingproject.common.ReadBuffer;
import io.jingproject.common.WriteBuffer;

import java.nio.ByteOrder;

// CBOR (RFC 8949) number encoding utilities: major-type/ai decomposition,
// head (initial byte + argument) construction and parsing, simple values,
// and half-precision floating-point conversion.
// the argument of a head is treated as an unsigned 64-bit value; callers must
// guarantee that it is non-negative.
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

    // write a head (initial byte + argument payload) using preferred
    // serialization: ai 0-23 is written inline, otherwise 24/25/26/27 selects
    // a 1/2/4/8-byte unsigned big-endian argument with no redundant zeros.
    public static void writeHead(int majorType, long argument, WriteBuffer writeBuffer) {
        int head = (majorType << 5) & 0xE0;
        if (Long.compareUnsigned(argument, 24L) < 0) {
            writeBuffer.writeByte((byte) (head | (int) argument));
            return;
        }
        if (Long.compareUnsigned(argument, 0x100L) < 0) {
            writeBuffer.writeBytes((byte) (head | 24), (byte) argument);
            return;
        }
        if (Long.compareUnsigned(argument, 0x10000L) < 0) {
            writeBuffer.writeByte((byte) (head | 25));
            writeBuffer.writeShort((short) argument, ByteOrder.BIG_ENDIAN);
            return;
        }
        if (Long.compareUnsigned(argument, 0x100000000L) < 0) {
            writeBuffer.writeByte((byte) (head | 26));
            writeBuffer.writeInt((int) argument, ByteOrder.BIG_ENDIAN);
            return;
        }
        writeBuffer.writeByte((byte) (head | 27));
        writeBuffer.writeLong(argument, ByteOrder.BIG_ENDIAN);
    }

    // write a major 7 simple value (bool/null/undefined) as a single byte;
    // simpleValue must be in [0, 31]. the caller is responsible for passing a
    // valid simple value.
    public static void writeSimple(int simpleValue, WriteBuffer writeBuffer) {
        writeBuffer.writeByte((byte) (0xE0 | simpleValue));
    }

    // read the argument payload selected by the additional information:
    // ai 0-23 returns the ai itself; ai 24/25/26/27 reads 1/2/4/8 unsigned
    // big-endian bytes; ai 28-31 (including the indefinite-length ai 31,
    // handled by the caller in container contexts) is rejected.
    public static long readArgument(int ai, ReadBuffer readBuffer) {
        if (ai < 24) {
            return ai;
        }
        return switch (ai) {
            case 24 -> readBuffer.readByte() & 0xFFL;
            case 25 -> readBuffer.readShort(ByteOrder.BIG_ENDIAN) & 0xFFFFL;
            case 26 -> readBuffer.readInt(ByteOrder.BIG_ENDIAN) & 0xFFFFFFFFL;
            case 27 -> readBuffer.readLong(ByteOrder.BIG_ENDIAN);
            default -> throw new CborDeserializerException("unsupported or illegal initial byte");
        };
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