package io.jingproject.marshalljson;

import io.jingproject.common.*;
import io.jingproject.marshall.MarshallInfo;
import io.jingproject.marshall.Marshalls;
import jdk.incubator.vector.*;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

// indent never overflows int, as its value is limited by the practical JSON nesting depth.
public sealed abstract class JsonSerializerContext permits JsonSerializerContext.JsonSerializerHeapContext, JsonSerializerContext.JsonSerializerSegmentContext {
    // whether to escape '/', which was suggested for safely embedding JSON in HTML, it's not required by the JSON spec, so we leave it optional and default to false
    protected static final boolean ESCAPE_SLASH =
            Boolean.parseBoolean(System.getProperty("jing.marshalljson.escapeslash", "false"));
    // whether to nable surrogate pair handling
    // when the ASCII fast path does not match, the surrogate pair filtering path is taken
    // this yields performance gains when 3‑byte UTF‑8 data dominates and 4‑byte surrogate pairs are absent
    protected static final boolean FILTER_SURR =
            Boolean.parseBoolean(System.getProperty("jing.marshalljson.filtersurr", "true"));
    protected static final VectorSpecies<Short> SHORT_SPECIES;
    protected static final VectorSpecies<Byte> BYTE_SPECIES;
    protected static final int VEC_MASK;
    protected static final byte[] ESCAPE_TABLE = makeEscapeTable();
    protected static final byte[] HEX_BYTES = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
    private static final Map<Class<?>, JsonSerializeFunc> BUILTIN_SERIALIZE_OBJ_FUNC_MAP;
    private static final Map<Class<?>, JsonSerializeFunc> BUILTIN_SERIALIZE_ARRAY_FUNC_MAP;

