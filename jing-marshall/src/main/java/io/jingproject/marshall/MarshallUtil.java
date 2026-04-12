package io.jingproject.marshall;

import io.jingproject.common.anno.ProcessorApi;
import io.jingproject.marshall.hash.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

@ProcessorApi
public final class MarshallUtil {

    private static final Hasher LENGTH_HASHER = new LengthHasher();

    private static final Hasher ONEBYTE_HASHER = new OneByteHasher();

    private static final Hasher TWOBYTE_HASHER = new TwoByteHasher();

    private static final Hasher THREEBYTE_HASHER = new ThreeByteHasher();

    private static final Hasher FOURBYTE_HASHER = new FourByteHasher();

    private static final Hasher FNV_MUL_HASHER = new FnvHasher();

    private static final List<Hasher> AVAILABLE_HASHERS = List.of(LENGTH_HASHER, ONEBYTE_HASHER, TWOBYTE_HASHER, THREEBYTE_HASHER, FOURBYTE_HASHER, FNV_MUL_HASHER);

    private MarshallUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Hasher lengthHasher() {
        return LENGTH_HASHER;
    }

    public static Hasher oneByteHasher() {
        return ONEBYTE_HASHER;
    }

    public static Hasher twoByteHasher() {
        return TWOBYTE_HASHER;
    }

    public static Hasher threeByteHasher() {
        return THREEBYTE_HASHER;
    }

    public static Hasher fourByteHasher() {
        return FOURBYTE_HASHER;
    }

    public static Hasher fnvMulHasher() {
        return FNV_MUL_HASHER;
    }

    public static Hasher calcHasher(List<String> names) {
        int maxCollisions = Integer.MAX_VALUE;
        Hasher fallback = FNV_MUL_HASHER;
        for (Hasher currentHasher : AVAILABLE_HASHERS) {
            Set<Integer> hs = new HashSet<>(names.size());
            int collisions = 0;
            for (String name : names) {
                int hash = currentHasher.hash(name);
                if(!hs.add(hash)) {
                    collisions = Math.incrementExact(collisions);
                }
            }
            if(collisions == 0) {
                return currentHasher;
            }
            if(collisions < maxCollisions) {
                maxCollisions = collisions;
                fallback = currentHasher;
            }
        }
        return fallback;
    }

    public static byte[] calcBytes(List<String> names) {
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            for (String name : names) {
                baos.write(name.getBytes(StandardCharsets.UTF_8));
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
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
        assert segment != null && Objects.checkFromIndexSize(offset, len, segment.byteSize()) >= 0L;
        long end = offset + len;
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
        assert segment != null && Objects.checkFromIndexSize(offset, len, segment.byteSize()) >= 0L;
        long end = offset + len;
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

}
