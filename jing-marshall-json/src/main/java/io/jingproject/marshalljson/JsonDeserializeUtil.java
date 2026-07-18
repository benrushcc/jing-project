package io.jingproject.marshalljson;

import io.jingproject.common.*;
import jdk.incubator.vector.ByteVector;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Objects;

// all deserialize* methods assume firstByte is already validated externally, and it's not null
public final class JsonDeserializeUtil {
    private static final int COMPACT_TRUE = Utils.compact(Utils.compact((byte) 't', (byte) 'r'), Utils.compact((byte) 'u', (byte) 'e'));
    private static final int COMPACT_ALSE = Utils.compact(Utils.compact((byte) 'a', (byte) 'l'), Utils.compact((byte) 's', (byte) 'e'));
    private static final int COMPACT_NULL = Utils.compact(Utils.compact((byte) 'n', (byte) 'u'), Utils.compact((byte) 'l', (byte) 'l'));
    private static final int ARR_INITIAL_SIZE = 4;
    private static final Object DUMMY = new Object();

    private JsonDeserializeUtil() {
        throw new UnsupportedOperationException("utility class");
    }

    public static Object dummyValue() {
        return DUMMY;
    }

    public static boolean validateJsonNonnullValueStart(byte firstByte) {
        return switch (firstByte) {
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', ' ', 't', 'f' -> true; // 14 cases
            default -> false;
        };
    }

    public static boolean validateJsonNullStart(byte firstByte) {
        return firstByte == (byte) 'n';
    }

    public static boolean validateJsonBoolStart(byte firstByte) {
        return firstByte == (byte) 't' || firstByte == (byte) 'f';
    }

    public static boolean validateJsonNumberStart(byte firstByte) {
        return JsonNumberUtil.validateNumberStart(firstByte);
    }

    public static boolean validateJsonStringStart(byte firstByte) {
        return firstByte == (byte) '"';
    }

    public static boolean validateJsonArrayStart(byte firstByte) {
        return firstByte == (byte) '[';
    }

    public static boolean validateJsonObjectStart(byte firstByte) {
        return firstByte == (byte) '{';
    }