    static {
        try {
            Class<Os> _ = MethodHandles.lookup().ensureInitialized(Os.class);
        } catch (IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
        int vecSize = Integer.parseInt(System.getProperty("jing.marshalljson.serialize.vecsize", "-1"));
        if (vecSize < 0) {
            vecSize = ShortVector.SPECIES_PREFERRED.vectorBitSize();
        }
        switch (vecSize) {
            case 128 -> {
                SHORT_SPECIES = ShortVector.SPECIES_128;
                BYTE_SPECIES = ByteVector.SPECIES_64;
            }
            case 256 -> {
                SHORT_SPECIES = ShortVector.SPECIES_256;
                BYTE_SPECIES = ByteVector.SPECIES_128;
            }
            case 512 -> {
                SHORT_SPECIES = ShortVector.SPECIES_512;
                BYTE_SPECIES = ByteVector.SPECIES_256;
            }
            default -> throw new UnsupportedOperationException("unknown vector size : " + vecSize);
        }
        VEC_MASK = SHORT_SPECIES.length() - 1;
    }

    static {
        Map<Class<?>, JsonSerializeFunc> r = new HashMap<>();
        r.put(Byte.class, (o, _, c) -> {
            c.serializeByte((Byte) o);
            return JsonSerializeResult.Continue;
        });
        r.put(Boolean.class, (o, _, c) -> {
            c.serializeBoolean((Boolean) o);
            return JsonSerializeResult.Continue;
        });
        r.put(Short.class, (o, _, c) -> {
            c.serializeShort((Short) o);
            return JsonSerializeResult.Continue;
        });
        r.put(Character.class, (o, _, c) -> {
            c.serializeChar((Character) o);
            return JsonSerializeResult.Continue;
        });
        r.put(Integer.class, (o, _, c) -> {
            c.serializeInt((Integer) o);
            return JsonSerializeResult.Continue;
        });
        r.put(Long.class, (o, _, c) -> {
            c.serializeLong((Long) o);
            return JsonSerializeResult.Continue;
        });
        r.put(Float.class, (o, _, c) -> {
            c.serializeFloat((Float) o);
            return JsonSerializeResult.Continue;
        });
        r.put(Double.class, (o, _, c) -> {
            c.serializeDouble((Double) o);
            return JsonSerializeResult.Continue;
        });
        r.put(CharSequence.class, (o, _, c) -> {
            c.serializeEscapedCharSequence((CharSequence) o);
            return JsonSerializeResult.Continue;
        });
        r.put(String.class, (o, _, c) -> {
            c.serializeEscapedString((String) o);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonPrimitiveType.class, (o, _, c) -> {
            c.serializeJsonPrimitiveType((JsonPrimitiveType) o);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonBoolType.class, (o, _, c) -> {
            c.serializeJsonBoolType((JsonBoolType) o);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonNumberType.class, (o, _, c) -> {
            c.serializeJsonNumberType((JsonNumberType) o);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonStrType.class, (o, _, c) -> {
            c.serializeJsonStrType((JsonStrType) o);
            return JsonSerializeResult.Continue;
        });
        BUILTIN_SERIALIZE_OBJ_FUNC_MAP = Map.copyOf(r);
    }

    static {
        Map<Class<?>, JsonSerializeFunc> r = new HashMap<>();
        r.put(byte[].class, (o, i, c) -> {
            c.serializeByteArray((byte[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(boolean[].class, (o, i, c) -> {
            c.serializeBooleanArray((boolean[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(short[].class, (o, i, c) -> {
            c.serializeShortArray((short[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(char[].class, (o, i, c) -> {
            c.serializeCharArray((char[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(int[].class, (o, i, c) -> {
            c.serializeIntArray((int[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(long[].class, (o, i, c) -> {
            c.serializeLongArray((long[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(float[].class, (o, i, c) -> {
            c.serializeFloatArray((float[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(double[].class, (o, i, c) -> {
            c.serializeDoubleArray((double[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(Byte[].class, (o, i, c) -> {
            c.serializeByteWrapperArray((Byte[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(Boolean[].class, (o, i, c) -> {
            c.serializeBooleanWrapperArray((Boolean[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(Short[].class, (o, i, c) -> {
            c.serializeShortWrapperArray((Short[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(Character[].class, (o, i, c) -> {
            c.serializeCharWrapperArray((Character[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(Integer[].class, (o, i, c) -> {
            c.serializeIntWrapperArray((Integer[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(Long[].class, (o, i, c) -> {
            c.serializeLongWrapperArray((Long[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(Float[].class, (o, i, c) -> {
            c.serializeFloatWrapperArray((Float[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(Double[].class, (o, i, c) -> {
            c.serializeDoubleWrapperArray((Double[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(CharSequence[].class, (o, i, c) -> {
            c.serializeEscapedCharSequenceArray((CharSequence[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(String[].class, (o, i, c) -> {
            c.serializeEscapedStringArray((String[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonPrimitiveType[].class, (o, i, c) -> {
            c.serializeJsonPrimitiveTypeArray((JsonPrimitiveType[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonBoolType[].class, (o, i, c) -> {
            c.serializeJsonBoolTypeArray((JsonBoolType[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonNumberType[].class, (o, i, c) -> {
            c.serializeJsonNumberTypeArray((JsonNumberType[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonStrType[].class, (o, i, c) -> {
            c.serializeJsonStrTypeArray((JsonStrType[]) o, i);
            return JsonSerializeResult.Continue;
        });
        BUILTIN_SERIALIZE_ARRAY_FUNC_MAP = Map.copyOf(r);
    }

    public static JsonSerializerContext newCtx(JsonSerializerOption option, WriteBuffer writeBuffer) {
        return switch (writeBuffer) {
            case HeapWriteBuffer heapWriteBuffer -> new JsonSerializerHeapContext(option, heapWriteBuffer);
            case SegmentWriteBuffer segmentWriteBuffer -> new JsonSerializerSegmentContext(option, segmentWriteBuffer);
        };
    }

    protected final JsonSerializerOption option;
    protected char[] charBuffer;
    protected Object obj;
    protected Class<?> type;

    protected JsonSerializerContext(JsonSerializerOption option) {
        this.option = option;
    }

    public final JsonSerializerOption option() {
        return option;
    }

    protected final char[] alignedBuf(int alignedLen) {
        if(charBuffer == null) {
            charBuffer = new char[alignedLen];
        } else if(charBuffer.length < alignedLen) {
            // doubling the current capacity preserves alignment since the initial length is aligned
            int growedLength = Math.addExact(charBuffer.length, charBuffer.length);
            charBuffer = new char[Math.max(alignedLen, growedLength)];
        }
        return charBuffer;
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

    private static byte[] makeEscapeTable() {
        byte[] table = new byte[Byte.MAX_VALUE - Byte.MIN_VALUE + 1];
        for (int i = 0x00; i < 0x20; i++) {
            table[i] = Byte.MIN_VALUE;
        }
        table[0x22] = (byte) '"';   // \"
        table[0x5C] = (byte) '\\'; // \\
        if (ESCAPE_SLASH) {
            table[0x2F] = (byte) '/';  // \/
        }
        table[0x08] = (byte) 'b';  // \b
        table[0x0C] = (byte) 'f';  // \f
        table[0x0A] = (byte) 'n';  // \n
        table[0x0D] = (byte) 'r';  // \r
        table[0x09] = (byte) 't';  // \t
        return table;
    }

    public abstract void append(byte b);

    public abstract void append(byte b1, byte b2);

    public abstract void serializeNull();

    public void serializeByte(byte value) {
        serializeInt(value);
    }

    public abstract void serializeBoolean(boolean value);

    public void serializeShort(short value) {
        serializeInt(value);
    }

    public abstract void serializeChar(char value);

    public abstract void serializeInt(int value);

    public abstract void serializeLong(long value);

    public abstract void serializeFloat(float value);

    public abstract void serializeDouble(double value);

    public abstract void serializePrefix(byte pre, int indent);

    public abstract void serializeSuffix(byte post, int indent);

    private <T> void serializeObjArray(T[] arr, int indent, ElementSerializer<T> elementSerializer) {
        if(arr.length == 0) {
            serializePrefix((byte) '[', 0);
        } else {
            for (int i = 0; i < arr.length; i++) {
                serializePrefix(i == 0 ? (byte) '[' : (byte) ',', indent + 1);
                T o = arr[i];
                if (o == null) {
                    serializeNull();
                } else {
                    elementSerializer.serialize(this, o);
                }
            }
        }
        serializeSuffix((byte) ']', indent);
    }

    public final void serializeByteArray(byte[] arr, int indent) {
        if(arr.length == 0) {
            serializePrefix((byte) '[', 0);
        } else {
            for (int i = 0; i < arr.length; i++) {
                serializePrefix(i == 0 ? (byte) '[' : (byte) ',', indent + 1);
                serializeByte(arr[i]);
            }
        }
        serializeSuffix((byte) ']', indent);
    }

    public final void serializeByteWrapperArray(Byte[] arr, int indent) {
        serializeObjArray(arr, indent, JsonSerializerContext::serializeByte);
    }

    public final void serializeBooleanArray(boolean[] arr, int indent) {
        if (arr.length == 0) {
            serializePrefix((byte) '[', 0);
        } else {
            for (int i = 0; i < arr.length; i++) {
                serializePrefix(i == 0 ? (byte) '[' : (byte) ',', indent + 1);
                serializeBoolean(arr[i]); // 输出 true/false
            }
        }
        serializeSuffix((byte) ']', indent);
    }

    public final void serializeBooleanWrapperArray(Boolean[] arr, int indent) {
        serializeObjArray(arr, indent, JsonSerializerContext::serializeBoolean);
    }

    public final void serializeShortArray(short[] arr, int indent) {
        if (arr.length == 0) {
            serializePrefix((byte) '[', 0);
        } else {
            for (int i = 0; i < arr.length; i++) {
                serializePrefix(i == 0 ? (byte) '[' : (byte) ',', indent + 1);
                serializeShort(arr[i]);
            }
        }
        serializeSuffix((byte) ']', indent);
    }

    public final void serializeShortWrapperArray(Short[] arr, int indent) {
        serializeObjArray(arr, indent, JsonSerializerContext::serializeShort);
    }

    public final void serializeCharArray(char[] arr, int indent) {
        if (arr.length == 0) {
            serializePrefix((byte) '[', 0);
        } else {
            for (int i = 0; i < arr.length; i++) {
                serializePrefix(i == 0 ? (byte) '[' : (byte) ',', indent + 1);
                serializeChar(arr[i]);
            }
        }
        serializeSuffix((byte) ']', indent);
    }

    public final void serializeCharWrapperArray(Character[] arr, int indent) {
        serializeObjArray(arr, indent, JsonSerializerContext::serializeChar);
    }

    public final void serializeIntArray(int[] arr, int indent) {
        if (arr.length == 0) {
            serializePrefix((byte) '[', 0);
        } else {
            for (int i = 0; i < arr.length; i++) {
                serializePrefix(i == 0 ? (byte) '[' : (byte) ',', indent + 1);
                serializeInt(arr[i]);
            }
        }
        serializeSuffix((byte) ']', indent);
    }

    public final void serializeIntWrapperArray(Integer[] arr, int indent) {
        serializeObjArray(arr, indent, JsonSerializerContext::serializeInt);
    }

    public final void serializeLongArray(long[] arr, int indent) {
        if (arr.length == 0) {
            serializePrefix((byte) '[', 0);
        } else {
            for (int i = 0; i < arr.length; i++) {
                serializePrefix(i == 0 ? (byte) '[' : (byte) ',', indent + 1);
                serializeLong(arr[i]);
            }
        }
        serializeSuffix((byte) ']', indent);
    }

    public final void serializeLongWrapperArray(Long[] arr, int indent) {
        serializeObjArray(arr, indent, JsonSerializerContext::serializeLong);
    }

    public final void serializeFloatArray(float[] arr, int indent) {
        if (arr.length == 0) {
            serializePrefix((byte) '[', 0);
        } else {
            for (int i = 0; i < arr.length; i++) {
                serializePrefix(i == 0 ? (byte) '[' : (byte) ',', indent + 1);
                serializeFloat(arr[i]);
            }
        }
        serializeSuffix((byte) ']', indent);
    }

    public final void serializeFloatWrapperArray(Float[] arr, int indent) {
        serializeObjArray(arr, indent, JsonSerializerContext::serializeFloat);
    }

    public final void serializeDoubleArray(double[] arr, int indent) {
        if (arr.length == 0) {
            serializePrefix((byte) '[', 0);
        } else {
            for (int i = 0; i < arr.length; i++) {
                serializePrefix(i == 0 ? (byte) '[' : (byte) ',', indent + 1);
                serializeDouble(arr[i]);
            }
        }
        serializeSuffix((byte) ']', indent);
    }

    public final void serializeDoubleWrapperArray(Double[] arr, int indent) {
        serializeObjArray(arr, indent, JsonSerializerContext::serializeDouble);
    }

    public abstract void serializeNonEscapedUtf8BytesAsStr(byte[] utf8Bytes);

    public abstract void serializeEscapedUtf8BytesAsStr(byte[] utf8Bytes);

    public abstract void serializeRawUtf8Bytes(byte[] utf8Bytes);

    public final void serializeEscapedCharSequence(CharSequence charSequence) {
        serializeEscapedString(charSequence.toString());
    }

    public final void serializeEscapedCharSequenceArray(CharSequence[] arr, int indent) {
        serializeObjArray(arr, indent, JsonSerializerContext::serializeEscapedCharSequence);
    }

    public final void serializeEscapedStringArray(String[] arr, int indent) {
        serializeObjArray(arr, indent, JsonSerializerContext::serializeEscapedString);
    }

    public abstract void serializeEscapedString(String str);

    public final void serializeJsonPrimitiveType(JsonPrimitiveType jsonPrimitiveType) {
        switch (jsonPrimitiveType) {
            case JsonBoolType jsonBoolType -> serializeJsonBoolType(jsonBoolType);
            case JsonNumberType jsonNumberType -> serializeJsonNumberType(jsonNumberType);
            case JsonStrType jsonStrType -> serializeJsonStrType(jsonStrType);
        }
    }

    public final void serializeJsonPrimitiveTypeArray(JsonPrimitiveType[] arr, int indent) {
        serializeObjArray(arr, indent, JsonSerializerContext::serializeJsonPrimitiveType);
    }

    public final void serializeJsonBoolType(JsonBoolType jsonBoolType) {
        serializeBoolean(jsonBoolType.data());
    }

    public final void serializeJsonBoolTypeArray(JsonBoolType[] arr, int indent) {
        serializeObjArray(arr, indent, JsonSerializerContext::serializeJsonBoolType);
    }

    public final void serializeJsonNumberType(JsonNumberType jsonNumberType) {
        serializeRawUtf8Bytes(jsonNumberType.data());
    }

    public final void serializeJsonNumberTypeArray(JsonNumberType[] arr, int indent) {
        serializeObjArray(arr, indent, JsonSerializerContext::serializeJsonNumberType);
    }

    public final void serializeJsonStrType(JsonStrType jsonStrType) {
        serializeEscapedString(jsonStrType.data());
    }

    public final void serializeJsonStrTypeArray(JsonStrType[] arr, int indent) {
        serializeObjArray(arr, indent, JsonSerializerContext::serializeJsonStrType);
    }

    public final void serializeEnum(Enum<?> enumValue) {
        MarshallInfo inf = Marshalls.enumItemMarshallInfo(enumValue);
        if (inf == null) {
            serializeEscapedString(enumValue.name());
        } else if (inf.mappedNameSimple()) {
            serializeNonEscapedUtf8BytesAsStr(inf.mappedNameUtf8Bytes());
        } else {
            serializeEscapedUtf8BytesAsStr(inf.mappedNameUtf8Bytes());
        }
    }

    public final void serializeEnumArray(Enum<?>[] arr, int indent) {
        serializeObjArray(arr, indent, JsonSerializerContext::serializeEnum);
    }

    public abstract void commit();

    public static JsonSerializeFunc builtinSerializeObjFunc(Class<?> rawType) {
        return BUILTIN_SERIALIZE_OBJ_FUNC_MAP.get(rawType);
    }

    public static JsonSerializeFunc builtinSerializeArrayFunc(Class<?> rawType) {
        return BUILTIN_SERIALIZE_ARRAY_FUNC_MAP.get(rawType);
    }

    // builtin type has the highest priority
    // then check if current type could be override by option
    // enum must be specially treated
    // finally assuming marshallable
    public final JsonSerializeFunc valueSerializeFunc(Class<?> rawType) {
        if (rawType.isArray()) {
            JsonSerializeFunc builtinSerializeArrFunc = builtinSerializeArrayFunc(rawType);
            if (builtinSerializeArrFunc != null) {
                return builtinSerializeArrFunc;
            }
            JsonSerializeFunc customArrFunc = option.customArrFunc(rawType);
            if (customArrFunc != null) {
                return customArrFunc;
            }
            Class<?> componentType = rawType.componentType();
            if(componentType.isEnum()) {
                return (o, i, c) -> {
                    c.serializeEnumArray((Enum<?>[]) o, i);
                    return JsonSerializeResult.Continue;
                };
            }
            return (o, _, c) -> {
                c.setObj(o);
                return JsonSerializeResult.NewArray;
            };
        }
        JsonSerializeFunc builtinSerializeFunc = builtinSerializeObjFunc(rawType);
        if (builtinSerializeFunc != null) {
            return builtinSerializeFunc;
        }
        JsonSerializeFunc customFunc = option.customFunc(rawType);
        if (customFunc != null) {
            return customFunc;
        }
        if (rawType.isEnum()) {
            return (o, _, c) -> {
                c.serializeEnum((Enum<?>) o);
                return JsonSerializeResult.Continue;
            };
        }
        return (o, _, c) -> {
            c.setObj(o);
            return JsonSerializeResult.NewMarshallable;
        };
    }

    @FunctionalInterface
    interface ElementSerializer<T> {
        void serialize(JsonSerializerContext c, T t);
    }

    public static final class JsonSerializerHeapContext extends JsonSerializerContext {
        private final HeapWriteBuffer heapWriteBuffer;
        private byte[] bytes;
        private int position;

        public JsonSerializerHeapContext(JsonSerializerOption option, HeapWriteBuffer heapWriteBuffer) {
            super(option);
            this.heapWriteBuffer = heapWriteBuffer;
            this.bytes = heapWriteBuffer.rawByteArray();
            this.position = heapWriteBuffer.intPosition();
        }

        private void ensureCapacity(int requiredCapacity) {
            int nextPosition = Math.addExact(position, requiredCapacity);
            if (nextPosition > bytes.length) {
                int newLength = Math.max(Math.addExact(bytes.length, bytes.length), nextPosition);
                int limit = heapWriteBuffer.rawLimit();
                if (newLength > limit) {
                    throw new JsonSerializerException("reaching heap buffer limit : " + limit);
                }
                byte[] newBytes = new byte[newLength];
                System.arraycopy(bytes, 0, newBytes, 0, position);
                bytes = newBytes;
            }
        }

        @Override
        public void append(byte b) {
            ensureCapacity(1);
            bytes[position++] = b;
        }

        @Override
        public void append(byte b1, byte b2) {
            ensureCapacity(2);
            bytes[position++] = b1;
            bytes[position++] = b2;
        }

        @Override
        public void serializeNull() {
            ensureCapacity(4);
            bytes[position++] = (byte) 'n';
            bytes[position++] = (byte) 'u';
            bytes[position++] = (byte) 'l';
            bytes[position++] = (byte) 'l';
        }

        @Override
        public void serializeBoolean(boolean value) {
            ensureCapacity(5);
            if(value) {
                bytes[position++] = (byte) 't';
                bytes[position++] = (byte) 'r';
                bytes[position++] = (byte) 'u';
            } else {
                bytes[position++] = (byte) 'f';
                bytes[position++] = (byte) 'a';
                bytes[position++] = (byte) 'l';
                bytes[position++] = (byte) 's';
            }
            bytes[position++] = (byte) 'e';
        }

        @Override
        public void serializeChar(char value) {
            if(Character.isSurrogate(value)) {
                throw new JsonSerializerException("surrogate not supported as single char : " + value);
            }
            ensureCapacity(8);
            bytes[position++] = (byte) '"';
            if(value < 0x80) {
                position = serializeCharToHeap(value, bytes, position);
            } else if(value < 0x800) {
                position = serializeCharToHeap2(value, bytes, position);
            } else {
                position = serializeCharToHeap3(value, bytes, position);
            }
            bytes[position++] = (byte) '"';
        }

        @Override
        public void serializeInt(int value) {
            ensureCapacity(JsonNumberUtil.MIN_INT_BYTES.length);
            position = JsonNumberUtil.writeIntToHeap(value, bytes, position);
        }

        @Override
        public void serializeLong(long value) {
            ensureCapacity(JsonNumberUtil.MIN_LONG_BYTES.length);
            position = JsonNumberUtil.writeLongToHeap(value, bytes, position);
        }

        @Override
        public void serializeFloat(float value) {
            ensureCapacity(JsonNumberUtil.MAX_FLOAT_CAPACITY);
            position = JsonNumberUtil.writeFloatToHeap(value, bytes, position);
        }

        @Override
        public void serializeDouble(double value) {
            ensureCapacity(JsonNumberUtil.MAX_DOUBLE_CAPACITY);
            position = JsonNumberUtil.writeDoubleToHeap(value, bytes, position);
        }

        @Override
        public void serializePrefix(byte pre, int indent) {
            final JsonIndentationLevel indentationLevel = option.indentationLevel();
            if(indentationLevel == JsonIndentationLevel.NONE) {
                ensureCapacity(1);
                bytes[position++] = pre;
                return ;
            }
            int spaces = (indentationLevel == JsonIndentationLevel.TWO ? 2 : 4) * indent;
            ensureCapacity(2 + spaces); // no overflow
            bytes[position++] = pre;
            bytes[position++] = (byte) '\n';
            Arrays.fill(bytes, position, position + spaces, (byte) ' ');
            position += spaces;
        }

        @Override
        public void serializeSuffix(byte post, int indent) {
            final JsonIndentationLevel indentationLevel = option.indentationLevel();
            if(indentationLevel == JsonIndentationLevel.NONE) {
                ensureCapacity(1);
                bytes[position++] = post;
                return ;
            }
            int spaces = (indentationLevel == JsonIndentationLevel.TWO ? 2 : 4) * indent;
            ensureCapacity(2 + spaces); // no overflow
            bytes[position++] = (byte) '\n';
            Arrays.fill(bytes, position, position + spaces, (byte) ' ');
            position += spaces;
            bytes[position++] = post;
        }

        @Override
        public void serializeNonEscapedUtf8BytesAsStr(byte[] utf8Bytes) {
            ensureCapacity(Math.addExact(2, utf8Bytes.length));
            bytes[position++] = (byte) '"';
            System.arraycopy(utf8Bytes, 0, bytes, position, utf8Bytes.length);
            position += utf8Bytes.length;
            bytes[position++] = (byte) '"';
        }

        @Override
        public void serializeEscapedUtf8BytesAsStr(byte[] utf8Bytes) {
            ensureCapacity(Math.addExact(Math.multiplyExact(utf8Bytes.length, 6), 2));
            bytes[position++] = (byte) '"';
            if(utf8Bytes.length != 0) {
                int start = 0;
                for (int index = 0; index < utf8Bytes.length; index++) {
                    byte b = utf8Bytes[index];
                    byte v = ESCAPE_TABLE[b & 0xFF];
                    if (v == 0) {
                        continue;
                    }
                    if (index > start) {
                        int available = index - start;
                        System.arraycopy(utf8Bytes, start, bytes, position, available);
                        position += available;
                    }
                    bytes[position++] = (byte) '\\';
                    if(v > 0) {
                        bytes[position++] = v;
                    } else {
                        bytes[position++] = (byte) 'u';
                        bytes[position++] = (byte) '0';
                        bytes[position++] = (byte) '0';
                        bytes[position++] = HEX_BYTES[(b >>> 4) & 0xF];
                        bytes[position++] = HEX_BYTES[b & 0xF];
                    }
                    start = index + 1;
                }
                if (start < utf8Bytes.length) {
                    int available = utf8Bytes.length - start;
                    System.arraycopy(utf8Bytes, start, bytes, position, available);
                    position += available;
                }
            }
            bytes[position++] = (byte) '"';
        }

        @Override
        public void serializeRawUtf8Bytes(byte[] utf8Bytes) {
            ensureCapacity(utf8Bytes.length);
            System.arraycopy(utf8Bytes, 0, bytes, position, utf8Bytes.length);
            position += utf8Bytes.length;
        }

        @Override
        public void serializeEscapedString(String str) {
            ensureCapacity(Math.addExact(Math.multiplyExact(str.length(), 6), 2));
            bytes[position++] = (byte) '"';
            if(!str.isEmpty()) {
                if(str.length() < SHORT_SPECIES.length()) {
                    position = serializeStrToHeap(str, bytes, position);
                }else {
                    position = serializeLongStrToHeap(str, bytes, position);
                }
            }
            bytes[position++] = (byte) '"';
        }

        private static int serializeStrToHeap(String str, byte[] bytes, int position) {
            final int len = str.length();
            int index = 0;
            while (index < len) {
                char c = str.charAt(index++);
                if(c < 0x80) {
                    position = serializeCharToHeap(c, bytes, position);
                } else if(c < 0x800) {
                    position = serializeCharToHeap2(c, bytes, position);
                } else {
                    // jdk string allows single surrogate, we still need to check them
                    final int v = c & 0xFC00;
                    if(v == 0xD800) {
                        if(index == len) {
                            throw new JsonSerializerException("illegal high surrogate without low surrogate");
                        }
                        char c1 = str.charAt(index++);
                        if((c1 & 0xFC00) != 0xDC00) {
                            throw new JsonSerializerException("illegal high surrogate without low surrogate");
                        }
                        position = serializeCharToHeap4(c, c1, bytes, position);
                    } else if(v == 0xDC00) {
                        throw new JsonSerializerException("illegal low surrogate without high surrogate");
                    } else {
                        position = serializeCharToHeap3(c, bytes, position);
                    }
                }
            }
            return position;
        }

        private int serializeLongStrToHeap(String str, byte[] bytes, int position) {
            final int len = str.length();
            final int alignedLen = Math.addExact(len, VEC_MASK) & (~VEC_MASK);
            final char[] buf = alignedBuf(alignedLen);
            str.getChars(0, len, buf, 0);
            int index = 0;
            for( ; index < alignedLen; index += SHORT_SPECIES.length()) {
                ShortVector shortVector = ShortVector.fromCharArray(SHORT_SPECIES, buf, index);
                ByteVector byteVector = (ByteVector) shortVector.convertShape(VectorOperators.S2B, BYTE_SPECIES, 0);
                byteVector.intoArray(bytes, position + index);
                VectorMask<Short> mask = shortVector.compare(VectorOperators.LT, (short) 0x20)
                        .or(shortVector.compare(VectorOperators.GT, (short) 0x7E))
                        .or(shortVector.compare(VectorOperators.EQ, (short) 0x22))
                        .or(shortVector.compare(VectorOperators.EQ, (short) 0x5C));
                if (ESCAPE_SLASH) {
                    mask = mask.or(shortVector.compare(VectorOperators.EQ, (short) 0x2F));
                }
                int asciiCount = mask.firstTrue();
                if(asciiCount != SHORT_SPECIES.length()) {
                    index += asciiCount;
                    break;
                }
            }
            if(index >= len) {
                return position + len;
            }
            if(FILTER_SURR) {
                int sIndex = index & (~VEC_MASK);
                for( ; sIndex < alignedLen; sIndex += SHORT_SPECIES.length()) {
                    VectorMask<Short> mask = ShortVector.fromCharArray(SHORT_SPECIES, buf, sIndex).lanewise(VectorOperators.AND, (short) 0xF800)
                            .compare(VectorOperators.EQ, (short) 0xD800);
                    int nonSurrCount = mask.firstTrue();
                    if(nonSurrCount != SHORT_SPECIES.length()) {
                        sIndex += nonSurrCount;
                        break;
                    }
                }
                if(sIndex >= len) {
                    return serializeNonSurrCharsToHeap(buf, index, len, bytes, position + index);
                }
            }
            return serializeCharsToHeap(buf, index, len, bytes, position + index);
        }

        private static int serializeNonSurrCharsToHeap(char[] buf, int index, int len, byte[] bytes, int position) {
            for( ; index < len; index++) {
                char c = buf[index];
                if(c < 0x80) {
                    position = serializeCharToHeap(c, bytes, position);
                } else if(c < 0x800) {
                    position = serializeCharToHeap2(c, bytes, position);
                } else {
                    position = serializeCharToHeap3(c, bytes, position);
                }
            }
            return position;
        }

        private static int serializeCharsToHeap(char[] buf, int index, int len, byte[] bytes, int position) {
            for( ; index < len; index++) {
                char c = buf[index];
                if(c < 0x80) {
                    position = serializeCharToHeap(c, bytes, position);
                } else if(c < 0x800) {
                    position = serializeCharToHeap2(c, bytes, position);
                } else {
                    // jdk string allows single surrogate, we still need to check them
                    final int v = c & 0xFC00;
                    if(v == 0xD800) {
                        final int nextIndex = index + 1;
                        if(nextIndex == len) {
                            throw new JsonSerializerException("illegal high surrogate without low surrogate");
                        }
                        char c1 = buf[nextIndex];
                        if((c1 & 0xFC00) != 0xDC00) {
                            throw new JsonSerializerException("illegal high surrogate without low surrogate");
                        }
                        position = serializeCharToHeap4(c, c1, bytes, position);
                        index = nextIndex;
                    } else if(v == 0xDC00) {
                        throw new JsonSerializerException("illegal low surrogate without high surrogate");
                    } else {
                        position = serializeCharToHeap3(c, bytes, position);
                    }
                }
            }
            return position;
        }

        private static int serializeCharToHeap(char value, byte[] bytes, int position) {
            int v = ESCAPE_TABLE[value];
            if (v == 0) {
                bytes[position++] = (byte) value;
                return position;
            }
            bytes[position++] = (byte) '\\';
            if (v > 0) {
                bytes[position++] = (byte) v;
                return position;
            }
            bytes[position++] = (byte) 'u';
            bytes[position++] = (byte) '0';
            bytes[position++] = (byte) '0';
            bytes[position++] = HEX_BYTES[value >>> 4];
            bytes[position++] = HEX_BYTES[value & 0xF];
            return position;
        }

        private static int serializeCharToHeap2(char value, byte[] bytes, int position) {
            bytes[position++] = (byte) (0xC0 | (value >> 6));
            bytes[position++] = (byte) (0x80 | (value & 0x3F));
            return position;
        }

        private static int serializeCharToHeap3(char value, byte[] bytes, int position) {
            bytes[position++] = (byte) (0xE0 | (value >> 12));
            bytes[position++] = (byte) (0x80 | ((value >> 6) & 0x3F));
            bytes[position++] = (byte) (0x80 | (value & 0x3F));
            return position;
        }

        private static int serializeCharToHeap4(char highSurrogate, char lowSurrogate, byte[] bytes, int position) {
            int cp = Character.toCodePoint(highSurrogate, lowSurrogate);
            bytes[position++] = (byte) (0xF0 | (cp >> 18));
            bytes[position++] = (byte) (0x80 | ((cp >> 12) & 0x3F));
            bytes[position++] = (byte) (0x80 | ((cp >> 6) & 0x3F));
            bytes[position++] = (byte) (0x80 | (cp & 0x3F));
            return position;
        }

        @Override
        public void commit() {
            heapWriteBuffer.setRawByteArray(bytes);
            heapWriteBuffer.setPosition(position);
        }
    }

    public static final class JsonSerializerSegmentContext extends JsonSerializerContext {
        private final SegmentWriteBuffer segmentWriteBuffer;
        private MemorySegment segment;
        private long position;

        public JsonSerializerSegmentContext(JsonSerializerOption option, SegmentWriteBuffer segmentWriteBuffer) {
            super(option);
            this.segmentWriteBuffer = segmentWriteBuffer;
            this.segment = segmentWriteBuffer.rawSegment();
            this.position = segmentWriteBuffer.longPosition();
        }

        private void ensureCapacity(long requiredCapacity) {
            long nextPosition = Math.addExact(position, requiredCapacity);
            long segmentByteSize = segment.byteSize();
            if (nextPosition > segmentByteSize) {
                long newLength = Math.max(Math.addExact(segmentByteSize, segmentByteSize), nextPosition);
                long limit = segmentWriteBuffer.rawLimit();
                if(newLength > limit) {
                    throw new JsonSerializerException("reaching segment buffer limit : " + limit);
                }
                MemorySegment newSegment = segmentWriteBuffer.rawAlloc().allocate(newLength);
                MemorySegment.copy(segment, 0L, newSegment, 0L, position);
                segment = newSegment;
            }
        }

        @Override
        public void append(byte b) {
            ensureCapacity(1L);
            SegmentAccess.setByte(segment, position++, b);
        }

        @Override
        public void append(byte b1, byte b2) {
            ensureCapacity(2L);
            SegmentAccess.setByte(segment, position++, b1);
            SegmentAccess.setByte(segment, position++, b2);
        }

        @Override
        public void serializeNull() {
            ensureCapacity(4L);
            SegmentAccess.setByte(segment, position++, (byte) 'n');
            SegmentAccess.setByte(segment, position++, (byte) 'u');
            SegmentAccess.setByte(segment, position++, (byte) 'l');
            SegmentAccess.setByte(segment, position++, (byte) 'l');
        }

        @Override
        public void serializeBoolean(boolean value) {
            ensureCapacity(5L);
            if(value) {
                SegmentAccess.setByte(segment, position++, (byte) 't');
                SegmentAccess.setByte(segment, position++, (byte) 'r');
                SegmentAccess.setByte(segment, position++, (byte) 'u');
            } else {
                SegmentAccess.setByte(segment, position++, (byte) 'f');
                SegmentAccess.setByte(segment, position++, (byte) 'a');
                SegmentAccess.setByte(segment, position++, (byte) 'l');
                SegmentAccess.setByte(segment, position++, (byte) 's');
            }
            SegmentAccess.setByte(segment, position++, (byte) 'e');
        }

        @Override
        public void serializeChar(char value) {
            if(Character.isSurrogate(value)) {
                throw new JsonSerializerException("surrogate not supported as single char : " + value);
            }
            ensureCapacity(8L);
            SegmentAccess.setByte(segment, position++, (byte) '"');
            if(value < 0x80) {
                position = serializeCharToSegment(value, segment, position);
            } else if(value < 0x800) {
                position = serializeCharToSegment2(value, segment, position);
            } else {
                position = serializeCharToSegment3(value, segment, position);
            }
            SegmentAccess.setByte(segment, position++, (byte) '"');
        }

        @Override
        public void serializeInt(int value) {
            ensureCapacity(JsonNumberUtil.MIN_INT_BYTES.length);
            position = JsonNumberUtil.writeIntToSegment(value, segment, position);
        }

        @Override
        public void serializeLong(long value) {
            ensureCapacity(JsonNumberUtil.MIN_LONG_BYTES.length);
            position = JsonNumberUtil.writeLongToSegment(value, segment, position);
        }

        @Override
        public void serializeFloat(float value) {
            ensureCapacity(JsonNumberUtil.MAX_FLOAT_CAPACITY);
            position = JsonNumberUtil.writeFloatToSegment(value, segment, position);
        }

        @Override
        public void serializeDouble(double value) {
            ensureCapacity(JsonNumberUtil.MAX_DOUBLE_CAPACITY);
            position = JsonNumberUtil.writeDoubleToSegment(value, segment, position);
        }

        @Override
        public void serializePrefix(byte pre, int indent) {
            final JsonIndentationLevel indentationLevel = option.indentationLevel();
            if (indentationLevel == JsonIndentationLevel.NONE) {
                ensureCapacity(1L);
                SegmentAccess.setByte(segment, position++, pre);
                return;
            }
            final int spaces = (indentationLevel == JsonIndentationLevel.TWO ? 2 : 4) * indent;
            ensureCapacity(2L + spaces); // no overflow
            SegmentAccess.setByte(segment, position++, pre);
            SegmentAccess.setByte(segment, position++, (byte) '\n');
            segment.asSlice(position, spaces).fill((byte) ' ');
            position += spaces;
        }

        @Override
        public void serializeSuffix(byte post, int indent) {
            final JsonIndentationLevel indentationLevel = option.indentationLevel();
            if (indentationLevel == JsonIndentationLevel.NONE) {
                ensureCapacity(1L);
                SegmentAccess.setByte(segment, position++, post);
                return;
            }
            final int spaces = (indentationLevel == JsonIndentationLevel.TWO ? 2 : 4) * indent;
            ensureCapacity(2L + spaces); // no overflow
            SegmentAccess.setByte(segment, position++, (byte) '\n');
            segment.asSlice(position, spaces).fill((byte) ' ');
            position += spaces;
            SegmentAccess.setByte(segment, position++, post);
        }

        @Override
        public void serializeNonEscapedUtf8BytesAsStr(byte[] utf8Bytes) {
            ensureCapacity(Math.addExact(2L, utf8Bytes.length));
            SegmentAccess.setByte(segment, position++, (byte) '"');
            MemorySegment.copy(utf8Bytes, 0, segment, ValueLayout.JAVA_BYTE, position, utf8Bytes.length);
            position += utf8Bytes.length;
            SegmentAccess.setByte(segment, position++, (byte) '"');
        }

        @Override
        public void serializeEscapedUtf8BytesAsStr(byte[] utf8Bytes) {
            ensureCapacity(Math.addExact(Math.multiplyExact(utf8Bytes.length, 6), 2));
            SegmentAccess.setByte(segment, position++, (byte) '"');
            if(utf8Bytes.length != 0) {
                int start = 0;
                for(int index = 0; index < utf8Bytes.length; index++) {
                    byte b = utf8Bytes[index];
                    byte v = ESCAPE_TABLE[b & 0xFF];
                    if (v == 0) {
                        continue;
                    }
                    if (index > start) {
                        int available = index - start;
                        MemorySegment.copy(utf8Bytes, start, segment, ValueLayout.JAVA_BYTE, position, available);
                        position += available;
                    }
                    SegmentAccess.setByte(segment, position++, (byte) '\\');
                    if(v > 0) {
                        SegmentAccess.setByte(segment, position++, v);
                    } else {
                        SegmentAccess.setByte(segment, position++, (byte) 'u');
                        SegmentAccess.setByte(segment, position++, (byte) '0');
                        SegmentAccess.setByte(segment, position++, (byte) '0');
                        SegmentAccess.setByte(segment, position++, HEX_BYTES[(b >>> 4) & 0xF]);
                        SegmentAccess.setByte(segment, position++, HEX_BYTES[b & 0xF]);
                    }
                    start = index + 1;
                }
                if (start < utf8Bytes.length) {
                    int available = utf8Bytes.length - start;
                    MemorySegment.copy(utf8Bytes, start, segment, ValueLayout.JAVA_BYTE, position, available);
                    position += available;
                }
            }
            SegmentAccess.setByte(segment, position++, (byte) '"');
        }

        @Override
        public void serializeRawUtf8Bytes(byte[] utf8Bytes) {
            ensureCapacity(utf8Bytes.length);
            MemorySegment.copy(utf8Bytes, 0, segment, ValueLayout.JAVA_BYTE, position, utf8Bytes.length);
            position += utf8Bytes.length;
        }

        @Override
        public void serializeEscapedString(String str) {
            ensureCapacity(Math.addExact(Math.multiplyExact(str.length(), 6), 2));
            SegmentAccess.setByte(segment, position++, (byte) '"');
            if(!str.isEmpty()) {
                if(str.length() < SHORT_SPECIES.length()) {
                    position = serializeStrToSegment(str, segment, position);
                }else {
                    position = serializeLongStrToSegment(str, segment, position);
                }
            }
            SegmentAccess.setByte(segment, position++, (byte) '"');
        }

        private static long serializeStrToSegment(String str, MemorySegment segment, long position) {
            final int len = str.length();
            int index = 0;
            while (index < len) {
                char c = str.charAt(index++);
                if(c < 0x80) {
                    position = serializeCharToSegment(c, segment, position);
                } else if(c < 0x800) {
                    position = serializeCharToSegment2(c, segment, position);
                } else {
                    // jdk string allows single surrogate, we still need to check them
                    final int v = c & 0xFC00;
                    if(v == 0xD800) {
                        if(index == len) {
                            throw new JsonSerializerException("illegal high surrogate without low surrogate");
                        }
                        char c1 = str.charAt(index++);
                        if((c1 & 0xFC00) != 0xDC00) {
                            throw new JsonSerializerException("illegal high surrogate without low surrogate");
                        }
                        position = serializeCharToSegment4(c, c1, segment, position);
                    } else if(v == 0xDC00) {
                        throw new JsonSerializerException("illegal low surrogate without high surrogate");
                    } else {
                        position = serializeCharToSegment3(c, segment, position);
                    }
                }
            }
            return position;
        }

        private long serializeLongStrToSegment(String str, MemorySegment segment, long position) {
            final int len = str.length();
            final int alignedLen = Math.addExact(len, VEC_MASK) & (~VEC_MASK);
            final char[] buf = alignedBuf(alignedLen);
            str.getChars(0, len, buf, 0);
            int index = 0;
            for( ; index < alignedLen; index += SHORT_SPECIES.length()) {
                ShortVector shortVector = ShortVector.fromCharArray(SHORT_SPECIES, buf, index);
                ByteVector byteVector = (ByteVector) shortVector.convertShape(VectorOperators.S2B, BYTE_SPECIES, 0);
                byteVector.intoMemorySegment(segment, position + index, ByteOrder.nativeOrder()); // byteOrder will be ignored
                VectorMask<Short> mask = shortVector.compare(VectorOperators.LT, (short) 0x20)
                        .or(shortVector.compare(VectorOperators.GT, (short) 0x7E))
                        .or(shortVector.compare(VectorOperators.EQ, (short) 0x22))
                        .or(shortVector.compare(VectorOperators.EQ, (short) 0x5C));
                if (ESCAPE_SLASH) {
                    mask = mask.or(shortVector.compare(VectorOperators.EQ, (short) 0x2F));
                }
                int asciiCount = mask.firstTrue();
                if(asciiCount != SHORT_SPECIES.length()) {
                    index += asciiCount;
                    break ;
                }
            }
            if(index >= len) {
                return position + len;
            }
            if(FILTER_SURR) {
                int sIndex = index & (~VEC_MASK);
                for( ; sIndex < alignedLen; sIndex += SHORT_SPECIES.length()) {
                    VectorMask<Short> mask = ShortVector.fromCharArray(SHORT_SPECIES, buf, sIndex).lanewise(VectorOperators.AND, (short) 0xF800)
                            .compare(VectorOperators.EQ, (short) 0xD800);
                    int nonSurrCount = mask.firstTrue();
                    if(nonSurrCount != SHORT_SPECIES.length()) {
                        sIndex += nonSurrCount;
                        break ;
                    }
                }
                if(sIndex >= len) {
                    return serializeNonSurrCharsToSegment(buf, index, len, segment, position + index);
                }
            }
            return serializeCharsToSegment(buf, index, len, segment, position + index);
        }

        private static long serializeNonSurrCharsToSegment(char[] buf, int index, int len, MemorySegment segment, long position) {
            for( ; index < len; index++) {
                char c = buf[index];
                if(c < 0x80) {
                    position = serializeCharToSegment(c, segment, position);
                } else if(c < 0x800) {
                    position = serializeCharToSegment2(c, segment, position);
                } else {
                    position = serializeCharToSegment3(c, segment, position);
                }
            }
            return position;
        }

        private static long serializeCharsToSegment(char[] buf, int index, int len, MemorySegment segment, long position) {
            for( ; index < len; index++) {
                char c = buf[index];
                if(c < 0x80) {
                    position = serializeCharToSegment(c, segment, position);
                } else if(c < 0x800) {
                    position = serializeCharToSegment2(c, segment, position);
                } else {
                    // jdk string allows single surrogate, we still need to check them
                    final int v = c & 0xFC00;
                    if(v == 0xD800) {
                        final int nextIndex = index + 1;
                        if(nextIndex == len) {
                            throw new JsonSerializerException("illegal high surrogate without low surrogate");
                        }
                        char c1 = buf[nextIndex];
                        if((c1 & 0xFC00) != 0xDC00) {
                            throw new JsonSerializerException("illegal high surrogate without low surrogate");
                        }
                        position = serializeCharToSegment4(c, c1, segment, position);
                        index = nextIndex;
                    } else if(v == 0xDC00) {
                        throw new JsonSerializerException("illegal low surrogate without high surrogate");
                    } else {
                        position = serializeCharToSegment3(c, segment, position);
                    }
                }
            }
            return position;
        }

        private static long serializeCharToSegment(char value, MemorySegment segment, long position) {
            int v = ESCAPE_TABLE[value];
            if (v == 0) {
                SegmentAccess.setByte(segment, position++, (byte) value);
                return position;
            }
            SegmentAccess.setByte(segment, position++, (byte) '\\');
            if (v > 0) {
                SegmentAccess.setByte(segment, position++, (byte) v);
                return position;
            }
            SegmentAccess.setByte(segment, position++, (byte) 'u');
            SegmentAccess.setByte(segment, position++, (byte) '0');
            SegmentAccess.setByte(segment, position++, (byte) '0');
            SegmentAccess.setByte(segment, position++, HEX_BYTES[value >>> 4]);
            SegmentAccess.setByte(segment, position++, HEX_BYTES[value & 0xF]);
            return position;
        }

        private static long serializeCharToSegment2(char value, MemorySegment bytes, long position) {
            SegmentAccess.setByte(bytes, position++, (byte) (0xC0 | (value >> 6)));
            SegmentAccess.setByte(bytes, position++, (byte) (0x80 | (value & 0x3F)));
            return position;
        }

        private static long serializeCharToSegment3(char value, MemorySegment segment, long position) {
            SegmentAccess.setByte(segment, position++, (byte) (0xE0 | (value >> 12)));
            SegmentAccess.setByte(segment, position++, (byte) (0x80 | ((value >> 6) & 0x3F)));
            SegmentAccess.setByte(segment, position++, (byte) (0x80 | (value & 0x3F)));
            return position;
        }

        private static long serializeCharToSegment4(char highSurrogate, char lowSurrogate, MemorySegment segment, long position) {
            int cp = Character.toCodePoint(highSurrogate, lowSurrogate);
            SegmentAccess.setByte(segment, position++, (byte) (0xF0 | (cp >> 18)));
            SegmentAccess.setByte(segment, position++, (byte) (0x80 | ((cp >> 12) & 0x3F)));
            SegmentAccess.setByte(segment, position++, (byte) (0x80 | ((cp >> 6) & 0x3F)));
            SegmentAccess.setByte(segment, position++, (byte) (0x80 | (cp & 0x3F)));
            return position;
        }

        @Override
        public void commit() {
            segmentWriteBuffer.setRawSegment(segment);
            segmentWriteBuffer.setPosition(position);
        }
    }
}
