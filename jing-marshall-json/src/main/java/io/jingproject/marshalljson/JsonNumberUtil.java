package io.jingproject.marshalljson;

import io.jingproject.common.*;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * floating-point parsing and printing algorithms are based on Russ Cox's
 * "Floating-Point Printing and Parsing Can Be Simple And Fast".
 *
 * @see <a href="https://research.swtch.com/fp-all">research.swtch.com/fp-all</a>
 */
public final class JsonNumberUtil {
    public static final byte BYTE_ZERO = (byte) '0';
    public static final byte BYTE_NINE = (byte) '9';
    public static final byte BYTE_MINUS = (byte) '-';
    public static final byte BYTE_PLUS = (byte) '+';
    public static final byte BYTE_PERIOD = (byte) '.';
    public static final byte BYTE_e = (byte) 'e';
    public static final byte BYTE_E = (byte) 'E';

    private static final byte[] MIN_INT_BYTES = String.valueOf(Integer.MIN_VALUE).getBytes(StandardCharsets.US_ASCII);
    private static final byte[] MIN_LONG_BYTES = String.valueOf(Long.MIN_VALUE).getBytes(StandardCharsets.US_ASCII);
    private static final int[] LEN_TABLE = makeLenTable();
    private static final long[] POW_TABLE = makePowTable();
    private static final short[] ITOA_LUT_TABLE = makeItoaLutTable();

    private static final FpSpec FLOAT_SPEC = new FpSpec(23, 8, -127, -189, 38, -45);
    private static final FpSpec DOUBLE_SPEC = new FpSpec(52, 11, -1023, -1085, 308, -324);
    private static final int MAX_FLOAT_CAPACITY = 15; // same as MAX_CHARS in jdk/internal/math/FloatToDecimal.java
    private static final int MAX_DOUBLE_CAPACITY = 24; // same as MAX_CHARS in jdk/internal/math/DoubleToDecimal.java
    private static final int POW10MIN = -348;
    private static final int POW10MAX = 347;
    private static final long[] POW10TAB = makePow10Table(); // huge table
    private static final byte[] ZERO_NINE_TABLE = makeZeroNineTable();

    private static final int MIN_SCI_EXP = -3; // align with jdk format, inclusive
    private static final int MAX_SCI_EXP = 7; // align with jdk format, exclusive

    private static final int N_DIV_10_I = Integer.MIN_VALUE / 10;
    private static final byte N_MOD_10_I = (byte) (Integer.MIN_VALUE % 10);

    private static final long N_DIV_10_L = Long.MIN_VALUE / 10;
    private static final byte N_MOD_10_L = (byte) (Long.MIN_VALUE % 10);

    // Maximum decimal digits that fit in a uint64 (19)
    private static final int MAX_DECIMAL_ND = 19;

    // Maximum exponent digits. For fp32/fp64, exponents beyond 10000 in absolute
    // value never affect the final result. Truncation is necessary to avoid overflow
    // in log2Pow10, skewed, and similar functions.
    private static final int MAX_DECIMAL_P = 10000;

    private static final float[] FLOAT_POW_10 = {
            1e0f, 1e1f, 1e2f, 1e3f, 1e4f, 1e5f, 1e6f, 1e7f, 1e8f, 1e9f, 1e10f
    };
    private static final int FLOAT_EXACT_I = 7;
    private static final int FLOAT_EXACT_P = 10;
    private static final float FLOAT_EXACT_I_HIGH = 1e7f;
    private static final float FLOAT_EXACT_I_LOW = 1e-7f;

    private static final double[] DOUBLE_POW_10 = {
            1e0, 1e1, 1e2, 1e3, 1e4, 1e5, 1e6, 1e7, 1e8, 1e9,
            1e10, 1e11, 1e12, 1e13, 1e14, 1e15, 1e16, 1e17,
            1e18, 1e19, 1e20, 1e21, 1e22
    };
    private static final int DOUBLE_EXACT_I = 15;
    private static final int DOUBLE_EXACT_P = 22;
    private static final double DOUBLE_EXACT_I_HIGH = 1e15;
    private static final double DOUBLE_EXACT_I_LOW = 1e-15;

    private JsonNumberUtil() {
        throw new UnsupportedOperationException("utility class");
    }

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

