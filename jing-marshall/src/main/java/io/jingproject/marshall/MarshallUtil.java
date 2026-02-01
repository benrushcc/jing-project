package io.jingproject.marshall;

import io.jingproject.common.ArrayAccess;
import io.jingproject.common.anno.ProcessorApi;
import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorOperators;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.stream.IntStream;

@ProcessorApi
public final class MarshallUtil {

    private static final int P2 = 31 * 31;
    private static final int P4 = P2 * P2;
    private static final int P8 = P4 * P4;
    private static final int U = -128 * (1 + 31) * (1 + P2) * (1 + P4);

    private MarshallUtil() {
        throw new AssertionError();
    }

    private static int checkAccess(byte[] bytes, int offset, int len) {
        int size = bytes.length;
        if((size | offset | len) < 0 || size - offset < len) {
            throw new IllegalArgumentException();
        }
        return len + offset;
    }

    private static long checkAccess(MemorySegment segment, long offset, long len) {
        long size = segment.byteSize();
        // 先检查正负性，确保都是正数，然后减法运算就不可能产生溢出了
        if((size | offset | len) < 0L || size - offset < len) {
            throw new IllegalArgumentException();
        }
        // len + offset的范围一定在size之内，所以这个地方也不可能溢出
        return len + offset;
    }

    public static int hash(byte[] content) {
        return Arrays.hashCode(content);
    }

    private static int hashSwar(byte[] content, int offset, int len) {
        int end = checkAccess(content, offset, len);
        int r = 1;
        for (; offset < end - Long.BYTES; offset += Long.BYTES) {
            long l = ArrayAccess.getLong(content, offset, ByteOrder.LITTLE_ENDIAN);
            l = 31 * (l & 0x00FF00FF00FF00FFL) + ((l >>> 8) & 0x00FF00FF00FF00FFL);
            l = P2 * (l & 0x0000FFFF0000FFFFL) + ((l >>> 16) & 0x0000FFFF0000FFFFL);
            r = P8 * r + P4 * (int) l + (int) (l >>> 32) + U;
        }
        for (; offset < end; offset++) {
            r = 31 * r + Byte.toUnsignedInt(content[offset]);
        }
        return r;
    }

    public static int hash(byte[] content, int offset, int len) {
        assert content != null;
        return switch (content.length) {
            case 0 -> 0;
            case 1 -> 31 + (content[0] & 0xFF);
            default -> hashSwar(content, offset, len);
        };
    }

    static final int L = ByteVector.SPECIES_PREFERRED.length();
    static final int[] P = calculatePowers();
    static final int V = -98 * (1 + 31) * (1 + P2);
    static final int I = 0xbdef7bdf;
    static final int[] F = calculateFactors();
    static final IntVector W =
            IntVector.fromArray(
                    IntVector.SPECIES_PREFERRED,
                    IntStream.range(0, IntVector.SPECIES_PREFERRED.length())
                            .map(i -> P[L - (1 + i) * 4])
                            .toArray(),
                    0);
    static final int PL = P[L];

    static int[] calculatePowers() {
        int[] result = new int[L + 1];
        result[0] = 1;
        for (int i = 1; i <= L; ++i) {
            result[i] = result[i - 1] * 31;
        }
        return result;
    }

    static int[] calculateFactors() {
        int[] factors = new int[L + 1];
        factors[L] = 1;
        for (int i = L; i > 0; --i) {
            factors[i - 1] = factors[i] * I;
        }
        return factors;
    }

    public static int hashCodeSIMD(byte[] b) {
        if (b == null) return 0;
        if (b.length == 0) return 1;
        if (b.length == 1) return 31 + b[0];
        var a = IntVector.zero(IntVector.SPECIES_PREFERRED);
        int remaining = b.length;
        int k = 0;
        while (remaining > L) {
            var s =
                    ByteVector.fromArray(ByteVector.SPECIES_PREFERRED, b, k)
                            .lanewise(VectorOperators.XOR, (byte) 0x80)
                            .reinterpretAsShorts();
            var i = s.and((short) 0xFF).mul((short) 31).add(s.lanewise(VectorOperators.LSHR, 8)).reinterpretAsInts();
            a = a.add(i.and(0xFFFF).mul(P2).add(i.lanewise(VectorOperators.LSHR, 16)));
            a = a.add(V);
            a = a.mul(PL);
            k += L;
            remaining -= L;
        }
        return finalizeSIMD(a, b, k, remaining);
    }

