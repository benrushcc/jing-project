package io.jingproject.marshalljson;

import io.jingproject.common.*;
import jdk.incubator.vector.ByteVector;

import java.lang.foreign.MemorySegment;
import java.lang.reflect.Array;
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
    private static final int COMPACT_NULL = Utils.compact(Utils.compact((byte) 'n', (byte) 'u'), Utils.compact((byte) 'l', (byte) 'l'));
    private static final int ARR_INITIAL_SIZE = 4;

    private JsonDeserializeUtil() {
        throw new UnsupportedOperationException("utility class");
    }

    public static byte nextFirstValuableByte(ReadBuffer readBuffer, JsonDeserializerOption option) {
        int maxEmptyBytes = option.maxEmptyBytes();
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> nextHeapFirstValuableByte(heapReadBuffer, maxEmptyBytes);
            case SegmentReadBuffer segmentReadBuffer -> nextSegmentFirstValuableByte(segmentReadBuffer, maxEmptyBytes);
        };
    }

    private static byte nextHeapFirstValuableByte(HeapReadBuffer heapReadBuffer, int maxEmptyBytes) {
        final byte[] bytes = heapReadBuffer.rawByteArray();
        final int position = heapReadBuffer.intPosition();
        final int range = Math.min(maxEmptyBytes, heapReadBuffer.intLength() - position);
        for (int i = position; i < position + range; i++) {
            byte b = bytes[i];
            switch (b) {
                case BYTE_space, BYTE_lf, BYTE_cr, BYTE_ht -> {
                }
                default -> {
                    heapReadBuffer.setPosition(i + 1); // no overflow
                    return b;
                }
            }
        }
        throw new JsonDeserializerException("too many empty bytes");
    }

    private static byte nextSegmentFirstValuableByte(SegmentReadBuffer segmentReadBuffer, int maxEmptyBytes) {
        final MemorySegment segment = segmentReadBuffer.rawSegment();
        final long position = segmentReadBuffer.longPosition();
        final long range = Math.min(maxEmptyBytes, segment.byteSize() - position);
        for (long i = position; i < position + range; i++) {
            byte b = SegmentAccess.getByte(segment, i);
            switch (b) {
                case BYTE_space, BYTE_lf, BYTE_cr, BYTE_ht -> {
                }
                default -> {
                    segmentReadBuffer.setPosition(i + 1L); // no overflow
                    return b;
                }
            }
        }
        throw new JsonDeserializerException("too many empty bytes");
    }

    public static boolean deserializeNull(ReadBuffer readBuffer, byte firstByte) {
        if(firstByte == (byte) 'n') {
            switch (readBuffer) {
                case HeapReadBuffer heapReadBuffer -> {
                    final byte[] bytes = heapReadBuffer.rawByteArray();
                    final int position = heapReadBuffer.intPosition();
                    final int newPosition = Math.addExact(position, 3);
                    if(newPosition > bytes.length) {
                        throw new JsonDeserializerException("eof reached while deserializing null");
                    }
                    if(ArrayAccess.getInt(bytes, position - 1) != COMPACT_NULL) {
                        throw new JsonDeserializerException("illegal null token, position : " + position);
                    }
                    readBuffer.setPosition(newPosition);
                    return true;
                }
                case SegmentReadBuffer segmentReadBuffer -> {
                    final MemorySegment segment = segmentReadBuffer.rawSegment();
                    final long position = segmentReadBuffer.longPosition();
                    final long newPosition = Math.addExact(position, 3L);
                    if(newPosition > segment.byteSize()) {
                        throw new JsonDeserializerException("eof reached while deserializing null");
                    }
                    if(SegmentAccess.getInt(segment, position - 1L) != COMPACT_NULL) {
                        throw new JsonDeserializerException("illegal null token, position : " + position);
                    }
                    readBuffer.setPosition(newPosition);
                    return true;
                }
                case null, default -> throw new AssertionError();
            }
        }
        return false;
    }

    public static byte deserializeByte(ReadBuffer readBuffer, byte firstByte) {
        int v = JsonNumberUtil.readInt(readBuffer, firstByte);
        if(v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
            return (byte) v;
        }
        throw new JsonDeserializerException("byte value overflow : " + v);
    }

    public static boolean deserializeBoolean(ReadBuffer readBuffer, byte firstByte) {
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> {
                final byte[] bytes = heapReadBuffer.rawByteArray();
                final int position = heapReadBuffer.intPosition();
                if(firstByte == 't') {
                    final int newPosition = Math.addExact(position, 3);
                    if(newPosition > bytes.length) {
                        throw new JsonDeserializerException("eof reached while deserializing boolean literal 'true' value");
                    }
                    if (ArrayAccess.getInt(bytes, position - 1) != COMPACT_TRUE) {
                        throw new JsonDeserializerException("illegal boolean literal 'true' value");
                    }
                    heapReadBuffer.setPosition(newPosition);
                    yield true;
                } else if(firstByte == 'f') {
                    final int newPosition = Math.addExact(position, 4);
                    if(newPosition > bytes.length) {
                        throw new JsonDeserializerException("eof reached while deserializing boolean literal 'false' value");
                    }
                    if (ArrayAccess.getInt(bytes, position) != COMPACT_ALSE) {
                        throw new JsonDeserializerException("illegal boolean literal 'false' value");
                    }
                    heapReadBuffer.setPosition(newPosition);
                    yield false;
                } else {
                    throw new JsonDeserializerException("illegal boolean literal value");
                }
            }
            case SegmentReadBuffer segmentReadBuffer -> {
                final MemorySegment segment = segmentReadBuffer.rawSegment();
                final long position = segmentReadBuffer.longPosition();
                if(firstByte == 't') {
                    final long newPosition = Math.addExact(position, 3L);
                    if(newPosition > segment.byteSize()) {
                        throw new JsonDeserializerException("eof reached while deserializing boolean literal 'true' value");
                    }
                    if (SegmentAccess.getInt(segment, position - 1L) != COMPACT_TRUE) {
                        throw new JsonDeserializerException("illegal boolean literal 'true' value");
                    }
                    segmentReadBuffer.setPosition(newPosition);
                    yield true;
                } else if(firstByte == 'f') {
                    final long newPosition = Math.addExact(position, 4L);
                    if(newPosition > segment.byteSize()) {
                        throw new JsonDeserializerException("eof reached while deserializing boolean literal 'false' value");
                    }
                    if (SegmentAccess.getInt(segment, position) != COMPACT_ALSE) {
                        throw new JsonDeserializerException("illegal boolean literal 'false' value");
                    }
                    segmentReadBuffer.setPosition(newPosition);
                    yield false;
                } else {
                    throw new JsonDeserializerException("illegal boolean literal value");
                }
            }
        };
    }

    public static short deserializeShort(ReadBuffer readBuffer, byte firstByte) {
        int v = JsonNumberUtil.readInt(readBuffer, firstByte);
        if(v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
            return (short) v;
        }
        throw new JsonDeserializerException("short value overflow : " + v);
    }

    public static char deserializeChar(ReadBuffer readBuffer, JsonDeserializerOption option, JsonDeserializerContext context, byte firstByte) {
        if (firstByte != '"') {
            throw new JsonDeserializerException("missing first quote, got : " + firstByte + " at index : " + readBuffer.intPosition());
        }
        parseString(readBuffer, option, context);
        return context.asChar();
    }

    public static int deserializeInt(ReadBuffer readBuffer, byte firstByte) {
        return JsonNumberUtil.readInt(readBuffer, firstByte);
    }

    public static long deserializeLong(ReadBuffer readBuffer, byte firstByte) {
        return JsonNumberUtil.readLong(readBuffer, firstByte);
    }

    public static float deserializeFloat(ReadBuffer readBuffer, byte firstByte) {
        return JsonNumberUtil.readFloat(readBuffer, firstByte);
    }

    public static double deserializeDouble(ReadBuffer readBuffer, byte firstByte) {
        return JsonNumberUtil.readDouble(readBuffer, firstByte);
    }

    public static byte[] deserializeByteArray(ReadBuffer readBuffer, JsonDeserializerOption option) {
        byte[] r = new byte[ARR_INITIAL_SIZE];
        byte b = nextFirstValuableByte(readBuffer, option);
        if (b != '[') {
            throw new JsonDeserializerException("array start not found, got : " + b);
        }
        b = nextFirstValuableByte(readBuffer, option);
        if(b == ']') {
            return Utils.emptyByteArray();
        }
        for (int index = 0; index < option.maxArrayElements(); ) {
            byte v = deserializeByte(readBuffer, b);
            if (index == r.length) {
                int newLength = Math.min(r.length << 1, option.maxArrayElements()); // no overflow
                r = Arrays.copyOf(r, newLength);
            }
            r[index++] = v;
            b = nextFirstValuableByte(readBuffer, option);
            if (b == ']') {
                return index == r.length ? r : Arrays.copyOf(r, index); // no overflow
            } else if (b == ',') {
                b = nextFirstValuableByte(readBuffer, option);
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + option.maxArrayElements());
    }

    public static boolean[] deserializeBooleanArray(ReadBuffer readBuffer, JsonDeserializerOption option) {
        boolean[] r = new boolean[ARR_INITIAL_SIZE];
        byte b = nextFirstValuableByte(readBuffer, option);
        if (b != '[') {
            throw new JsonDeserializerException("array start not found, got : " + b);
        }
        b = nextFirstValuableByte(readBuffer, option);
        if (b == ']') {
            return Utils.emptyBooleanArray();
        }
        for (int index = 0; index < option.maxArrayElements(); ) {
            boolean v = deserializeBoolean(readBuffer, b);
            if (index == r.length) {
                int newLength = Math.min(r.length << 1, option.maxArrayElements());
                r = Arrays.copyOf(r, newLength);
            }
            r[index++] = v;
            b = nextFirstValuableByte(readBuffer, option);
            if (b == ']') {
                return Arrays.copyOf(r, index);
            } else if (b == ',') {
                b = nextFirstValuableByte(readBuffer, option);
            } else {
                throw new JsonDeserializerException("array separator expected, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + option.maxArrayElements());
    }

    public static short[] deserializeShortArray(ReadBuffer readBuffer, JsonDeserializerOption option) {
        short[] r = new short[ARR_INITIAL_SIZE];
        byte b = nextFirstValuableByte(readBuffer, option);
        if (b != '[') {
            throw new JsonDeserializerException("array start not found, got : " + b);
        }
        b = nextFirstValuableByte(readBuffer, option);
        if (b == ']') {
            return Utils.emptyShortArray();
        }
        for (int index = 0; index < option.maxArrayElements(); ) {
            short v = deserializeShort(readBuffer, b);
            if (index == r.length) {
                int newLength = Math.min(r.length << 1, option.maxArrayElements());
                r = Arrays.copyOf(r, newLength);
            }
            r[index++] = v;
            b = nextFirstValuableByte(readBuffer, option);
            if (b == ']') {
                return Arrays.copyOf(r, index);
            } else if (b == ',') {
                b = nextFirstValuableByte(readBuffer, option);
            } else {
                throw new JsonDeserializerException("array separator expected, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + option.maxArrayElements());
    }

    public static char[] deserializeCharArray(ReadBuffer readBuffer, JsonDeserializerOption option, JsonDeserializerContext context) {
        char[] r = new char[ARR_INITIAL_SIZE];
        byte b = nextFirstValuableByte(readBuffer, option);
        if (b != '[') {
            throw new JsonDeserializerException("array start not found, got : " + b);
        }
        b = nextFirstValuableByte(readBuffer, option);
        if (b == ']') {
            return Utils.emptyCharArray();
        }
        for (int index = 0; index < option.maxArrayElements(); ) {
            char v = deserializeChar(readBuffer, option, context, b);
            if (index == r.length) {
                int newLength = Math.min(r.length << 1, option.maxArrayElements());
                r = Arrays.copyOf(r, newLength);
            }
            r[index++] = v;
            b = nextFirstValuableByte(readBuffer, option);
            if (b == ']') {
                return Arrays.copyOf(r, index);
            } else if (b == ',') {
                b = nextFirstValuableByte(readBuffer, option);
            } else {
                throw new JsonDeserializerException("array separator expected, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + option.maxArrayElements());
    }

    public static int[] deserializeIntArray(ReadBuffer readBuffer, JsonDeserializerOption option) {
        int[] r = new int[ARR_INITIAL_SIZE];
        byte b = nextFirstValuableByte(readBuffer, option);
        if (b != '[') {
            throw new JsonDeserializerException("array start not found, got : " + b);
        }
        b = nextFirstValuableByte(readBuffer, option);
        if (b == ']') {
            return Utils.emptyIntArray();
        }
        for (int index = 0; index < option.maxArrayElements(); ) {
            int v = deserializeInt(readBuffer, b);
            if (index == r.length) {
                int newLength = Math.min(r.length << 1, option.maxArrayElements());
                r = Arrays.copyOf(r, newLength);
            }
            r[index++] = v;
            b = nextFirstValuableByte(readBuffer, option);
            if (b == ']') {
                return Arrays.copyOf(r, index);
            } else if (b == ',') {
                b = nextFirstValuableByte(readBuffer, option);
            } else {
                throw new JsonDeserializerException("array separator expected, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + option.maxArrayElements());
    }

    public static long[] deserializeLongArray(ReadBuffer readBuffer, JsonDeserializerOption option) {
        long[] r = new long[ARR_INITIAL_SIZE];
        byte b = nextFirstValuableByte(readBuffer, option);
        if (b != '[') {
            throw new JsonDeserializerException("array start not found, got : " + b);
        }
        b = nextFirstValuableByte(readBuffer, option);
        if (b == ']') {
            return Utils.emptyLongArray();
        }
        for (int index = 0; index < option.maxArrayElements(); ) {
            long v = deserializeLong(readBuffer, b);
            if (index == r.length) {
                int newLength = Math.min(r.length << 1, option.maxArrayElements());
                r = Arrays.copyOf(r, newLength);
            }
            r[index++] = v;
            b = nextFirstValuableByte(readBuffer, option);
            if (b == ']') {
                return Arrays.copyOf(r, index);
            } else if (b == ',') {
                b = nextFirstValuableByte(readBuffer, option);
            } else {
                throw new JsonDeserializerException("array separator expected, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + option.maxArrayElements());
    }

    public static float[] deserializeFloatArray(ReadBuffer readBuffer, JsonDeserializerOption option) {
        float[] r = new float[ARR_INITIAL_SIZE];
        byte b = nextFirstValuableByte(readBuffer, option);
        if (b != '[') {
            throw new JsonDeserializerException("array start not found, got : " + b);
        }
        b = nextFirstValuableByte(readBuffer, option);
        if (b == ']') {
            return Utils.emptyFloatArray();
        }
        for (int index = 0; index < option.maxArrayElements(); ) {
            float v = deserializeFloat(readBuffer, b);
            if (index == r.length) {
                int newLength = Math.min(r.length << 1, option.maxArrayElements());
                r = Arrays.copyOf(r, newLength);
            }
            r[index++] = v;
            b = nextFirstValuableByte(readBuffer, option);
            if (b == ']') {
                return Arrays.copyOf(r, index);
            } else if (b == ',') {
                b = nextFirstValuableByte(readBuffer, option);
            } else {
                throw new JsonDeserializerException("array separator expected, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + option.maxArrayElements());
    }

    public static double[] deserializeDoubleArray(ReadBuffer readBuffer, JsonDeserializerOption option) {
        double[] r = new double[ARR_INITIAL_SIZE];
        byte b = nextFirstValuableByte(readBuffer, option);
        if (b != '[') {
            throw new JsonDeserializerException("array start not found, got : " + b);
        }
        b = nextFirstValuableByte(readBuffer, option);
        if (b == ']') {
            return Utils.emptyDoubleArray();
        }
        for (int index = 0; index < option.maxArrayElements(); ) {
            double v = deserializeDouble(readBuffer, b);
            if (index == r.length) {
                int newLength = Math.min(r.length << 1, option.maxArrayElements());
                r = Arrays.copyOf(r, newLength);
            }
            r[index++] = v;
            b = nextFirstValuableByte(readBuffer, option);
            if (b == ']') {
                return Arrays.copyOf(r, index);
            } else if (b == ',') {
                b = nextFirstValuableByte(readBuffer, option);
            } else {
                throw new JsonDeserializerException("array separator expected, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + option.maxArrayElements());
    }

    @FunctionalInterface
    interface ObjectDeserializer<T> {
        T deserialize(ReadBuffer readBuffer, JsonDeserializerOption option, JsonDeserializerContext context, byte firstByte);
    }

    private static <T> T[] deserializeObjectArray(ReadBuffer readBuffer, JsonDeserializerOption option, Class<T> componentType, ObjectDeserializer<T> deserializer) {
        return deserializeObjectArray(readBuffer, option, null, componentType, deserializer);
    }

    @SuppressWarnings("unchecked")
    private static <T> T[] deserializeObjectArray(ReadBuffer readBuffer, JsonDeserializerOption option, JsonDeserializerContext context, Class<T> componentType, ObjectDeserializer<T> deserializer) {
        T[] r = (T[]) Array.newInstance(componentType, ARR_INITIAL_SIZE);
        byte b = nextFirstValuableByte(readBuffer, option);
        if (b != '[') {
            throw new JsonDeserializerException("array start not found, got : " + b);
        }
        b = nextFirstValuableByte(readBuffer, option);
        if(b == ']') {
            return (T[]) Utils.emptyObjectArray();
        }
        for (int index = 0; index < option.maxArrayElements(); ) {
            T v = deserializeNull(readBuffer, b) ? null : deserializer.deserialize(readBuffer, option, context, b);
            if (index == r.length) {
                int newLength = Math.min(r.length << 1, option.maxArrayElements()); // no overflow
                r = Arrays.copyOf(r, newLength);
            }
            r[index++] = v;
            b = nextFirstValuableByte(readBuffer, option);
            if (b == ']') {
                return index == r.length ? r : Arrays.copyOf(r, index); // no overflow
            } else if (b == ',') {
                b = nextFirstValuableByte(readBuffer, option);
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + option.maxArrayElements());
    }

    public static Byte[] deserializeByteWrapperArray(ReadBuffer readBuffer, JsonDeserializerOption option) {
        return deserializeObjectArray(readBuffer, option, Byte.class, (r, _, _, b) -> deserializeByte(r, b));
    }

    public static Boolean[] deserializeBooleanWrapperArray(ReadBuffer readBuffer, JsonDeserializerOption option) {
        return deserializeObjectArray(readBuffer, option, Boolean.class, (r, _, _, b) -> deserializeBoolean(r, b));
    }

    public static Short[] deserializeShortWrapperArray(ReadBuffer readBuffer, JsonDeserializerOption option) {
        return deserializeObjectArray(readBuffer, option, Short.class, (r, _, _, b) -> deserializeShort(r, b));
    }

    public static Character[] deserializeCharacterWrapperArray(ReadBuffer readBuffer, JsonDeserializerOption option, JsonDeserializerContext context) {
        return deserializeObjectArray(readBuffer, option, Character.class, JsonDeserializeUtil::deserializeChar);
    }

    public static Integer[] deserializeIntegerWrapperArray(ReadBuffer readBuffer, JsonDeserializerOption option) {
        return deserializeObjectArray(readBuffer, option, Integer.class, (r, _, _, b) -> deserializeInt(r, b));
    }

    public static Long[] deserializeLongWrapperArray(ReadBuffer readBuffer, JsonDeserializerOption option) {
        return deserializeObjectArray(readBuffer, option, Long.class, (r, _, _, b) -> deserializeLong(r, b));
    }

    public static Float[] deserializeFloatWrapperArray(ReadBuffer readBuffer, JsonDeserializerOption option) {
        return deserializeObjectArray(readBuffer, option, Float.class, (r, _, _, b) -> deserializeFloat(r, b));
    }

    public static Double[] deserializeDoubleWrapperArray(ReadBuffer readBuffer, JsonDeserializerOption option) {
        return deserializeObjectArray(readBuffer, option, Double.class, (r, _, _, b) -> deserializeDouble(r, b));
    }


    public static String deserializeString(ReadBuffer readBuffer, JsonDeserializerOption option, JsonDeserializerContext context) {
        byte b = nextFirstValuableByte(readBuffer, option);
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