    private static long[] makePow10Table() {
        BigInteger mask64 = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);
        BigInteger[] pow10 = new BigInteger[-POW10MIN + 1];
        pow10[0] = BigInteger.ONE;
        for (int i = 1; i <= -POW10MIN; i++) {
            pow10[i] = pow10[i - 1].multiply(BigInteger.TEN);
        }
        int count = POW10MAX - POW10MIN + 1;
        long[] r = new long[count * 2];
        int index = 0;
        for (int e = POW10MIN; e <= POW10MAX; e++) {
            BigInteger[] qr = computeScaledPower(e, pow10);
            long uhi = qr[0].shiftRight(64).longValue();
            long ulo = qr[0].and(mask64).longValue();
            if (qr[1].signum() != 0) {
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

    private static BigInteger[] computeScaledPower(int e, BigInteger[] pow10) {
        BigInteger num = e >= 0 ? pow10[e] : BigInteger.ONE;
        BigInteger den = e >= 0 ? BigInteger.ONE : pow10[-e];
        BigInteger shifted = den.shiftLeft(128);
        int numPreShifted = shifted.bitLength() - num.bitLength() - 1;
        if (numPreShifted > 0) {
            num = num.shiftLeft(numPreShifted);
        }
        while (num.compareTo(shifted) < 0) {
            num = num.shiftLeft(1);
        }
        int denPreShifted = num.bitLength() - shifted.bitLength() - 1;
        if (denPreShifted > 0) {
            den = den.shiftLeft(denPreShifted);
            shifted = shifted.shiftLeft(denPreShifted);
        }
        while (num.compareTo(shifted) >= 0) {
            den = den.shiftLeft(1);
            shifted = shifted.shiftLeft(1);
        }
        return num.divideAndRemainder(den);
    }

    private static byte[] makeZeroNineTable() {
        byte[] r = new byte[Byte.MAX_VALUE - Byte.MIN_VALUE + 1];
        Arrays.fill(r, Byte.MAX_VALUE);
        for (byte b = BYTE_ZERO; b <= BYTE_NINE; b++) {
            int index = Byte.toUnsignedInt(b);
            r[index] = (byte) (BYTE_ZERO - b);
        }
        return r;
    }

    private static int digitCount(int n) {
        int leadingZeros = Integer.numberOfLeadingZeros(n);
        int count = LEN_TABLE[leadingZeros + Long.SIZE - Integer.SIZE];
        if (n < POW_TABLE[count]) {
            return count - 1;
        }
        return count;
    }

    private static int digitCount(long n) {
        int leadingZeros = Long.numberOfLeadingZeros(n);
        int count = LEN_TABLE[leadingZeros];
        if (n < POW_TABLE[count]) {
            return count - 1;
        }
        return count;
    }

    public static void writeInt(int value, WriteBuffer writeBuffer) {
        writeBuffer.ensureCapacity(MIN_INT_BYTES.length);
        switch (writeBuffer) {
            case HeapWriteBuffer heapWriteBuffer -> writeIntToHeap(value, heapWriteBuffer);
            case SegmentWriteBuffer segmentWriteBuffer -> writeIntToSegment(value, segmentWriteBuffer);
        }
    }

    private static void writeIntToHeap(int value, HeapWriteBuffer heapWriteBuffer) {
        final byte[] bytes = heapWriteBuffer.rawByteArray();
        int position = heapWriteBuffer.intPosition();
        if(value == 0L) {
            bytes[position] = BYTE_ZERO;
            heapWriteBuffer.setPosition(position + 1);
            return ;
        }
        if(value == Integer.MIN_VALUE) {
            System.arraycopy(MIN_INT_BYTES, 0, bytes, position, MIN_INT_BYTES.length);
            heapWriteBuffer.setPosition(position + MIN_INT_BYTES.length);
            return ;
        }
        if(value < 0L){
            bytes[position++] = BYTE_MINUS;
            value = -value;
        }
        heapWriteBuffer.setPosition(writePositiveIntToHeap(value, digitCount(value), bytes, position));
    }

    private static void writeIntToSegment(int value, SegmentWriteBuffer segmentWriteBuffer) {
        final MemorySegment segment = segmentWriteBuffer.rawSegment();
        int position = segmentWriteBuffer.intPosition();
        if(value == 0L) {
            SegmentAccess.setByte(segment, position, BYTE_ZERO);
            segmentWriteBuffer.setPosition(position + 1);
            return ;
        }
        if(value == Integer.MIN_VALUE) {
            MemorySegment.copy(MIN_INT_BYTES, 0, segment, ValueLayout.JAVA_BYTE, position, MIN_INT_BYTES.length);
            segmentWriteBuffer.setPosition(position + MIN_INT_BYTES.length);
            return ;
        }
        if(value < 0L){
            SegmentAccess.setByte(segment, position++, BYTE_MINUS);
            value = -value;
        }
        segmentWriteBuffer.setPosition(writePositiveIntToSegment(value, digitCount(value), segment, position));
    }

    private static int writePositiveIntToHeap(int value, int digitCount, byte[] bytes, int position) {
        for(int index = position + digitCount - 2; index >= position; index -= 2) {
            int d = value / 100;
            short v = ITOA_LUT_TABLE[value - 100 * d];
            value = d;
            ArrayAccess.setShort(bytes, index, v);
        }
        if((digitCount & 1) != 0) {
            bytes[position] = (byte) (BYTE_ZERO + value);
        }
        return position + digitCount;
    }

    private static int writePositiveIntToSegment(int value, int digitCount, MemorySegment segment, int position) {
        for(int index = position + digitCount - 2; index >= position; index -= 2) {
            int d = value / 100;
            short v = ITOA_LUT_TABLE[value - 100 * d];
            value = d;
            SegmentAccess.setShort(segment, index, v);
        }
        if((digitCount & 1) != 0) {
            SegmentAccess.setByte(segment, position, (byte) (BYTE_ZERO + value));
        }
        return position + digitCount;
    }

    public static void writeLong(long value, WriteBuffer writeBuffer) {
        writeBuffer.ensureCapacity(MIN_LONG_BYTES.length);
        switch (writeBuffer) {
            case HeapWriteBuffer heapWriteBuffer -> writeLongToHeap(value, heapWriteBuffer);
            case SegmentWriteBuffer segmentWriteBuffer -> writeLongToSegment(value, segmentWriteBuffer);
        }
    }

    private static void writeLongToHeap(long value, HeapWriteBuffer heapWriteBuffer) {
        final byte[] bytes = heapWriteBuffer.rawByteArray();
        int position = heapWriteBuffer.intPosition();
        if(value == 0L) {
            bytes[position] = BYTE_ZERO;
            heapWriteBuffer.setPosition(position + 1);
            return ;
        }
        if(value == Long.MIN_VALUE) {
            System.arraycopy(MIN_LONG_BYTES, 0, bytes, position, MIN_LONG_BYTES.length);
            heapWriteBuffer.setPosition(position + MIN_LONG_BYTES.length);
            return ;
        }
        if(value < 0L){
            bytes[position++] = BYTE_MINUS;
            value = -value;
        }
        heapWriteBuffer.setPosition(writePositiveLongToHeap(value, digitCount(value), bytes, position));
    }

    private static void writeLongToSegment(long value, SegmentWriteBuffer segmentWriteBuffer) {
        final MemorySegment segment = segmentWriteBuffer.rawSegment();
        int position = segmentWriteBuffer.intPosition();
        if(value == 0L) {
            SegmentAccess.setByte(segment, position, BYTE_ZERO);
            segmentWriteBuffer.setPosition(position + 1);
            return ;
        }
        if(value == Long.MIN_VALUE) {
            MemorySegment.copy(MIN_LONG_BYTES, 0, segment, ValueLayout.JAVA_BYTE, position, MIN_LONG_BYTES.length);
            segmentWriteBuffer.setPosition(position + MIN_LONG_BYTES.length);
            return ;
        }
        if(value < 0L){
            SegmentAccess.setByte(segment, position++, BYTE_MINUS);
            value = -value;
        }
        segmentWriteBuffer.setPosition(writePositiveLongToSegment(value, digitCount(value), segment, position));
    }

    private static int writePositiveLongToHeap(long value, int digitCount, byte[] bytes, int position) {
        for(int index = position + digitCount - 2; index >= position; index -= 2) {
            long d = value / 100L;
            short v = ITOA_LUT_TABLE[(int) (value - 100L * d)];
            value = d;
            ArrayAccess.setShort(bytes, index, v);
        }
        if((digitCount & 1) != 0) {
            bytes[position] = (byte) (BYTE_ZERO + value);
        }
        return position + digitCount;
    }

    private static int writePositiveLongToSegment(long value, int digitCount, MemorySegment segment, int position) {
        for(int index = position + digitCount - 2; index >= position; index -= 2) {
            long d = value / 100L;
            short v = ITOA_LUT_TABLE[(int) (value - 100L * d)];
            value = d;
            SegmentAccess.setShort(segment, index, v);
        }
        if((digitCount & 1) != 0) {
            SegmentAccess.setByte(segment, position, (byte) (BYTE_ZERO + value));
        }
        return position + digitCount;
    }

    public static void writeFloat(float f, WriteBuffer writeBuffer) {
        writeBuffer.ensureCapacity(MAX_FLOAT_CAPACITY);
        final int bits = Float.floatToRawIntBits(f);
        final boolean negative = bits < 0;
        switch (writeBuffer) {
            case HeapWriteBuffer heapWriteBuffer -> {
                final byte[] bytes = heapWriteBuffer.rawByteArray();
                int position = heapWriteBuffer.intPosition();
                if(negative) {
                    bytes[position++] = BYTE_MINUS;
                }
                if((bits & 0x7FFFFFFF) == 0) {
                    bytes[position++] = BYTE_ZERO;
                    heapWriteBuffer.setPosition(position);
                    return ;
                }
                BinaryFp binaryFp = buildBinaryFp(bits, FLOAT_SPEC);
                DecimalFp decimalFp = toDecimalFp(binaryFp, FLOAT_SPEC);
                heapWriteBuffer.setPosition(writeDecimalFpToHeap(decimalFp, bytes, position));
            }
            case SegmentWriteBuffer segmentWriteBuffer -> {
                final MemorySegment segment = segmentWriteBuffer.rawSegment();
                int position = segmentWriteBuffer.intPosition();
                if(negative) {
                    SegmentAccess.setByte(segment, position++, BYTE_MINUS);
                }
                if((bits & 0x7FFFFFFF) == 0) {
                    SegmentAccess.setByte(segment, position++, BYTE_ZERO);
                    segmentWriteBuffer.setPosition(position);
                    return ;
                }
                BinaryFp binaryFp = buildBinaryFp(bits, FLOAT_SPEC);
                DecimalFp decimalFp = toDecimalFp(binaryFp, FLOAT_SPEC);
                segmentWriteBuffer.setPosition(writeDecimalFpToSegment(decimalFp, segment, position));
            }
        }
    }

    public static void writeDouble(double f, WriteBuffer writeBuffer) {
        writeBuffer.ensureCapacity(MAX_DOUBLE_CAPACITY);
        final long bits = Double.doubleToRawLongBits(f);
        final boolean negative = bits < 0L;
        switch (writeBuffer) {
            case HeapWriteBuffer heapWriteBuffer -> {
                final byte[] bytes = heapWriteBuffer.rawByteArray();
                int position = heapWriteBuffer.intPosition();
                if(negative) {
                    bytes[position++] = BYTE_MINUS;
                }
                if((bits & 0x7FFFFFFFFFFFFFFFL) == 0L) {
                    bytes[position++] = BYTE_ZERO;
                    heapWriteBuffer.setPosition(position);
                    return ;
                }
                BinaryFp binaryFp = buildBinaryFp(bits, DOUBLE_SPEC);
                DecimalFp decimalFp = toDecimalFp(binaryFp, DOUBLE_SPEC);
                heapWriteBuffer.setPosition(writeDecimalFpToHeap(decimalFp, bytes, position));
            }
            case SegmentWriteBuffer segmentWriteBuffer -> {
                final MemorySegment segment = segmentWriteBuffer.rawSegment();
                int position = segmentWriteBuffer.intPosition();
                if(negative) {
                    SegmentAccess.setByte(segment, position++, BYTE_MINUS);
                }
                if((bits & 0x7FFFFFFFFFFFFFFFL) == 0L) {
                    SegmentAccess.setByte(segment, position++, BYTE_ZERO);
                    segmentWriteBuffer.setPosition(position);
                    return ;
                }
                BinaryFp binaryFp = buildBinaryFp(bits, DOUBLE_SPEC);
                DecimalFp decimalFp = toDecimalFp(binaryFp, DOUBLE_SPEC);
                segmentWriteBuffer.setPosition(writeDecimalFpToSegment(decimalFp, segment, position));
            }
        }
    }

    private static int writeDecimalFpToHeap(DecimalFp decimalFp, byte[] bytes, int position) {
        long d = decimalFp.d();
        int p = decimalFp.p();
        int digitCount = digitCount(d);
        int sciE = p + digitCount - 1;
        if (sciE >= MIN_SCI_EXP && sciE < MAX_SCI_EXP) {
            return writeFixedDecimalFpToHeap(d, p, digitCount, bytes, position);
        }
        return writeSciDecimalFpToHeap(d, sciE, digitCount, bytes, position);
    }

    private static int writeDecimalFpToSegment(DecimalFp decimalFp, MemorySegment segment, int position) {
        long d = decimalFp.d();
        int p = decimalFp.p();
        int digitCount = digitCount(d);
        int sciE = p + digitCount - 1;
        if (sciE >= MIN_SCI_EXP && sciE < MAX_SCI_EXP) {
            return writeFixedDecimalFpToSegment(d, p, digitCount, segment, position);
        }
        return writeSciDecimalFpToSegment(d, sciE, digitCount, segment, position);
    }

    private static int writeFixedDecimalFpToHeap(long d, int p, int digitCount, byte[] bytes, int position) {
        int sum = digitCount + p;
        int shift = sum <= 0 ? 2 - sum : (p < 0 ? 1 : 0);
        int r = writePositiveLongToHeap(d, digitCount, bytes, position + shift);
        if(shift == 0) {
            int end = r + p;
            Arrays.fill(bytes, r, end, BYTE_ZERO);
            return end;
        }
        if(shift == 1) {
            System.arraycopy(bytes, position + 1, bytes, position, sum);
            bytes[position + sum] = BYTE_PERIOD;
            return r;
        }
        Arrays.fill(bytes, position, position + shift, BYTE_ZERO);
        bytes[position + 1] = BYTE_PERIOD;
        return r;
    }

    private static int writeFixedDecimalFpToSegment(long d, int p, int digitCount, MemorySegment segment, int position) {
        int sum = digitCount + p;
        int shift = sum <= 0 ? 2 - sum : (p < 0 ? 1 : 0);
        int r = writePositiveLongToSegment(d, digitCount, segment, position + shift);
        if(shift == 0) {
            segment.asSlice(r, p).fill(BYTE_ZERO);
            return r + p;
        }
        if(shift == 1) {
            MemorySegment.copy(segment, position + 1, segment, position, sum);
            SegmentAccess.setByte(segment, position + sum, BYTE_PERIOD);
            return r;
        }
        segment.asSlice(position, shift).fill(BYTE_ZERO);
        SegmentAccess.setByte(segment, position + 1, BYTE_PERIOD);
        return r;
    }

    private static int writeSciDecimalFpToHeap(long d, int sciE, int digitCount, byte[] bytes, int position) {
        if(digitCount == 1) {
            bytes[position++] = (byte) (BYTE_ZERO + d);
        } else {
            int r = writePositiveLongToHeap(d, digitCount, bytes, position + 1);
            bytes[position] = bytes[position + 1];
            bytes[position + 1] = BYTE_PERIOD;
            position = r;
        }
        bytes[position++] = BYTE_E;
        if (sciE < 0) {
            bytes[position++] = BYTE_MINUS;
            sciE = -sciE;
        }
        if(sciE < 10) {
            bytes[position] = (byte) (BYTE_ZERO + sciE);
            return position + 1;
        }
        if(sciE < 100) {
            ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[sciE]);
            return position + 2;
        }
        ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[sciE / 10]);
        bytes[position + 2] = (byte) (BYTE_ZERO + (sciE % 10));
        return position + 3;
    }

