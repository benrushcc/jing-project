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

    public static final byte[] MIN_INT_BYTES = String.valueOf(Integer.MIN_VALUE).getBytes(StandardCharsets.US_ASCII);
    public static final byte[] MIN_LONG_BYTES = String.valueOf(Long.MIN_VALUE).getBytes(StandardCharsets.US_ASCII);
    private static final int[] LEN_TABLE = makeLenTable();
    private static final long[] POW_TABLE = makePowTable();
    private static final short[] ITOA_LUT_TABLE = makeItoaLutTable();

    private static final FpSpec FLOAT_SPEC = new FpSpec(23, 8, -127, -189, 38, -45);
    private static final FpSpec DOUBLE_SPEC = new FpSpec(52, 11, -1023, -1085, 308, -324);
    public static final int MAX_FLOAT_CAPACITY = 15; // same as MAX_CHARS in jdk/internal/math/FloatToDecimal.java
    public static final int MAX_DOUBLE_CAPACITY = 24; // same as MAX_CHARS in jdk/internal/math/DoubleToDecimal.java
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

    // maximum decimal digits that fit in a uint64 (19)
    public static final int MAX_DECIMAL_ND = 19;

    // maximum exponent digits. For fp32/fp64, exponents beyond 4096 in absolute
    // value never affect the final result. Truncation is necessary to avoid overflow
    // in log2Pow10, skewed, and similar functions.
    public static final int MAX_DECIMAL_P = 4096;

    // acts as a safe guard: for any value < MAX_DECIMAL_P_GUARD,
    // (value * 10 + digit) + MAX_DECIMAL_ND (where digit is 0..9)
    // will never exceed MAX_DECIMAL_P, so we avoid overflow in exponent math.
    private static final int MAX_DECIMAL_P_GUARD = 404;

    // if d can be exactly represented within the mantissa range, the exponent
    // calculation for the EXACT case is accurate; try computing the final result directly.
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
            case HeapWriteBuffer heapWriteBuffer ->
                heapWriteBuffer.setPosition(writeIntToHeap(value, heapWriteBuffer.rawByteArray(), heapWriteBuffer.intPosition()));
            case SegmentWriteBuffer segmentWriteBuffer ->
                segmentWriteBuffer.setPosition(writeIntToSegment(value, segmentWriteBuffer.rawSegment(), segmentWriteBuffer.longPosition()));
        }
    }

    public static int writeIntToHeap(int value, byte[] bytes, int position) {
        if(value == 0) {
            bytes[position] = BYTE_ZERO;
            return position + 1;
        }
        if(value == Integer.MIN_VALUE) {
            System.arraycopy(MIN_INT_BYTES, 0, bytes, position, MIN_INT_BYTES.length);
            return position + MIN_INT_BYTES.length;
        }
        if(value < 0){
            bytes[position++] = BYTE_MINUS;
            value = -value;
        }
        return writePositiveIntToHeap(value, digitCount(value), bytes, position);
    }

    public static int writePositiveIntToHeap(int value, int digitCount, byte[] bytes, int position) {
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

    public static long writeIntToSegment(int value, MemorySegment segment, long position) {
        if(value == 0L) {
            SegmentAccess.setByte(segment, position, BYTE_ZERO);
            return position + 1L;
        }
        if(value == Integer.MIN_VALUE) {
            MemorySegment.copy(MIN_INT_BYTES, 0, segment, ValueLayout.JAVA_BYTE, position, MIN_INT_BYTES.length);
            return position + MIN_INT_BYTES.length;
        }
        if(value < 0L){
            SegmentAccess.setByte(segment, position++, BYTE_MINUS);
            value = -value;
        }
        return writePositiveIntToSegment(value, digitCount(value), segment, position);
    }

    public static long writePositiveIntToSegment(int value, int digitCount, MemorySegment segment, long position) {
        for(long index = position + digitCount - 2; index >= position; index -= 2L) {
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
            case HeapWriteBuffer heapWriteBuffer ->
                heapWriteBuffer.setPosition(writeLongToHeap(value, heapWriteBuffer.rawByteArray(), heapWriteBuffer.intPosition()));
            case SegmentWriteBuffer segmentWriteBuffer ->
                segmentWriteBuffer.setPosition(writeLongToSegment(value, segmentWriteBuffer.rawSegment(), segmentWriteBuffer.longPosition()));
        }
    }

    public static int writeLongToHeap(long value, byte[] bytes, int position) {
        if(value == 0L) {
            bytes[position] = BYTE_ZERO;
            return position + 1;
        }
        if(value == Long.MIN_VALUE) {
            System.arraycopy(MIN_LONG_BYTES, 0, bytes, position, MIN_LONG_BYTES.length);
            return position + MIN_LONG_BYTES.length;
        }
        if(value < 0L){
            bytes[position++] = BYTE_MINUS;
            value = -value;
        }
        return writePositiveLongToHeap(value, digitCount(value), bytes, position);
    }

    public static int writePositiveLongToHeap(long value, int digitCount, byte[] bytes, int position) {
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

    public static long writeLongToSegment(long value, MemorySegment segment, long position) {
        if(value == 0L) {
            SegmentAccess.setByte(segment, position, BYTE_ZERO);
            return position + 1L;
        }
        if(value == Long.MIN_VALUE) {
            MemorySegment.copy(MIN_LONG_BYTES, 0, segment, ValueLayout.JAVA_BYTE, position, MIN_LONG_BYTES.length);
            return position + MIN_LONG_BYTES.length;
        }
        if(value < 0L){
            SegmentAccess.setByte(segment, position++, BYTE_MINUS);
            value = -value;
        }
        return writePositiveLongToSegment(value, digitCount(value), segment, position);
    }

    public static long writePositiveLongToSegment(long value, int digitCount, MemorySegment segment, long position) {
        for(long index = position + digitCount - 2; index >= position; index -= 2L) {
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

    public static void writeFloat(float value, WriteBuffer writeBuffer) {
        writeBuffer.ensureCapacity(MAX_FLOAT_CAPACITY);
        switch (writeBuffer) {
            case HeapWriteBuffer heapWriteBuffer ->
                    heapWriteBuffer.setPosition(writeFloatToHeap(value, heapWriteBuffer.rawByteArray(), heapWriteBuffer.intPosition()));
            case SegmentWriteBuffer segmentWriteBuffer ->
                    segmentWriteBuffer.setPosition(writeFloatToSegment(value, segmentWriteBuffer.rawSegment(), segmentWriteBuffer.longPosition()));
        }
    }

    public static int writeFloatToHeap(float value, byte[] bytes, int position) {
        int bits = Float.floatToRawIntBits(value);
        if(bits < 0) {
            bytes[position++] = BYTE_MINUS;
        }
        if((bits & 0x7FFFFFFF) == 0) {
            bytes[position] = BYTE_ZERO;
            return position + 1;
        }
        BinaryFp binaryFp = buildBinaryFp(bits, FLOAT_SPEC);
        DecimalFp decimalFp = toDecimalFp(binaryFp, FLOAT_SPEC);
        return writeDecimalFpToHeap(decimalFp, bytes, position);
    }

    public static long writeFloatToSegment(float value, MemorySegment segment, long position) {
        int bits = Float.floatToRawIntBits(value);
        if(bits < 0) {
            SegmentAccess.setByte(segment, position++, BYTE_MINUS);
        }
        if((bits & 0x7FFFFFFF) == 0) {
            SegmentAccess.setByte(segment, position, BYTE_ZERO);
            return position + 1L;
        }
        BinaryFp binaryFp = buildBinaryFp(bits, FLOAT_SPEC);
        DecimalFp decimalFp = toDecimalFp(binaryFp, FLOAT_SPEC);
        return writeDecimalFpToSegment(decimalFp, segment, position);
    }

    public static void writeDouble(double f, WriteBuffer writeBuffer) {
        writeBuffer.ensureCapacity(MAX_DOUBLE_CAPACITY);
        switch (writeBuffer) {
            case HeapWriteBuffer heapWriteBuffer ->
                heapWriteBuffer.setPosition(writeDoubleToHeap(f, heapWriteBuffer.rawByteArray(), heapWriteBuffer.intPosition()));
            case SegmentWriteBuffer segmentWriteBuffer ->
                segmentWriteBuffer.setPosition(writeDoubleToSegment(f, segmentWriteBuffer.rawSegment(), segmentWriteBuffer.longPosition()));
        }
    }

    public static int writeDoubleToHeap(double value, byte[] bytes, int position) {
        long bits = Double.doubleToRawLongBits(value);
        if(bits < 0L) {
            bytes[position++] = BYTE_MINUS;
        }
        if((bits & 0x7FFFFFFFFFFFFFFFL) == 0L) {
            bytes[position] = BYTE_ZERO;
            return position + 1;
        }
        BinaryFp binaryFp = buildBinaryFp(bits, DOUBLE_SPEC);
        DecimalFp decimalFp = toDecimalFp(binaryFp, DOUBLE_SPEC);
        return writeDecimalFpToHeap(decimalFp, bytes, position);
    }

    public static long writeDoubleToSegment(double value, MemorySegment segment, long position) {
        long bits = Double.doubleToRawLongBits(value);
        if(bits < 0L) {
            SegmentAccess.setByte(segment, position++, BYTE_MINUS);
        }
        if((bits & 0x7FFFFFFFFFFFFFFFL) == 0L) {
            SegmentAccess.setByte(segment, position, BYTE_ZERO);
            return position + 1L;
        }
        BinaryFp binaryFp = buildBinaryFp(bits, DOUBLE_SPEC);
        DecimalFp decimalFp = toDecimalFp(binaryFp, DOUBLE_SPEC);
        return writeDecimalFpToSegment(decimalFp, segment, position);
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

    private static long writeDecimalFpToSegment(DecimalFp decimalFp, MemorySegment segment, long position) {
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

    private static long writeFixedDecimalFpToSegment(long d, int p, int digitCount, MemorySegment segment, long position) {
        int sum = digitCount + p;
        int shift = sum <= 0 ? 2 - sum : (p < 0 ? 1 : 0);
        long r = writePositiveLongToSegment(d, digitCount, segment, position + shift);
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

    private static long writeSciDecimalFpToSegment(long d, int sciE, int digitCount, MemorySegment segment, long position) {
        if(digitCount == 1) {
            SegmentAccess.setByte(segment, position++, (byte) (BYTE_ZERO + d));
        } else {
            long r = writePositiveLongToSegment(d, digitCount, segment, position + 1);
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
        switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> {
                IntIntPair p = readIntFromHeap(firstByte, heapReadBuffer.rawByteArray(), heapReadBuffer.intPosition());
                heapReadBuffer.setPosition(p.position());
                return p.value();
            }
            case SegmentReadBuffer segmentReadBuffer -> {
                IntLongPair p = readIntFromSegment(firstByte, segmentReadBuffer.rawSegment(), segmentReadBuffer.longPosition());
                segmentReadBuffer.setPosition(p.position());
                return p.value();
            }
        }
    }

    public static IntIntPair readIntFromHeap(byte firstByte, byte[] bytes, int position) {
        boolean positive = true;
        int r;
        if (firstByte == (byte) '-') {
            if (position == bytes.length) {
                throw new JsonDeserializerException("illegal leading minus sign");
            }
            positive = false;
            firstByte = bytes[position++];
        }
        r = '0' - firstByte;
        if (r < -9 || r > 0) {
            throw new JsonDeserializerException("not a number start : " + firstByte);
        }
        if (r == 0) {
            if (position < bytes.length) {
                r = bytes[position] - '0';
                if(r >= 0 && r <= 9) {
                    throw new JsonDeserializerException("leading zero");
                }
            }
            return new IntIntPair(0, position);
        }
        while (position < bytes.length) {
            int n = '0' - bytes[position];
            if (n < -9 || n > 0) {
                break;
            }
            if (r < N_DIV_10_I || (r == N_DIV_10_I && n < N_MOD_10_I)) {
                throw new JsonDeserializerException("integer overflow");
            }
            r = r * 10 + n;
            position++;
        }
        if (positive && r == Integer.MIN_VALUE) {
            throw new JsonDeserializerException("integer overflow");
        }
        return new IntIntPair(positive ? -r : r, position);
    }

    public static IntLongPair readIntFromSegment(byte firstByte, MemorySegment segment, long position) {
        boolean positive = true;
        int r;
        if (firstByte == (byte) '-') {
            if (position == segment.byteSize()) {
                throw new JsonDeserializerException("illegal leading minus sign");
            }
            positive = false;
            firstByte = SegmentAccess.getByte(segment, position++);
        }
        r = '0' - firstByte;
        if (r < -9 || r > 0) {
            throw new JsonDeserializerException("not a number start : " + firstByte);
        }
        if (r == 0) {
            if (position < segment.byteSize()) {
                r = SegmentAccess.getByte(segment, position) - '0';
                if(r >= 0 && r <= 9) {
                    throw new JsonDeserializerException("leading zero");
                }
            }
            return new IntLongPair(0, position);
        }
        while (position < segment.byteSize()) {
            int n = '0' - SegmentAccess.getByte(segment, position);
            if (n < -9 || n > 0) {
                break;
            }
            if (r < N_DIV_10_I || (r == N_DIV_10_I && n < N_MOD_10_I)) {
                throw new JsonDeserializerException("integer overflow");
            }
            r = r * 10 + n;
            position++;
        }
        if (positive && r == Integer.MIN_VALUE) {
            throw new JsonDeserializerException("integer overflow");
        }
        return new IntLongPair(positive ? -r : r, position);
    }

    public static long readLong(ReadBuffer readBuffer, byte firstByte) {
        switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> {
                LongIntPair p = readLongFromHeap(firstByte, heapReadBuffer.rawByteArray(), heapReadBuffer.intPosition());
                heapReadBuffer.setPosition(p.position());
                return p.value();
            }
            case SegmentReadBuffer segmentReadBuffer -> {
                LongLongPair p = readLongFromSegment(firstByte, segmentReadBuffer.rawSegment(), segmentReadBuffer.longPosition());
                segmentReadBuffer.setPosition(p.position());
                return p.value();
            }
        }
    }

    public static LongIntPair readLongFromHeap(byte firstByte, byte[] bytes, int position) {
        boolean positive = true;
        long r;
        if (firstByte == (byte) '-') {
            if (position == bytes.length) {
                throw new JsonDeserializerException("illegal leading minus sign");
            }
            positive = false;
            firstByte = bytes[position++];
        }
        r = '0' - firstByte;
        if (r < -9L || r > 0L) {
            throw new JsonDeserializerException("not a number start : " + firstByte);
        }
        if (r == 0L) {
            if (position < bytes.length) {
                r = bytes[position] - '0';
                if(r >= 0L && r <= 9L) {
                    throw new JsonDeserializerException("leading zero");
                }
            }
            return new LongIntPair(0, position);
        }
        while (position < bytes.length) {
            long n = '0' - bytes[position];
            if (n < -9L || n > 0L) {
                break;
            }
            if (r < N_DIV_10_L || (r == N_DIV_10_L && n < N_MOD_10_L)) {
                throw new JsonDeserializerException("long overflow");
            }
            r = r * 10L + n;
            position++;
        }
        if (positive && r == Long.MIN_VALUE) {
            throw new JsonDeserializerException("long overflow");
        }
        return new LongIntPair(positive ? -r : r, position);
    }

    public static LongLongPair readLongFromSegment(byte firstByte, MemorySegment segment, long position) {
        boolean positive = true;
        long r;
        if (firstByte == (byte) '-') {
            if (position == segment.byteSize()) {
                throw new JsonDeserializerException("illegal leading minus sign");
            }
            positive = false;
            firstByte = SegmentAccess.getByte(segment, position++);
        }
        r = '0' - firstByte;
        if (r < -9L || r > 0L) {
            throw new JsonDeserializerException("not a number start : " + firstByte);
        }
        if (r == 0L) {
            if (position < segment.byteSize()) {
                r = SegmentAccess.getByte(segment, position) - '0';
                if(r >= 0L && r <= 9L) {
                    throw new JsonDeserializerException("leading zero");
                }
            }
            return new LongLongPair(0, position);
        }
        while (position < segment.byteSize()) {
            long n = '0' - SegmentAccess.getByte(segment, position);
            if (n < -9L || n > 0L) {
                break;
            }
            if (r < N_DIV_10_L || (r == N_DIV_10_L && n < N_MOD_10_L)) {
                throw new JsonDeserializerException("long overflow");
            }
            r = r * 10L + n;
            position++;
        }
        if (positive && r == Long.MIN_VALUE) {
            throw new JsonDeserializerException("long overflow");
        }
        return new LongLongPair(positive ? -r : r, position);
    }

    private static Ndi readNdFromHeap(byte firstByte, byte[] bytes, int position, int end) {
        boolean negative = false;
        if(firstByte == (byte) '-') {
            if(position >= end) {
                throw new JsonDeserializerException("illegal leading minus sign");
            }
            negative = true;
            firstByte = bytes[position++];
        }
        long d = firstByte - '0';
        if(d < 0L || d > 9L) {
            throw new JsonDeserializerException("not a number start : " + firstByte);
        }
        if(d == 0L) {
            if(position >= end) {
                return new Ndi(negative, d, position);
            }
            int v = bytes[position] - '0';
            if(v >= 0 && v <= 9) {
                throw new JsonDeserializerException("leading zero");
            }
        }
        while (position < end) {
            int v = bytes[position] - '0';
            if(v < 0 || v > 9) {
                break ;
            }
            d = d * 10L + v; // safe overflow
            position++;
        }
        return new Ndi(negative, d, position);
    }

    private static Nfi readNfFromHeap(long d, byte[] bytes, int position, int end) {
        if(position >= end) {
            throw new JsonDeserializerException("leading period with no digits");
        }
        int first = bytes[position++] - '0';
        if(first < 0 || first > 9) {
            throw new JsonDeserializerException("leading period with illegal digits");
        }
        d = d * 10L + first;
        while (position < end) {
            int v = bytes[position] - '0';
            if(v < 0 || v > 9) {
                break ;
            }
            d = d * 10L + v;
            position++;
        }
        return new Nfi(d, position);
//        final int origin = position;
//        while(position < end) {
//            int v = bytes[position] - '0';
//            if(v < 0 || v > 9) {
//                break ;
//            }
//            d = d * 10L + v;
//            position++;
//        }
//        if(origin == position) {
//            throw new JsonDeserializerException("leading period with no digits");
//        }
//        return new Nfi(d, position);
    }

    private static Npi readNpFromHeap(byte[] bytes, int position, int end) {
        if(position >= end) {
            throw new JsonDeserializerException("leading exponent with no digits");
        }
        boolean negative = false;
        byte firstByte = bytes[position++];
        if(firstByte == (byte) '+' || firstByte == (byte) '-') {
            if(position >= end) {
                throw new JsonDeserializerException("leading exponent sign with no digits");
            }
            negative = firstByte == (byte) '-';
            firstByte = bytes[position++];
        }
        int p = firstByte - '0';
        if(p < 0 || p > 9) {
            throw new JsonDeserializerException("not a number start : " + firstByte);
        }
        while(position < end) {
            int v = bytes[position] - '0';
            if(v < 0 || v > 9) {
                break ;
            }
            if(p < MAX_DECIMAL_P_GUARD) {
                p = p * 10 + v;
            }
            position++;
        }
        return new Npi(negative ? -p : p, position);
    }

    public static FpRep readFpFromHeap(byte firstByte, byte[] bytes, final int position, int maxNumberBytes) {
        int end = position + Math.min(bytes.length - position, maxNumberBytes - 1);
        Ndi nd = readNdFromHeap(firstByte, bytes, position, end);
        boolean negative = nd.negative();
        long d = nd.d();
        int nextPosition = nd.position();
        int dLen = nextPosition - position + 1;
        int p = 0;
        if(nextPosition >= end) {
            return new FpRep(negative, dLen > MAX_DECIMAL_ND, d, p, dLen);
        }
        byte followingByte = bytes[nextPosition];
        if(followingByte == (byte) '.') {
            Nfi nf = readNfFromHeap(d, bytes, ++nextPosition, end);
            int nfPosition = nf.position();
            int shifted = nfPosition - nextPosition;
            p -= shifted;
            dLen += shifted;
            d = nf.d();
            if(nfPosition >= end) {
                return new FpRep(negative, dLen > MAX_DECIMAL_ND, d, p, dLen + 1);
            }
            nextPosition = nfPosition;
            followingByte = bytes[nextPosition];
        }
        if(followingByte == (byte) 'e' || followingByte == (byte) 'E') {
            Npi np = readNpFromHeap(bytes, ++nextPosition, end);
            p += np.p();
            nextPosition = np.position();
        }
        return new FpRep(negative, dLen > MAX_DECIMAL_P, d, p, nextPosition - position + 1);
    }

    private static Ndl readNdFromSegment(byte firstByte, MemorySegment segment, long position, long end) {
        boolean negative = false;
        if(firstByte == (byte) '-') {
            if(position >= end) {
                throw new JsonDeserializerException("illegal leading minus sign");
            }
            negative = true;
            firstByte = SegmentAccess.getByte(segment, position++);
        }
        long d = firstByte - '0';
        if(d < 0L || d > 9L) {
            throw new JsonDeserializerException("not a number start : " + firstByte);
        }
        if(d == 0L) {
            if(position >= end) {
                return new Ndl(negative, d, position);
            }
            int v = SegmentAccess.getByte(segment, position) - '0';
            if(v >= 0 && v <= 9) {
                throw new JsonDeserializerException("leading zero");
            }
        }
        while (position < end) {
            int v = SegmentAccess.getByte(segment, position) - '0';
            if(v < 0 || v > 9) {
                break ;
            }
            d = d * 10L + v; // safe overflow
            position++;
        }
        return new Ndl(negative, d, position);
    }

    private static Nfl readNfFromSegment(long d, MemorySegment segment, long position, long end) {
        if(position >= end) {
            throw new JsonDeserializerException("leading period with no digits");
        }
        int first = SegmentAccess.getByte(segment, position++) - '0';
        if(first < 0 || first > 9) {
            throw new JsonDeserializerException("leading period with illegal digits");
        }
        d = d * 10L + first;
        while (position < end) {
            int v = SegmentAccess.getByte(segment, position) - '0';
            if(v < 0 || v > 9) {
                break ;
            }
            d = d * 10L + v;
            position++;
        }
        return new Nfl(d, position);
//        if(position >= end) {
//            throw new JsonDeserializerException("leading period with no digits");
//        }
//        while(position < end) {
//            int v = SegmentAccess.getByte(segment, position) - '0';
//            if(v < 0 || v > 9) {
//                break ;
//            }
//            d = d * 10L + v;
//            position++;
//        }
//        return new Nfl(d, position);
    }

    private static Npl readNpFromSegment(MemorySegment segment, long position, long end) {
        if(position >= end) {
            throw new JsonDeserializerException("leading exponent with no digits");
        }
        boolean negative = false;
        byte firstByte = SegmentAccess.getByte(segment, position++);
        if(firstByte == (byte) '+' || firstByte == (byte) '-') {
            if(position >= end) {
                throw new JsonDeserializerException("leading exponent sign with no digits");
            }
            negative = firstByte == (byte) '-';
            firstByte = SegmentAccess.getByte(segment, position++);
        }
        int p = firstByte - '0';
        if(p < 0 || p > 9) {
            throw new JsonDeserializerException("not a number start : " + firstByte);
        }
        while(position < end) {
            int v = SegmentAccess.getByte(segment, position) - '0';
            if(v < 0 || v > 9) {
                break ;
            }
            if(p < MAX_DECIMAL_P_GUARD) {
                p = p * 10 + v;
            }
            position++;
        }
        return new Npl(negative ? -p : p, position);
    }

    public static FpRep readFpFromSegment(byte firstByte, MemorySegment segment, long position, int maxNumberBytes) {
        long end = position + Math.min(segment.byteSize() - position, maxNumberBytes - 1);
        Ndl nd = readNdFromSegment(firstByte, segment, position, end);
        boolean negative = nd.negative();
        long d = nd.d();
        long nextPosition = nd.position();
        int dLen = Math.toIntExact(nextPosition - position + 1);
        int p = 0;
        if(nextPosition >= end) {
            return new FpRep(negative, dLen > MAX_DECIMAL_ND, d, p, dLen);
        }
        byte followingByte = SegmentAccess.getByte(segment, nextPosition);
        if(followingByte == (byte) '.') {
            Nfl nf = readNfFromSegment(d, segment, ++nextPosition, end);
            long nfPosition = nf.position();
            int shifted = Math.toIntExact(nfPosition - nextPosition);
            p -= shifted;
            dLen += shifted;
            d = nf.d();
            if(nfPosition >= end) {
                return new FpRep(negative, dLen > MAX_DECIMAL_ND, d, p, dLen + 1);
            }
            nextPosition = nfPosition;
            followingByte = SegmentAccess.getByte(segment, nextPosition);
        }
        if(followingByte == (byte) 'e' || followingByte == (byte) 'E') {
            Npl np = readNpFromSegment(segment, ++nextPosition, end);
            p += np.p();
            nextPosition = np.position();
        }
        return new FpRep(negative, dLen > MAX_DECIMAL_P, d, p, Math.toIntExact(nextPosition - position + 1));
    }

    // read a string-format floating-point number into a specific format.
    // enforces strict format validation; patterns like ".123E0123" are rejected,
    // although they are acceptable by the JDK parser.
    public static FpRep readFpStrRep(ReadBuffer readBuffer, int maxNumberBytes, byte firstByte) {
        switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> {
                final int position = heapReadBuffer.intPosition();
                FpRep r = readFpFromHeap(firstByte, heapReadBuffer.rawByteArray(), position, maxNumberBytes);
                heapReadBuffer.setPosition(position - 1 + r.len());
                return r;
            }
            case SegmentReadBuffer segmentReadBuffer -> {
                final long position = segmentReadBuffer.longPosition();
                FpRep r = readFpFromSegment(firstByte, segmentReadBuffer.rawSegment(), position, maxNumberBytes);
                segmentReadBuffer.setPosition(position - 1L + r.len());
                return r;
            }
        }
    }

    public static FpRep readFpStrRepFromHeap(HeapReadBuffer heapReadBuffer, int maxNumberBytes, byte firstByte) {
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
                return new FpRep(neg, false, 0L, p, 0);
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
        // note that leading zeros are allowed in the exponent part according to the JSON specification
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
        return new FpRep(neg, trunc, d, p, index - position + 1);
    }

    public static FpRep readFpStrRepFromSegment(SegmentReadBuffer segmentReadBuffer, int maxNumberBytes, byte firstByte) {
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
                return new FpRep(neg, false, 0L, p, 0);
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
        return new FpRep(neg, trunc, d, p, Math.toIntExact(index - position + 1L));
    }

    public static float readFloat(ReadBuffer readBuffer, int maxNumberBytes, byte firstByte) {
        FpRep rep = readFpStrRep(readBuffer, maxNumberBytes, firstByte);
        Fp32 fp32 = parseFloat(rep);
        if(fp32.trunc()) {
            return readFloatFallback(readBuffer, rep.len());
        }
        return fp32.value();
    }

    public static Fp32 parseFloat(FpRep rep) {
        if (rep.trunc()) {
            return new Fp32(Float.NaN, true);
        }
        final int sign = rep.negative() ? (1 << (FLOAT_SPEC.mantBits() + FLOAT_SPEC.expBits())) : 0;
        final long d = rep.d();
        final int p = rep.p();
        if (d == 0L || p < FLOAT_SPEC.minDecExp() - MAX_DECIMAL_ND - 2) {
            return new Fp32(Float.intBitsToFloat(sign), false);
        }
        if (p > FLOAT_SPEC.maxDecExp() + 2) {
            return new Fp32(Float.intBitsToFloat(sign | (0xff << FLOAT_SPEC.mantBits())), false);
        }
        if (d >> FLOAT_SPEC.mantBits() == 0L) {
            float f = (float) Math.toIntExact(rep.negative() ? -d : d);
            if (p == 0) {
                return new Fp32(f, false);
            } else if (p > 0 && p <= FLOAT_EXACT_I + FLOAT_EXACT_P) {
                int tp = p;
                if (tp > FLOAT_EXACT_P) {
                    f *= FLOAT_POW_10[tp - FLOAT_EXACT_P];
                    tp = FLOAT_EXACT_P;
                }
                if (f >= FLOAT_EXACT_I_LOW && f <= FLOAT_EXACT_I_HIGH) {
                    return new Fp32(f * FLOAT_POW_10[tp], false);
                }
            } else if (p < 0 && p >= -FLOAT_EXACT_P) {
                return new Fp32(f / FLOAT_POW_10[-p], false);
            }
        }
        final int lp = log2Pow10(p);
        final int shift = Long.numberOfLeadingZeros(d);
        final int b = Long.SIZE - shift;
        int fe = Math.min(FLOAT_SPEC.mantBits() - FLOAT_SPEC.bias() - 1, FLOAT_SPEC.mantBits() + 1 - b - lp);
        Scalers scalers = prescale(fe - shift, p, lp);
        if (scalers.s() >= Long.SIZE) {
            return new Fp32(Float.intBitsToFloat(sign), false);
        }
        long u = uscale(d << shift, scalers);
        if (u >= umin(1L << (FLOAT_SPEC.mantBits() + 1))) {
            u = (u >>> 1) | (u & 1);
            fe--;
        }
        final int m = sign | Math.toIntExact(uround(u));
        if ((m & (1 << FLOAT_SPEC.mantBits())) == 0) {
            return new Fp32(Float.intBitsToFloat(m), false);
        }
        final int e = -fe;
        if (e >= (1 << FLOAT_SPEC.expBits()) - 1 - FLOAT_SPEC.mantBits() + FLOAT_SPEC.bias()) {
            return new Fp32(Float.NaN, true);
        }
        return new Fp32(Float.intBitsToFloat(
                (m & ~(1 << FLOAT_SPEC.mantBits())) |
                        (FLOAT_SPEC.mantBits() - FLOAT_SPEC.bias() + e) << FLOAT_SPEC.mantBits()), false);
    }

    private static float readFloatFallback(ReadBuffer readBuffer, int len) {
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
        FpRep rep = readFpStrRep(readBuffer, numberMaxBytes, firstByte);
        Fp64 fp64 = parseDouble(rep);
        if(fp64.trunc()) {
            return readDoubleFallback(readBuffer, rep.len());
        }
        return fp64.value();
    }

    public static Fp64 parseDouble(FpRep rep) {
        if (rep.trunc()) {
            return new Fp64(Double.NaN, true);
        }
        final long sign = rep.negative() ? (1L << (DOUBLE_SPEC.mantBits() + DOUBLE_SPEC.expBits())) : 0L;
        final long d = rep.d();
        final int p = rep.p();
        if (d == 0L || p < DOUBLE_SPEC.minDecExp() - MAX_DECIMAL_ND - 2) {
            return new Fp64(Double.longBitsToDouble(sign), false);
        }
        if (p > DOUBLE_SPEC.maxDecExp() + 2) {
            return new Fp64(Double.longBitsToDouble(sign | (0x7ffL << DOUBLE_SPEC.mantBits())), false);
        }
        if (d >> DOUBLE_SPEC.mantBits() == 0L) {
            double f = (double) (rep.negative() ? -d : d);
            if (p == 0) {
                return new Fp64(f, false);
            } else if (p > 0 && p <= DOUBLE_EXACT_I + DOUBLE_EXACT_P) {
                int tp = p;
                if (tp > DOUBLE_EXACT_P) {
                    f *= DOUBLE_POW_10[tp - DOUBLE_EXACT_P];
                    tp = DOUBLE_EXACT_P;
                }
                if (f >= DOUBLE_EXACT_I_LOW && f <= DOUBLE_EXACT_I_HIGH) {
                    return new Fp64(f * DOUBLE_POW_10[tp], false);
                }
            } else if (p < 0 && p >= -DOUBLE_EXACT_P) {
                return new Fp64(f / DOUBLE_POW_10[-p], false);
            }
        }
        final int lp = log2Pow10(p);
        final int shift = Long.numberOfLeadingZeros(d);
        final int b = Long.SIZE - shift;
        int fe = Math.min(DOUBLE_SPEC.mantBits() - DOUBLE_SPEC.bias() - 1, DOUBLE_SPEC.mantBits() + 1 - b - lp);
        Scalers scalers = prescale(fe - shift, p, lp);
        if (scalers.s() >= Long.SIZE) {
            return new Fp64(Double.longBitsToDouble(sign), false);
        }
        long u = uscale(d << shift, scalers);
        if (u >= umin(1L << (DOUBLE_SPEC.mantBits() + 1))) {
            u = (u >>> 1) | (u & 1);
            fe--;
        }
        final long m = sign | uround(u);
        if ((m & (1L << DOUBLE_SPEC.mantBits())) == 0L) {
            return new Fp64(Double.longBitsToDouble(m), false);
        }
        final int e = -fe;
        if (e >= (1 << DOUBLE_SPEC.expBits()) - 1 - DOUBLE_SPEC.mantBits() + DOUBLE_SPEC.bias()) {
            return new Fp64(Double.NaN, true);
        }
        return new Fp64(Double.longBitsToDouble(
                (m & ~(1L << DOUBLE_SPEC.mantBits())) |
                        ((long) (DOUBLE_SPEC.mantBits() - DOUBLE_SPEC.bias() + e)) << DOUBLE_SPEC.mantBits()), false);
    }

    private static double readDoubleFallback(ReadBuffer readBuffer, int len) {
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

    public value record IntIntPair(int value, int position) {

    }

    public value record IntLongPair(int value, long position) {

    }

    public value record LongIntPair(long value, int position) {

    }

    public value record LongLongPair(long value, long position) {

    }

    public value record Ndi(boolean negative, long d, int position) {

    }

    public value record Ndl(boolean negative, long d, long position) {

    }

    public value record Nfi(long d, int position) {

    }

    public value record Nfl(long d, long position) {

    }

    public value record Npi(int p, int position) {

    }

    public value record Npl(int p, long position) {

    }

    public value record Fp32(float value, boolean trunc) {

    }

    public value record Fp64(double value, boolean trunc) {

    }

    /**
     * Represents a binary floating-point number as m * 2^e.
     */
    public value record BinaryFp(long m, int e) {
    }

    /**
     * Represents a decimal floating-point number as d * 10^p.
     */
    public value record DecimalFp(long d, int p) {
    }

    /**
     * Defines the specification parameters for a floating-point format (e.g., mantissa bits, exponent bits, bias, etc.).
     * Predefined constants are available for 32-bit and 64-bit floats.
     * Note: This implementation is strictly limited to processing 64-bit float values.
     * Do not apply this to larger formats like FP128, as the current algorithms cannot handle the extended range.
     */
    public value record FpSpec(
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
    public value record Scalers(
            long pmHi,
            long pmLo,
            int s
    ) {
    }

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
     * @param len      length of the original floating-point string, including the first byte
     */
    public value record FpRep (
            boolean negative,
            boolean trunc,
            long d,
            int p,
            int len
    ) {
    }
}