    public static byte nextFirstValuableByte(JsonDeserializerOption option, ReadBuffer readBuffer) {
        assert option != null && readBuffer != null;
        final int maxEmptyBytes = option.maxEmptyBytes();
        switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> {
                final byte[] bytes = heapReadBuffer.rawByteArray();
                final int position = heapReadBuffer.intPosition();
                final int end = position + Math.min(bytes.length - position, maxEmptyBytes); // no overflow
                for (int i = position; i < end; i++) {
                    byte b = bytes[i];
                    switch (b) {
                        case (byte) ' ', (byte) '\n', (byte) '\r', (byte) '\t' -> {
                        }
                        default -> {
                            heapReadBuffer.setPosition(i + 1); // no overflow
                            return b;
                        }
                    }
                }
            }
            case SegmentReadBuffer segmentReadBuffer -> {
                final MemorySegment segment = segmentReadBuffer.rawSegment();
                final long position = segmentReadBuffer.longPosition();
                final long end = position + Math.min(segment.byteSize() - position, maxEmptyBytes); // no overflow
                for (long i = position; i < end; i++) {
                    byte b = SegmentAccess.getByte(segment, i);
                    switch (b) {
                        case (byte) ' ', (byte) '\n', (byte) '\r', (byte) '\t' -> {
                        }
                        default -> {
                            segmentReadBuffer.setPosition(i + 1L); // no overflow
                            return b;
                        }
                    }
                }
            }
        }
        throw new JsonDeserializerException("too many empty bytes");
    }

    public static void deserializeNull(ReadBuffer readBuffer, byte firstByte) {
        assert readBuffer != null && validateJsonNullStart(firstByte);
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
            }
        }
    }

    public static byte deserializeByte(ReadBuffer readBuffer, byte firstByte) {
        assert readBuffer != null && validateJsonNumberStart(firstByte);
        int v = JsonNumberUtil.readInt(readBuffer, firstByte);
        if(v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
            return (byte) v;
        }
        throw new JsonDeserializerException("byte value overflow : " + v);
    }

    public static boolean deserializeBoolean(ReadBuffer readBuffer, byte firstByte) {
        assert readBuffer != null && validateJsonBoolStart(firstByte);
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
                } else {
                    final int newPosition = Math.addExact(position, 4);
                    if(newPosition > bytes.length) {
                        throw new JsonDeserializerException("eof reached while deserializing boolean literal 'false' value");
                    }
                    if (ArrayAccess.getInt(bytes, position) != COMPACT_ALSE) {
                        throw new JsonDeserializerException("illegal boolean literal 'false' value");
                    }
                    heapReadBuffer.setPosition(newPosition);
                    yield false;
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
                } else {
                    final long newPosition = Math.addExact(position, 4L);
                    if(newPosition > segment.byteSize()) {
                        throw new JsonDeserializerException("eof reached while deserializing boolean literal 'false' value");
                    }
                    if (SegmentAccess.getInt(segment, position) != COMPACT_ALSE) {
                        throw new JsonDeserializerException("illegal boolean literal 'false' value");
                    }
                    segmentReadBuffer.setPosition(newPosition);
                    yield false;
                }
            }
        };
    }

    public static short deserializeShort(ReadBuffer readBuffer, byte firstByte) {
        assert readBuffer != null && validateJsonNumberStart(firstByte);
        int v = JsonNumberUtil.readInt(readBuffer, firstByte);
        if(v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
            return (short) v;
        }
        throw new JsonDeserializerException("short value overflow : " + v);
    }

    public static char deserializeChar(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, byte firstByte) {
        assert option != null && readBuffer != null && validateJsonStringStart(firstByte);
        parseString(option, readBuffer, context,firstByte);
        return context.asSingleChar();
    }

    public static int deserializeInt(ReadBuffer readBuffer, byte firstByte) {
        assert readBuffer != null && validateJsonNumberStart(firstByte);
        return JsonNumberUtil.readInt(readBuffer, firstByte);
    }

    public static long deserializeLong(ReadBuffer readBuffer, byte firstByte) {
        assert readBuffer != null && validateJsonNumberStart(firstByte);
        return JsonNumberUtil.readLong(readBuffer, firstByte);
    }

    public static float deserializeFloat(JsonDeserializerOption option, ReadBuffer readBuffer, byte firstByte) {
        assert readBuffer != null && validateJsonNumberStart(firstByte);
        return JsonNumberUtil.readFloat(readBuffer, option.maxNumberBytes(), firstByte);
    }

    public static double deserializeDouble(JsonDeserializerOption option, ReadBuffer readBuffer, byte firstByte) {
        assert readBuffer != null && validateJsonNumberStart(firstByte);
        return JsonNumberUtil.readDouble(readBuffer, option.maxNumberBytes(), firstByte);
    }

    public static byte[] deserializeByteArray(JsonDeserializerOption option, ReadBuffer readBuffer, byte firstByte) {
        assert option != null && readBuffer != null && validateJsonArrayStart(firstByte);
        byte b = nextFirstValuableByte(option, readBuffer);
        if(b == ']') {
            return Utils.emptyByteArray();
        }
        byte[] r = new byte[ARR_INITIAL_SIZE];
        for (int index = 0; index < option.maxArrayElements(); ) {
            if(!validateJsonNumberStart(b)) {
                throw new JsonDeserializerException("not a number start : " + b);
            }
            byte v = deserializeByte(readBuffer, b);
            if (index == r.length) {
                int newLength = Math.min(r.length << 1, option.maxArrayElements()); // no overflow
                r = Arrays.copyOf(r, newLength);
            }
            r[index++] = v;
            b = nextFirstValuableByte(option, readBuffer);
            if (b == ']') {
                return index == r.length ? r : Arrays.copyOf(r, index); // no overflow
            } else if (b == ',') {
                b = nextFirstValuableByte(option, readBuffer);
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + option.maxArrayElements());
    }

    public static boolean[] deserializeBooleanArray(JsonDeserializerOption option, ReadBuffer readBuffer, byte firstByte) {
        assert option != null && readBuffer != null && validateJsonArrayStart(firstByte);
        byte b = nextFirstValuableByte(option, readBuffer);
        if (b == ']') {
            return Utils.emptyBooleanArray();
        }
        boolean[] r = new boolean[ARR_INITIAL_SIZE];
        for (int index = 0; index < option.maxArrayElements(); ) {
            if(b != (byte) 't' && b != (byte) 'f') {
                throw new JsonDeserializerException("not a bool start : " + b);
            }
            boolean v = deserializeBoolean(readBuffer, b);
            if (index == r.length) {
                int newLength = Math.min(r.length << 1, option.maxArrayElements());
                r = Arrays.copyOf(r, newLength);
            }
            r[index++] = v;
            b = nextFirstValuableByte(option, readBuffer);
            if (b == ']') {
                return Arrays.copyOf(r, index);
            } else if (b == ',') {
                b = nextFirstValuableByte(option, readBuffer);
            } else {
                throw new JsonDeserializerException("array separator expected, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + option.maxArrayElements());
    }

    public static short[] deserializeShortArray(JsonDeserializerOption option, ReadBuffer readBuffer, byte firstByte) {
        assert option != null && readBuffer != null && validateJsonArrayStart(firstByte);
        byte b = nextFirstValuableByte(option, readBuffer);
        if (b == ']') {
            return Utils.emptyShortArray();
        }
        short[] r = new short[ARR_INITIAL_SIZE];
        for (int index = 0; index < option.maxArrayElements(); ) {
            if(!validateJsonNumberStart(b)) {
                throw new JsonDeserializerException("not a number start : " + b);
            }
            short v = deserializeShort(readBuffer, b);
            if (index == r.length) {
                int newLength = Math.min(r.length << 1, option.maxArrayElements());
                r = Arrays.copyOf(r, newLength);
            }
            r[index++] = v;
            b = nextFirstValuableByte(option, readBuffer);
            if (b == ']') {
                return Arrays.copyOf(r, index);
            } else if (b == ',') {
                b = nextFirstValuableByte(option, readBuffer);
            } else {
                throw new JsonDeserializerException("array separator expected, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + option.maxArrayElements());
    }

    public static char[] deserializeCharArray(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, byte firstByte) {
        assert option != null && readBuffer != null && context != null && validateJsonArrayStart(firstByte);
        byte b = nextFirstValuableByte(option, readBuffer);
        if (b == ']') {
            return Utils.emptyCharArray();
        }
        char[] r = new char[ARR_INITIAL_SIZE];
        for (int index = 0; index < option.maxArrayElements(); ) {
            if(b != (byte) '"') {
                throw new JsonDeserializerException("not a string start : " + b);
            }
            char v = deserializeChar(option, readBuffer, context, b);
            if (index == r.length) {
                int newLength = Math.min(r.length << 1, option.maxArrayElements());
                r = Arrays.copyOf(r, newLength);
            }
            r[index++] = v;
            b = nextFirstValuableByte(option, readBuffer);
            if (b == ']') {
                return Arrays.copyOf(r, index);
            } else if (b == ',') {
                b = nextFirstValuableByte(option, readBuffer);
            } else {
                throw new JsonDeserializerException("array separator expected, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + option.maxArrayElements());
    }

    public static int[] deserializeIntArray(JsonDeserializerOption option, ReadBuffer readBuffer, byte firstByte) {
        assert option != null && readBuffer != null && validateJsonArrayStart(firstByte);
        byte b = nextFirstValuableByte(option, readBuffer);
        if (b == ']') {
            return Utils.emptyIntArray();
        }
        int[] r = new int[ARR_INITIAL_SIZE];
        for (int index = 0; index < option.maxArrayElements(); ) {
            if(!validateJsonNumberStart(b)) {
                throw new JsonDeserializerException("not a number start : " + b);
            }
            int v = deserializeInt(readBuffer, b);
            if (index == r.length) {
                int newLength = Math.min(r.length << 1, option.maxArrayElements());
                r = Arrays.copyOf(r, newLength);
            }
            r[index++] = v;
            b = nextFirstValuableByte(option, readBuffer);
            if (b == ']') {
                return Arrays.copyOf(r, index);
            } else if (b == ',') {
                b = nextFirstValuableByte(option, readBuffer);
            } else {
                throw new JsonDeserializerException("array separator expected, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + option.maxArrayElements());
    }

    public static long[] deserializeLongArray(JsonDeserializerOption option, ReadBuffer readBuffer, byte firstByte) {
        assert option != null && readBuffer != null && validateJsonArrayStart(firstByte);
        byte b = nextFirstValuableByte(option, readBuffer);
        if (b == ']') {
            return Utils.emptyLongArray();
        }
        long[] r = new long[ARR_INITIAL_SIZE];
        for (int index = 0; index < option.maxArrayElements(); ) {
            if(!validateJsonNumberStart(b)) {
                throw new JsonDeserializerException("not a number start : " + b);
            }
            long v = deserializeLong(readBuffer, b);
            if (index == r.length) {
                int newLength = Math.min(r.length << 1, option.maxArrayElements());
                r = Arrays.copyOf(r, newLength);
            }
            r[index++] = v;
            b = nextFirstValuableByte(option, readBuffer);
            if (b == ']') {
                return Arrays.copyOf(r, index);
            } else if (b == ',') {
                b = nextFirstValuableByte(option, readBuffer);
            } else {
                throw new JsonDeserializerException("array separator expected, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + option.maxArrayElements());
    }

    public static float[] deserializeFloatArray(JsonDeserializerOption option, ReadBuffer readBuffer, byte firstByte) {
        assert option != null && readBuffer != null && validateJsonArrayStart(firstByte);
        byte b = nextFirstValuableByte(option, readBuffer);
        if (b == ']') {
            return Utils.emptyFloatArray();
        }
        float[] r = new float[ARR_INITIAL_SIZE];
        for (int index = 0; index < option.maxArrayElements(); ) {
            if(!validateJsonNumberStart(b)) {
                throw new JsonDeserializerException("not a number start : " + b);
            }
            float v = deserializeFloat(option, readBuffer, b);
            if (index == r.length) {
                int newLength = Math.min(r.length << 1, option.maxArrayElements());
                r = Arrays.copyOf(r, newLength);
            }
            r[index++] = v;
            b = nextFirstValuableByte(option, readBuffer);
            if (b == ']') {
                return Arrays.copyOf(r, index);
            } else if (b == ',') {
                b = nextFirstValuableByte(option, readBuffer);
            } else {
                throw new JsonDeserializerException("array separator expected, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + option.maxArrayElements());
    }

    public static double[] deserializeDoubleArray(JsonDeserializerOption option, ReadBuffer readBuffer, byte firstByte) {
        assert option != null && readBuffer != null && validateJsonArrayStart(firstByte);
        byte b = nextFirstValuableByte(option, readBuffer);
        if (b == ']') {
            return Utils.emptyDoubleArray();
        }
        double[] r = new double[ARR_INITIAL_SIZE];
        for (int index = 0; index < option.maxArrayElements(); ) {
            if(!validateJsonNumberStart(b)) {
                throw new JsonDeserializerException("not a number start : " + b);
            }
            double v = deserializeDouble(option, readBuffer, b);
            if (index == r.length) {
                int newLength = Math.min(r.length << 1, option.maxArrayElements());
                r = Arrays.copyOf(r, newLength);
            }
            r[index++] = v;
            b = nextFirstValuableByte(option, readBuffer);
            if (b == ']') {
                return Arrays.copyOf(r, index);
            } else if (b == ',') {
                b = nextFirstValuableByte(option, readBuffer);
            } else {
                throw new JsonDeserializerException("array separator expected, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + option.maxArrayElements());
    }

    @FunctionalInterface
    interface ObjectDeserializer<T> {
        T deserialize(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, byte firstByte);
    }

    @SuppressWarnings("unchecked")
    private static <T> T[] deserializeObjectArray(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context,
                                                  byte firstByte, Class<T> componentType, ObjectDeserializer<T> deserializer) {
        assert option != null && readBuffer != null && componentType != null && deserializer != null && validateJsonArrayStart(firstByte);
        byte b = nextFirstValuableByte(option, readBuffer);
        if(b == ']') {
            return (T[]) Utils.emptyObjectArray();
        }
        T[] r = (T[]) Array.newInstance(componentType, ARR_INITIAL_SIZE);
        for (int index = 0; index < option.maxArrayElements(); ) {
            T v = null;
            if(b == (byte) 'n') {
                deserializeNull(readBuffer, b);
            } else {
                v = deserializer.deserialize(option, readBuffer, context, b);
            }
            if (index == r.length) {
                int newLength = Math.min(r.length << 1, option.maxArrayElements()); // no overflow
                r = Arrays.copyOf(r, newLength);
            }
            r[index++] = v;
            b = nextFirstValuableByte(option, readBuffer);
            if (b == ']') {
                return index == r.length ? r : Arrays.copyOf(r, index); // no overflow
            } else if (b == ',') {
                b = nextFirstValuableByte(option, readBuffer);
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + option.maxArrayElements());
    }

    public static Byte[] deserializeByteWrapperArray(JsonDeserializerOption option, ReadBuffer readBuffer, byte firstByte) {
        assert option != null && readBuffer != null && validateJsonArrayStart(firstByte);
        return deserializeObjectArray(option, readBuffer, null, firstByte,
                Byte.class, (_, r, _, b) -> {
                    if(!validateJsonNumberStart(b)) {
                        throw new JsonDeserializerException("not a number start : " + b);
                    }
                    return deserializeByte(r, b);
                });
    }

    public static Boolean[] deserializeBooleanWrapperArray(JsonDeserializerOption option, ReadBuffer readBuffer, byte firstByte) {
        assert option != null && readBuffer != null && validateJsonArrayStart(firstByte);
        return deserializeObjectArray(option, readBuffer, null, firstByte,
                Boolean.class, (_, r, _, b) -> {
                    if(!validateJsonBoolStart(b)) {
                        throw new JsonDeserializerException("not a bool start : " + b);
                    }
                    return deserializeBoolean(r, b);
                });
    }

    public static Short[] deserializeShortWrapperArray(JsonDeserializerOption option, ReadBuffer readBuffer, byte firstByte) {
        assert option != null && readBuffer != null && validateJsonArrayStart(firstByte);
        return deserializeObjectArray(option, readBuffer, null, firstByte,
                Short.class, (_, r, _, b) -> {
                    if(!validateJsonNumberStart(b)) {
                        throw new JsonDeserializerException("not a number start : " + b);
                    }
                    return deserializeShort(r, b);
                });
    }

    public static Character[] deserializeCharacterWrapperArray(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, byte firstByte) {
        assert option != null && readBuffer != null && validateJsonArrayStart(firstByte);
        return deserializeObjectArray(option, readBuffer, context, firstByte,
                Character.class, (op, r, c, b) -> {
                    if(!validateJsonStringStart(b)) {
                        throw new JsonDeserializerException("not a string start : " + b);
                    }
                    return deserializeChar(op, r, c, b);
                });
    }

    public static Integer[] deserializeIntegerWrapperArray(JsonDeserializerOption option, ReadBuffer readBuffer, byte firstByte) {
        assert option != null && readBuffer != null && validateJsonArrayStart(firstByte);
        return deserializeObjectArray(option, readBuffer, null, firstByte,
                Integer.class, (_, r, _, b) -> {
                    if(!validateJsonNumberStart(b)) {
                        throw new JsonDeserializerException("not a number start : " + b);
                    }
                    return deserializeInt(r, b);
                });
    }

    public static Long[] deserializeLongWrapperArray(JsonDeserializerOption option, ReadBuffer readBuffer, byte firstByte) {
        assert option != null && readBuffer != null && validateJsonArrayStart(firstByte);
        return deserializeObjectArray(option, readBuffer, null, firstByte,
                Long.class, (_, r, _, b) -> {
                    if(!validateJsonNumberStart(b)) {
                        throw new JsonDeserializerException("not a number start : " + b);
                    }
                    return deserializeLong(r, b);
                });
    }

    public static Float[] deserializeFloatWrapperArray(JsonDeserializerOption option, ReadBuffer readBuffer, byte firstByte) {
        assert option != null && readBuffer != null && validateJsonArrayStart(firstByte);
        return deserializeObjectArray(option, readBuffer, null, firstByte,
                Float.class, (op, r, _, b) -> {
                    if(!validateJsonNumberStart(b)) {
                        throw new JsonDeserializerException("not a number start : " + b);
                    }
                    return deserializeFloat(op, r, b);
                });
    }

    public static Double[] deserializeDoubleWrapperArray(JsonDeserializerOption option, ReadBuffer readBuffer, byte firstByte) {
        assert option != null && readBuffer != null && validateJsonArrayStart(firstByte);
        return deserializeObjectArray(option, readBuffer, null, firstByte,
                Double.class, (op, r, _, b) -> {
                    if(!validateJsonNumberStart(b)) {
                        throw new JsonDeserializerException("not a number start : " + b);
                    }
                    return deserializeDouble(op, r, b);
                });
    }

    public static String deserializeString(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, byte firstByte) {
        assert option != null && readBuffer != null && context != null && validateJsonStringStart(firstByte);
        parseString(option, readBuffer, context, firstByte);
        return context.asUtf8String();
    }

    public static String[] deserializeStringArray(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, byte firstByte) {
        assert option != null && readBuffer != null && context != null && validateJsonArrayStart(firstByte);
        return deserializeObjectArray(option, readBuffer, context, firstByte,
                String.class, (op, r, c, b) -> {
                    if(!validateJsonStringStart(b)) {
                        throw new JsonDeserializerException("not a string start : " + b);
                    }
                    return deserializeString(op, r, c, b);
                });
    }

    public static JsonPrimitiveType deserializeJsonPrimitiveType(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, byte firstByte) {
        assert option != null && readBuffer != null && context != null && validateJsonNonnullValueStart(firstByte);
        if(validateJsonBoolStart(firstByte)) {
            return new JsonBoolType(deserializeBoolean(readBuffer, firstByte));
        }else if(validateJsonStringStart(firstByte)) {
            return new JsonStrType(deserializeString(option, readBuffer, context, firstByte));
        }else {
            return deserializeJsonNumberType(option, readBuffer, firstByte);
        }
    }

    public static JsonPrimitiveType[] deserializeJsonPrimitiveTypeArray(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, byte firstByte) {
        assert option != null && readBuffer != null && context != null && validateJsonArrayStart(firstByte);
        return deserializeObjectArray(option, readBuffer, context, firstByte,
                JsonPrimitiveType.class, (op, r, c, b) -> {
                    if(!validateJsonNonnullValueStart(firstByte)) {
                        throw new JsonDeserializerException("not a value start : " + firstByte);
                    }
                    return deserializeJsonPrimitiveType(op, r, c, b);
                });
    }

    public static JsonBoolType deserializeJsonBoolType(ReadBuffer readBuffer, byte firstByte) {
        assert readBuffer != null && validateJsonBoolStart(firstByte);
        return new JsonBoolType(deserializeBoolean(readBuffer, firstByte));
    }

    public static JsonBoolType[] deserializeJsonBoolTypeArray(JsonDeserializerOption option, ReadBuffer readBuffer, byte firstByte) {
        assert option != null && readBuffer != null && validateJsonArrayStart(firstByte);
        return deserializeObjectArray(option, readBuffer, null, firstByte,
                JsonBoolType.class, (_, r, _, b) -> {
                    if(!validateJsonBoolStart(b)) {
                        throw new JsonDeserializerException("not a bool start : " + b);
                    }
                    return deserializeJsonBoolType(r, b);
                });
    }

    public static JsonNumberType deserializeJsonNumberType(JsonDeserializerOption option, ReadBuffer readBuffer, byte firstByte) {
        assert option != null && readBuffer != null && validateJsonNumberStart(firstByte);
        FpStrRep rep = JsonNumberUtil.parseFpStrRep(readBuffer, option.maxNumberBytes(), firstByte);
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> {
                final byte[] bytes = heapReadBuffer.rawByteArray();
                final int position = heapReadBuffer.intPosition();
                yield new JsonNumberType(Arrays.copyOfRange(bytes, position - 1, position + rep.len())); // no overflow
            }
            case SegmentReadBuffer segmentReadBuffer -> {
                final MemorySegment segment = segmentReadBuffer.rawSegment();
                final long position = segmentReadBuffer.longPosition();
                yield new JsonNumberType(segment.asSlice(position - 1L, rep.len() + 1L).toArray(ValueLayout.JAVA_BYTE)); // overflow guarded
            }
        };
    }

    public static JsonNumberType[] deserializeJsonNumberTypeArray(JsonDeserializerOption option, ReadBuffer readBuffer, byte firstByte) {
        assert option != null && readBuffer != null && validateJsonArrayStart(firstByte);
        return deserializeObjectArray(option, readBuffer, null, firstByte,
                JsonNumberType.class, (op, r, _, b) -> {
                    if(!validateJsonNumberStart(firstByte)) {
                        throw new JsonDeserializerException("not a number start : " + firstByte);
                    }
                    return deserializeJsonNumberType(op, r, b);
                });
    }

    public static JsonStrType deserializeJsonStrType(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, byte firstByte) {
        assert option != null && readBuffer != null && context != null && validateJsonStringStart(firstByte);
        parseString(option, readBuffer, context, firstByte);
        return new JsonStrType(context.asUtf8String());
    }

    public static JsonStrType[] deserializeJsonStrTypeArray(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, byte firstByte) {
        assert option != null && readBuffer != null && context != null && validateJsonArrayStart(firstByte);
        return deserializeObjectArray(option, readBuffer, context, firstByte,
                JsonStrType.class, (op, r, c, b) -> {
                    if(!validateJsonStringStart(b)) {
                        throw new JsonDeserializerException("not a string start : " + b);
                    }
                    return deserializeJsonStrType(op, r, c, b);
                });
    }

    public static void parseString(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, byte firstByte) {
        assert option != null && readBuffer != null && context != null && validateJsonStringStart(firstByte);
        switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> parseHeapString(option, heapReadBuffer, context);
            case SegmentReadBuffer segmentReadBuffer -> parseSegmentString(option, segmentReadBuffer, context);
        }
    }

    private static void parseHeapString(JsonDeserializerOption option, HeapReadBuffer heapReadBuffer, JsonDeserializerContext context) {
        assert option != null && heapReadBuffer != null && context != null;
        byte[] bytes = heapReadBuffer.rawByteArray();
        int position = heapReadBuffer.intPosition();
        int end = position + Math.min(bytes.length - position, option.maxStringBytes());
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
                        if(end - index < 4) {
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
                            if(end - index < 6 || bytes[index] != '\\' || bytes[index + 1] != 'u') {
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

    private static void parseSegmentString(JsonDeserializerOption option, SegmentReadBuffer segmentReadBuffer, JsonDeserializerContext context) {
        assert option != null && segmentReadBuffer != null && context != null;
        MemorySegment segment = segmentReadBuffer.rawSegment();
        long position = segmentReadBuffer.longPosition();
        long end = position + Math.min(segment.byteSize() - position, option.maxStringBytes()); // no overflow
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
                        if(end - index < 4L) {
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
                            if(end - index < 6L || SegmentAccess.getByte(segment, index) != '\\' || SegmentAccess.getByte(segment, index + 1L) != 'u') {
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

    public static void skipColon(JsonDeserializerOption option, ReadBuffer readBuffer) {
        assert option != null && readBuffer != null;
        final int maxEmptyBytes = option.maxEmptyBytes();
        switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> {
                final byte[] bytes = heapReadBuffer.rawByteArray();
                final int position = heapReadBuffer.intPosition();
                final int end = position + Math.min(bytes.length - position, maxEmptyBytes); // no overflow
                for (int i = position; i < end; i++) {
                    byte b = bytes[i];
                    if(b == (byte) ':') {
                        heapReadBuffer.setPosition(i + 1); // no overflow
                        return ;
                    }
                }
            }
            case SegmentReadBuffer segmentReadBuffer -> {
                final MemorySegment segment = segmentReadBuffer.rawSegment();
                final long position = segmentReadBuffer.longPosition();
                final long end = position + Math.min(segment.byteSize() - position, maxEmptyBytes); // no overflow
                for (long i = position; i < end; i++) {
                    byte b = SegmentAccess.getByte(segment, i);
                    if(b == (byte) ':') {
                        segmentReadBuffer.setPosition(i + 1L); // no overflow
                        return ;
                    }
                }
            }
        }
        throw new JsonDeserializerException("colon not found");
    }

    public static void skipNullValue(ReadBuffer readBuffer, byte firstByte) {
        assert readBuffer != null && validateJsonNullStart(firstByte);
        if(readBuffer.readByte() != (byte) 'u' || readBuffer.readByte() != (byte) 'l' || readBuffer.readByte() != (byte) 'l') {
            throw new JsonDeserializerException("skipped illegal null value");
        }
    }

    public static void skipBoolValue(ReadBuffer readBuffer, byte firstByte) {
        assert readBuffer != null && validateJsonBoolStart(firstByte);
        if(firstByte == (byte) 't') {
            if(readBuffer.readByte() != (byte) 'u' || readBuffer.readByte() != (byte) 'l' || readBuffer.readByte() != (byte) 'l') {
                throw new JsonDeserializerException("skipped illegal true value");
            }
        } else {
            if(readBuffer.readByte() != (byte) 'a' || readBuffer.readByte() != (byte) 'l' || readBuffer.readByte() != (byte) 's' || readBuffer.readByte() != (byte) 'e') {
                throw new JsonDeserializerException("skipped illegal false value");
            }
        }
    }

    public static void skipNumberValue(JsonDeserializerOption option, ReadBuffer readBuffer, byte firstByte) {
        assert option != null && readBuffer != null && validateJsonNumberStart(firstByte);
        FpStrRep rep = JsonNumberUtil.parseFpStrRep(readBuffer, option.maxNumberBytes(), firstByte);
        readBuffer.setPosition(readBuffer.intPosition() + rep.len()); // no overflow
    }

    public static void skipStringValue(JsonDeserializerOption option, ReadBuffer readBuffer, byte firstByte) {
        assert option != null && readBuffer != null && validateJsonStringStart(firstByte);
        final int len = Math.min(option.maxStringBytes(), readBuffer.intLength() - readBuffer.intPosition());
        int i = 0;
        while (i++ < len) {
            byte b = readBuffer.readByte();
            if(b == (byte) '\\') {
                if(i++ == len) {
                    throw new JsonDeserializerException("illegal escape");
                }
                switch (readBuffer.readByte()) {
                    case '\"', '\\', '/', 'b',  'f', 'n', 'r', 't' -> {}
                    case 'u' -> {
                        if(len - i < 4) {
                            throw new JsonDeserializerException("illegal unicode");
                        }
                        int cp = parseUnicode(readBuffer);
                        i += 4;
                        if(!Character.isValidCodePoint(cp)) {
                            throw new JsonDeserializerException("illegal unicode codepoint : " + cp);
                        }
                        if(cp >= 0xDC00 && cp <= 0xDFFF) {
                            throw new JsonDeserializerException("illegal low surrogate : " + cp);
                        }
                        if(cp >= 0xD800 && cp <= 0xDBFF) {
                            if(len - i < 6 || readBuffer.readByte() != '\\' || readBuffer.readByte() != 'u') {
                                throw new JsonDeserializerException("illegal high surrogate without low surrogate");
                            }
                            int lcp = parseUnicode(readBuffer);
                            i += 6;
                            if(lcp < 0xDC00 || lcp > 0xDFFF) {
                                throw new JsonDeserializerException("illegal low surrogate : " + lcp);
                            }
                        }
                    }
                    default -> throw new JsonDeserializerException("illegal escaped byte : " + b);
                }
            } else if(b == (byte) '"') {
                return ;
            } else if(b < (byte) 0x20) {
                throw new JsonDeserializerException("illegal unescaped byte : " + b);
            }
        }
        throw new JsonDeserializerException("illegal json string");
    }

    public static boolean skipValue(JsonDeserializerOption option, ReadBuffer readBuffer, byte firstByte) {
        assert option != null && readBuffer != null && validateJsonNonnullValueStart(firstByte);
        if(validateJsonBoolStart(firstByte)) {
            skipBoolValue(readBuffer, firstByte);
        } else if(validateJsonStringStart(firstByte)) {
            skipStringValue(option, readBuffer, firstByte);
        } else if(validateJsonNumberStart(firstByte)) {
            skipNumberValue(option, readBuffer, firstByte);
        } else {
            return false;
        }
        return true;
    }

    private static int parseUnicode(ReadBuffer readBuffer) {
        int i1 = parseHex(readBuffer.readByte()) << 12;
        int i2 = parseHex(readBuffer.readByte()) << 8;
        int i3 = parseHex(readBuffer.readByte()) << 4;
        int i4 = parseHex(readBuffer.readByte());
        return i1 | i2 | i3 | i4;
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