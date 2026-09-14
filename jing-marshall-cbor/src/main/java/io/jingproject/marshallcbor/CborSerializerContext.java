package io.jingproject.marshallcbor;

import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.common.SegmentWriteBuffer;
import io.jingproject.common.WriteBuffer;
import io.jingproject.marshall.MarshallInfo;
import io.jingproject.marshall.Marshalls;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

// sealed context with a heap and a segment implementation, mirroring the json
// serializer. every write primitive encodes the CBOR binary format: integers
// via the shortest initial-byte head, floats via 0xfa/0xfb plus big-endian
// payload, text as raw UTF-8 (no escaping, no surrogate filtering) and byte
// strings via major type 2. the encoding logic itself is shared in this base
// class because the two subclasses only differ in how the internal write
// buffer is allocated and how the committed bytes reach the caller's buffer.
public sealed abstract class CborSerializerContext
        permits CborSerializerContext.CborSerializerHeapContext, CborSerializerContext.CborSerializerSegmentContext {

    private static final Map<Class<?>, CborSerializeFunc> BUILTIN_SERIALIZE_OBJ_FUNC_MAP;
    private static final Map<Class<?>, CborSerializeFunc> BUILTIN_SERIALIZE_ARRAY_FUNC_MAP;

    static {
        Map<Class<?>, CborSerializeFunc> objFuncMap = new HashMap<>();
        objFuncMap.put(Byte.class, (o, c) -> {
            c.serializeByte((Byte) o);
            return CborSerializeResult.Continue;
        });
        objFuncMap.put(Boolean.class, (o, c) -> {
            c.serializeBoolean((Boolean) o);
            return CborSerializeResult.Continue;
        });
        objFuncMap.put(Short.class, (o, c) -> {
            c.serializeShort((Short) o);
            return CborSerializeResult.Continue;
        });
        objFuncMap.put(Character.class, (o, c) -> {
            c.serializeChar((Character) o);
            return CborSerializeResult.Continue;
        });
        objFuncMap.put(Integer.class, (o, c) -> {
            c.serializeInt((Integer) o);
            return CborSerializeResult.Continue;
        });
        objFuncMap.put(Long.class, (o, c) -> {
            c.serializeLong((Long) o);
            return CborSerializeResult.Continue;
        });
        objFuncMap.put(Float.class, (o, c) -> {
            c.serializeFloat((Float) o);
            return CborSerializeResult.Continue;
        });
        objFuncMap.put(Double.class, (o, c) -> {
            c.serializeDouble((Double) o);
            return CborSerializeResult.Continue;
        });
        objFuncMap.put(CharSequence.class, (o, c) -> {
            c.serializeStr((CharSequence) o);
            return CborSerializeResult.Continue;
        });
        objFuncMap.put(String.class, (o, c) -> {
            c.serializeStr((String) o);
            return CborSerializeResult.Continue;
        });
        objFuncMap.put(CborPrimitiveType.class, (o, c) -> {
            c.serializeCborPrimitiveType((CborPrimitiveType) o);
            return CborSerializeResult.Continue;
        });
        objFuncMap.put(CborBoolType.class, (o, c) -> {
            c.serializeCborBoolType((CborBoolType) o);
            return CborSerializeResult.Continue;
        });
        objFuncMap.put(CborNumberType.class, (o, c) -> {
            c.serializeCborNumberType((CborNumberType) o);
            return CborSerializeResult.Continue;
        });
        objFuncMap.put(CborStrType.class, (o, c) -> {
            c.serializeCborStrType((CborStrType) o);
            return CborSerializeResult.Continue;
        });
        objFuncMap.put(CborBytesType.class, (o, c) -> {
            c.serializeCborBytesType((CborBytesType) o);
            return CborSerializeResult.Continue;
        });
        BUILTIN_SERIALIZE_OBJ_FUNC_MAP = Map.copyOf(objFuncMap);
    }

    static {
        Map<Class<?>, CborSerializeFunc> arrFuncMap = new HashMap<>();
        arrFuncMap.put(byte[].class, (o, c) -> {
            c.serializeBytes((byte[]) o);
            return CborSerializeResult.Continue;
        });
        arrFuncMap.put(boolean[].class, (o, c) -> {
            c.serializeBooleanArray((boolean[]) o);
            return CborSerializeResult.Continue;
        });
        arrFuncMap.put(short[].class, (o, c) -> {
            c.serializeShortArray((short[]) o);
            return CborSerializeResult.Continue;
        });
        arrFuncMap.put(char[].class, (o, c) -> {
            c.serializeCharArray((char[]) o);
            return CborSerializeResult.Continue;
        });
        arrFuncMap.put(int[].class, (o, c) -> {
            c.serializeIntArray((int[]) o);
            return CborSerializeResult.Continue;
        });
        arrFuncMap.put(long[].class, (o, c) -> {
            c.serializeLongArray((long[]) o);
            return CborSerializeResult.Continue;
        });
        arrFuncMap.put(float[].class, (o, c) -> {
            c.serializeFloatArray((float[]) o);
            return CborSerializeResult.Continue;
        });
        arrFuncMap.put(double[].class, (o, c) -> {
            c.serializeDoubleArray((double[]) o);
            return CborSerializeResult.Continue;
        });
        arrFuncMap.put(Byte[].class, (o, c) -> {
            c.serializeByteWrapperArray((Byte[]) o);
            return CborSerializeResult.Continue;
        });
        arrFuncMap.put(Boolean[].class, (o, c) -> {
            c.serializeBooleanWrapperArray((Boolean[]) o);
            return CborSerializeResult.Continue;
        });
        arrFuncMap.put(Short[].class, (o, c) -> {
            c.serializeShortWrapperArray((Short[]) o);
            return CborSerializeResult.Continue;
        });
        arrFuncMap.put(Character[].class, (o, c) -> {
            c.serializeCharWrapperArray((Character[]) o);
            return CborSerializeResult.Continue;
        });
        arrFuncMap.put(Integer[].class, (o, c) -> {
            c.serializeIntWrapperArray((Integer[]) o);
            return CborSerializeResult.Continue;
        });
        arrFuncMap.put(Long[].class, (o, c) -> {
            c.serializeLongWrapperArray((Long[]) o);
            return CborSerializeResult.Continue;
        });
        arrFuncMap.put(Float[].class, (o, c) -> {
            c.serializeFloatWrapperArray((Float[]) o);
            return CborSerializeResult.Continue;
        });
        arrFuncMap.put(Double[].class, (o, c) -> {
            c.serializeDoubleWrapperArray((Double[]) o);
            return CborSerializeResult.Continue;
        });
        arrFuncMap.put(CharSequence[].class, (o, c) -> {
            c.serializeStrArray((CharSequence[]) o);
            return CborSerializeResult.Continue;
        });
        arrFuncMap.put(String[].class, (o, c) -> {
            c.serializeStrArray((String[]) o);
            return CborSerializeResult.Continue;
        });
        arrFuncMap.put(CborPrimitiveType[].class, (o, c) -> {
            c.serializeCborPrimitiveTypeArray((CborPrimitiveType[]) o);
            return CborSerializeResult.Continue;
        });
        arrFuncMap.put(CborBoolType[].class, (o, c) -> {
            c.serializeCborBoolTypeArray((CborBoolType[]) o);
            return CborSerializeResult.Continue;
        });
        arrFuncMap.put(CborNumberType[].class, (o, c) -> {
            c.serializeCborNumberTypeArray((CborNumberType[]) o);
            return CborSerializeResult.Continue;
        });
        arrFuncMap.put(CborStrType[].class, (o, c) -> {
            c.serializeCborStrTypeArray((CborStrType[]) o);
            return CborSerializeResult.Continue;
        });
        arrFuncMap.put(CborBytesType[].class, (o, c) -> {
            c.serializeCborBytesTypeArray((CborBytesType[]) o);
            return CborSerializeResult.Continue;
        });
        BUILTIN_SERIALIZE_ARRAY_FUNC_MAP = Map.copyOf(arrFuncMap);
    }

    public static CborSerializerContext newCtx(CborSerializerOption option, WriteBuffer writeBuffer) {
        return switch (writeBuffer) {
            case HeapWriteBuffer heapWriteBuffer -> new CborSerializerHeapContext(option, heapWriteBuffer);
            case SegmentWriteBuffer segmentWriteBuffer -> new CborSerializerSegmentContext(option, segmentWriteBuffer);
        };
    }

    protected final CborSerializerOption option;
    protected final WriteBuffer internal;
    protected Object obj;
    protected Class<?> type;

    protected CborSerializerContext(CborSerializerOption option, WriteBuffer internal) {
        this.option = option;
        this.internal = internal;
    }

    public final CborSerializerOption option() {
        return option;
    }

    public final Object obj() {
        return obj;
    }

    public final void setObj(Object obj) {
        this.obj = obj;
    }

    public final Class<?> type() {
        return type;
    }

    public final void setType(Class<?> type) {
        this.type = type;
    }

    public abstract void append(byte b);

    public abstract void append(byte b1, byte b2);

    public abstract void commit();

    // null is the simple value 0xf6
    public final void serializeNull() {
        append((byte) 0xF6);
    }

    // true is 0xf5, false is 0xf4
    public final void serializeBoolean(boolean value) {
        append(value ? (byte) 0xF5 : (byte) 0xF4);
    }

    public final void serializeByte(byte value) {
        serializeInt(value);
    }

    public final void serializeShort(short value) {
        serializeInt(value);
    }

    public final void serializeInt(int value) {
        if (value >= 0) {
            CborNumberUtil.writeHead(CborNumberUtil.TYPE_UNSIGNED, value, internal);
        } else {
            CborNumberUtil.writeHead(CborNumberUtil.TYPE_NEGATIVE, -1L - value, internal);
        }
    }

    public final void serializeLong(long value) {
        if (value >= 0) {
            CborNumberUtil.writeHead(CborNumberUtil.TYPE_UNSIGNED, value, internal);
        } else {
            CborNumberUtil.writeHead(CborNumberUtil.TYPE_NEGATIVE, -1L - value, internal);
        }
    }

    // float is encoded as 0xfa plus a big-endian float32 payload
    public final void serializeFloat(float value) {
        append((byte) 0xFA);
        internal.writeInt(Float.floatToRawIntBits(value), ByteOrder.BIG_ENDIAN);
    }

    // double is encoded as 0xfb plus a big-endian float64 payload
    public final void serializeDouble(double value) {
        append((byte) 0xFB);
        internal.writeLong(Double.doubleToRawLongBits(value), ByteOrder.BIG_ENDIAN);
    }

    // a char is a length-1-ish text string: major 3 head with the UTF-8 byte
    // count of the single character, followed by its UTF-8 bytes
    public final void serializeChar(char value) {
        if (value < 0x80) {
            CborNumberUtil.writeHead(CborNumberUtil.TYPE_TEXT, 1L, internal);
            append((byte) value);
        } else if (value < 0x800) {
            CborNumberUtil.writeHead(CborNumberUtil.TYPE_TEXT, 2L, internal);
            append((byte) (0xC0 | (value >> 6)), (byte) (0x80 | (value & 0x3F)));
        } else {
            CborNumberUtil.writeHead(CborNumberUtil.TYPE_TEXT, 3L, internal);
            internal.writeBytes((byte) (0xE0 | (value >> 12)),
                    (byte) (0x80 | ((value >> 6) & 0x3F)), (byte) (0x80 | (value & 0x3F)));
        }
    }

    // text strings are written as raw UTF-8 bytes, no escaping and no surrogate
    // filtering; String.getBytes(UTF_8) always produces legal UTF-8
    public final void serializeStr(String str) {
        byte[] utf8Bytes = str.getBytes(StandardCharsets.UTF_8);
        CborNumberUtil.writeHead(CborNumberUtil.TYPE_TEXT, utf8Bytes.length, internal);
        internal.writeBytes(utf8Bytes);
    }

    public final void serializeStr(CharSequence charSequence) {
        serializeStr(charSequence.toString());
    }

    // pre-encoded UTF-8 text (mapped names, enum mapped names) written directly
    public final void serializeUtf8BytesAsStr(byte[] utf8Bytes) {
        CborNumberUtil.writeHead(CborNumberUtil.TYPE_TEXT, utf8Bytes.length, internal);
        internal.writeBytes(utf8Bytes);
    }

    // a byte string (major type 2) with a length head and raw bytes
    public final void serializeBytes(byte[] bytes) {
        CborNumberUtil.writeHead(CborNumberUtil.TYPE_BYTES, bytes.length, internal);
        internal.writeBytes(bytes);
    }

    // container head helpers used by the explicit-stack nodes
    public final void writeArrayHead(long count) {
        CborNumberUtil.writeHead(CborNumberUtil.TYPE_ARRAY, count, internal);
    }

    public final void writeMapHead(long count) {
        CborNumberUtil.writeHead(CborNumberUtil.TYPE_MAP, count, internal);
    }

    // indefinite-length forms: 0x9f array start, 0xbf map start, 0xff break
    public final void writeIndefiniteArrayStart() {
        append((byte) 0x9F);
    }

    public final void writeIndefiniteMapStart() {
        append((byte) 0xBF);
    }

    public final void writeBreak() {
        append((byte) 0xFF);
    }

    public final void serializeBooleanArray(boolean[] arr) {
        CborNumberUtil.writeHead(CborNumberUtil.TYPE_ARRAY, arr.length, internal);
        for (boolean value : arr) {
            serializeBoolean(value);
        }
    }

    public final void serializeShortArray(short[] arr) {
        CborNumberUtil.writeHead(CborNumberUtil.TYPE_ARRAY, arr.length, internal);
        for (short value : arr) {
            serializeShort(value);
        }
    }

    public final void serializeCharArray(char[] arr) {
        CborNumberUtil.writeHead(CborNumberUtil.TYPE_ARRAY, arr.length, internal);
        for (char value : arr) {
            serializeChar(value);
        }
    }

    public final void serializeIntArray(int[] arr) {
        CborNumberUtil.writeHead(CborNumberUtil.TYPE_ARRAY, arr.length, internal);
        for (int value : arr) {
            serializeInt(value);
        }
    }

    public final void serializeLongArray(long[] arr) {
        CborNumberUtil.writeHead(CborNumberUtil.TYPE_ARRAY, arr.length, internal);
        for (long value : arr) {
            serializeLong(value);
        }
    }

    public final void serializeFloatArray(float[] arr) {
        CborNumberUtil.writeHead(CborNumberUtil.TYPE_ARRAY, arr.length, internal);
        for (float value : arr) {
            serializeFloat(value);
        }
    }

    public final void serializeDoubleArray(double[] arr) {
        CborNumberUtil.writeHead(CborNumberUtil.TYPE_ARRAY, arr.length, internal);
        for (double value : arr) {
            serializeDouble(value);
        }
    }

    public final void serializeByteWrapperArray(Byte[] arr) {
        serializeObjArray(arr, CborSerializerContext::serializeByte);
    }

    public final void serializeBooleanWrapperArray(Boolean[] arr) {
        serializeObjArray(arr, CborSerializerContext::serializeBoolean);
    }

    public final void serializeShortWrapperArray(Short[] arr) {
        serializeObjArray(arr, CborSerializerContext::serializeShort);
    }

    public final void serializeCharWrapperArray(Character[] arr) {
        serializeObjArray(arr, CborSerializerContext::serializeChar);
    }

    public final void serializeIntWrapperArray(Integer[] arr) {
        serializeObjArray(arr, CborSerializerContext::serializeInt);
    }

    public final void serializeLongWrapperArray(Long[] arr) {
        serializeObjArray(arr, CborSerializerContext::serializeLong);
    }

    public final void serializeFloatWrapperArray(Float[] arr) {
        serializeObjArray(arr, CborSerializerContext::serializeFloat);
    }

    public final void serializeDoubleWrapperArray(Double[] arr) {
        serializeObjArray(arr, CborSerializerContext::serializeDouble);
    }

    public final void serializeStrArray(CharSequence[] arr) {
        serializeObjArray(arr, CborSerializerContext::serializeStr);
    }

    public final void serializeEnum(Enum<?> enumValue) {
        MarshallInfo inf = Marshalls.enumItemMarshallInfo(enumValue);
        if (inf == null) {
            serializeStr(enumValue.name());
        } else {
            serializeUtf8BytesAsStr(inf.mappedNameUtf8Bytes());
        }
    }

    public final void serializeEnumArray(Enum<?>[] arr) {
        serializeObjArray(arr, CborSerializerContext::serializeEnum);
    }

    public final void serializeCborPrimitiveType(CborPrimitiveType cborPrimitiveType) {
        switch (cborPrimitiveType) {
            case CborBoolType cborBoolType -> serializeCborBoolType(cborBoolType);
            case CborNumberType cborNumberType -> serializeCborNumberType(cborNumberType);
            case CborStrType cborStrType -> serializeCborStrType(cborStrType);
            case CborBytesType cborBytesType -> serializeCborBytesType(cborBytesType);
        }
    }

    public final void serializeCborPrimitiveTypeArray(CborPrimitiveType[] arr) {
        serializeObjArray(arr, CborSerializerContext::serializeCborPrimitiveType);
    }

    public final void serializeCborBoolType(CborBoolType cborBoolType) {
        serializeBoolean(cborBoolType.data());
    }

    public final void serializeCborBoolTypeArray(CborBoolType[] arr) {
        serializeObjArray(arr, CborSerializerContext::serializeCborBoolType);
    }

    // CborNumberType carries integer semantics only, encoded as major 0/1
    public final void serializeCborNumberType(CborNumberType cborNumberType) {
        serializeLong(cborNumberType.data());
    }

    public final void serializeCborNumberTypeArray(CborNumberType[] arr) {
        serializeObjArray(arr, CborSerializerContext::serializeCborNumberType);
    }

    public final void serializeCborStrType(CborStrType cborStrType) {
        serializeStr(cborStrType.data());
    }

    public final void serializeCborStrTypeArray(CborStrType[] arr) {
        serializeObjArray(arr, CborSerializerContext::serializeCborStrType);
    }

    public final void serializeCborBytesType(CborBytesType cborBytesType) {
        serializeBytes(cborBytesType.data());
    }

    public final void serializeCborBytesTypeArray(CborBytesType[] arr) {
        serializeObjArray(arr, CborSerializerContext::serializeCborBytesType);
    }

    private <T> void serializeObjArray(T[] arr, ElementSerializer<T> elementSerializer) {
        CborNumberUtil.writeHead(CborNumberUtil.TYPE_ARRAY, arr.length, internal);
        for (T element : arr) {
            if (element == null) {
                serializeNull();
            } else {
                elementSerializer.serialize(this, element);
            }
        }
    }

    public static CborSerializeFunc builtinSerializeObjFunc(Class<?> rawType) {
        return BUILTIN_SERIALIZE_OBJ_FUNC_MAP.get(rawType);
    }

    public static CborSerializeFunc builtinSerializeArrayFunc(Class<?> rawType) {
        return BUILTIN_SERIALIZE_ARRAY_FUNC_MAP.get(rawType);
    }

    // builtin type has the highest priority
    // then check if current type could be overridden by option
    // enum must be specially treated
    // finally assuming marshallable
    public final CborSerializeFunc valueSerializeFunc(Class<?> rawType) {
        if (rawType.isArray()) {
            CborSerializeFunc builtinSerializeArrFunc = builtinSerializeArrayFunc(rawType);
            if (builtinSerializeArrFunc != null) {
                return builtinSerializeArrFunc;
            }
            CborSerializeFunc customArrFunc = option.customArrFunc(rawType);
            if (customArrFunc != null) {
                return customArrFunc;
            }
            Class<?> componentType = rawType.componentType();
            if (componentType.isEnum()) {
                return (o, c) -> {
                    c.serializeEnumArray((Enum<?>[]) o);
                    return CborSerializeResult.Continue;
                };
            }
            return (o, c) -> {
                c.setObj(o);
                return CborSerializeResult.NewArray;
            };
        }
        CborSerializeFunc builtinSerializeFunc = builtinSerializeObjFunc(rawType);
        if (builtinSerializeFunc != null) {
            return builtinSerializeFunc;
        }
        CborSerializeFunc customFunc = option.customFunc(rawType);
        if (customFunc != null) {
            return customFunc;
        }
        if (rawType.isEnum()) {
            return (o, c) -> {
                c.serializeEnum((Enum<?>) o);
                return CborSerializeResult.Continue;
            };
        }
        return (o, c) -> {
            c.setObj(o);
            return CborSerializeResult.NewMarshallable;
        };
    }

    @FunctionalInterface
    interface ElementSerializer<T> {
        void serialize(CborSerializerContext c, T t);
    }

    public static final class CborSerializerHeapContext extends CborSerializerContext {
        private final HeapWriteBuffer writeBuffer;

        public CborSerializerHeapContext(CborSerializerOption option, HeapWriteBuffer writeBuffer) {
            super(option, new HeapWriteBuffer(CborSerializer.INITIAL_SIZE, writeBuffer.rawLimit()));
            this.writeBuffer = writeBuffer;
        }

        @Override
        public void append(byte b) {
            internal.writeByte(b);
        }

        @Override
        public void append(byte b1, byte b2) {
            internal.writeBytes(b1, b2);
        }

        @Override
        public void commit() {
            HeapWriteBuffer heap = (HeapWriteBuffer) internal;
            writeBuffer.writeBytes(heap.rawByteArray(), 0, heap.intPosition());
        }
    }

    public static final class CborSerializerSegmentContext extends CborSerializerContext {
        private final SegmentWriteBuffer writeBuffer;

        public CborSerializerSegmentContext(CborSerializerOption option, SegmentWriteBuffer writeBuffer) {
            super(option, new SegmentWriteBuffer(writeBuffer.rawAlloc(), CborSerializer.INITIAL_SIZE, writeBuffer.rawLimit()));
            this.writeBuffer = writeBuffer;
        }

        @Override
        public void append(byte b) {
            internal.writeByte(b);
        }

        @Override
        public void append(byte b1, byte b2) {
            internal.writeBytes(b1, b2);
        }

        @Override
        public void commit() {
            SegmentWriteBuffer segment = (SegmentWriteBuffer) internal;
            writeBuffer.writeSegment(segment.rawSegment(), 0L, segment.longPosition());
        }
    }
}