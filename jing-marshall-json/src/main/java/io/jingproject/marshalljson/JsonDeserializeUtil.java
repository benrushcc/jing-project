package io.jingproject.marshalljson;

import io.jingproject.common.*;
import jdk.incubator.vector.ByteVector;

import java.lang.foreign.MemorySegment;
import java.util.Arrays;
import java.util.Objects;

public final class JsonDeserializeUtil {
    private static final byte BYTE_space = (byte) ' ';
    private static final byte BYTE_lf = (byte) '\n';
    private static final byte BYTE_cr = (byte) '\r';
    private static final byte BYTE_ht = (byte) '\t';
    private static final byte BYTE_colon = (byte) ':';
    private static final int COMPACT_TRUE = Utils.compact(Utils.compact((byte) 't', (byte) 'r'), Utils.compact((byte) 'u', (byte) 'e'));
    private static final int COMPACT_ALSE = Utils.compact(Utils.compact((byte) 'a', (byte) 'l'), Utils.compact((byte) 's', (byte) 'e'));
    private static final int ARR_INITIAL_SIZE = 4;

    private JsonDeserializeUtil() {
        throw new UnsupportedOperationException("utility class");
    }

    public static byte nextFirstValuableByte(ReadBuffer readBuffer, JsonDeserializerOption option, boolean consume) {
        int maxEmptyBytes = option.maxEmptyBytes();
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> nextHeapFirstValuableByte(heapReadBuffer, maxEmptyBytes, consume);
            case SegmentReadBuffer segmentReadBuffer -> nextSegmentFirstValuableByte(segmentReadBuffer, maxEmptyBytes, consume);
        };
    }

    private static byte nextHeapFirstValuableByte(HeapReadBuffer heapReadBuffer, int maxEmptyBytes, boolean consume) {
        byte[] bytes = heapReadBuffer.rawByteArray();
        int position = heapReadBuffer.intPosition();
        int range = Math.min(maxEmptyBytes, heapReadBuffer.intLength() - position);
        for (int i = position; i < position + range; i++) {
            byte b = bytes[i];
            switch (b) {
                case BYTE_space, BYTE_lf, BYTE_cr, BYTE_ht -> {
                }
                default -> {
                    heapReadBuffer.setPosition(consume ? i + 1 : i); // no overflow
                    return b;
                }
            }
        }
        throw new JsonDeserializerException("too many empty bytes");
    }

    private static byte nextSegmentFirstValuableByte(SegmentReadBuffer segmentReadBuffer, int maxEmptyBytes, boolean consume) {
        MemorySegment segment = segmentReadBuffer.rawSegment();
        int position = segmentReadBuffer.intPosition();
        int range = Math.min(maxEmptyBytes, segmentReadBuffer.intLength() - position);
        for (int i = position; i < position + range; i++) {
            byte b = SegmentAccess.getByte(segment, i);
            switch (b) {
                case BYTE_space, BYTE_lf, BYTE_cr, BYTE_ht -> {
                }
                default -> {
                    segmentReadBuffer.setPosition(consume ? i + 1 : i); // no overflow
                    return b;
                }
            }
        }
        throw new JsonDeserializerException("too many empty bytes");
    }

    public static byte deserializeByte(ReadBuffer readBuffer, JsonDeserializerOption option) {
        byte firstByte = nextFirstValuableByte(readBuffer, option, true);
        int v = JsonNumberUtil.readInt(readBuffer, firstByte);
        if(v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
            return (byte) v;
        }
        throw new JsonDeserializerException("byte value overflow : " + v);
    }

    public static boolean deserializeBoolean(ReadBuffer readBuffer, JsonDeserializerOption option) {
        byte firstByte = nextFirstValuableByte(readBuffer, option, false);
        return deserializeBoolean(readBuffer, firstByte);
    }

    public static boolean deserializeBoolean(ReadBuffer readBuffer, byte firstByte) {
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> deserializeHeapBoolean(heapReadBuffer, firstByte);
            case SegmentReadBuffer segmentReadBuffer -> deserializeSegmentBoolean(segmentReadBuffer, firstByte);
        };
    }

    private static boolean deserializeHeapBoolean(HeapReadBuffer heapReadBuffer, byte firstByte) {
        byte[] bytes = heapReadBuffer.rawByteArray();
        int position = heapReadBuffer.intPosition();
        if(firstByte == 't') {
            int newPosition = position + 4;
            if(newPosition > bytes.length) {
                throw new JsonDeserializerException("eof reached while reading boolean literal 'true' value");
            }
            if (ArrayAccess.getInt(bytes, position) != COMPACT_TRUE) {
                throw new JsonDeserializerException("illegal boolean literal 'true' value");
            }
            heapReadBuffer.setPosition(newPosition);
            return true;
        } else if(firstByte == 'f') {
            int newPosition = position + 5;
            if(newPosition > bytes.length) {
                throw new JsonDeserializerException("eof reached while reading boolean literal 'false' value");
            }
            if (ArrayAccess.getInt(bytes, position + 1) != COMPACT_ALSE) {
                throw new JsonDeserializerException("illegal boolean literal 'false' value");
            }
            heapReadBuffer.setPosition(newPosition);
            return false;
        } else {
            throw new JsonDeserializerException("illegal boolean literal value");
        }
    }

    private static boolean deserializeSegmentBoolean(SegmentReadBuffer segmentReadBuffer, byte firstByte) {
        MemorySegment segment = segmentReadBuffer.rawSegment();
        long position = segmentReadBuffer.longPosition();
        if(firstByte == 't') {
            long newPosition = position + 4L;
            if(newPosition > segment.byteSize()) {
                throw new JsonDeserializerException("eof reached while reading boolean literal 'true' value");
            }
            if (SegmentAccess.getInt(segment, position) != COMPACT_TRUE) {
                throw new JsonDeserializerException("illegal boolean literal 'true' value");
            }
            segmentReadBuffer.setPosition(newPosition);
            return true;
        } else if(firstByte == 'f') {
            long newPosition = position + 5L;
            if(newPosition > segment.byteSize()) {
                throw new JsonDeserializerException("eof reached while reading boolean literal 'false' value");
            }
            if (SegmentAccess.getInt(segment, position + 1L) != COMPACT_ALSE) {
                throw new JsonDeserializerException("illegal boolean literal 'false' value");
            }
            segmentReadBuffer.setPosition(newPosition);
            return false;
        } else {
            throw new JsonDeserializerException("illegal boolean literal value");
        }
    }

    public static short deserializeShort(ReadBuffer readBuffer, JsonDeserializerOption option) {
        byte firstByte = nextFirstValuableByte(readBuffer, option, true);
        int v = JsonNumberUtil.readInt(readBuffer, firstByte);
        if(v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
            return (short) v;
        }
        throw new JsonDeserializerException("short value overflow : " + v);
    }

    public static char deserializeChar(ReadBuffer readBuffer, JsonDeserializerOption option, JsonDeserializerContext context) {
        byte b = nextFirstValuableByte(readBuffer, option, true);
        if (b != '"') {
            throw new JsonDeserializerException("missing first quote, got : " + b);
        }
        parseString(readBuffer, option, context);
        return context.asChar();
    }

    public static int deserializeInt(ReadBuffer readBuffer, JsonDeserializerOption option) {
        byte firstByte = nextFirstValuableByte(readBuffer, option, true);
        return JsonNumberUtil.readInt(readBuffer, firstByte);
    }

    public static long deserializeLong(ReadBuffer readBuffer, JsonDeserializerOption option) {
        byte firstByte = nextFirstValuableByte(readBuffer, option, true);
        return JsonNumberUtil.readLong(readBuffer, firstByte);
    }

    public static float deserializeFloat(ReadBuffer readBuffer, JsonDeserializerOption option) {
        byte firstByte = nextFirstValuableByte(readBuffer, option, true);
        return JsonNumberUtil.readFloat(readBuffer, firstByte);
    }

    public static double deserializeDouble(ReadBuffer readBuffer, JsonDeserializerOption option) {
        byte firstByte = nextFirstValuableByte(readBuffer, option, true);
        return JsonNumberUtil.readDouble(readBuffer, firstByte);
    }

    public static byte[] deserializeByteArray(ReadBuffer readBuffer, JsonDeserializerOption option) {
        byte[] r = new byte[ARR_INITIAL_SIZE];
        int index = 0;
        byte b = nextFirstValuableByte(readBuffer, option, true);
        if (b != '[') {
            throw new JsonDeserializerException("array start not found, got : " + b);
        }
        b = nextFirstValuableByte(readBuffer, option, false);
        if(b == ']') {
            return Utils.emptyByteArray();
        }
        for (int i = 0; i < option.maxArrayElements(); i++) {
            byte v = deserializeByte(readBuffer, option);
            if (index == r.length) {
                int newLength = Math.addExact(r.length, r.length);
                if (newLength > option.maxArrayElements()) {
                    throw new JsonDeserializerException("too many array elements, current length : " + r.length);
                }
                r = Arrays.copyOf(r, newLength);
            }
            r[index++] = v;
            b = nextFirstValuableByte(readBuffer, option, true);
            if (b == ']') {
                return index == r.length ? r : Arrays.copyOf(r, index);
            } else if (b != ',') {
                throw new JsonDeserializerException("sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, current length : " + r.length);
    }

    public static boolean[] deserializeBooleanArray(ReadBuffer readBuffer, JsonDeserializerOption option) {
        boolean[] r = new boolean[ARR_INITIAL_SIZE];
        int index = 0;
        byte b = nextFirstValuableByte(readBuffer, option, true);
        if (b != '[') {
            throw new JsonDeserializerException("array start not found, got : " + b);
        }
        b = nextFirstValuableByte(readBuffer, option, false);
        if(b == ']') {
            return Utils.emptyBooleanArray();
        }
        for (int i = 0; i < option.maxArrayElements(); i++) {
            boolean v = deserializeBoolean(readBuffer, option);
            if (index == r.length) {
                int newLength = Math.addExact(r.length, r.length);
                if (newLength > option.maxArrayElements()) {
                    throw new JsonDeserializerException("too many array elements, current length : " + r.length);
                }
                r = Arrays.copyOf(r, newLength);
            }
            r[index++] = v;
            b = nextFirstValuableByte(readBuffer, option, true);
            if (b == ']') {
                return index == r.length ? r : Arrays.copyOf(r, index);
            } else if (b != ',') {
                throw new JsonDeserializerException("sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, current length : " + r.length);
    }

    public static short[] deserializeShortArray(ReadBuffer readBuffer, JsonDeserializerOption option) {
        short[] r = new short[ARR_INITIAL_SIZE];
        int index = 0;
        byte b = nextFirstValuableByte(readBuffer, option, true);
        if (b != '[') {
            throw new JsonDeserializerException("array start not found, got : " + b);
        }
        b = nextFirstValuableByte(readBuffer, option, false);
        if(b == ']') {
            return Utils.emptyShortArray();
        }
        for (int i = 0; i < option.maxArrayElements(); i++) {
            short v = deserializeShort(readBuffer, option);
            if (index == r.length) {
                int newLength = Math.addExact(r.length, r.length);
                if (newLength > option.maxArrayElements()) {
                    throw new JsonDeserializerException("too many array elements, current length : " + r.length);
                }
                r = Arrays.copyOf(r, newLength);
            }
            r[index++] = v;
            b = nextFirstValuableByte(readBuffer, option, true);
            if (b == ']') {
                return index == r.length ? r : Arrays.copyOf(r, index);
            } else if (b != ',') {
                throw new JsonDeserializerException("sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, current length : " + r.length);
    }

    public static char[] deserializeCharArray(ReadBuffer readBuffer, JsonDeserializerOption option, JsonDeserializerContext context) {
        char[] r = new char[ARR_INITIAL_SIZE];
        int index = 0;
        byte b = nextFirstValuableByte(readBuffer, option, true);
        if (b != '[') {
            throw new JsonDeserializerException("array start not found, got : " + b);
        }
        b = nextFirstValuableByte(readBuffer, option, false);
        if(b == ']') {
            return Utils.emptyCharArray();
        }
        for (int i = 0; i < option.maxArrayElements(); i++) {
            char v = deserializeChar(readBuffer, option, context);
            if (index == r.length) {
                int newLength = Math.addExact(r.length, r.length);
                if (newLength > option.maxArrayElements()) {
                    throw new JsonDeserializerException("too many array elements, current length : " + r.length);
                }
                r = Arrays.copyOf(r, newLength);
            }
            r[index++] = v;
            b = nextFirstValuableByte(readBuffer, option, true);
            if (b == ']') {
                return index == r.length ? r : Arrays.copyOf(r, index);
            } else if (b != ',') {
                throw new JsonDeserializerException("sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, current length : " + r.length);
    }

    public static int[] deserializeIntArray(ReadBuffer readBuffer, JsonDeserializerOption option) {
        int[] r = new int[ARR_INITIAL_SIZE];
        int index = 0;
        byte b = nextFirstValuableByte(readBuffer, option, true);
        if (b != '[') {
            throw new JsonDeserializerException("array start not found, got : " + b);
        }
        b = nextFirstValuableByte(readBuffer, option, false);
        if(b == ']') {
            return Utils.emptyIntArray();
        }
        for (int i = 0; i < option.maxArrayElements(); i++) {
            int v = deserializeInt(readBuffer, option);
            if (index == r.length) {
                int newLength = Math.addExact(r.length, r.length);
                if (newLength > option.maxArrayElements()) {
                    throw new JsonDeserializerException("too many array elements, current length : " + r.length);
                }
                r = Arrays.copyOf(r, newLength);
            }
            r[index++] = v;
            b = nextFirstValuableByte(readBuffer, option, true);
            if (b == ']') {
                return index == r.length ? r : Arrays.copyOf(r, index);
            } else if (b != ',') {
                throw new JsonDeserializerException("sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, current length : " + r.length);
    }

    public static long[] deserializeLongArray(ReadBuffer readBuffer, JsonDeserializerOption option) {
        long[] r = new long[ARR_INITIAL_SIZE];
        int index = 0;
        byte b = nextFirstValuableByte(readBuffer, option, true);
        if (b != '[') {
            throw new JsonDeserializerException("array start not found, got : " + b);
        }
        b = nextFirstValuableByte(readBuffer, option, false);
        if(b == ']') {
            return Utils.emptyLongArray();
        }
        for (int i = 0; i < option.maxArrayElements(); i++) {
            long v = deserializeLong(readBuffer, option);
            if (index == r.length) {
                int newLength = Math.addExact(r.length, r.length);
                if (newLength > option.maxArrayElements()) {
                    throw new JsonDeserializerException("too many array elements, current length : " + r.length);
                }
                r = Arrays.copyOf(r, newLength);
            }
            r[index++] = v;
            b = nextFirstValuableByte(readBuffer, option, true);
            if (b == ']') {
                return index == r.length ? r : Arrays.copyOf(r, index);
            } else if (b != ',') {
                throw new JsonDeserializerException("sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, current length : " + r.length);
    }

    public static float[] deserializeFloatArray(ReadBuffer readBuffer, JsonDeserializerOption option) {
        float[] r = new float[ARR_INITIAL_SIZE];
        int index = 0;
        byte b = nextFirstValuableByte(readBuffer, option, true);
        if (b != '[') {
            throw new JsonDeserializerException("array start not found, got : " + b);
        }
        b = nextFirstValuableByte(readBuffer, option, false);
        if(b == ']') {
            return Utils.emptyFloatArray();
        }
        for (int i = 0; i < option.maxArrayElements(); i++) {
            float v = deserializeFloat(readBuffer, option);
            if (index == r.length) {
                int newLength = Math.addExact(r.length, r.length);
                if (newLength > option.maxArrayElements()) {
                    throw new JsonDeserializerException("too many array elements, current length : " + r.length);
                }
                r = Arrays.copyOf(r, newLength);
            }
            r[index++] = v;
            b = nextFirstValuableByte(readBuffer, option, true);
            if (b == ']') {
                return index == r.length ? r : Arrays.copyOf(r, index);
            } else if (b != ',') {
                throw new JsonDeserializerException("sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, current length : " + r.length);
    }

    public static double[] deserializeDoubleArray(ReadBuffer readBuffer, JsonDeserializerOption option) {
        double[] r = new double[ARR_INITIAL_SIZE];
        int index = 0;
        byte b = nextFirstValuableByte(readBuffer, option, true);
        if (b != '[') {
            throw new JsonDeserializerException("array start not found, got : " + b);
        }
        b = nextFirstValuableByte(readBuffer, option, false);
        if(b == ']') {
            return Utils.emptyDoubleArray();
        }
        for (int i = 0; i < option.maxArrayElements(); i++) {
            double v = deserializeDouble(readBuffer, option);
            if (index == r.length) {
                int newLength = Math.addExact(r.length, r.length);
                if (newLength > option.maxArrayElements()) {
                    throw new JsonDeserializerException("too many array elements, current length : " + r.length);
                }
                r = Arrays.copyOf(r, newLength);
            }
            r[index++] = v;
            b = nextFirstValuableByte(readBuffer, option, true);
            if (b == ']') {
                return index == r.length ? r : Arrays.copyOf(r, index);
            } else if (b != ',') {
                throw new JsonDeserializerException("sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, current length : " + r.length);
    }

    public static String deserializeString(ReadBuffer readBuffer, JsonDeserializerOption option, JsonDeserializerContext context) {
        byte b = nextFirstValuableByte(readBuffer, option, true);
        if (b != '"') {
            throw new JsonDeserializerException("missing first quote, got : " + b);
        }
        parseString(readBuffer, option, context);
        return context.asString();
    }

    public static void parseString(ReadBuffer readBuffer, JsonDeserializerOption option, JsonDeserializerContext context) {
        switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> parseHeapString(heapReadBuffer, option, context);
            case SegmentReadBuffer segmentReadBuffer -> parseSegmentString(segmentReadBuffer, option, context);
            default -> throw new AssertionError();
        }
    }

    private static void parseHeapString(HeapReadBuffer heapReadBuffer, JsonDeserializerOption option, JsonDeserializerContext context) {
        byte[] bytes = heapReadBuffer.rawByteArray();
        int position = heapReadBuffer.intPosition();
        int end = Math.min(bytes.length - position, option.maxStringBytes()) + position;
        int index = position, offset = position;
        while (index < end) {
            byte b = bytes[index++];
            if(b == '\\') {
                int len = index - offset - 1;
                if(len > 0) {
                    context.appendBytes(bytes, offset, len);
                }
                if(index == end) {
                    throw new JsonDeserializerException("illegal escape");
                }
                b = bytes[index++];
                switch (b) {
                    case '\"' -> context.appendByte((byte) '\"');
                    case '\\' -> context.appendByte((byte) '\\');
                    case '/' -> context.appendByte((byte) '/');
                    case 'b' -> context.appendByte((byte) '\b');
                    case 'f' -> context.appendByte((byte) '\f');
                    case 'n' -> context.appendByte((byte) '\n');
                    case 'r' -> context.appendByte((byte) '\r');
                    case 't' -> context.appendByte((byte) '\t');
                    case 'u' -> {
                        if(index + 4 > end) {
                            throw new JsonDeserializerException("illegal unicode");
                        }
                        int cp = parseUnicode(bytes, index);
                        index += 4;
                        if(!Character.isValidCodePoint(cp)) {
                            throw new JsonDeserializerException("illegal unicode codepoint : " + cp);
                        }
                        if(cp >= 0xDC00 && cp <= 0xDFFF) {
                            throw new JsonDeserializerException("illegal low surrogate : " + cp);
                        }
                        if(cp >= 0xD800 && cp <= 0xDBFF) {
                            if(index + 6 > end || bytes[index] != '\\' || bytes[index + 1] != 'u') {
                                throw new JsonDeserializerException("illegal high surrogate without low surrogate");
                            }
                            int lcp = parseUnicode(bytes, index + 2);
                            index += 6;
                            if(lcp < 0xDC00 || lcp > 0xDFFF) {
                                throw new JsonDeserializerException("illegal low surrogate : " + lcp);
                            }
                            cp = ((cp - 0xD800) << 10) + (lcp - 0xDC00) + 0x10000;
                        }
                        context.appendutf8CodePoint(cp);
                    }
                    default -> throw new JsonDeserializerException("illegal escaped byte : " + b);
                }
                offset = index;
            } else if(b == '"') {
                int len = index - offset - 1;
                if(len > 0) {
                    context.appendBytes(bytes, offset, len);
                }
                heapReadBuffer.setPosition(index);
                return ;
            } else if(b < (byte) 0x20){
                throw new JsonDeserializerException("illegal unescaped byte : " + b);
            }
        }
        throw new JsonDeserializerException("illegal json string");
    }

    private static void parseSegmentString(SegmentReadBuffer segmentReadBuffer, JsonDeserializerOption option, JsonDeserializerContext context) {
        MemorySegment segment = segmentReadBuffer.rawSegment();
        long position = segmentReadBuffer.longPosition();
        long end = Math.min(segment.byteSize() - position, option.maxStringBytes()) + position;
        long index = position, offset = position;
        while (index < end) {
            byte b = SegmentAccess.getByte(segment, index++);
            if(b == '\\') {
                int len = Math.toIntExact(index - offset - 1L);
                if(len > 0) {
                    context.appendSegment(segment, offset, len);
                }
                if(index == end) {
                    throw new JsonDeserializerException("illegal escape");
                }
                b = SegmentAccess.getByte(segment, index++);
                switch (b) {
                    case '\"' -> context.appendByte((byte) '\"');
                    case '\\' -> context.appendByte((byte) '\\');
                    case '/' -> context.appendByte((byte) '/');
                    case 'b' -> context.appendByte((byte) '\b');
                    case 'f' -> context.appendByte((byte) '\f');
                    case 'n' -> context.appendByte((byte) '\n');
                    case 'r' -> context.appendByte((byte) '\r');
                    case 't' -> context.appendByte((byte) '\t');
                    case 'u' -> {
                        if(index + 4L > end) {
                            throw new JsonDeserializerException("illegal unicode");
                        }
                        int cp = parseUnicode(segment, index);
                        index += 4L;
                        if(!Character.isValidCodePoint(cp)) {
                            throw new JsonDeserializerException("illegal unicode codepoint : " + cp);
                        }
                        if(cp >= 0xDC00 && cp <= 0xDFFF) {
                            throw new JsonDeserializerException("illegal low surrogate : " + cp);
                        }
                        if(cp >= 0xD800 && cp <= 0xDBFF) {
                            if(index + 6L > end || SegmentAccess.getByte(segment, index) != '\\' || SegmentAccess.getByte(segment, index + 1L) != 'u') {
                                throw new JsonDeserializerException("illegal high surrogate without low surrogate");
                            }
                            int lcp = parseUnicode(segment, index + 2L);
                            index += 6L;
                            if(lcp < 0xDC00 || lcp > 0xDFFF) {
                                throw new JsonDeserializerException("illegal low surrogate : " + lcp);
                            }
                            cp = ((cp - 0xD800) << 10) + (lcp - 0xDC00) + 0x10000;
                        }
                        context.appendutf8CodePoint(cp);
                    }
                    default -> throw new JsonDeserializerException("illegal escaped byte : " + b);
                }
                offset = index;
            } else if(b == '"') {
                int len = Math.toIntExact(index - offset - 1L);
                if(len > 0) {
                    context.appendSegment(segment, offset, len);
                }
                segmentReadBuffer.setPosition(index);
                return ;
            } else if(b < (byte) 0x20){
                throw new JsonDeserializerException("illegal unescaped byte : " + b);
            }
        }
        throw new JsonDeserializerException("illegal json string");
    }

    private static int parseUnicode(byte[] bytes, int index) {
        int i1 = parseHex(bytes[index]) << 12;
        int i2 = parseHex(bytes[index + 1]) << 8;
        int i3 = parseHex(bytes[index + 2]) << 4;
        int i4 = parseHex(bytes[index + 3]);
        return i1 | i2 | i3 | i4;
    }

    private static int parseUnicode(MemorySegment segment, long index) {
        int i1 = parseHex(SegmentAccess.getByte(segment, index)) << 12;
        int i2 = parseHex(SegmentAccess.getByte(segment, index + 1)) << 8;
        int i3 = parseHex(SegmentAccess.getByte(segment, index + 2)) << 4;
        int i4 = parseHex(SegmentAccess.getByte(segment, index + 3));
        return i1 | i2 | i3 | i4;
    }

    private static int parseHex(byte b) {
        if ((b >= '0' && b <= '9')) {
            return b - '0';
        } else if((b >= 'a' && b <= 'f')) {
            return b - 'a' + 10;
        } else if((b >= 'A' && b <= 'F')) {
            return b - 'A' + 10;
        } else {
            throw new JsonDeserializerException("illegal hex character: " + b);
        }
    }

    // copied from guava, with vector optimization for ascii fast path
    public static boolean validateUtf8(byte[] bytes, int offset, int length) {
        Objects.checkFromIndexSize(offset, length, bytes.length);
        int end = offset + length;
        int upperBound = ByteVector.SPECIES_PREFERRED.loopBound(end);
        for(; offset < upperBound; offset += ByteVector.SPECIES_PREFERRED.length()) {
            ByteVector byteVector = ByteVector.fromArray(ByteVector.SPECIES_PREFERRED, bytes, offset);
            long l = byteVector.lt((byte) 0).toLong();
            if(l != 0) {
                offset += Long.numberOfTrailingZeros(l);
                break ;
            }
        }
        int b1, b2;
        for( ; ; ) {
            do {
                if (offset >= end) {
                    return true;
                }
            } while ((b1 = bytes[offset++]) >= 0);
            if (b1 < (byte) 0xE0) {
                if (offset == end) {
                    return false;
                }
                if (b1 < (byte) 0xC2 || bytes[offset++] > (byte) 0xBF) {
                    return false;
                }
            } else if (b1 < (byte) 0xF0) {
                if (offset + 1 >= end) {
                    return false;
                }
                b2 = bytes[offset++];
                if (b2 > (byte) 0xBF
                        || (b1 == (byte) 0xE0 && b2 < (byte) 0xA0)
                        || (b1 == (byte) 0xED && b2 >= (byte) 0xA0)
                        || bytes[offset++] > (byte) 0xBF) {
                    return false;
                }
            } else {
                if (offset + 2 >= end) {
                    return false;
                }
                b2 = bytes[offset++];
                if (b2 > (byte) 0xBF
                        || (((b1 << 28) + (b2 - (byte) 0x90)) >> 30) != 0
                        || bytes[offset++] > (byte) 0xBF
                        || bytes[offset++] > (byte) 0xBF) {
                    return false;
                }
            }
        }
    }
}