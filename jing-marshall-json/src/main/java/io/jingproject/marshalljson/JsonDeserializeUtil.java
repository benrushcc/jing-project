package io.jingproject.marshalljson;

import io.jingproject.common.*;
import jdk.incubator.vector.*;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.nio.ByteOrder;
import java.util.Arrays;

// all deserialize* methods assume firstByte is already validated externally, and it's not null
public final class JsonDeserializeUtil {
    private static final VectorSpecies<Short> SHORT_SPECIES;
    private static final VectorSpecies<Byte> BYTE_SPECIES;
    private static final int COMPACT_TRUE = Utils.compact(Utils.compact((byte) 't', (byte) 'r'), Utils.compact((byte) 'u', (byte) 'e'));
    private static final int COMPACT_ALSE = Utils.compact(Utils.compact((byte) 'a', (byte) 'l'), Utils.compact((byte) 's', (byte) 'e'));
    private static final int COMPACT_NULL = Utils.compact(Utils.compact((byte) 'n', (byte) 'u'), Utils.compact((byte) 'l', (byte) 'l'));
    private static final int OBJ_ARR_INITIAL_SIZE = 8;
    private static final Object DUMMY = new Object();

    static {
        try {
            Class<Os> _ = MethodHandles.lookup().ensureInitialized(Os.class);
        } catch (IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
        int vecSize = Integer.parseInt(System.getProperty("jing.marshalljson.deserialize.vecsize", "-1"));
        if(vecSize < 0) {
            vecSize = ShortVector.SPECIES_PREFERRED.vectorBitSize();
        }
        switch (vecSize) {
            case 64 -> {
                SHORT_SPECIES = ShortVector.SPECIES_64;
                BYTE_SPECIES = ByteVector.SPECIES_64;
            }
            case 128 -> {
                SHORT_SPECIES = ShortVector.SPECIES_128;
                BYTE_SPECIES = ByteVector.SPECIES_128;
            }
            case 256 -> {
                SHORT_SPECIES = ShortVector.SPECIES_256;
                BYTE_SPECIES = ByteVector.SPECIES_256;
            }
            case 512 -> {
                SHORT_SPECIES = ShortVector.SPECIES_512;
                BYTE_SPECIES = ByteVector.SPECIES_512;
            }
            default -> throw new UnsupportedOperationException("unknown vector size : " + vecSize);
        }
    }

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
        parseStringIntoChars(option, readBuffer, context, firstByte);
        return context.asChar();
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

    public static byte[] deserializeByteArray(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, byte firstByte) {
        assert option != null && readBuffer != null && validateJsonArrayStart(firstByte);
        byte b = nextFirstValuableByte(option, readBuffer);
        if(b == ']') {
            return Utils.emptyByteArray();
        }
        for (int index = 0; index < option.maxArrayElements(); ) {
            if(!validateJsonNumberStart(b)) {
                throw new JsonDeserializerException("not a number start : " + b);
            }
            byte v = deserializeByte(readBuffer, b);
            context.appendByte(v);
            b = nextFirstValuableByte(option, readBuffer);
            if (b == ']') {
                return context.asByteArray();
            } else if (b == ',') {
                b = nextFirstValuableByte(option, readBuffer);
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + option.maxArrayElements());
    }

    public static boolean[] deserializeBooleanArray(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, byte firstByte) {
        assert option != null && readBuffer != null && validateJsonArrayStart(firstByte);
        byte b = nextFirstValuableByte(option, readBuffer);
        if (b == ']') {
            return Utils.emptyBooleanArray();
        }
        for (int index = 0; index < option.maxArrayElements(); ) {
            if(b != (byte) 't' && b != (byte) 'f') {
                throw new JsonDeserializerException("not a bool start : " + b);
            }
            boolean v = deserializeBoolean(readBuffer, b);
            context.appendByte(v ? Byte.MAX_VALUE : Byte.MIN_VALUE);
            b = nextFirstValuableByte(option, readBuffer);
            if (b == ']') {
                return context.asBooleanArray();
            } else if (b == ',') {
                b = nextFirstValuableByte(option, readBuffer);
            } else {
                throw new JsonDeserializerException("array separator expected, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + option.maxArrayElements());
    }

    public static short[] deserializeShortArray(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, byte firstByte) {
        assert option != null && readBuffer != null && validateJsonArrayStart(firstByte);
        byte b = nextFirstValuableByte(option, readBuffer);
        if (b == ']') {
            return Utils.emptyShortArray();
        }
        for (int index = 0; index < option.maxArrayElements(); ) {
            if(!validateJsonNumberStart(b)) {
                throw new JsonDeserializerException("not a number start : " + b);
            }
            short v = deserializeShort(readBuffer, b);
            context.appendShort(v);
            b = nextFirstValuableByte(option, readBuffer);
            if (b == ']') {
                return context.asShortArray();
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
        for (int index = 0; index < option.maxArrayElements(); ) {
            if(b != (byte) '"') {
                throw new JsonDeserializerException("not a string start : " + b);
            }
            char v = deserializeChar(option, readBuffer, context, b);
            context.appendChar(v);
            b = nextFirstValuableByte(option, readBuffer);
            if (b == ']') {
                return context.asCharArray();
            } else if (b == ',') {
                b = nextFirstValuableByte(option, readBuffer);
            } else {
                throw new JsonDeserializerException("array separator expected, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + option.maxArrayElements());
    }

    public static int[] deserializeIntArray(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, byte firstByte) {
        assert option != null && readBuffer != null && validateJsonArrayStart(firstByte);
        byte b = nextFirstValuableByte(option, readBuffer);
        if (b == ']') {
            return Utils.emptyIntArray();
        }
        for (int index = 0; index < option.maxArrayElements(); ) {
            if(!validateJsonNumberStart(b)) {
                throw new JsonDeserializerException("not a number start : " + b);
            }
            int v = deserializeInt(readBuffer, b);
            context.appendInt(v);
            b = nextFirstValuableByte(option, readBuffer);
            if (b == ']') {
                return context.asIntArray();
            } else if (b == ',') {
                b = nextFirstValuableByte(option, readBuffer);
            } else {
                throw new JsonDeserializerException("array separator expected, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + option.maxArrayElements());
    }

    public static long[] deserializeLongArray(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, byte firstByte) {
        assert option != null && readBuffer != null && validateJsonArrayStart(firstByte);
        byte b = nextFirstValuableByte(option, readBuffer);
        if (b == ']') {
            return Utils.emptyLongArray();
        }
        for (int index = 0; index < option.maxArrayElements(); ) {
            if(!validateJsonNumberStart(b)) {
                throw new JsonDeserializerException("not a number start : " + b);
            }
            long v = deserializeLong(readBuffer, b);
            context.appendLong(v);
            b = nextFirstValuableByte(option, readBuffer);
            if (b == ']') {
                return context.asLongArray();
            } else if (b == ',') {
                b = nextFirstValuableByte(option, readBuffer);
            } else {
                throw new JsonDeserializerException("array separator expected, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + option.maxArrayElements());
    }

    public static float[] deserializeFloatArray(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, byte firstByte) {
        assert option != null && readBuffer != null && validateJsonArrayStart(firstByte);
        byte b = nextFirstValuableByte(option, readBuffer);
        if (b == ']') {
            return Utils.emptyFloatArray();
        }
        for (int index = 0; index < option.maxArrayElements(); ) {
            if(!validateJsonNumberStart(b)) {
                throw new JsonDeserializerException("not a number start : " + b);
            }
            float v = deserializeFloat(option, readBuffer, b);
            context.appendFloat(v);
            b = nextFirstValuableByte(option, readBuffer);
            if (b == ']') {
                return context.asFloatArray();
            } else if (b == ',') {
                b = nextFirstValuableByte(option, readBuffer);
            } else {
                throw new JsonDeserializerException("array separator expected, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + option.maxArrayElements());
    }

    public static double[] deserializeDoubleArray(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, byte firstByte) {
        assert option != null && readBuffer != null && validateJsonArrayStart(firstByte);
        byte b = nextFirstValuableByte(option, readBuffer);
        if (b == ']') {
            return Utils.emptyDoubleArray();
        }
        for (int index = 0; index < option.maxArrayElements(); ) {
            if(!validateJsonNumberStart(b)) {
                throw new JsonDeserializerException("not a number start : " + b);
            }
            double v = deserializeDouble(option, readBuffer, b);
            context.appendDouble(v);
            b = nextFirstValuableByte(option, readBuffer);
            if (b == ']') {
                return context.asDoubleArray();
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
        assert option != null && readBuffer != null && context != null && componentType != null && deserializer != null && validateJsonArrayStart(firstByte);
        byte b = nextFirstValuableByte(option, readBuffer);
        if(b == ']') {
            return (T[]) Utils.emptyObjectArray();
        }
        T[] r = (T[]) context.objArr();
        if(r == null) {
            r = (T[]) Array.newInstance(componentType, OBJ_ARR_INITIAL_SIZE);
        }
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
                context.setObjArr(r);
                return Arrays.copyOf(r, index);
            } else if (b == ',') {
                b = nextFirstValuableByte(option, readBuffer);
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + option.maxArrayElements());
    }

    public static Byte[] deserializeByteWrapperArray(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, byte firstByte) {
        assert option != null && readBuffer != null && context != null && validateJsonArrayStart(firstByte);
        return deserializeObjectArray(option, readBuffer, context, firstByte,
                Byte.class, (_, r, _, b) -> {
                    if(!validateJsonNumberStart(b)) {
                        throw new JsonDeserializerException("not a number start : " + b);
                    }
                    return deserializeByte(r, b);
                });
    }

    public static Boolean[] deserializeBooleanWrapperArray(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, byte firstByte) {
        assert option != null && readBuffer != null && context != null && validateJsonArrayStart(firstByte);
        return deserializeObjectArray(option, readBuffer, context, firstByte,
                Boolean.class, (_, r, _, b) -> {
                    if(!validateJsonBoolStart(b)) {
                        throw new JsonDeserializerException("not a bool start : " + b);
                    }
                    return deserializeBoolean(r, b);
                });
    }

    public static Short[] deserializeShortWrapperArray(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, byte firstByte) {
        assert option != null && readBuffer != null && context != null && validateJsonArrayStart(firstByte);
        return deserializeObjectArray(option, readBuffer, context, firstByte,
                Short.class, (_, r, _, b) -> {
                    if(!validateJsonNumberStart(b)) {
                        throw new JsonDeserializerException("not a number start : " + b);
                    }
                    return deserializeShort(r, b);
                });
    }

    public static Character[] deserializeCharWrapperArray(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, byte firstByte) {
        assert option != null && readBuffer != null && context != null && validateJsonArrayStart(firstByte);
        return deserializeObjectArray(option, readBuffer, context, firstByte,
                Character.class, (op, r, c, b) -> {
                    if(!validateJsonStringStart(b)) {
                        throw new JsonDeserializerException("not a string start : " + b);
                    }
                    return deserializeChar(op, r, c, b);
                });
    }

    public static Integer[] deserializeIntWrapperArray(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, byte firstByte) {
        assert option != null && readBuffer != null && context != null && validateJsonArrayStart(firstByte);
        return deserializeObjectArray(option, readBuffer, context, firstByte,
                Integer.class, (_, r, _, b) -> {
                    if(!validateJsonNumberStart(b)) {
                        throw new JsonDeserializerException("not a number start : " + b);
                    }
                    return deserializeInt(r, b);
                });
    }

    public static Long[] deserializeLongWrapperArray(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, byte firstByte) {
        assert option != null && readBuffer != null && context != null && validateJsonArrayStart(firstByte);
        return deserializeObjectArray(option, readBuffer, context, firstByte,
                Long.class, (_, r, _, b) -> {
                    if(!validateJsonNumberStart(b)) {
                        throw new JsonDeserializerException("not a number start : " + b);
                    }
                    return deserializeLong(r, b);
                });
    }

    public static Float[] deserializeFloatWrapperArray(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, byte firstByte) {
        assert option != null && readBuffer != null && context != null && validateJsonArrayStart(firstByte);
        return deserializeObjectArray(option, readBuffer, context, firstByte,
                Float.class, (op, r, _, b) -> {
                    if(!validateJsonNumberStart(b)) {
                        throw new JsonDeserializerException("not a number start : " + b);
                    }
                    return deserializeFloat(op, r, b);
                });
    }

    public static Double[] deserializeDoubleWrapperArray(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, byte firstByte) {
        assert option != null && readBuffer != null && context != null && validateJsonArrayStart(firstByte);
        return deserializeObjectArray(option, readBuffer, context, firstByte,
                Double.class, (op, r, _, b) -> {
                    if(!validateJsonNumberStart(b)) {
                        throw new JsonDeserializerException("not a number start : " + b);
                    }
                    return deserializeDouble(op, r, b);
                });
    }

    public static String deserializeString(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, byte firstByte) {
        assert option != null && readBuffer != null && context != null && validateJsonStringStart(firstByte);
        parseStringIntoChars(option, readBuffer, context, firstByte);
        return context.asString();
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
        parseStringIntoChars(option, readBuffer, context, firstByte);
        return new JsonStrType(context.asString());
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

    public static void parseStringIntoChars(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, byte firstByte) {
        assert option != null && readBuffer != null && context != null && validateJsonStringStart(firstByte);
        switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> parseHeapStringIntoChars(option, heapReadBuffer, context);
            case SegmentReadBuffer segmentReadBuffer -> parseSegmentStringIntoChars(option, segmentReadBuffer, context);
            case null, default -> throw new AssertionError();
        }
    }

    private static int parseNonEscapedHeapStringIntoChars(byte[] bytes, int position, int avail, JsonDeserializerContext context) {
        assert bytes != null && position >= 0 && avail > 0 && context != null;
        final char[] buf = context.chars();
        final int upper = BYTE_SPECIES.loopBound(Math.min(avail, buf.length));
        for(int i = 0; i < upper; i += BYTE_SPECIES.length(), position += BYTE_SPECIES.length()) {
            ByteVector byteVector = ByteVector.fromArray(BYTE_SPECIES, bytes, position);
            ShortVector part0 = (ShortVector) byteVector.convertShape(VectorOperators.B2S, SHORT_SPECIES, 0);
            ShortVector part1 = (ShortVector) byteVector.convertShape(VectorOperators.B2S, SHORT_SPECIES, 1);
            part0.intoCharArray(buf, i);
            part1.intoCharArray(buf, i + SHORT_SPECIES.length()); // no overflow
            long mask = byteVector.lt((byte) 0x20)
                    .or(byteVector.eq((byte) '\\'))
                    .or(byteVector.eq((byte) '"')).toLong();
            if(mask != 0L) {
                int range = Long.numberOfTrailingZeros(mask);
                context.setCharsIndex(i + range); // no overflow
                return position + range; // no overflow
            }
        }
        context.setCharsIndex(upper);
        return position;
    }

    private static int parseEscapedHeapStringIntoChars(byte[] bytes, int position, int end, JsonDeserializerContext context) {
        byte b = bytes[position++];
        switch (b) {
            case '\"' -> context.appendChar('\"');
            case '\\' -> context.appendChar('\\');
            case '/' -> context.appendChar('/');
            case 'b' -> context.appendChar('\b');
            case 'f' -> context.appendChar('\f');
            case 'n' -> context.appendChar('\n');
            case 'r' -> context.appendChar('\r');
            case 't' -> context.appendChar('\t');
            case 'u' -> {
                if(end - position < 4) {
                    throw new JsonDeserializerException("illegal unicode");
                }
                char c = parseUnicode(bytes, position);
                position += 4;
                if(c >= 0xDC00 && c <= 0xDFFF) {
                    throw new JsonDeserializerException("illegal low surrogate : " + c);
                }
                if(c >= 0xD800 && c <= 0xDBFF) {
                    if(end - position < 6 || bytes[position] != '\\' || bytes[position + 1] != 'u') {
                        throw new JsonDeserializerException("illegal high surrogate without low surrogate");
                    }
                    char c1 = parseUnicode(bytes, position + 2);
                    position += 6;
                    if(c1 < 0xDC00 || c1 > 0xDFFF) {
                        throw new JsonDeserializerException("illegal low surrogate : " + c1);
                    }
                    context.appendChars(c, c1);
                } else {
                    context.appendChar(c);
                }
            }
            default -> throw new JsonDeserializerException("illegal escaped byte : " + b);
        }
        return position;
    }

    private static void parseHeapStringIntoChars(JsonDeserializerOption option, HeapReadBuffer heapReadBuffer, JsonDeserializerContext context) {
        assert option != null && heapReadBuffer != null && context != null;
        final byte[] bytes = heapReadBuffer.rawByteArray();
        final int position = heapReadBuffer.intPosition();
        final int avail = Math.min(bytes.length - position, option.maxStringBytes());
        if(avail == 0) {
            throw new JsonDeserializerException("no string avaliable");
        }
        if(bytes[position] == '"') {
            heapReadBuffer.setPosition(position + 1);
            return ;
        }
        int index = parseNonEscapedHeapStringIntoChars(bytes, position, avail, context);
        final int end = position + avail;
        while (index < end) {
            char c = (char) (bytes[index++] & 0xFF);
            if (c == '\\') {
                index = parseEscapedHeapStringIntoChars(bytes, index, end, context);
            } else if(c == '"') {
                heapReadBuffer.setPosition(index);
                return ;
            } else if(c < 0x20) {
                throw new JsonDeserializerException("illegal unescaped ascii control byte : " + c);
            } else if(c < 0x80) {
                context.appendChar(c);
            } else if(c < 0xE0) {
                char c1 = (char) (bytes[index++] & 0xFF);
                context.appendChar((char) (((c & 0x1F) << 6) | (c1 & 0x3F)));
            } else if(c < 0xF0) {
                char c1 = (char) (bytes[index++] & 0xFF);
                char c2 = (char) (bytes[index++] & 0xFF);
                context.appendChar((char) (((c & 0x0F) << 12) | ((c1 & 0x3F) << 6) | (c2 & 0x3F)));
            } else {
                char c1 = (char) (bytes[index++] & 0xFF);
                char c2 = (char) (bytes[index++] & 0xFF);
                char c3 = (char) (bytes[index++] & 0xFF);
                char high = (char) (0xD800 | ((c & 0x07) << 8) | ((c1 & 0x3F) << 2) | ((c2 & 0x30) >>> 4));
                char low = (char) (0xDC00 | ((c2 & 0x0F) << 6) | (c3 & 0x3F));
                context.appendChars(high, low);
            }
        }
        throw new JsonDeserializerException("illegal json string");
    }

    private static long parseNonEscapedSegmentStringIntoChars(MemorySegment segment, long position, int avail, JsonDeserializerContext context) {
        assert segment != null && position >= 0L && avail > 0L && context != null;
        final char[] buf = context.chars();
        final int upper = BYTE_SPECIES.loopBound(Math.min(avail, buf.length));
        for(int i = 0; i < upper; i += BYTE_SPECIES.length(), position += BYTE_SPECIES.length()) {
            ByteVector byteVector = ByteVector.fromMemorySegment(BYTE_SPECIES, segment, position, ByteOrder.nativeOrder()); // byteOrder will be ignored
            ShortVector part0 = (ShortVector) byteVector.convertShape(VectorOperators.B2S, SHORT_SPECIES, 0);
            ShortVector part1 = (ShortVector) byteVector.convertShape(VectorOperators.B2S, SHORT_SPECIES, 1);
            part0.intoCharArray(buf, i);
            part1.intoCharArray(buf, i + SHORT_SPECIES.length()); // no overflow
            long mask = byteVector.lt((byte) 0x20)
                    .or(byteVector.eq((byte) '\\'))
                    .or(byteVector.eq((byte) '"')).toLong();
            if(mask != 0L) {
                int range = Long.numberOfTrailingZeros(mask);
                context.setCharsIndex(i + range); // no overflow
                return position + range; // no overflow
            }
        }
        context.setCharsIndex(upper);
        return position;
    }

    private static long parseEscapedSegmentStringIntoChars(MemorySegment segment, long position, long end, JsonDeserializerContext context) {
        byte b = SegmentAccess.getByte(segment, position++);
        switch (b) {
            case '\"' -> context.appendChar('\"');
            case '\\' -> context.appendChar('\\');
            case '/' -> context.appendChar('/');
            case 'b' -> context.appendChar('\b');
            case 'f' -> context.appendChar('\f');
            case 'n' -> context.appendChar('\n');
            case 'r' -> context.appendChar('\r');
            case 't' -> context.appendChar('\t');
            case 'u' -> {
                if(end - position < 4) {
                    throw new JsonDeserializerException("illegal unicode");
                }
                char c = parseUnicode(segment, position);
                position += 4;
                if(c >= 0xDC00 && c <= 0xDFFF) {
                    throw new JsonDeserializerException("illegal low surrogate : " + c);
                }
                if(c >= 0xD800 && c <= 0xDBFF) {
                    if(end - position < 6L || SegmentAccess.getByte(segment, position) != '\\' || SegmentAccess.getByte(segment, position + 1L) != 'u') {
                        throw new JsonDeserializerException("illegal high surrogate without low surrogate");
                    }
                    char c1 = parseUnicode(segment, position + 2L);
                    position += 6;
                    if(c1 < 0xDC00 || c1 > 0xDFFF) {
                        throw new JsonDeserializerException("illegal low surrogate : " + c1);
                    }
                    context.appendChars(c, c1);
                } else {
                    context.appendChar(c);
                }
            }
            default -> throw new JsonDeserializerException("illegal escaped byte : " + b);
        }
        return position;
    }

    private static void parseSegmentStringIntoChars(JsonDeserializerOption option, SegmentReadBuffer segmentReadBuffer, JsonDeserializerContext context) {
        assert option != null && segmentReadBuffer != null && context != null;
        final MemorySegment segment = segmentReadBuffer.rawSegment();
        final long position = segmentReadBuffer.longPosition();
        final int avail = Math.min(Math.toIntExact(segment.byteSize() - position), option.maxStringBytes());
        if(avail == 0) {
            throw new JsonDeserializerException("no string avaliable");
        }
        if(SegmentAccess.getByte(segment, position) == '"') {
            segmentReadBuffer.setPosition(position + 1L);
            return ;
        }
        long index = parseNonEscapedSegmentStringIntoChars(segment, position, avail, context);
        final long end = position + avail;
        while (index < end) {
            char c = (char) (SegmentAccess.getByte(segment, index++) & 0xFF);
            if (c == '\\') {
                index = parseEscapedSegmentStringIntoChars(segment, index, end, context);
            } else if(c == '"') {
                segmentReadBuffer.setPosition(index);
                return ;
            } else if(c < 0x20) {
                throw new JsonDeserializerException("illegal unescaped ascii control byte : " + c);
            } else if(c < 0x80) {
                context.appendChar(c);
            } else if(c < 0xE0) {
                char c1 = (char) (SegmentAccess.getByte(segment, index++) & 0xFF);
                context.appendChar((char) (((c & 0x1F) << 6) | (c1 & 0x3F)));
            } else if(c < 0xF0) {
                char c1 = (char) (SegmentAccess.getByte(segment, index++) & 0xFF);
                char c2 = (char) (SegmentAccess.getByte(segment, index++) & 0xFF);
                context.appendChar((char) (((c & 0x0F) << 12) | ((c1 & 0x3F) << 6) | (c2 & 0x3F)));
            } else {
                char c1 = (char) (SegmentAccess.getByte(segment, index++) & 0xFF);
                char c2 = (char) (SegmentAccess.getByte(segment, index++) & 0xFF);
                char c3 = (char) (SegmentAccess.getByte(segment, index++) & 0xFF);
                char high = (char) (0xD800 | ((c & 0x07) << 8) | ((c1 & 0x3F) << 2) | ((c2 & 0x30) >>> 4));
                char low = (char) (0xDC00 | ((c2 & 0x0F) << 6) | (c3 & 0x3F));
                context.appendChars(high, low);
            }
        }
        throw new JsonDeserializerException("illegal json string");
    }

    public static void parseStringIntoBytes(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, byte firstByte) {
        assert option != null && readBuffer != null && context != null && validateJsonStringStart(firstByte);
        switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> parseHeapStringIntoBytes(option, heapReadBuffer, context);
            case SegmentReadBuffer segmentReadBuffer -> parseSegmentStringIntoBytes(option, segmentReadBuffer, context);
            case null, default -> throw new AssertionError();
        }
    }

    private static int parseNonEscapedHeapStringIntoBytes(byte[] bytes, int position, int avail, JsonDeserializerContext context) {
        assert bytes != null && position >= 0 && avail > 0 && context != null;
        final byte[] buf = context.bytes();
        final int upper = BYTE_SPECIES.loopBound(Math.min(avail, buf.length));
        for(int i = 0; i < upper; i += BYTE_SPECIES.length(), position += BYTE_SPECIES.length()) {
            ByteVector byteVector = ByteVector.fromArray(BYTE_SPECIES, bytes, position);
            byteVector.intoArray(buf, i);
            long mask = byteVector.compare(VectorOperators.ULT, (byte) 0x20)
                    .or(byteVector.eq((byte) '\\'))
                    .or(byteVector.eq((byte) '"')).toLong();
            if(mask != 0L) {
                int range = Long.numberOfTrailingZeros(mask);
                context.setBytesIndex(i + range); // no overflow
                return position + range; // no overflow
            }
        }
        context.setBytesIndex(upper);
        return position;
    }

    private static int parseEscapedHeapStringIntoBytes(byte[] bytes, int position, int end, JsonDeserializerContext context) {
        byte b = bytes[position++];
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
                if(end - position < 4) {
                    throw new JsonDeserializerException("illegal unicode");
                }
                char c = parseUnicode(bytes, position);
                position += 4;
                if(c >= 0xDC00 && c <= 0xDFFF) {
                    throw new JsonDeserializerException("illegal low surrogate : " + c);
                }
                if(c >= 0xD800 && c <= 0xDBFF) {
                    if(end - position < 6 || bytes[position] != '\\' || bytes[position + 1] != 'u') {
                        throw new JsonDeserializerException("illegal high surrogate without low surrogate");
                    }
                    char c1 = parseUnicode(bytes, position + 2);
                    position += 6;
                    if(c1 < 0xDC00 || c1 > 0xDFFF) {
                        throw new JsonDeserializerException("illegal low surrogate : " + c1);
                    }
                    context.appendSurr(c, c1);
                } else {
                    context.appendNonSurr(c);
                }
            }
            default -> throw new JsonDeserializerException("illegal escaped byte : " + b);
        }
        return position;
    }

    private static void parseHeapStringIntoBytes(JsonDeserializerOption option, HeapReadBuffer heapReadBuffer, JsonDeserializerContext context) {
        assert option != null && heapReadBuffer != null && context != null;
        final byte[] bytes = heapReadBuffer.rawByteArray();
        final int position = heapReadBuffer.intPosition();
        final int avail = Math.min(bytes.length - position, option.maxStringBytes());
        if(avail == 0) {
            throw new JsonDeserializerException("no string avaliable");
        }
        if(bytes[position] == '"') {
            heapReadBuffer.setPosition(position + 1);
            return ;
        }
        int p1 = parseNonEscapedHeapStringIntoBytes(bytes, position, avail, context);
        int p2 = p1;
        final int end = position + avail;
        while (p1 < end) {
            byte b = bytes[p1++];
            if(b == '\\') {
                int len = p1 - p2 - 1;
                if(len > 0) {
                    context.appendBytes(bytes, p2, len);
                }
                if(p1 == end) {
                    throw new JsonDeserializerException("illegal escape at end of string");
                }
                p1 = parseEscapedHeapStringIntoBytes(bytes, p1, end, context);
                p2 = p1;
            } else if(b == '"') {
                int len = p1 - p2 - 1;
                if(len > 0) {
                    context.appendBytes(bytes, p2, len);
                }
                heapReadBuffer.setPosition(p1);
                return ;
            } else if(b >= (byte) 0 && b < (byte) 0x20) {
                throw new JsonDeserializerException("illegal unescaped ascii control byte : " + b);
            }
        }
        throw new JsonDeserializerException("illegal json string");
    }

    private static long parseNonEscapedSegmentStringToBytes(MemorySegment segment, long position, int avail, JsonDeserializerContext context) {
        assert segment != null && position >= 0L && avail > 0 && context != null;
        final byte[] buf = context.bytes();
        final int upper = BYTE_SPECIES.loopBound(Math.min(avail, buf.length));
        for(int i = 0; i < upper; i += BYTE_SPECIES.length(), position += BYTE_SPECIES.length()) {
            ByteVector byteVector = ByteVector.fromMemorySegment(BYTE_SPECIES, segment, position, ByteOrder.nativeOrder()); // byteOrder will be ignored
            byteVector.intoArray(buf, i);
            long mask = byteVector.compare(VectorOperators.ULT, (byte) 0x20)
                    .or(byteVector.eq((byte) '\\'))
                    .or(byteVector.eq((byte) '"')).toLong();
            if(mask != 0L) {
                int range = Long.numberOfTrailingZeros(mask);
                context.setBytesIndex(i + range); // no overflow
                return position + range; // no overflow
            }
        }
        context.setBytesIndex(upper);
        return position;
    }

    private static long parseEscapedSegmentSequenceToBytes(MemorySegment segment, long position, long end, JsonDeserializerContext context) {
        byte b = SegmentAccess.getByte(segment, position++);
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
                if(end - position < 4) {
                    throw new JsonDeserializerException("illegal unicode");
                }
                char c = parseUnicode(segment, position);
                position += 4;
                if(c >= 0xDC00 && c <= 0xDFFF) {
                    throw new JsonDeserializerException("illegal low surrogate : " + c);
                }
                if(c >= 0xD800 && c <= 0xDBFF) {
                    if(end - position < 6 || SegmentAccess.getByte(segment, position) != '\\' || SegmentAccess.getByte(segment, position + 1L) != 'u') {
                        throw new JsonDeserializerException("illegal high surrogate without low surrogate");
                    }
                    char c1 = parseUnicode(segment, position + 2L);
                    position += 6;
                    if(c1 < 0xDC00 || c1 > 0xDFFF) {
                        throw new JsonDeserializerException("illegal low surrogate : " + c1);
                    }
                    context.appendSurr(c, c1);
                } else {
                    context.appendNonSurr(c);
                }
            }
            default -> throw new JsonDeserializerException("illegal escaped byte : " + b);
        }
        return position;
    }

    private static void parseSegmentStringIntoBytes(JsonDeserializerOption option, SegmentReadBuffer segmentReadBuffer, JsonDeserializerContext context) {
        assert option != null && segmentReadBuffer != null && context != null;
        final MemorySegment segment = segmentReadBuffer.rawSegment();
        final long position = segmentReadBuffer.longPosition();
        final int avail = Math.min(Math.toIntExact(segment.byteSize() - position), option.maxStringBytes());
        if(avail == 0) {
            throw new JsonDeserializerException("no string avaliable");
        }
        if(SegmentAccess.getByte(segment, position) == '"') {
            segmentReadBuffer.setPosition(position + 1L);
            return ;
        }
        long p1 = parseNonEscapedSegmentStringToBytes(segment, position, avail, context);
        long p2 = p1;
        final long end = position + avail; // no overflow
        while (p1 < end) {
            byte b = SegmentAccess.getByte(segment, p1++);
            if(b == '\\') {
                int len = Math.toIntExact(p1 - p2 - 1L);
                if(len > 0) {
                    context.appendSegment(segment, p2, len);
                }
                if(p1 == end) {
                    throw new JsonDeserializerException("illegal escape at end of string");
                }
                p1 = parseEscapedSegmentSequenceToBytes(segment, p1, end, context);
                p2 = p1;
            } else if(b == '"') {
                int len = Math.toIntExact(p1 - p2 - 1L);
                if(len > 0) {
                    context.appendSegment(segment, p2, len);
                }
                segmentReadBuffer.setPosition(p1);
                return ;
            } else if(b >= (byte) 0 && b < (byte) 0x20) {
                throw new JsonDeserializerException("illegal unescaped ascii control byte : " + b);
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
                        char c = parseUnicode(readBuffer);
                        i += 4;
                        if(c >= 0xDC00 && c <= 0xDFFF) {
                            throw new JsonDeserializerException("illegal low surrogate : " + c);
                        }
                        if(c >= 0xD800 && c <= 0xDBFF) {
                            if(len - i < 6 || readBuffer.readByte() != '\\' || readBuffer.readByte() != 'u') {
                                throw new JsonDeserializerException("illegal high surrogate without low surrogate");
                            }
                            char c1 = parseUnicode(readBuffer);
                            i += 6;
                            if(c1 < 0xDC00 || c1 > 0xDFFF) {
                                throw new JsonDeserializerException("illegal low surrogate : " + c1);
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

    public static boolean skipAnyValue(JsonDeserializerOption option, ReadBuffer readBuffer, byte firstByte) {
        assert option != null && readBuffer != null && validateJsonNonnullValueStart(firstByte);
        if(validateJsonNullStart(firstByte)) {
            skipNullValue(readBuffer, firstByte);
        }else if(validateJsonBoolStart(firstByte)) {
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

    private static char parseUnicode(ReadBuffer readBuffer) {
        assert readBuffer != null;
        int i1 = parseHex(readBuffer.readByte()) << 12;
        int i2 = parseHex(readBuffer.readByte()) << 8;
        int i3 = parseHex(readBuffer.readByte()) << 4;
        int i4 = parseHex(readBuffer.readByte());
        return (char) (i1 | i2 | i3 | i4);
    }

    private static char parseUnicode(byte[] bytes, int index) {
        assert bytes != null && index >= 0 && index <= bytes.length - 4;
        int i1 = parseHex(bytes[index]) << 12;
        int i2 = parseHex(bytes[index + 1]) << 8;
        int i3 = parseHex(bytes[index + 2]) << 4;
        int i4 = parseHex(bytes[index + 3]);
        return (char) (i1 | i2 | i3 | i4);
    }

    private static char parseUnicode(MemorySegment segment, long index) {
        assert segment != null && index >= 0L && index <= segment.byteSize() - 4L;
        int i1 = parseHex(SegmentAccess.getByte(segment, index)) << 12;
        int i2 = parseHex(SegmentAccess.getByte(segment, index + 1)) << 8;
        int i3 = parseHex(SegmentAccess.getByte(segment, index + 2)) << 4;
        int i4 = parseHex(SegmentAccess.getByte(segment, index + 3));
        return (char) (i1 | i2 | i3 | i4);
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

    public static void validateUtf8ReadBuffer(ReadBuffer readBuffer) {
        assert readBuffer != null;
        switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> {
                byte[] bytes = heapReadBuffer.rawByteArray();
                int position = heapReadBuffer.intPosition();
                if(!Utf8Validator.validate(bytes, position, bytes.length)) {
                    throw new JsonDeserializerException("illegal utf-8 encoded heap readBuffer");
                }
            }
            case SegmentReadBuffer segmentReadBuffer -> {
                MemorySegment segment = segmentReadBuffer.rawSegment();
                long position = segmentReadBuffer.longPosition();
                if(!Utf8Validator.validate(segment, position, segment.byteSize())) {
                    throw new JsonDeserializerException("illegal utf-8 encoded segment readBuffer");
                }
            }
            case null, default -> throw new AssertionError();
        }
    }
}