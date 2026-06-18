package io.jingproject.marshalljson;

import io.jingproject.common.*;
import java.lang.foreign.MemorySegment;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public final class JsonNumberUtil {
    public static final byte BYTE_ZERO = (byte) '0';
    public static final byte BYTE_NINE = (byte) '9';
    public static final byte BYTE_MINUS = (byte) '-';
    public static final byte BYTE_PLUS = (byte) '+';
    public static final byte BYTE_PERIOD = (byte) '.';
    public static final byte BYTE_e = (byte) 'e';
    public static final byte BYTE_E = (byte) 'E';
    public static final byte BYTE_a = (byte) 'a';
    public static final byte BYTE_z = (byte) 'z';
    public static final byte BYTE_A = (byte) 'A';
    public static final byte BYTE_Z = (byte) 'Z';

    private static final byte[] MIN_INT_BYTES = String.valueOf(Integer.MIN_VALUE).getBytes(StandardCharsets.US_ASCII);
    private static final byte[] MIN_LONG_BYTES = String.valueOf(Long.MIN_VALUE).getBytes(StandardCharsets.US_ASCII);
    private static final int[] LEN_TABLE = makeLenTable();
    private static final long[] POW_TABLE = makePowTable();
    private static final short[] ITOA_LUT_TABLE = makeItoaLutTable();

    private static final FpSpec FLOAT_SPEC = new FpSpec(23, 8, -127, -189, 38, -45);
    private static final FpSpec DOUBLE_SPEC = new FpSpec(52, 11, -1023, -1085, 308, -324);
    private static final int MAX_FLOAT_CAPACITY = 15; // same as MAX_CHARS in jdk/internal/math/FloatToDecimal.java
    private static final int MAX_DOUBLE_CAPACITY = 24; // same as MAX_CHARS in jdk/internal/math/DoubleToDecimal.java
    private static final short NEG_ZERO = Utils.compact(BYTE_MINUS, BYTE_ZERO);
    private static final short ZERO_PERIOD = Utils.compact(BYTE_ZERO, BYTE_PERIOD);
    private static final short E_MINUS = Utils.compact(BYTE_E, BYTE_MINUS);
    private static final int POW10MIN = -348;
    private static final int POW10MAX = 347;
    private static final long[] POW10TAB = makePow10Table(); // huge table

    // no overflow
    private static int[] makeLenTable() {
        int[] r = new int[64];
        for (int i = 1; i <= 63; i++) {
            long maxNum = -1L >>> i;
            r[i] = String.valueOf(maxNum).getBytes(StandardCharsets.US_ASCII).length;
        }
        return r;
    }

    // no overflow
    private static long[] makePowTable() {
        long[] r = new long[20];
        for (int i = 2; i <= 19; i++) {
            r[i] = Math.powExact(10L, i - 1);
        }
        return r;
    }

    // no overflow
    private static short[] makeItoaLutTable() {
        short[] r = new short[100];
        for (int i = 0; i < 100; i++) {
            byte b0 = (byte) (BYTE_ZERO + (i / 10));
            byte b1 = (byte) (BYTE_ZERO + (i % 10));
            r[i] = Utils.compact(b0, b1);
        }
        return r;
    }

    // no overflow
    private static long[] makePow10Table() {
        BigInteger two = BigInteger.ONE.add(BigInteger.ONE);
        BigInteger oneShift64 = BigInteger.ONE.shiftLeft(64);
        BigInteger oneShift128 = BigInteger.ONE.shiftLeft(128);
        int count = POW10MAX - POW10MIN + 1;
        long[] r = new long[count * 2];
        int index = 0;
        for (int e = POW10MIN; e <= POW10MAX; e++) {
            BigInteger num, den;
            if (e >= 0) {
                num = BigInteger.TEN.pow(e);
                den = BigInteger.ONE;
            } else {
                num = BigInteger.ONE;
                den = BigInteger.TEN.pow(-e);
            }
            while (num.compareTo(den.multiply(oneShift128)) < 0) {
                num = num.multiply(two);
            }
            while (num.compareTo(den.multiply(oneShift128)) >= 0) {
                den = den.multiply(two);
            }
            BigInteger d = num.divide(den);
            BigInteger[] hiLo = d.divideAndRemainder(oneShift64);
            long uhi = hiLo[0].longValue();
            long ulo = hiLo[1].longValue();
            if (!num.mod(den).equals(BigInteger.ZERO)) {
                ulo = Math.incrementExact(ulo);
                if (ulo == 0L) {
                    uhi = Math.incrementExact(uhi);
                }
            }
            if (ulo != 0L) {
                uhi = Math.incrementExact(uhi);
                ulo = Math.negateExact(ulo);
            }
            r[index++] = uhi;
            r[index++] = ulo;
        }
        return r;
    }

    private JsonNumberUtil() {
        throw new UnsupportedOperationException("utility class");
    }

    private static int digitCount(int n) {
        assert n > 0;
        int leadingZeros = Integer.numberOfLeadingZeros(n);
        int count = LEN_TABLE[leadingZeros + Long.SIZE - Integer.SIZE];
        if (n < POW_TABLE[count]) {
            return count - 1;
        }
        return count;
    }

    private static int digitCount(long n) {
        assert n > 0L;
        int leadingZeros = Long.numberOfLeadingZeros(n);
        int count = LEN_TABLE[leadingZeros];
        if (n < POW_TABLE[count]) {
            return count - 1;
        }
        return count;
    }

    public static void writeInt(int value, WriteBuffer writeBuffer) {
        switch (value) {
            case 0 -> writeBuffer.writeByte(BYTE_ZERO);
            case Integer.MIN_VALUE -> writeBuffer.writeBytes(MIN_INT_BYTES);
            case int i when i > 0 -> writeBuffer.setPosition(writePositiveInt(value, writeBuffer));
            default -> {
                writeBuffer.writeByte(BYTE_MINUS);
                writeBuffer.setPosition(writePositiveInt(-value, writeBuffer));
            }
        }
    }

    private static int writePositiveInt(int value, WriteBuffer writeBuffer) {
        assert value > 0;
        int n = digitCount(value);
        writeBuffer.ensureCapacity(n);
        int position = writeBuffer.intPosition();
        return switch (writeBuffer) {
            case HeapWriteBuffer heapWriteBuffer -> writePositiveIntToHeap(value, n, heapWriteBuffer.rawByteArray(), position);
            case SegmentWriteBuffer segmentWriteBuffer -> writePositiveIntToSegment(value, n, segmentWriteBuffer.rawSegment(), position);
        };
    }

    private static int writePositiveIntToHeap(int value, int n, byte[] bytes, int position) {
        assert bytes != null && Objects.checkFromIndexSize(position, n, bytes.length) >= 0;
        int v;
        switch (n) {
            case 10:
                v = value / 100000000;
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 100000000;
            case 8:
                v = value / 1000000;
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1000000;
            case 6:
                v = value / 10000;
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 10000;
            case 4:
                v = value / 100;
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 100;
            case 2:
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[value]);
                position += 2;
                break;
            case 9:
                v = value / 10000000;
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 10000000;
            case 7:
                v = value / 100000;
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 100000;
            case 5:
                v = value / 1000;
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1000;
            case 3:
                v = value / 10;
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 10;
            case 1:
                bytes[position] = (byte) (BYTE_ZERO + value);
                position++;
        }
        return position;
    }

    private static int writePositiveIntToSegment(int value, int n, MemorySegment segment, int position) {
        int v;
        switch (n) {
            case 10:
                v = value / 100000000;
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 100000000;
            case 8:
                v = value / 1000000;
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1000000;
            case 6:
                v = value / 10000;
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 10000;
            case 4:
                v = value / 100;
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 100;
            case 2:
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[value]);
                position += 2;
                break;
            case 9:
                v = value / 10000000;
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 10000000;
            case 7:
                v = value / 100000;
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 100000;
            case 5:
                v = value / 1000;
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1000;
            case 3:
                v = value / 10;
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 10;
            case 1:
                SegmentAccess.setByte(segment, position, (byte) (BYTE_ZERO + value));
                position++;
        }
        return position;
    }

    public static void writeLong(long value, WriteBuffer writeBuffer) {
        switch (value) {
            case 0L -> writeBuffer.writeByte(BYTE_ZERO);
            case Long.MIN_VALUE -> writeBuffer.writeBytes(MIN_LONG_BYTES);
            case long l when l > 0 -> writeBuffer.setPosition(writePositiveLong(value, writeBuffer));
            default -> {
                writeBuffer.writeByte(BYTE_MINUS);
                writeBuffer.setPosition(writePositiveLong(-value, writeBuffer));
            }
        }
    }

    private static int writePositiveLong(long value, WriteBuffer writeBuffer) {
        assert value > 0L;
        int n = digitCount(value);
        writeBuffer.ensureCapacity(n);
        int position = writeBuffer.intPosition();
        return switch (writeBuffer) {
            case HeapWriteBuffer heapWriteBuffer -> writePositiveLongToHeap(value, n, heapWriteBuffer.rawByteArray(), position);
            case SegmentWriteBuffer segmentWriteBuffer -> writePositiveLongToSegment(value, n, segmentWriteBuffer.rawSegment(), position);
        };
    }

    private static int writePositiveLongToHeap(long value, int n, byte[] bytes, int position) {
        int v;
        switch (n) {
            case 18:
                v = (int) (value / 1_000_000_000_000_000_0L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_000_000_0L;
            case 16:
                v = (int) (value / 1_000_000_000_000_00L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_000_00L;
            case 14:
                v = (int) (value / 1_000_000_000_000L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_000L;
            case 12:
                v = (int) (value / 1_000_000_000_0L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_0L;
            case 10:
                v = (int) (value / 1_000_000_00L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_00L;
            case 8:
                v = (int) (value / 1_000_000L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000L;
            case 6:
                v = (int) (value / 1_000_0L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_0L;
            case 4:
                v = (int) (value / 100L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 100L;
            case 2:
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[(int) value]);
                position += 2;
                break;
            case 19:
                v = (int) (value / 1_000_000_000_000_000_00L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_000_000_00L;
            case 17:
                v = (int) (value / 1_000_000_000_000_000L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_000_000L;
            case 15:
                v = (int) (value / 1_000_000_000_000_0L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_000_0L;
            case 13:
                v = (int) (value / 1_000_000_000_00L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_00L;
            case 11:
                v = (int) (value / 1_000_000_000L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000L;
            case 9:
                v = (int) (value / 1_000_000_0L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_0L;
            case 7:
                v = (int) (value / 1_000_00L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_00L;
            case 5:
                v = (int) (value / 1_000L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000L;
            case 3:
                v = (int) (value / 10L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 10L;
            case 1:
                bytes[position] = (byte) (BYTE_ZERO + value);
                position++;
        }
        return position;
    }

    private static int writePositiveLongToSegment(long value, int n, MemorySegment segment, int position) {
        int v;
        switch (n) {
            case 18:
                v = (int) (value / 1_000_000_000_000_000_0L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_000_000_0L;
            case 16:
                v = (int) (value / 1_000_000_000_000_00L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_000_00L;
            case 14:
                v = (int) (value / 1_000_000_000_000L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_000L;
            case 12:
                v = (int) (value / 1_000_000_000_0L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_0L;
            case 10:
                v = (int) (value / 1_000_000_00L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_00L;
            case 8:
                v = (int) (value / 1_000_000L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000L;
            case 6:
                v = (int) (value / 1_000_0L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_0L;
            case 4:
                v = (int) (value / 100L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 100L;
            case 2:
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[(int) value]);
                position += 2;
                break;
            case 19:
                v = (int) (value / 1_000_000_000_000_000_00L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_000_000_00L;
            case 17:
                v = (int) (value / 1_000_000_000_000_000L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_000_000L;
            case 15:
                v = (int) (value / 1_000_000_000_000_0L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_000_0L;
            case 13:
                v = (int) (value / 1_000_000_000_00L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_00L;
            case 11:
                v = (int) (value / 1_000_000_000L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000L;
            case 9:
                v = (int) (value / 1_000_000_0L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_0L;
            case 7:
                v = (int) (value / 1_000_00L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_00L;
            case 5:
                v = (int) (value / 1_000L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000L;
            case 3:
                v = (int) (value / 10L);
                SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 10L;
            case 1:
                SegmentAccess.setByte(segment, position, (byte) (BYTE_ZERO + value));
                position++;
        }
        return position;
    }

    public static void writeFloat(float f, WriteBuffer writeBuffer) {
        assert Float.isFinite(f);
        int bits = Float.floatToRawIntBits(f);
        boolean negative = (bits >>> (Float.SIZE - 1)) == 1;
        if ((bits & 0x7FFFFFFF) == 0) {
            if (negative) {
                writeBuffer.writeShort(NEG_ZERO);
            } else {
                writeBuffer.writeByte(BYTE_ZERO);
            }
            return;
        }
        writeBuffer.ensureCapacity(MAX_FLOAT_CAPACITY);
        if (negative) {
            bits &= ~(1 << (Float.SIZE - 1));
            writeBuffer.writeByte(BYTE_MINUS);
        }
        BinaryFp binaryFp = buildBinaryFp(bits, FLOAT_SPEC);
        DecimalFp decimalFp = toDecimalFp(binaryFp, FLOAT_SPEC);
        writeDecimalFp(decimalFp, writeBuffer);
    }

    public static void writeDouble(double f, WriteBuffer writeBuffer) {
        assert Double.isFinite(f);
        long bits = Double.doubleToRawLongBits(f);
        boolean negative = (bits >>> (Double.SIZE - 1)) == 1L;
        if ((bits & 0x7FFFFFFFFFFFFFFFL) == 0L) {
            if (negative) {
                writeBuffer.writeShort(NEG_ZERO);
            } else {
                writeBuffer.writeByte(BYTE_ZERO);
            }
            return;
        }
        writeBuffer.ensureCapacity(MAX_DOUBLE_CAPACITY);
        if (negative) {
            bits &= ~(1L << (Double.SIZE - 1));
            writeBuffer.writeByte(BYTE_MINUS);
        }
        BinaryFp binaryFp = buildBinaryFp(bits, DOUBLE_SPEC);
        DecimalFp decimalFp = toDecimalFp(binaryFp, DOUBLE_SPEC);
        writeDecimalFp(decimalFp, writeBuffer);
    }

    private static BinaryFp buildBinaryFp(long b, FpSpec fpSpec) {
        long mant = b & ((1L << fpSpec.mantBits()) - 1);
        int exp = (int) ((b >>> fpSpec.mantBits()) & ((1L << fpSpec.expBits()) - 1));
        if (exp == 0) {
            exp++;
        } else {
            mant |= (1L << fpSpec.mantBits());
        }
        exp += fpSpec.bias();
        int s = Long.numberOfLeadingZeros(mant);
        return new BinaryFp(mant << s, exp - s - fpSpec.mantBits());
    }

    private static long ufloor(long u) {
        return (u) >>> 2;
    }

    private static long uceil(long u) {
        return (u + 3L) >>> 2;
    }

    private static long unudge(long u, int d) {
        return u + d;
    }

    private static long uround(long u) {
        return (u + 1L + ((u >>> 2) & 1L)) >>> 2;
    }

    private static long umin(long u) {
        return (u << 2) - 2L;
    }

    private static int skewed(int e) {
        // skewed computes the skewed footprint of m * 2**e,
        // which is ⌊log₁₀ 3/4 * 2**e⌋ = ⌊e*(log₁₀ 2)-(log₁₀ 4/3)⌋.
        assert e <= Integer.MAX_VALUE / 631305;
        return (e * 631305 - 261663) >> 21;
    }

    private static int log10Pow2(int x) {
        // log₁₀ 2 ≈ 0.30102999566 ≈ 78913 / 2^18
        assert x <= Integer.MAX_VALUE / 78913;
        return (x * 78913) >> 18;
    }

    private static int log2Pow10(int x) {
        // log₂ 10 ≈ 3.32192809489 ≈ 108853 / 2^15
        assert x <= Integer.MAX_VALUE / 108853;
        return (x * 108853) >> 15;
    }

    // no overflow
    private static Scalers prescale(int e, int p, int lp) {
        assert p >= POW10MIN && p <= POW10MAX;
        int s = -(e + lp + 3);
        assert s >= 0;
        int idx = (p - POW10MIN) << 1;
        long pmHi = POW10TAB[idx];
        long pmLo = POW10TAB[idx + 1];
        return new Scalers(pmHi, pmLo, s);
    }

    // no overflow
    private static long uscale(long x, Scalers c) {
        assert c.s() >=0 && c.s() < 64;
        long hi = Math.unsignedMultiplyHigh(x, c.pmHi());
        long mid1 = x * c.pmHi();
        long sticky = 1L;
        if ((hi & ((1L << c.s()) - 1L)) == 0L) {
            long mid2 = Math.unsignedMultiplyHigh(x, c.pmLo());
            sticky = Long.compareUnsigned(mid1 - mid2, 1L) > 0 ? 1L : 0L;
            if (Long.compareUnsigned(mid1, mid2) < 0) {
                hi -= 1L;
            }
        }
        return (hi >>> c.s()) | sticky;
    }

    // precomputed constants used by trimZeros()
    private static final long MAX_UINT_64 = 0xFFFFFFFFFFFFFFFFL;
    private static final long DIV_1_E_8_M = 0xc767074b22e90e21L;  // inverse of 5^8
    private static final long DIV_1_E_4_M = 0xd288ce703afb7e91L;  // inverse of 5^4
    private static final long DIV_1_E_2_M = 0x8f5c28f5c28f5c29L;  // inverse of 5^2
    private static final long DIV_1_E_1_M = 0xcccccccccccccccdL;  // inverse of 5
    private static final long DIV_1_E_8_LE = Long.divideUnsigned(MAX_UINT_64, 100_000_000L);
    private static final long DIV_1_E_4_LE = Long.divideUnsigned(MAX_UINT_64, 10_000L);
    private static final long DIV_1_E_2_LE = Long.divideUnsigned(MAX_UINT_64, 100L);
    private static final long DIV_1_E_1_LE = Long.divideUnsigned(MAX_UINT_64, 10L);

    private static DecimalFp trimZeros(DecimalFp decimalFp) {
        long d = decimalFp.d();
        int p = decimalFp.e();
        // cut 1 zero, or else return.
        long tmp = Long.rotateRight(d * DIV_1_E_1_M, 1);
        if (Long.compareUnsigned(tmp, DIV_1_E_1_LE) > 0) {
            return decimalFp;
        }
        d = tmp;
        p += 1;
        // cut 8 zeros, then 4, then 2, then 1.
        tmp = Long.rotateRight(d * DIV_1_E_8_M, 8);
        if (Long.compareUnsigned(tmp, DIV_1_E_8_LE) <= 0) {
            d = tmp;
            p += 8;
        }
        tmp = Long.rotateRight(d * DIV_1_E_4_M, 4);
        if (Long.compareUnsigned(tmp, DIV_1_E_4_LE) <= 0) {
            d = tmp;
            p += 4;
        }
        tmp = Long.rotateRight(d * DIV_1_E_2_M, 2);
        if (Long.compareUnsigned(tmp, DIV_1_E_2_LE) <= 0) {
            d = tmp;
            p += 2;
        }
        tmp = Long.rotateRight(d * DIV_1_E_1_M, 1);
        if (Long.compareUnsigned(tmp, DIV_1_E_1_LE) <= 0) {
            d = tmp;
            p += 1;
        }
        return new DecimalFp(d, p);
    }

    private static DecimalFp toDecimalFp(BinaryFp binaryFp, FpSpec fpSpec) {
        long m = binaryFp.m();
        int e = binaryFp.e();
        int p;
        long min;
        int z = Long.SIZE - 1 - fpSpec.mantBits();
        if (m == (1L << (Double.SIZE - 1)) && e > fpSpec.minExp()) {
            p = -skewed(e + z);
            min = m - (1L << (z - 2));
        } else {
            if (e < fpSpec.minExp()) {
                z += (fpSpec.minExp() - e);
            }
            p = -log10Pow2(e + z);
            min = m - (1L << (z - 1));
        }
        long max = m + (1L << (z - 1));
        int odd = (int) (m >>> z) & 1;
        Scalers pre = prescale(e, p, log2Pow10(p));
        long dmin = uceil(unudge(uscale(min, pre), odd));
        long dmax = ufloor(unudge(uscale(max, pre), -odd));
        long d = Long.divideUnsigned(dmax, 10L);
        if(Long.compareUnsigned(d * 10L, dmin) >= 0) {
            return trimZeros(new DecimalFp(d, -(p - 1)));
        }
        return new DecimalFp(Long.compareUnsigned(dmin, dmax) < 0 ? uround(uscale(m, pre)) : dmin, -p);
    }

    private static final int MIN_SCI_EXP = -3; // align with jdk format, inclusive
    private static final int MAX_SCI_EXP = 7; // align with jdk format, exclusive

    private static void writeDecimalFp(DecimalFp decimalFp, WriteBuffer writeBuffer) {
        long d = decimalFp.d();
        int e = decimalFp.e();
        int n = digitCount(d);
        int sciE = e + n - 1;
        switch (writeBuffer) {
            case HeapWriteBuffer heapWriteBuffer -> writeDecimalFpToHeap(d, e, n, sciE, heapWriteBuffer);
            case SegmentWriteBuffer segmentWriteBuffer ->  writeDecimalFpToSegment(d, e, n, sciE, segmentWriteBuffer);
        }
    }

    private static void writeDecimalFpToHeap(long d, int e, int n, int sciE, HeapWriteBuffer heapWriteBuffer) {
        int position = heapWriteBuffer.intPosition();
        byte[] bytes = heapWriteBuffer.rawByteArray();
        if (sciE >= MIN_SCI_EXP && sciE < MAX_SCI_EXP) {
            position = writeFixedDecimalFpToHeap(d, e, n, bytes, position);
        } else {
            position = writeSciDecimalFpToHeap(d, sciE, n, bytes, position);
        }
        heapWriteBuffer.setPosition(position);
    }

    private static int writeFixedDecimalFpToHeap(long d, int e, int n, byte[] bytes, int position) {
        if(e >= 0) {
            position = writePositiveLongToHeap(d, n, bytes, position);
            if(e > 0) {
                int newPosition = position + e;
                Arrays.fill(bytes, position, newPosition, BYTE_ZERO);
                position = newPosition;
            }
        } else {
            int fracDigits = -e;
            if (fracDigits >= n) {
                ArrayAccess.setShort(bytes, position, ZERO_PERIOD);
                position += 2;
                int leadingZeros = fracDigits - n;
                if (leadingZeros > 0) {
                    int newPosition = position + leadingZeros;
                    Arrays.fill(bytes, position, newPosition, BYTE_ZERO);
                    position = newPosition;
                }
                position = writePositiveLongToHeap(d, n, bytes, position);
            } else {
                int dotPosition = position + (n - fracDigits);
                position = writePositiveLongToHeap(d, n, bytes, position) + 1;
                System.arraycopy(bytes, dotPosition, bytes, dotPosition + 1, fracDigits);
                bytes[dotPosition] = BYTE_PERIOD;
            }
        }
        return position;
    }

    private static int writeSciDecimalFpToHeap(long d, int sciE, int n, byte[] bytes, int position) {
        if (n > 1) {
            int startPosition = position + 1;
            int endPosition = writePositiveLongToHeap(d, n, bytes, startPosition);
            short shifted = Utils.compact(bytes[startPosition], BYTE_PERIOD);
            ArrayAccess.setShort(bytes, position, shifted);
            position = endPosition;
        } else {
            bytes[position++] = (byte) (BYTE_ZERO + d);
        }
        if(sciE < 0) {
            ArrayAccess.setShort(bytes, position, E_MINUS);
            position += 2;
            sciE = -sciE;
        } else {
            bytes[position++] = BYTE_E;
        }
        position = writePositiveIntToHeap(sciE, digitCount(sciE), bytes, position);
        return position;
    }


    private static void writeDecimalFpToSegment(long d, int e, int n, int sciE, SegmentWriteBuffer segmentWriteBuffer) {
        int position = segmentWriteBuffer.intPosition();
        MemorySegment segment = segmentWriteBuffer.rawSegment();
        if (sciE >= MIN_SCI_EXP && sciE < MAX_SCI_EXP) {
            position = writeFixedDecimalFpToSegment(d, e, n, segment, position);
        } else {
            position = writeSciDecimalFpToSegment(d, sciE, n, segment, position);
        }
        segmentWriteBuffer.setPosition(position);
    }

    private static int writeFixedDecimalFpToSegment(long d, int e, int n, MemorySegment segment, int position) {
        if (e >= 0) {
            position = writePositiveLongToSegment(d, n, segment, position);
            if (e > 0) {
                segment.asSlice(position, e).fill(BYTE_ZERO);
                position += e;
            }
        } else {
            int fracDigits = -e;
            if (fracDigits >= n) {
                SegmentAccess.setShort(segment, position, ZERO_PERIOD);
                position += 2;
                int leadingZeros = fracDigits - n;
                if (leadingZeros > 0) {
                    segment.asSlice(position, leadingZeros).fill(BYTE_ZERO);
                    position += leadingZeros;
                }
                position = writePositiveLongToSegment(d, n, segment, position);
            } else {
                long dotPosition = position + (n - fracDigits);
                position = writePositiveLongToSegment(d, n, segment, position) + 1;
                MemorySegment.copy(segment, dotPosition, segment, dotPosition + 1L, fracDigits);
                SegmentAccess.setByte(segment, dotPosition, BYTE_PERIOD);
            }
        }
        return position;
    }

    private static int writeSciDecimalFpToSegment(long d, int sciE, int n, MemorySegment segment, int position) {
        if (n > 1) {
            int startPosition = position + 1;
            int endPosition = writePositiveLongToSegment(d, n, segment, startPosition);
            short shifted = Utils.compact(SegmentAccess.getByte(segment, startPosition), BYTE_PERIOD);
            SegmentAccess.setShort(segment, position, shifted);
            position = endPosition;
        } else {
            SegmentAccess.setByte(segment, position++, (byte) (BYTE_ZERO + d));
        }
        if(sciE < 0) {
            SegmentAccess.setShort(segment, position, E_MINUS);
            position += 2;
            sciE = -sciE;
        } else {
            SegmentAccess.setByte(segment, position++, BYTE_E);
        }
        position = writePositiveIntToSegment(sciE, digitCount(sciE), segment, position);
        return position;
    }

    public record BinaryFp(long m, int e) {
    }

    public record DecimalFp(long d, int e) {
    }

    public record FpSpec(
            int mantBits,
            int expBits,
            int bias,
            int minExp,
            int maxDecExp,
            int minDecExp
    ) {
    }

    public record Scalers (
            long pmHi,
            long pmLo,
            int s
    ) {
    }
}