    static int finalizeSIMD(IntVector a, byte[] b, int k, int remaining) {
        var s =
                ByteVector.fromArray(
                                ByteVector.SPECIES_PREFERRED,
                                b,
                                k,
                                ByteVector.SPECIES_PREFERRED.indexInRange(0, remaining))
                        .lanewise(VectorOperators.XOR, (byte) 0x80)
                        .reinterpretAsShorts();
        var i = s.and((short) 0xFF).mul((short) 31).add(s.lanewise(VectorOperators.LSHR, 8)).reinterpretAsInts();
        a = a.add(i.and(0xFFFF).mul(P2).add(i.lanewise(VectorOperators.LSHR, 16)));
        a = a.add(V);
        return (1 + a.mul(W).reduceLanes(VectorOperators.ADD)) * F[remaining];
    }

    public static byte parseByte(MemorySegment segment, long offset, long len) {
        int r = parseInt(segment, offset, len);
        if(r instanceof byte b) {
            return b;
        }
        throw new ArithmeticException("byte overflow");
    }

    public static short parseShort(MemorySegment segment, long offset, long len) {
        int r = parseInt(segment, offset, len);
        if(r instanceof short s) {
            return s;
        }
        throw new ArithmeticException("short overflow");
    }

    // 算术溢出是ArithmeticException，如果是数据有问题就是NumberFormatException
    public static int parseInt(MemorySegment segment, long offset, long len) {
        assert segment != null;
        long end = checkAccess(segment, offset, len);
        byte firstByte = segment.get(ValueLayout.JAVA_BYTE, offset);
        boolean negative = false;
        if(firstByte == '-') {
            negative = true;
            offset = Math.incrementExact(offset);
        }
        int r = 0;
        for(long index = offset; index < end; index++) {
            byte b = segment.get(ValueLayout.JAVA_BYTE, index);
            if(b < '0' || b > '9') {
                throw new NumberFormatException("invalid byte : " + b);
            }
            int current = b - '0';
            r = Math.subtractExact(Math.multiplyExact(r, 10), current);
        }
        if(negative) {
            return r;
        }
        if(r == Integer.MIN_VALUE) {
            throw new ArithmeticException("integer overflow");
        }
        return -r;
    }

    public static long parseLong(MemorySegment segment, long offset, long len) {
        assert segment != null;
        long end = checkAccess(segment, offset, len);
        byte firstByte = segment.get(ValueLayout.JAVA_BYTE, offset);
        boolean negative = false;
        if(firstByte == '-') {
            negative = true;
            offset = Math.incrementExact(offset);
        }
        long r = 0L;
        for(long index = offset; index < end; index++) {
            byte b = segment.get(ValueLayout.JAVA_BYTE, index);
            if(b < '0' || b > '9') {
                throw new NumberFormatException("invalid byte : " + b);
            }
            long current = b - '0';
            r = Math.subtractExact(Math.multiplyExact(r, 10L), current);
        }
        if(negative) {
            return r;
        }
        if(r == Long.MIN_VALUE) {
            throw new ArithmeticException("long overflow");
        }
        return -r;
    }

    private record Spec(int significandBits, int expBits, int bias) {

    }

    private static final Spec FLOAT_SPEC = new Spec(23, 8, -127);
    private static final Spec DOUBLE_SPEC = new Spec(52, 11, -1023);

    private static final class Dec {
        // CAP不应该被更改，这是一个经过验证的，可以覆盖所有浮点值的固定大小
        private static final int CAP = 800;
        private final byte[] d = new byte[CAP];
        private int nd = 0;
        private int dp = 0;
        private boolean neg = false;
        private boolean trunc = false;

        public void init() {

        }
    }

    private static double buildDouble(boolean neg, long significand, long exp) {
        long r = significand & ((1L << DOUBLE_SPEC.significandBits()) - 1L);
        r |= ((exp - DOUBLE_SPEC.bias()) & ((1L << DOUBLE_SPEC.expBits()) - 1L)) << DOUBLE_SPEC.significandBits();
        if(neg) {
            r |= 1L << (DOUBLE_SPEC.significandBits() + DOUBLE_SPEC.expBits());
        }
        return Double.longBitsToDouble(r);
    }

    public static float parseFloat(MemorySegment segment, long offset, long len) {
        assert segment != null;
        checkAccess(segment, offset, len);
        // TODO
        return Float.NaN;
    }

    public static double parseDouble(MemorySegment segment, long offset, long len) {
        assert segment != null;
        checkAccess(segment, offset, len);
        byte firstByte = segment.get(ValueLayout.JAVA_BYTE, offset);
        // TODO
        return Double.NaN;
    }

}