    private static int writeSciDecimalFpToSegment(long d, int sciE, int digitCount, MemorySegment segment, int position) {
        if(digitCount == 1) {
            SegmentAccess.setByte(segment, position++, (byte) (BYTE_ZERO + d));
        } else {
            int r = writePositiveLongToSegment(d, digitCount, segment, position + 1);
            SegmentAccess.setByte(segment, position, SegmentAccess.getByte(segment, position + 1));
            SegmentAccess.setByte(segment, position + 1, BYTE_PERIOD);
            position = r;
        }
        SegmentAccess.setByte(segment, position++, BYTE_E);
        if (sciE < 0) {
            SegmentAccess.setByte(segment, position++, BYTE_MINUS);
            sciE = -sciE;
        }
        if(sciE < 10) {
            SegmentAccess.setByte(segment, position, (byte) (BYTE_ZERO + sciE));
            return position + 1;
        }
        if(sciE < 100) {
            SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[sciE]);
            return position + 2;
        }
        SegmentAccess.setShort(segment, position, ITOA_LUT_TABLE[sciE / 10]);
        SegmentAccess.setByte(segment, position + 2, (byte) (BYTE_ZERO + (sciE % 10)));
        return position + 3;
    }

    // currently only float32 and float64 are supported
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
        return (e * 631305 - 261663) >> 21;
    }

    private static int log10Pow2(int x) {
        // log₁₀ 2 ≈ 0.30102999566 ≈ 78913 / 2^18
        return (x * 78913) >> 18;
    }

    private static int log2Pow10(int x) {
        // log₂ 10 ≈ 3.32192809489 ≈ 108853 / 2^15
        return (x * 108853) >> 15;
    }

    // no overflow
    private static Scalers prescale(int e, int p, int lp) {
        int s = -(e + lp + 3);
        int idx = (p - POW10MIN) << 1;
        long pmHi = POW10TAB[idx];
        long pmLo = POW10TAB[idx + 1];
        return new Scalers(pmHi, pmLo, s);
    }

    // no overflow, current VM implementation still emits three multiplication instructions; with the advent of int128, this can be shortened to two multiplication instructions
    private static long uscale(long x, Scalers c) {
        final long pmHi = c.pmHi();
        final long mid1 = x * pmHi;
        long hi = Math.unsignedMultiplyHigh(x, pmHi);
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

    private static DecimalFp trimZeros(DecimalFp decimalFp) {
        long d = decimalFp.d();
        int p = decimalFp.p();
        long div = Math.unsignedMultiplyHigh(d, 0xCCCCCCCCCCCCCCCDL) >>> 3;
        if(d - div * 10L != 0L) {
            return decimalFp;
        }
        do {
            d = div;
            div = d / 10;
            p += 1;
        } while (d - div * 10L == 0L);
        return new DecimalFp(d, p);
    }

    // currently only float32 and float64 are supported
    private static DecimalFp toDecimalFp(BinaryFp binaryFp, FpSpec fpSpec) {
        final long m = binaryFp.m();
        final int e = binaryFp.e();
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
        final long max = m + (1L << (z - 1));
        final int odd = (int) (m >>> z) & 1;
        Scalers pre = prescale(e, p, log2Pow10(p));
        final long dmin = uceil(unudge(uscale(min, pre), odd));
        final long dmax = ufloor(unudge(uscale(max, pre), -odd));
        final long d = Math.unsignedMultiplyHigh(dmax, 0xCCCCCCCCCCCCCCCDL) >>> 3;
        if (Long.compareUnsigned(d * 10L, dmin) >= 0) {
            return trimZeros(new DecimalFp(d, -(p - 1)));
        }
        return new DecimalFp(Long.compareUnsigned(dmin, dmax) < 0 ? uround(uscale(m, pre)) : dmin, -p);
    }

    public static int readInt(ReadBuffer readBuffer, byte firstByte) {
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> readHeapInt(heapReadBuffer, firstByte);
            case SegmentReadBuffer segmentReadBuffer -> readSegmentInt(segmentReadBuffer, firstByte);
        };
    }

    private static boolean negativeIntOverflow(int r, byte b) {
        return r < N_DIV_10_I || (r == N_DIV_10_I && b < N_MOD_10_I);
    }

    private static int readHeapInt(HeapReadBuffer heapReadBuffer, byte firstByte) {
        final byte[] bytes = heapReadBuffer.rawByteArray();
        int position = heapReadBuffer.intPosition();
        boolean positive = true;
        int r;
        if (firstByte == BYTE_MINUS) {
            if (position == bytes.length) {
                throw new JsonDeserializerException("illegal leading minus sign");
            }
            positive = false;
            firstByte = bytes[position++];
        }
        if (firstByte == BYTE_ZERO) {
            if (position < bytes.length && ZERO_NINE_TABLE[Byte.toUnsignedInt(bytes[position])] <= 0) {
                throw new JsonDeserializerException("leading zero");
            }
            return 0;
        } else {
            r = ZERO_NINE_TABLE[Byte.toUnsignedInt(firstByte)];
        }
        while (position < bytes.length) {
            byte b = ZERO_NINE_TABLE[Byte.toUnsignedInt(bytes[position])];
            if (b > 0) {
                break;
            }
            if (negativeIntOverflow(r, b)) {
                throw new JsonDeserializerException("integer overflow");
            }
            r = r * 10 + b;
            position++;
        }
        if (positive && r == Integer.MIN_VALUE) {
            throw new JsonDeserializerException("integer overflow");
        }
        heapReadBuffer.setPosition(position);
        return positive ? -r : r;
    }

    private static int readSegmentInt(SegmentReadBuffer segmentReadBuffer, byte firstByte) {
        final MemorySegment segment = segmentReadBuffer.rawSegment();
        int position = segmentReadBuffer.intPosition();
        int len = Math.toIntExact(segment.byteSize());
        boolean positive = true;
        int r;
        if (firstByte == BYTE_MINUS) {
            if (position == segment.byteSize()) {
                throw new JsonDeserializerException("illegal leading minus sign");
            }
            positive = false;
            firstByte = SegmentAccess.getByte(segment, position++);
        }
        if (firstByte == BYTE_ZERO) {
            if (position < len && ZERO_NINE_TABLE[Byte.toUnsignedInt(SegmentAccess.getByte(segment, position))] <= 0) {
                throw new JsonDeserializerException("leading zero");
            }
            return 0;
        } else {
            r = ZERO_NINE_TABLE[Byte.toUnsignedInt(firstByte)];
        }
        while (position < len) {
            byte b = ZERO_NINE_TABLE[Byte.toUnsignedInt(SegmentAccess.getByte(segment, position))];
            if (b > 0) {
                break;
            }
            if (negativeIntOverflow(r, b)) {
                throw new JsonDeserializerException("integer overflow");
            }
            r = r * 10 + b;
            position++;
        }
        if (positive && r == Integer.MIN_VALUE) {
            throw new JsonDeserializerException("integer overflow");
        }
        segmentReadBuffer.setPosition(position);
        return positive ? -r : r;
    }

    public static long readLong(ReadBuffer readBuffer, byte firstByte) {
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> readHeapLong(heapReadBuffer, firstByte);
            case SegmentReadBuffer segmentReadBuffer -> readSegmentLong(segmentReadBuffer, firstByte);
        };
    }

    private static boolean negativeLongOverflow(long r, byte b) {
        return r < N_DIV_10_L || (r == N_DIV_10_L && b < N_MOD_10_L);
    }

    private static long readHeapLong(HeapReadBuffer heapReadBuffer, byte firstByte) {
        final byte[] bytes = heapReadBuffer.rawByteArray();
        int position = heapReadBuffer.intPosition();
        boolean positive = true;
        long r;
        if (firstByte == BYTE_MINUS) {
            if (position == bytes.length) {
                throw new JsonDeserializerException("illegal leading minus sign");
            }
            positive = false;
            firstByte = bytes[position++];
        }
        if (firstByte == BYTE_ZERO) {
            if (position < bytes.length && ZERO_NINE_TABLE[Byte.toUnsignedInt(bytes[position])] <= 0) {
                throw new JsonDeserializerException("leading zero");
            }
            return 0L;
        } else {
            r = ZERO_NINE_TABLE[Byte.toUnsignedInt(firstByte)];
        }
        while (position < bytes.length) {
            byte b = ZERO_NINE_TABLE[Byte.toUnsignedInt(bytes[position])];
            if (b > 0) {
                break;
            }
            if (negativeLongOverflow(r, b)) {
                throw new JsonDeserializerException("long overflow");
            }
            r = r * 10L + b;
            position++;
        }
        if (positive && r == Long.MIN_VALUE) {
            throw new JsonDeserializerException("long overflow");
        }
        heapReadBuffer.setPosition(position);
        return positive ? -r : r;
    }

    private static long readSegmentLong(SegmentReadBuffer segmentReadBuffer, byte firstByte) {
        final MemorySegment segment = segmentReadBuffer.rawSegment();
        int position = segmentReadBuffer.intPosition();
        final int len = Math.toIntExact(segment.byteSize());
        boolean positive = true;
        long r;
        if (firstByte == BYTE_MINUS) {
            if (position == segment.byteSize()) {
                throw new JsonDeserializerException("illegal leading minus sign");
            }
            positive = false;
            firstByte = SegmentAccess.getByte(segment, position++);
        }
        if (firstByte == BYTE_ZERO) {
            if (position < len && ZERO_NINE_TABLE[Byte.toUnsignedInt(SegmentAccess.getByte(segment, position))] <= 0) {
                throw new JsonDeserializerException("leading zero");
            }
            return 0L;
        } else {
            r = ZERO_NINE_TABLE[Byte.toUnsignedInt(firstByte)];
        }
        while (position < len) {
            byte b = ZERO_NINE_TABLE[Byte.toUnsignedInt(SegmentAccess.getByte(segment, position))];
            if (b > 0) {
                break;
            }
            if (negativeLongOverflow(r, b)) {
                throw new JsonDeserializerException("long overflow");
            }
            r = r * 10L + b;
            position++;
        }
        if (positive && r == Long.MIN_VALUE) {
            throw new JsonDeserializerException("long overflow");
        }
        segmentReadBuffer.setPosition(position);
        return positive ? -r : r;
    }

    // read a string-format floating-point number into a specific format.
    // enforces strict format validation; patterns like ".123E0123" are rejected,
    // although they are acceptable by the JDK parser.
    public static FpStrRep readFpStrRep(ReadBuffer readBuffer, int maxNumberBytes, byte firstByte) {
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> readFpStrRepFromHeap(heapReadBuffer, maxNumberBytes, firstByte);
            case SegmentReadBuffer segmentReadBuffer -> readFpStrRepFromSegment(segmentReadBuffer, maxNumberBytes, firstByte);
        };
    }

    public static FpStrRep readFpStrRepFromHeap(HeapReadBuffer heapReadBuffer, int maxNumberBytes, byte firstByte) {
        final byte[] bytes = heapReadBuffer.rawByteArray();
        final int position = heapReadBuffer.intPosition();
        final int end = position + Math.min(bytes.length - position, maxNumberBytes - 1); // excluding first byte, no overflow
        int index = position;
        boolean neg = false;
        boolean negExp = false;
        boolean trunc = false;
        long d;
        int frac = 0;
        int p = 0;
        if (firstByte == BYTE_MINUS) {
            if (index == end) {
                throw new JsonDeserializerException("illegal leading minus sign");
            }
            neg = true;
            firstByte = bytes[index++];
        }
        if (firstByte == BYTE_ZERO) {
            if (index == end) {
                return new FpStrRep(neg, false, 0L, p, 0);
            }
            byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(bytes[index])];
            if (v <= 0) {
                throw new JsonDeserializerException("leading zero");
            }
            d = 0L;
        } else {
            d = -ZERO_NINE_TABLE[Byte.toUnsignedInt(firstByte)];
        }
        // process following digits part, we can ensure that d will not be 0 here
        byte b = Byte.MIN_VALUE;
        int nd = 1;
        while (index < end) {
            b = bytes[index];
            byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(b)];
            if (v > 0) {
                break;
            }
            if (nd < MAX_DECIMAL_ND) {
                d = d * 10L - v;
                nd++;
            } else {
                trunc = true;
            }
            index++;
        }
        // processing optional fraction part
        if (b == BYTE_PERIOD) {
            if (++index == end) {
                throw new JsonDeserializerException("leading period with no digits");
            }
            b = bytes[index++];
            byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(b)];
            if (v > 0) {
                throw new JsonDeserializerException("illegal start of number : " + b);
            }
            if (nd < MAX_DECIMAL_ND) {
                d = d * 10L - v;
                nd++;
                frac++;
            }
            while (index < end) {
                b = bytes[index];
                v = ZERO_NINE_TABLE[Byte.toUnsignedInt(b)];
                if (v > 0) {
                    break;
                }
                if (nd < MAX_DECIMAL_ND) {
                    d = d * 10L - v;
                    nd++;
                    frac++;
                } else {
                    trunc = true;
                }
                index++;
            }
        }
        // processing optional exponent part
        // note that leading zeros are allowed in the exponent partaccording to the JSON specification
        if (b == BYTE_E || b == BYTE_e) {
            if (++index == end) {
                throw new JsonDeserializerException("leading exponent with no digits");
            }
            b = bytes[index++];
            if (b == BYTE_MINUS || b == BYTE_PLUS) {
                if (index == end) {
                    throw new JsonDeserializerException("leading exponent sign with no digits");
                }
                negExp = b == BYTE_MINUS;
                b = bytes[index++];
            }
            byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(b)];
            if (v > 0) {
                throw new JsonDeserializerException("illegal start of number : " + b);
            }
            p = -v;
            while (index < end) {
                b = bytes[index];
                v = ZERO_NINE_TABLE[Byte.toUnsignedInt(b)];
                if (v > 0) {
                    break;
                }
                if (p < MAX_DECIMAL_P) {
                    p = p * 10 - v;
                }
                index++;
            }
        }
        p = (negExp ? -p : p) - frac;
        heapReadBuffer.setPosition(index);
        return new FpStrRep(neg, trunc, d, p, index - position + 1);
    }

    public static FpStrRep readFpStrRepFromSegment(SegmentReadBuffer segmentReadBuffer, int maxNumberBytes, byte firstByte) {
        final MemorySegment segment = segmentReadBuffer.rawSegment();
        final long position = segmentReadBuffer.longPosition();
        final long end = Math.addExact(position, Math.min(segment.byteSize() - position, maxNumberBytes - 1)); // excluding first byte, no overflow
        long index = position;
        boolean neg = false;
        boolean negExp = false;
        boolean trunc = false;
        long d;
        int frac = 0;
        int p = 0;
        if (firstByte == BYTE_MINUS) {
            if (index == end) {
                throw new JsonDeserializerException("illegal leading minus sign");
            }
            neg = true;
            firstByte = SegmentAccess.getByte(segment, index++);
        }
        if (firstByte == BYTE_ZERO) {
            if (index == end) {
                return new FpStrRep(neg, false, 0L, p, 0);
            }
            byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(SegmentAccess.getByte(segment, index))];
            if (v <= 0) {
                throw new JsonDeserializerException("leading zero");
            }
            d = 0L;
        } else {
            d = -ZERO_NINE_TABLE[Byte.toUnsignedInt(firstByte)];
        }
        // process following digits part, we can ensure that d will not be 0 here
        byte b = Byte.MIN_VALUE;
        int nd = 1;
        while (index < end) {
            b = SegmentAccess.getByte(segment, index);
            byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(b)];
            if (v > 0) {
                break;
            }
            if (nd < MAX_DECIMAL_ND) {
                d = d * 10L - v;
                nd++;
            } else {
                trunc = true;
            }
            index++;
        }
        // processing optional fraction part
        if (b == BYTE_PERIOD) {
            if (++index == end) {
                throw new JsonDeserializerException("leading period with no digits");
            }
            b = SegmentAccess.getByte(segment, index++);
            byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(b)];
            if (v > 0) {
                throw new JsonDeserializerException("illegal start of number : " + b);
            }
            if (nd < MAX_DECIMAL_ND) {
                d = d * 10L - v;
                nd++;
                frac++;
            }
            while (index < end) {
                b = SegmentAccess.getByte(segment, index);
                v = ZERO_NINE_TABLE[Byte.toUnsignedInt(b)];
                if (v > 0) {
                    break;
                }
                if (nd < MAX_DECIMAL_ND) {
                    d = d * 10L - v;
                    nd++;
                    frac++;
                } else {
                    trunc = true;
                }
                index++;
            }
        }
        // processing optional exponent part
        // note that leading zeros are allowed in the exponent partaccording to the JSON specification
        if (b == BYTE_E || b == BYTE_e) {
            if (++index == end) {
                throw new JsonDeserializerException("leading exponent with no digits");
            }
            b = SegmentAccess.getByte(segment, index++);
            if (b == BYTE_MINUS || b == BYTE_PLUS) {
                if (index == end) {
                    throw new JsonDeserializerException("leading exponent sign with no digits");
                }
                negExp = b == BYTE_MINUS;
                b = SegmentAccess.getByte(segment, index++);
            }
            byte v = ZERO_NINE_TABLE[Byte.toUnsignedInt(b)];
            if (v > 0) {
                throw new JsonDeserializerException("illegal start of number : " + b);
            }
            p = -v;
            while (index < end) {
                b = SegmentAccess.getByte(segment, index);
                v = ZERO_NINE_TABLE[Byte.toUnsignedInt(b)];
                if (v > 0) {
                    break;
                }
                if (p < MAX_DECIMAL_P) {
                    p = p * 10 - v;
                }
                index++;
            }
        }
        p = (negExp ? -p : p) - frac;
        segmentReadBuffer.setPosition(index);
        return new FpStrRep(neg, trunc, d, p, Math.toIntExact(index - position + 1L));
    }

    public static float readFloat(ReadBuffer readBuffer, int maxNumberBytes, byte firstByte) {
        FpStrRep rep = readFpStrRep(readBuffer, maxNumberBytes, firstByte);
        return parseFloat(readBuffer, rep);
    }

    public static float parseFloat(ReadBuffer readBuffer, FpStrRep rep) {
        if (rep.trunc()) {
            return parseFloatFallback(readBuffer, rep);
        }
        final int sign = rep.negative() ? (1 << (FLOAT_SPEC.mantBits() + FLOAT_SPEC.expBits())) : 0;
        final long d = rep.d();
        final int p = rep.p();
        if (d == 0L || p < FLOAT_SPEC.minDecExp() - MAX_DECIMAL_ND - 2) {
            return Float.intBitsToFloat(sign);
        }
        if (p > FLOAT_SPEC.maxDecExp() + 2) {
            return Float.intBitsToFloat(sign | (0xff << FLOAT_SPEC.mantBits()));
        }
        if (d >> FLOAT_SPEC.mantBits() == 0L) {
            float f = (float) Math.toIntExact(rep.negative() ? -d : d);
            if (p == 0) {
                return f;
            } else if (p > 0 && p <= FLOAT_EXACT_I + FLOAT_EXACT_P) {
                int tp = p;
                if (tp > FLOAT_EXACT_P) {
                    f *= FLOAT_POW_10[tp - FLOAT_EXACT_P];
                    tp = FLOAT_EXACT_P;
                }
                if (f >= FLOAT_EXACT_I_LOW && f <= FLOAT_EXACT_I_HIGH) {
                    return f * FLOAT_POW_10[tp];
                }
            } else if (p < 0 && p >= -FLOAT_EXACT_P) {
                return f / FLOAT_POW_10[-p];
            }
        }
        final int lp = log2Pow10(p);
        final int shift = Long.numberOfLeadingZeros(d);
        final int b = Long.SIZE - shift;
        int fe = Math.min(FLOAT_SPEC.mantBits() - FLOAT_SPEC.bias() - 1, FLOAT_SPEC.mantBits() + 1 - b - lp);
        Scalers scalers = prescale(fe - shift, p, lp);
        if (scalers.s() >= Long.SIZE) {
            return Float.intBitsToFloat(sign);
        }
        long u = uscale(d << shift, scalers);
        if (u >= umin(1L << (FLOAT_SPEC.mantBits() + 1))) {
            u = (u >>> 1) | (u & 1);
            fe--;
        }
        final int m = sign | Math.toIntExact(uround(u));
        if ((m & (1 << FLOAT_SPEC.mantBits())) == 0) {
            return Float.intBitsToFloat(m);
        }
        final int e = -fe;
        if (e >= (1 << FLOAT_SPEC.expBits()) - 1 - FLOAT_SPEC.mantBits() + FLOAT_SPEC.bias()) {
            return parseFloatFallback(readBuffer, rep);
        }
        return Float.intBitsToFloat(
                (m & ~(1 << FLOAT_SPEC.mantBits())) |
                        (FLOAT_SPEC.mantBits() - FLOAT_SPEC.bias() + e) << FLOAT_SPEC.mantBits());
    }

    private static float parseFloatFallback(ReadBuffer readBuffer, FpStrRep FpStrRep) {
        final int len = FpStrRep.len();
        switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> {
                byte[] bytes = heapReadBuffer.rawByteArray();
                int position = heapReadBuffer.intPosition();
                return Float.parseFloat(new String(bytes, position - len, len, StandardCharsets.US_ASCII));
            }
            case SegmentReadBuffer segmentReadBuffer -> {
                MemorySegment segment = segmentReadBuffer.rawSegment();
                long position = segmentReadBuffer.longPosition();
                byte[] bytes = new byte[len];
                MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, position - len, bytes, 0, len);
                return Float.parseFloat(new String(bytes, StandardCharsets.US_ASCII));
            }
        }
    }

    public static double readDouble(ReadBuffer readBuffer, int numberMaxBytes, byte firstByte) {
        FpStrRep rep = readFpStrRep(readBuffer, numberMaxBytes, firstByte);
        return parseDouble(readBuffer, rep);
    }

    public static double parseDouble(ReadBuffer readBuffer, FpStrRep rep) {
        if (rep.trunc()) {
            return parseDoubleFallback(readBuffer, rep);
        }
        final long sign = rep.negative() ? (1L << (DOUBLE_SPEC.mantBits() + DOUBLE_SPEC.expBits())) : 0L;
        final long d = rep.d();
        final int p = rep.p();
        if (d == 0L || p < DOUBLE_SPEC.minDecExp() - MAX_DECIMAL_ND - 2) {
            return Double.longBitsToDouble(sign);
        }
        if (p > DOUBLE_SPEC.maxDecExp() + 2) {
            return Double.longBitsToDouble(sign | (0x7ffL << DOUBLE_SPEC.mantBits()));
        }
        if (d >>> DOUBLE_SPEC.mantBits() == 0L) {
            double f = (double) (rep.negative() ? -d : d);
            if (p == 0) {
                return f;
            } else if (p > 0 && p <= DOUBLE_EXACT_I + DOUBLE_EXACT_P) {
                int tp = p;
                if (tp > DOUBLE_EXACT_P) {
                    f *= DOUBLE_POW_10[tp - DOUBLE_EXACT_P];
                    tp = DOUBLE_EXACT_P;
                }
                if (f >= DOUBLE_EXACT_I_LOW && f <= DOUBLE_EXACT_I_HIGH) {
                    return f * DOUBLE_POW_10[tp];
                }
            } else if (p < 0 && p >= -DOUBLE_EXACT_P) {
                return f / DOUBLE_POW_10[-p];
            }
        }
        final int lp = log2Pow10(p);
        final int shift = Long.numberOfLeadingZeros(d);
        final int b = Long.SIZE - shift;
        int fe = Math.min(DOUBLE_SPEC.mantBits() - DOUBLE_SPEC.bias() - 1, DOUBLE_SPEC.mantBits() + 1 - b - lp);
        Scalers scalers = prescale(fe - shift, p, lp);
        if (scalers.s() >= Long.SIZE) {
            return Double.longBitsToDouble(sign);
        }
        long u = uscale(d << shift, scalers);
        if (u >= umin(1L << (DOUBLE_SPEC.mantBits() + 1))) {
            u = (u >>> 1) | (u & 1);
            fe--;
        }
        final long m = sign | uround(u);
        if ((m & (1L << DOUBLE_SPEC.mantBits())) == 0L) {
            return Double.longBitsToDouble(m);
        }
        final int e = -fe;
        if (e >= (1 << DOUBLE_SPEC.expBits()) - 1 - DOUBLE_SPEC.mantBits() + DOUBLE_SPEC.bias()) {
            return parseDoubleFallback(readBuffer, rep);
        }
        return Double.longBitsToDouble(
                (m & ~(1L << DOUBLE_SPEC.mantBits())) |
                        ((long) (DOUBLE_SPEC.mantBits() - DOUBLE_SPEC.bias() + e)) << DOUBLE_SPEC.mantBits());
    }

    private static double parseDoubleFallback(ReadBuffer readBuffer, FpStrRep FpStrRep) {
        final int len = FpStrRep.len();
        switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> {
                byte[] bytes = heapReadBuffer.rawByteArray();
                int position = heapReadBuffer.intPosition();
                return Double.parseDouble(new String(bytes, position - len, len, StandardCharsets.US_ASCII));
            }
            case SegmentReadBuffer segmentReadBuffer -> {
                MemorySegment segment = segmentReadBuffer.rawSegment();
                long position = segmentReadBuffer.longPosition();
                byte[] bytes = new byte[len];
                MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, position - len, bytes, 0, len);
                return Double.parseDouble(new String(bytes, StandardCharsets.US_ASCII));
            }
        }
    }

    /**
     * Represents a binary floating-point number as m * 2^e.
     */
    public record BinaryFp(long m, int e) {
    }

    /**
     * Represents a decimal floating-point number as d * 10^p.
     */
    public record DecimalFp(long d, int p) {
    }

    /**
     * Defines the specification parameters for a floating-point format (e.g., mantissa bits, exponent bits, bias, etc.).
     * Predefined constants are available for 32-bit and 64-bit floats.
     * Note: This implementation is strictly limited to processing 64-bit float values.
     * Do not apply this to larger formats like FP128, as the current algorithms cannot handle the extended range.
     */
    public record FpSpec(
            int mantBits,
            int expBits,
            int bias,
            int minExp,
            int maxDecExp,
            int minDecExp
    ) {
    }

    /**
     * Holds precomputed scaling constants (a 128-bit multiplier pm and a shift count s) for a given BinaryFp value.
     */
    public record Scalers(
            long pmHi,
            long pmLo,
            int s
    ) {
    }
}
