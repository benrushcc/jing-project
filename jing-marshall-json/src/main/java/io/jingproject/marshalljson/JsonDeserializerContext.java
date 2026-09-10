package io.jingproject.marshalljson;

import io.jingproject.common.*;
import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.MarshallInfo;
import io.jingproject.marshall.Marshalls;
import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;

public sealed abstract class JsonDeserializerContext permits JsonDeserializerContext.JsonDeserializerHeapContext, JsonDeserializerContext.JsonDeserializerSegmentContext {
    public static final int CHAR_BUFFER_INITIAL_SIZE = 128;
    public static final int BYTE_BUFFER_INITIAL_SIZE = 16;
    protected static final VectorSpecies<Short> SHORT_SPECIES;
    protected static final VectorSpecies<Byte> BYTE_SPECIES;
    protected static final int COMPACT_TRUE = Utils.compact(Utils.compact((byte) 't', (byte) 'r'), Utils.compact((byte) 'u', (byte) 'e'));
    protected static final int COMPACT_ALSE = Utils.compact(Utils.compact((byte) 'a', (byte) 'l'), Utils.compact((byte) 's', (byte) 'e')); // 'f' should be handled by firstByte
    protected static final int COMPACT_NULL = Utils.compact(Utils.compact((byte) 'n', (byte) 'u'), Utils.compact((byte) 'l', (byte) 'l'));
    protected static final int OBJ_ARR_INITIAL_SIZE = 8;
    protected static final byte[] ESCAPE_TABLE = makeEscapeTable();
    protected static final byte[] HEX_TABLE = makeHexTable();
    protected static final Map<Class<?>, JsonDeserializeFunc> BUILTIN_DESERIALIZE_OBJ_FUNC_MAP;
    protected static final Map<Class<?>, JsonDeserializeFunc> BUILTIN_DESERIALIZE_ARRAY_FUNC_MAP;

    static {
        try {
            Class<Os> _ = MethodHandles.lookup().ensureInitialized(Os.class);
        } catch (IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
        int vecSize = Integer.parseInt(System.getProperty("jing.marshalljson.deserialize.vecsize", "-1"));
        if (vecSize < 0) {
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

    static {
        Map<Class<?>, JsonDeserializeFunc> r = new HashMap<>();
        r.put(Byte.class, (b, c) -> {
            c.setObj(c.deserializeByte(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(Boolean.class, (b, c) -> {
            c.setObj(c.deserializeBoolean(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(Short.class, (b, c) -> {
            c.setObj(c.deserializeShort(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(Character.class, (b, c) -> {
            c.setObj(c.deserializeChar(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(Integer.class, (b, c) -> {
            c.setObj(c.deserializeInt(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(Long.class, (b, c) -> {
            c.setObj(c.deserializeLong(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(Float.class, (b, c) -> {
            c.setObj(c.deserializeFloat(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(Double.class, (b, c) -> {
            c.setObj(c.deserializeDouble(b));
            return JsonDeserializeResult.Continue;
        });
        JsonDeserializeFunc strFunc = (b, c) -> {
            c.setObj(c.deserializeString(b));
            return JsonDeserializeResult.Continue;
        };
        r.put(CharSequence.class, strFunc);
        r.put(String.class, strFunc);
        r.put(JsonPrimitiveType.class, (b, c) -> {
            c.setObj(c.deserializeJsonPrimitiveType(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(JsonBoolType.class, (b, c) -> {
            c.setObj(c.deserializeJsonBoolType(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(JsonNumberType.class, (b, c) -> {
            c.setObj(c.deserializeJsonNumberType(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(JsonStrType.class, (b, c) -> {
            c.setObj(c.deserializeJsonStrType(b));
            return JsonDeserializeResult.Continue;
        });
        BUILTIN_DESERIALIZE_OBJ_FUNC_MAP = Map.copyOf(r);
    }

    static {
        Map<Class<?>, JsonDeserializeFunc> r = new HashMap<>();
        r.put(byte[].class, (b, c) -> {
            c.setObj(c.deserializeByteArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(boolean[].class, (b, c) -> {
            c.setObj(c.deserializeBooleanArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(short[].class, (b, c) -> {
            c.setObj(c.deserializeShortArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(char[].class, (b, c) -> {
            c.setObj(c.deserializeCharArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(int[].class, (b, c) -> {
            c.setObj(c.deserializeIntArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(long[].class, (b, c) -> {
            c.setObj(c.deserializeLongArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(float[].class, (b, c) -> {
            c.setObj(c.deserializeFloatArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(double[].class, (b, c) -> {
            c.setObj(c.deserializeDoubleArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(Byte[].class, (b, c) -> {
            c.setObj(c.deserializeByteWrapperArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(Boolean[].class, (b, c) -> {
            c.setObj(c.deserializeBooleanWrapperArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(Short[].class, (b, c) -> {
            c.setObj(c.deserializeShortWrapperArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(Character[].class, (b, c) -> {
            c.setObj(c.deserializeCharWrapperArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(Integer[].class, (b, c) -> {
            c.setObj(c.deserializeIntWrapperArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(Long[].class, (b, c) -> {
            c.setObj(c.deserializeLongWrapperArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(Float[].class, (b, c) -> {
            c.setObj(c.deserializeFloatWrapperArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(Double[].class, (b, c) -> {
            c.setObj(c.deserializeDoubleWrapperArray(b));
            return JsonDeserializeResult.Continue;
        });
        JsonDeserializeFunc strArrayFunc = (b, c) -> {
            c.setObj(c.deserializeStringArray(b));
            return JsonDeserializeResult.Continue;
        };
        r.put(CharSequence[].class, strArrayFunc);
        r.put(String[].class, strArrayFunc);
        r.put(JsonPrimitiveType[].class, (b, c) -> {
            c.setObj(c.deserializeJsonPrimitiveTypeArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(JsonBoolType[].class, (b, c) -> {
            c.setObj(c.deserializeJsonBoolTypeArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(JsonNumberType[].class, (b, c) -> {
            c.setObj(c.deserializeJsonNumberTypeArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(JsonStrType[].class, (b, c) -> {
            c.setObj(c.deserializeJsonStrTypeArray(b));
            return JsonDeserializeResult.Continue;
        });
        BUILTIN_DESERIALIZE_ARRAY_FUNC_MAP = Map.copyOf(r);
    }

    public static JsonDeserializerContext newContext(JsonDeserializerOption option, ReadBuffer readBuffer) {
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> new JsonDeserializerHeapContext(option, heapReadBuffer);
            case SegmentReadBuffer segmentReadBuffer -> new JsonDeserializerSegmentContext(option, segmentReadBuffer);
        };
    }

    public static void checkStrStart(byte b) {
        if(b != (byte) '"') {
            throw new JsonDeserializerException("not a string start : " + b);
        }
    }

    public static void checkArrayStart(byte b) {
        if(b != (byte) '[') {
            throw new JsonDeserializerException("not an array start : " + b);
        }
    }

    public static void checkObjStart(byte b) {
        if(b != (byte) '{') {
            throw new JsonDeserializerException("not an object start : " + b);
        }
    }

    private static byte[] makeEscapeTable() {
        byte[] table = new byte[Byte.MAX_VALUE - Byte.MIN_VALUE + 1];
        table['\"'] = '\"';
        table['\\'] = '\\';
        table['/'] = '/';
        table['b'] = '\b';
        table['f'] = '\f';
        table['n'] = '\n';
        table['r'] = '\r';
        table['t'] = '\t';
        return table;
    }

    private static byte[] makeHexTable() {
        byte[] table = new byte[Byte.MAX_VALUE - Byte.MIN_VALUE + 1];
        Arrays.fill(table, Byte.MIN_VALUE);
        for(int i = '0'; i <= '9'; i++) {
            table[i] = (byte) (i - '0');
        }
        for(int i = 'A'; i <= 'F'; i++) {
            table[i] = (byte) (i - 'A' + 10);
        }
        for(int i = 'a'; i <= 'f'; i++) {
            table[i] = (byte) (i - 'a' + 10);
        }
        return table;
    }

    protected final JsonDeserializerOption option;
    protected char[] charBuffer;
    protected byte[] byteBuffer;
    protected Object obj;
    protected Class<?> type;
    protected Object[] arr;

    protected JsonDeserializerContext(JsonDeserializerOption option) {
        this.option = option;
        this.charBuffer = new char[option.charBufferSize()];
    }

    public final JsonDeserializerOption option() {
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

    protected final void ensureCharBufferCapacity(int requiredCapacity) {
        int currentLength = charBuffer.length;
        if(requiredCapacity > currentLength) {
            char[] newBuffer = new char[Math.addExact(currentLength, currentLength)]; // at least grow CHAR_BUFFER_INITIAL_SIZE
            System.arraycopy(charBuffer, 0, newBuffer, 0, currentLength);
            charBuffer = newBuffer;
        }
    }

    protected final void ensureByteBufferCapacity(int requiredCapacity) {
        if(byteBuffer == null) {
            byteBuffer = new byte[BYTE_BUFFER_INITIAL_SIZE]; // big enough for all primitive types
        }else if(requiredCapacity > byteBuffer.length) {
            byte[] newBuffer = new byte[Math.addExact(byteBuffer.length, byteBuffer.length)]; // at least grow BYTE_BUFFER_INITIAL_SIZE
            System.arraycopy(byteBuffer, 0, newBuffer, 0, byteBuffer.length);
            byteBuffer = newBuffer;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public abstract boolean validate();

    public abstract void rewind();

    public abstract byte nextValuableByte();

    public abstract void deserializeNull();

    public final byte deserializeByte(byte firstByte) {
        int value = deserializeInt(firstByte);
        if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            return (byte) value;
        }
        throw new JsonDeserializerException("byte value overflow : " + value);
    }

    public abstract boolean deserializeBoolean(byte firstByte);

    public final short deserializeShort(byte firstByte) {
        int value = deserializeInt(firstByte);
        if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            return (short) value;
        }
        throw new JsonDeserializerException("short value overflow : " + value);
    }

    public abstract char deserializeChar(byte firstByte);

    public abstract int deserializeInt(byte firstByte);

    public abstract long deserializeLong(byte firstByte);

    public abstract float deserializeFloat(byte firstByte);

    public abstract double deserializeDouble(byte firstByte);

    public final byte[] deserializeByteArray(byte firstByte) {
        checkArrayStart(firstByte);
        byte b = nextValuableByte();
        if (b == (byte) ']') {
            return Utils.emptyByteArray();
        }
        final int end = option.maxArrayElements();
        for(int i = 0; i < end; ) {
            byte v = deserializeByte(b);
            int i1 = Math.incrementExact(i);
            ensureByteBufferCapacity(i1);
            byteBuffer[i] = v;
            i = i1;
            b = nextValuableByte();
            if(b == (byte) ']') {
                return Arrays.copyOfRange(byteBuffer, 0, i);
            } else if(b == (byte) ',') {
                b = nextValuableByte();
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, exceeded limit : " + end);
    }

    public final boolean[] deserializeBooleanArray(byte firstByte) {
        checkArrayStart(firstByte);
        byte b = nextValuableByte();
        if (b == (byte) ']') {
            return Utils.emptyBooleanArray();
        }
        final int end = option.maxArrayElements();
        for(int i = 0; i < end; ) {
            boolean v = deserializeBoolean(b);
            int i1 = Math.incrementExact(i);
            ensureByteBufferCapacity(i1);
            byteBuffer[i] = v ? Byte.MAX_VALUE : Byte.MIN_VALUE;
            i = i1;
            b = nextValuableByte();
            if(b == (byte) ']') {
                boolean[] r = new boolean[i];
                for(int t = 0; t < i; t++) {
                    r[t] = byteBuffer[t] > 0;
                }
                return r;
            } else if(b == (byte) ',') {
                b = nextValuableByte();
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, exceeded limit : " + end);
    }

    public final short[] deserializeShortArray(byte firstByte) {
        checkArrayStart(firstByte);
        byte b = nextValuableByte();
        if (b == (byte) ']') {
            return Utils.emptyShortArray();
        }
        final int end = Math.multiplyExact(option.maxArrayElements(), 2);
        for(int i = 0; i < end; ) {
            short v = deserializeShort(b);
            int i1 = Math.addExact(i, 2);
            ensureByteBufferCapacity(i1);
            ArrayAccess.setShort(byteBuffer, i, v);
            i = i1;
            b = nextValuableByte();
            if(b == (byte) ']') {
                int len = i / 2;
                short[] r = new short[len];
                MemorySegment.copy(MemorySegment.ofArray(byteBuffer), ValueLayout.JAVA_SHORT_UNALIGNED, 0, r, 0, len);
                return r;
            } else if(b == (byte) ',') {
                b = nextValuableByte();
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, exceeded limit : " + end);
    }

    public final char[] deserializeCharArray(byte firstByte) {
        checkArrayStart(firstByte);
        byte b = nextValuableByte();
        if (b == (byte) ']') {
            return Utils.emptyCharArray();
        }
        final int end = option.maxArrayElements();
        for(int i = 0; i < end; ) {
            checkStrStart(b);
            char v = deserializeChar(b);
            int i1 = Math.incrementExact(i);
            ensureCharBufferCapacity(i1);
            charBuffer[i] = v;
            i = i1;
            b = nextValuableByte();
            if(b == (byte) ']') {
                char[] r = new char[i];
                System.arraycopy(charBuffer, 0, r, 0, i);
                return r;
            } else if(b == (byte) ',') {
                b = nextValuableByte();
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, exceeded limit : " + end);
    }

    public final int[] deserializeIntArray(byte firstByte) {
        checkArrayStart(firstByte);
        byte b = nextValuableByte();
        if (b == (byte) ']') {
            return Utils.emptyIntArray();
        }
        final int end = Math.multiplyExact(option.maxArrayElements(), 4);
        for(int i = 0; i < end; ) {
            int v = deserializeInt(b);
            int i1 = Math.addExact(i, 4);
            ensureByteBufferCapacity(i1);
            ArrayAccess.setInt(byteBuffer, i, v);
            i = i1;
            b = nextValuableByte();
            if(b == (byte) ']') {
                int len = i / 4;
                int[] r = new int[len];
                MemorySegment.copy(MemorySegment.ofArray(byteBuffer), ValueLayout.JAVA_INT_UNALIGNED, 0, r, 0, len);
                return r;
            } else if(b == (byte) ',') {
                b = nextValuableByte();
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, exceeded limit : " + end);
    }

    public final long[] deserializeLongArray(byte firstByte) {
        checkArrayStart(firstByte);
        byte b = nextValuableByte();
        if (b == (byte) ']') {
            return Utils.emptyLongArray();
        }
        final int end = Math.multiplyExact(option.maxArrayElements(), 8);
        for(int i = 0; i < end; ) {
            long v = deserializeLong(b);
            int i1 = Math.addExact(i, 8);
            ensureByteBufferCapacity(i1);
            ArrayAccess.setLong(byteBuffer, i, v);
            i = i1;
            b = nextValuableByte();
            if(b == (byte) ']') {
                int len = i / 8;
                long[] r = new long[len];
                MemorySegment.copy(MemorySegment.ofArray(byteBuffer), ValueLayout.JAVA_LONG_UNALIGNED, 0, r, 0, len);
                return r;
            } else if(b == (byte) ',') {
                b = nextValuableByte();
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, exceeded limit : " + end);
    }

    public final float[] deserializeFloatArray(byte firstByte) {
        checkArrayStart(firstByte);
        byte b = nextValuableByte();
        if (b == (byte) ']') {
            return Utils.emptyFloatArray();
        }
        final int end = Math.multiplyExact(option.maxArrayElements(), 4);
        for(int i = 0; i < end; ) {
            float v = deserializeFloat(b);
            int i1 = Math.addExact(i, 4);
            ensureByteBufferCapacity(i1);
            ArrayAccess.setFloat(byteBuffer, i, v);
            i = i1;
            b = nextValuableByte();
            if(b == (byte) ']') {
                int len = i / 4;
                float[] r = new float[len];
                MemorySegment.copy(MemorySegment.ofArray(byteBuffer), ValueLayout.JAVA_FLOAT_UNALIGNED, 0, r, 0, len);
                return r;
            } else if(b == (byte) ',') {
                b = nextValuableByte();
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, exceeded limit : " + end);
    }

    public final double[] deserializeDoubleArray(byte firstByte) {
        checkArrayStart(firstByte);
        byte b = nextValuableByte();
        if (b == (byte) ']') {
            return Utils.emptyDoubleArray();
        }
        final int maxArrayElements = Math.multiplyExact(option.maxArrayElements(), 8);
        for(int i = 0; i < maxArrayElements; ) {
            double v = deserializeDouble(b);
            int i1 = Math.addExact(i, 8);
            ensureByteBufferCapacity(i1);
            ArrayAccess.setDouble(byteBuffer, i, v);
            i = i1;
            b = nextValuableByte();
            if(b == (byte) ']') {
                int len = i / 8;
                double[] r = new double[len];
                MemorySegment.copy(MemorySegment.ofArray(byteBuffer), ValueLayout.JAVA_DOUBLE_UNALIGNED, 0, r, 0, len);
                return r;
            } else if(b == (byte) ',') {
                b = nextValuableByte();
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, exceeded limit : " + maxArrayElements);
    }

    protected final <T> T[] deserializeObjArray(ElementDeserializer<T> deserializer, IntFunction<T[]> arrayFactory) {
        byte b = nextValuableByte();
        if(b == (byte) ']') {
            return arrayFactory.apply(0);
        }
        if(arr == null) {
            arr = new Object[OBJ_ARR_INITIAL_SIZE];
        }
        final int maxArrayElements = option.maxArrayElements();
        for (int i = 0; i < maxArrayElements; ) {
            if(i >= arr.length) {
                Object[] newArr = new Object[Math.addExact(arr.length, arr.length)];
                System.arraycopy(arr, 0, newArr, 0, arr.length);
                arr = newArr;
            }
            if(b == (byte) 'n') {
                deserializeNull();
                arr[i++] = null;
            } else {
                arr[i++] = deserializer.deserialize(this, b);
            }
            b = nextValuableByte();
            if(b == (byte) ']') {
                T[] r = arrayFactory.apply(i);
                //noinspection SuspiciousSystemArraycopy
                System.arraycopy(arr, 0, r, 0, i);
                return r;
            } else if(b == (byte) ',') {
                b = nextValuableByte();
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + maxArrayElements);
    }

    public final Byte[] deserializeByteWrapperArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(JsonDeserializerContext::deserializeByte, Byte[]::new);
    }

    public final Boolean[] deserializeBooleanWrapperArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(JsonDeserializerContext::deserializeBoolean, Boolean[]::new);
    }

    public final Short[] deserializeShortWrapperArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(JsonDeserializerContext::deserializeShort, Short[]::new);
    }

    public final Character[] deserializeCharWrapperArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(JsonDeserializerContext::deserializeChar, Character[]::new);
    }

    public final Integer[] deserializeIntWrapperArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(JsonDeserializerContext::deserializeInt, Integer[]::new);
    }

    public final Long[] deserializeLongWrapperArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(JsonDeserializerContext::deserializeLong, Long[]::new);
    }

    public final Float[] deserializeFloatWrapperArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(JsonDeserializerContext::deserializeFloat, Float[]::new);
    }

    public final Double[] deserializeDoubleWrapperArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(JsonDeserializerContext::deserializeDouble, Double[]::new);
    }

    public abstract String deserializeString(byte firstByte);

    public final String[] deserializeStringArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(JsonDeserializerContext::deserializeString, String[]::new);
    }

    public final JsonPrimitiveType deserializeJsonPrimitiveType(byte firstByte) {
        if (firstByte == (byte) 't' || firstByte == (byte) 'f') {
            return deserializeJsonBoolType(firstByte);
        } else if(firstByte == '"') {
            return deserializeJsonStrType(firstByte);
        } else {
            return deserializeJsonNumberType(firstByte);
        }
    }

    public final JsonPrimitiveType[] deserializeJsonPrimitiveTypeArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(JsonDeserializerContext::deserializeJsonPrimitiveType, JsonPrimitiveType[]::new);
    }

    public final JsonBoolType deserializeJsonBoolType(byte firstByte) {
        return new JsonBoolType(deserializeBoolean(firstByte));
    }

    public final JsonBoolType[] deserializeJsonBoolTypeArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(JsonDeserializerContext::deserializeJsonBoolType, JsonBoolType[]::new);
    }

    public abstract JsonNumberType deserializeJsonNumberType(byte firstByte);

    public final JsonNumberType[] deserializeJsonNumberTypeArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(JsonDeserializerContext::deserializeJsonNumberType, JsonNumberType[]::new);
    }

    public final JsonStrType deserializeJsonStrType(byte firstByte) {
        return new JsonStrType(deserializeString(firstByte));
    }

    public final JsonStrType[] deserializeJsonStrTypeArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(JsonDeserializerContext::deserializeJsonStrType, JsonStrType[]::new);
    }

    public final byte skipColon() {
        if (nextValuableByte() != (byte) ':') {
            throw new JsonDeserializerException("colon not found");
        }
        return nextValuableByte();
    }

    public abstract void skipNumber(byte firstByte);

    public abstract void skipString(byte firstByte);

    public abstract MarshallInfo deserializeMarshallInfo(MarshallFacade fc, byte firstByte);

    public abstract void commit();

    public final Enum<?> deserializeEnum(Class<?> enumType, byte firstByte) {
        Enum<?>[] enumConstants = (Enum<?>[]) enumType.getEnumConstants();
        MarshallFacade fc = Marshalls.enumMarshallFacade(enumType);
        if(fc == null) {
            String name = deserializeString(firstByte);
            for (Enum<?> e : enumConstants) {
                if (e.name().equals(name)) {
                    return e;
                }
            }
            throw new JsonDeserializerException("enum item not found for item : " + name);
        }
        MarshallInfo inf = deserializeMarshallInfo(fc, firstByte);
        if(inf == null) {
            throw new JsonDeserializerException("enum item not found for type : " + enumType.getName());
        }
        return enumConstants[inf.index()];
    }

    public final Enum<?>[] deserializeEnumArray(Class<?> enumType, byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray((c, b) -> c.deserializeEnum(enumType, b), Enum<?>[]::new);
    }

    public static JsonDeserializeFunc builtinDeserializeObjFunc(Class<?> rawType) {
        return BUILTIN_DESERIALIZE_OBJ_FUNC_MAP.get(rawType);
    }

    public static JsonDeserializeFunc builtinDeserializeArrayFunc(Class<?> rawType) {
        return BUILTIN_DESERIALIZE_ARRAY_FUNC_MAP.get(rawType);
    }

    // builtin type has the highest priority
    // then check if current type could be override by option
    // enum must be specially treated
    // finally assuming marshallable
    public final JsonDeserializeFunc valueDeserializeFunc(Class<?> rawType) {
        // builtin type has the highest priority
        if (rawType.isArray()) {
            JsonDeserializeFunc builtinDeserializeArrFunc = builtinDeserializeArrayFunc(rawType);
            if (builtinDeserializeArrFunc != null) {
                return builtinDeserializeArrFunc;
            }
            JsonDeserializeFunc customArrFunc = option.customArrFunc(rawType);
            if(customArrFunc != null) {
                return customArrFunc;
            }
            Class<?> componentType = rawType.componentType();
            if(componentType.isEnum()) {
                return (b, c) -> {
                    c.setObj(c.deserializeEnumArray(rawType, b));
                    return JsonDeserializeResult.Continue;
                };
            }
            return (b, c) -> {
                checkArrayStart(b);
                c.setType(componentType);
                return JsonDeserializeResult.NewArr;
            };
        }
        JsonDeserializeFunc builtinDeserializeFunc = builtinDeserializeObjFunc(rawType);
        if(builtinDeserializeFunc != null) {
            return builtinDeserializeFunc;
        }
        // check if current type could be override by option
        JsonDeserializeFunc customFunc = option.customFunc(rawType);
        if(customFunc != null) {
            return customFunc;
        }
        // enum must be specially treated
        if(rawType.isEnum()) {
            return (b, c) -> {
                c.setObj(c.deserializeEnum(rawType, b));
                return JsonDeserializeResult.Continue;
            };
        }
        return (b, c) -> {
            checkObjStart(b);
            c.setType(rawType);
            return JsonDeserializeResult.NewMarshallable;
        };
    }

    @FunctionalInterface
    public interface ElementDeserializer<T> {
        T deserialize(JsonDeserializerContext c, byte firstByte);
    }

    value record Surr(char high, char low) {

    }

    public static final class JsonDeserializerHeapContext extends JsonDeserializerContext {
        private final HeapReadBuffer heapReadBuffer;
        private final byte[] bytes;
        private int position;

        JsonDeserializerHeapContext(JsonDeserializerOption option, HeapReadBuffer heapReadBuffer) {
            super(option);
            this.heapReadBuffer = heapReadBuffer;
            this.bytes = heapReadBuffer.rawByteArray();
            this.position = heapReadBuffer.intPosition();
        }

        @Override
        public boolean validate() {
            return Utf8Validator.validateHeap(bytes, position, bytes.length);
        }

        @Override
        public void rewind() {
            position--;
        }

        @Override
        public byte nextValuableByte() {
            final int end = position + Math.min(option.maxEmptyBytes(), bytes.length - position);
            while (position < end) {
                byte b = bytes[position++];
                if(b != (byte) ' ' && b != '\n' && b != '\t' && b != '\r') {
                    return b;
                }
            }
            throw new JsonDeserializerException("valuable byte not found");
        }

        @Override
        public void deserializeNull() {
            if (position > bytes.length - 3) {
                throw new JsonDeserializerException("eof reached while deserializing null");
            }
            if (ArrayAccess.getInt(bytes, position - 1) != COMPACT_NULL) {
                throw new JsonDeserializerException("illegal null token, position : " + position);
            }
            position += 3;
        }

        @Override
        public boolean deserializeBoolean(byte firstByte) {
            if(firstByte == (byte) 't') {
                if(position > bytes.length - 3) {
                    throw new JsonDeserializerException("eof reached while deserializing boolean 'true' value");
                }
                if(ArrayAccess.getInt(bytes, position - 1) != COMPACT_TRUE) {
                    throw new JsonDeserializerException("illegal boolean 'true' value");
                }
                position += 3;
                return true;
            } else if(firstByte == (byte) 'f') {
                if(position > bytes.length - 4) {
                    throw new JsonDeserializerException("eof reached while deserializing boolean 'false' value");
                }
                if(ArrayAccess.getInt(bytes, position) != COMPACT_ALSE) {
                    throw new JsonDeserializerException("illegal boolean 'false' value");
                }
                position += 4;
                return false;
            } else {
                throw new JsonDeserializerException("not a bool start");
            }
        }

        private char deserializeHexFromHeap() {
            int i1 = HEX_TABLE[bytes[position++] & 0xFF];
            int i2 = HEX_TABLE[bytes[position++] & 0xFF];
            int i3 = HEX_TABLE[bytes[position++] & 0xFF];
            int i4 = HEX_TABLE[bytes[position++] & 0xFF];
            if((i1 | i2 | i3 | i4) < 0) {
                throw new JsonDeserializerException("illegal escaped unicode sequence");
            }
            return (char) ((i1 << 12) | (i2 << 8) | (i3 << 4) | i4);
        }

        private char deserializeEscapedChar() {
            if(position >= bytes.length) {
                throw new JsonDeserializerException("eof reached while deserializing escaped char from heap");
            }
            byte escaped = bytes[position++];
            if(escaped == (byte) 'u') {
                if(position > bytes.length - 4) {
                    throw new JsonDeserializerException("illegal escaped unicode sequence");
                }
                char r = deserializeHexFromHeap();
                if(Character.isSurrogate(r)) {
                    throw new JsonDeserializerException("illegal escaped unicode surrogate sequence");
                }
                return r;
            }
            byte b = ESCAPE_TABLE[escaped & 0xFF];
            if(b == 0) {
                throw new JsonDeserializerException("illegal escaped char from heap");
            }
            return (char) b;
        }

        private char deserializeCharFromHeap2(int i) {
            int i1 = bytes[position++] & 0x3F;
            return (char) (((i & 0x1F) << 6) | i1);
        }

        private char deserializeCharFromHeap3(int i) {
            int i1 = bytes[position++] & 0x3F;
            int i2 = bytes[position++] & 0x3F;
            return (char) (((i & 0x0F) << 12) | (i1 << 6) | i2);
        }

        private Surr deserializeCharFromHeap4(int i) {
            int i1 = bytes[position++] & 0x3F;
            int i2 = bytes[position++] & 0x3F;
            int i3 = bytes[position++] & 0x3F;
            int cp = (((i & 0x07) << 18) | (i1 << 12) | (i2 << 6) | i3) - 0x10000;
            return new Surr((char) (0xD800 | (cp >> 10)), (char) (0xDC00 | (cp & 0x3FF)));
        }

        @Override
        public char deserializeChar(byte firstByte) {
            checkStrStart(firstByte);
            if(position >= bytes.length) {
                throw new JsonDeserializerException("eof reached while deserializing char from heap");
            }
            int i = bytes[position++] & 0xFF;
            char r;
            if(i == '\\') {
                r = deserializeEscapedChar();
            } else if(i < 0x20) {
                throw new JsonDeserializerException("illegal unescaped ascii control byte : " + i);
            } else if(i < 0x80) {
                r = (char) i;
            } else if(i < 0xE0) {
                r = deserializeCharFromHeap2(i);
            } else if(i < 0xF0) {
                r = deserializeCharFromHeap3(i);
            } else {
                throw new JsonDeserializerException("illegal surrogate start : " + i);
            }
            if(position >= bytes.length || bytes[position++] != (byte) '"') {
                throw new JsonDeserializerException("not a single char");
            }
            return r;
        }

        @Override
        public int deserializeInt(byte firstByte) {
            JsonNumberUtil.IntIntPair p = JsonNumberUtil.readIntFromHeap(firstByte, bytes, position);
            position = p.position();
            return p.value();
        }

        @Override
        public long deserializeLong(byte firstByte) {
            JsonNumberUtil.LongIntPair p = JsonNumberUtil.readLongFromHeap(firstByte, bytes, position);
            position = p.position();
            return p.value();
        }

        @Override
        public float deserializeFloat(byte firstByte) {
            JsonNumberUtil.FpRep rep = JsonNumberUtil.readFpFromHeap(firstByte, bytes, position, option.maxNumberBytes());
            JsonNumberUtil.Fp32 fp32 = JsonNumberUtil.parseFloat(rep);
            float r = fp32.trunc() ? Float.parseFloat(new String(bytes, position - 1, rep.len(), StandardCharsets.US_ASCII)) : fp32.value();
            position += rep.len() - 1;
            return r;
        }

        @Override
        public double deserializeDouble(byte firstByte) {
            JsonNumberUtil.FpRep rep = JsonNumberUtil.readFpFromHeap(firstByte, bytes, position, option.maxNumberBytes());
            JsonNumberUtil.Fp64 fp64 = JsonNumberUtil.parseDouble(rep);
            double r = fp64.trunc() ? Double.parseDouble(new String(bytes, position - 1, rep.len(), StandardCharsets.US_ASCII)) : fp64.value();
            position += rep.len() - 1;
            return r;
        }

        private int copyAscii(int end) {
            final int avail = Math.min(charBuffer.length, end - position);
            int copied = 0;
            for( ; copied <= avail - BYTE_SPECIES.length(); copied += BYTE_SPECIES.length()) {
                ByteVector byteVector = ByteVector.fromArray(BYTE_SPECIES, bytes, position + copied);
                ShortVector part0 = (ShortVector) byteVector.convertShape(VectorOperators.B2S, SHORT_SPECIES, 0);
                ShortVector part1 = (ShortVector) byteVector.convertShape(VectorOperators.B2S, SHORT_SPECIES, 1);
                part0.intoCharArray(charBuffer, copied);
                part1.intoCharArray(charBuffer, copied + SHORT_SPECIES.length());
                int matched = byteVector.lt((byte) 0x20)
                        .or(byteVector.eq((byte) '\\'))
                        .or(byteVector.eq((byte) '"')).firstTrue();
                if(matched != BYTE_SPECIES.length()) {
                    copied += matched;
                    break ;
                }
            }
            position += copied;
            return copied;
        }

        private int deserializeEscapedSequenceFromHeap(int index, int end) {
            if(position >= end) {
                throw new JsonDeserializerException("eof reached while reading escaped sequence");
            }
            int i = bytes[position++] & 0xFF;
            if(i == 'u') {
                if(position > end - 4) {
                    throw new JsonDeserializerException("illegal escaped unicode sequence");
                }
                char c = deserializeHexFromHeap();
                charBuffer[index++] = c;
                if(Character.isHighSurrogate(c)) {
                    if(position > end - 6 || bytes[position++] != '\\' || bytes[position++] != 'u') {
                        throw new JsonDeserializerException("illegal escaped surrogate unicode sequence");
                    }
                    char c2 = deserializeHexFromHeap();
                    if(!Character.isLowSurrogate(c2)) {
                        throw new JsonDeserializerException("illegal escaped low surrogate unicode sequence");
                    }
                    charBuffer[index++] = c2;
                }
            } else {
                byte escaped = ESCAPE_TABLE[i];
                if(escaped == 0) {
                    throw new JsonDeserializerException("illegal escape sequence : " + escaped);
                }
                charBuffer[index++] = (char) escaped;
            }
            return index;
        }

        @Override
        public String deserializeString(byte firstByte) {
            checkStrStart(firstByte);
            int end = Math.min(option.maxStringBytes(), bytes.length - position) + position;
            int index = copyAscii(end);
            while (position < end) {
                ensureCharBufferCapacity(Math.addExact(index, 2));
                int i = bytes[position++] & 0xFF;
                if(i == '"') {
                    return new String(charBuffer, 0, index);
                } else if(i == '\\') {
                    index = deserializeEscapedSequenceFromHeap(index, end);
                } else if(i < 0x20) {
                    throw new JsonDeserializerException("illegal unescaped ascii control byte : " + i);
                } else if(i < 0x80) {
                    charBuffer[index++] = (char) i;
                } else if(i < 0xE0) {
                    charBuffer[index++] = deserializeCharFromHeap2(i);
                } else if(i < 0xF0) {
                    charBuffer[index++] = deserializeCharFromHeap3(i);
                } else {
                    Surr surr = deserializeCharFromHeap4(i);
                    charBuffer[index++] = surr.high();
                    charBuffer[index++] = surr.low();
                }
            }
            throw new JsonDeserializerException("illegal json string, closing quote not found");
        }

        @Override
        public JsonNumberType deserializeJsonNumberType(byte firstByte) {
            JsonNumberUtil.FpRep rep = JsonNumberUtil.readFpFromHeap(firstByte, bytes, position, option.maxNumberBytes());
            JsonNumberType r = new JsonNumberType(Arrays.copyOfRange(bytes, position - 1, position + rep.len() - 1));
            position += rep.len() - 1;
            return r;
        }

        @Override
        public void skipNumber(byte firstByte) {
            JsonNumberUtil.FpRep rep = JsonNumberUtil.readFpFromHeap(firstByte, bytes, position, option.maxNumberBytes());
            position += rep.len() - 1;
        }

        private void skipAscii(int end, boolean allowUtf) {
            for( ; position <= end - BYTE_SPECIES.length(); position += BYTE_SPECIES.length()) {
                ByteVector byteVector = ByteVector.fromArray(BYTE_SPECIES, bytes, position);
                int matched = byteVector.compare(allowUtf ? VectorOperators.ULT : VectorOperators.LT, (byte) 0x20)
                        .or(byteVector.eq((byte) '\\'))
                        .or(byteVector.eq((byte) '"')).firstTrue();
                if(matched != BYTE_SPECIES.length()) {
                    position += matched;
                    return ;
                }
            }
        }

        private void skipEscapedSequence(int end) {
            if(position >= end) {
                throw new JsonDeserializerException("eof reached while reading escaped sequence");
            }
            int i = bytes[position++] & 0xFF;
            if(i == 'u') {
                if(position > end - 4) {
                    throw new JsonDeserializerException("illegal escaped unicode sequence");
                }
                char c = deserializeHexFromHeap();
                if(Character.isHighSurrogate(c)) {
                    if(position > end - 6 || bytes[position++] != '\\' || bytes[position++] != 'u') {
                        throw new JsonDeserializerException("illegal escaped surrogate unicode sequence");
                    }
                    char c2 = deserializeHexFromHeap();
                    if(!Character.isLowSurrogate(c2)) {
                        throw new JsonDeserializerException("illegal escaped low surrogate unicode sequence");
                    }
                }
            } else {
                byte escaped = ESCAPE_TABLE[i];
                if(escaped == 0) {
                    throw new JsonDeserializerException("illegal escape sequence : " + escaped);
                }
            }
        }

        @SuppressWarnings("StatementWithEmptyBody")
        @Override
        public void skipString(byte firstByte) {
            checkStrStart(firstByte);
            int end = Math.min(option.maxStringBytes(), bytes.length - position) + position;
            skipAscii(end, false);
            while (position < end) {
                int i = bytes[position++] & 0xFF;
                if(i == '"') {
                    return ;
                } else if(i == '\\') {
                    skipEscapedSequence(end);
                } else if(i < 0x20) {
                    throw new JsonDeserializerException("illegal unescaped ascii control byte : " + i);
                } else if(i < 0x80) {
                    // single-byte ASCII: already consumed by loop head position++
                } else if(i < 0xE0) {
                    position += 1;
                } else if(i < 0xF0) {
                    position += 2;
                } else {
                    position += 3;
                }
            }
            throw new JsonDeserializerException("illegal json string, closing quote not found");
        }

        @Override
        public MarshallInfo deserializeMarshallInfo(MarshallFacade fc, byte firstByte) {
            checkStrStart(firstByte);
            int start = position;
            int end = Math.min(option.maxStringBytes(), bytes.length - start) + start;
            skipAscii(end, true);
            if(position >= end) {
                throw new JsonDeserializerException("illegal json string, closing quote not found");
            }
            if(bytes[position] == (byte) '"') {
                return fc.marshallInfoByMappedName(bytes, start, position++);
            }
            return fc.marshallInfoByMappedName(deserializeString(firstByte));
        }

        @Override
        public void commit() {
            heapReadBuffer.setPosition(position);
        }
    }

    public static final class JsonDeserializerSegmentContext extends JsonDeserializerContext {
        private final SegmentReadBuffer segmentReadBuffer;
        private final MemorySegment segment;
        private long position;

        JsonDeserializerSegmentContext(JsonDeserializerOption option, SegmentReadBuffer segmentReadBuffer) {
            super(option);
            this.segmentReadBuffer = segmentReadBuffer;
            this.segment = segmentReadBuffer.rawSegment();
            this.position = segmentReadBuffer.longPosition();
        }

        @Override
        public boolean validate() {
            return Utf8Validator.validateSegment(segment, position, segment.byteSize());
        }

        @Override
        public void rewind() {
            position--;
        }

        @Override
        public byte nextValuableByte() {
            final long end = position + Math.min(option.maxEmptyBytes(), segment.byteSize() - position);
            while (position < end) {
                byte b = SegmentAccess.getByte(segment, position++);
                if(b != (byte) ' ' && b != '\n' && b != '\t' && b != '\r') {
                    return b;
                }
            }
            throw new JsonDeserializerException("valuable byte not found");
        }

        @Override
        public void deserializeNull() {
            if (position > segment.byteSize() - 3L) {
                throw new JsonDeserializerException("eof reached while deserializing null");
            }
            if (SegmentAccess.getInt(segment, position - 1L) != COMPACT_NULL) {
                throw new JsonDeserializerException("illegal null token, position : " + position);
            }
            position += 3L;
        }

        @Override
        public boolean deserializeBoolean(byte firstByte) {
            if(firstByte == (byte) 't') {
                if(position > segment.byteSize() - 3L) {
                    throw new JsonDeserializerException("eof reached while deserializing boolean 'true' value");
                }
                if(SegmentAccess.getInt(segment, position - 1L) != COMPACT_TRUE) {
                    throw new JsonDeserializerException("illegal boolean 'true' value");
                }
                position += 3;
                return true;
            } else if(firstByte == (byte) 'f') {
                if(position > segment.byteSize() - 4) {
                    throw new JsonDeserializerException("eof reached while deserializing boolean 'false' value");
                }
                if(SegmentAccess.getInt(segment, position) != COMPACT_ALSE) {
                    throw new JsonDeserializerException("illegal boolean 'false' value");
                }
                position += 4;
                return false;
            } else {
                throw new JsonDeserializerException("not a bool start");
            }
        }

        private char deserializeHexFromSegment() {
            int i1 = HEX_TABLE[SegmentAccess.getByte(segment, position++) & 0xFF];
            int i2 = HEX_TABLE[SegmentAccess.getByte(segment, position++) & 0xFF];
            int i3 = HEX_TABLE[SegmentAccess.getByte(segment, position++) & 0xFF];
            int i4 = HEX_TABLE[SegmentAccess.getByte(segment, position++) & 0xFF];
            if((i1 | i2 | i3 | i4) < 0) {
                throw new JsonDeserializerException("illegal escaped unicode sequence");
            }
            return (char) ((i1 << 12) | (i2 << 8) | (i3 << 4) | i4);
        }

        private char deserializeEscapedChar() {
            if(position >= segment.byteSize()) {
                throw new JsonDeserializerException("eof reached while deserializing escaped char from segment");
            }
            byte escaped = SegmentAccess.getByte(segment, position++);
            if(escaped == (byte) 'u') {
                if(position > segment.byteSize() - 4) {
                    throw new JsonDeserializerException("illegal escaped unicode sequence");
                }
                char r = deserializeHexFromSegment();
                if(Character.isSurrogate(r)) {
                    throw new JsonDeserializerException("illegal escaped unicode surrogate sequence");
                }
                return r;
            }
            byte b = ESCAPE_TABLE[escaped & 0xFF];
            if(b == 0) {
                throw new JsonDeserializerException("illegal escaped char from segment");
            }
            return (char) b;
        }

        private char deserializeCharFromSegment2(int i) {
            int i1 = SegmentAccess.getByte(segment, position++) & 0x3F;
            return (char) (((i & 0x1F) << 6) | i1);
        }

        private char deserializeCharFromSegment3(int i) {
            int i1 = SegmentAccess.getByte(segment, position++) & 0x3F;
            int i2 = SegmentAccess.getByte(segment, position++) & 0x3F;
            return (char) (((i & 0x0F) << 12) | (i1 << 6) | i2);
        }

        private Surr deserializeCharFromSegment4(int i) {
            int i1 = SegmentAccess.getByte(segment, position++) & 0x3F;
            int i2 = SegmentAccess.getByte(segment, position++) & 0x3F;
            int i3 = SegmentAccess.getByte(segment, position++) & 0x3F;
            int cp = (((i & 0x07) << 18) | (i1 << 12) | (i2 << 6) | i3) - 0x10000;
            return new Surr((char) (0xD800 | (cp >> 10)), (char) (0xDC00 | (cp & 0x3FF)));
        }

        @Override
        public char deserializeChar(byte firstByte) {
            checkStrStart(firstByte);
            if(position >= segment.byteSize()) {
                throw new JsonDeserializerException("eof reached while deserializing char from segment");
            }
            int i = SegmentAccess.getByte(segment, position++) & 0xFF;
            char r;
            if(i == '\\') {
                r = deserializeEscapedChar();
            } else if(i < 0x20) {
                throw new JsonDeserializerException("illegal unescaped ascii control byte : " + i);
            } else if(i < 0x80) {
                r = (char) i;
            } else if(i < 0xE0) {
                r = deserializeCharFromSegment2(i);
            } else if(i < 0xF0) {
                r = deserializeCharFromSegment3(i);
            } else {
                throw new JsonDeserializerException("illegal surrogate start : " + i);
            }
            if(position >= segment.byteSize() || SegmentAccess.getByte(segment, position++) != (byte) '"') {
                throw new JsonDeserializerException("not a single char");
            }
            return r;
        }

        @Override
        public int deserializeInt(byte firstByte) {
            JsonNumberUtil.IntLongPair p = JsonNumberUtil.readIntFromSegment(firstByte, segment, position);
            position = p.position();
            return p.value();
        }

        @Override
        public long deserializeLong(byte firstByte) {
            JsonNumberUtil.LongLongPair p = JsonNumberUtil.readLongFromSegment(firstByte, segment, position);
            position = p.position();
            return p.value();
        }

        @Override
        public float deserializeFloat(byte firstByte) {
            JsonNumberUtil.FpRep rep = JsonNumberUtil.readFpFromSegment(firstByte, segment, position, option.maxNumberBytes());
            JsonNumberUtil.Fp32 fp32 = JsonNumberUtil.parseFloat(rep);
            float r = fp32.trunc() ? Float.parseFloat(new String(segment.asSlice(position - 1L, rep.len()).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.US_ASCII)) : fp32.value();
            position += rep.len() - 1;
            return r;
        }

        @Override
        public double deserializeDouble(byte firstByte) {
            JsonNumberUtil.FpRep rep = JsonNumberUtil.readFpFromSegment(firstByte, segment, position, option.maxNumberBytes());
            JsonNumberUtil.Fp64 fp64 = JsonNumberUtil.parseDouble(rep);
            double r = fp64.trunc() ? Double.parseDouble(new String(segment.asSlice(position - 1L, rep.len()).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.US_ASCII)) : fp64.value();
            position += rep.len() - 1;
            return r;
        }

        private int copyAscii(long end) {
            final int avail = Math.min(charBuffer.length, Math.toIntExact(end - position));
            int copied = 0;
            for( ; copied <= avail - BYTE_SPECIES.length(); copied += BYTE_SPECIES.length()) {
                ByteVector byteVector = ByteVector.fromMemorySegment(BYTE_SPECIES, segment, position + copied, ByteOrder.nativeOrder()); // byteOrder will be ignored
                ShortVector part0 = (ShortVector) byteVector.convertShape(VectorOperators.B2S, SHORT_SPECIES, 0);
                ShortVector part1 = (ShortVector) byteVector.convertShape(VectorOperators.B2S, SHORT_SPECIES, 1);
                part0.intoCharArray(charBuffer, copied);
                part1.intoCharArray(charBuffer, copied + SHORT_SPECIES.length());
                int matched = byteVector.lt((byte) 0x20)
                        .or(byteVector.eq((byte) '\\'))
                        .or(byteVector.eq((byte) '"')).firstTrue();
                if(matched != BYTE_SPECIES.length()) {
                    copied += matched;
                    break ;
                }
            }
            position += copied;
            return copied;
        }

        private int deserializeEscapedSequenceFromSegment(int index, long end) {
            if(position >= end) {
                throw new JsonDeserializerException("eof reached while reading escaped sequence");
            }
            int i = SegmentAccess.getByte(segment, position++) & 0xFF;
            if(i == 'u') {
                if(position > end - 4L) {
                    throw new JsonDeserializerException("illegal escaped unicode sequence");
                }
                char c = deserializeHexFromSegment();
                charBuffer[index++] = c;
                if(Character.isHighSurrogate(c)) {
                    if(position > end - 6L || SegmentAccess.getByte(segment, position++) != '\\' || SegmentAccess.getByte(segment, position++) != 'u') {
                        throw new JsonDeserializerException("illegal escaped surrogate unicode sequence");
                    }
                    char c2 = deserializeHexFromSegment();
                    if(!Character.isLowSurrogate(c2)) {
                        throw new JsonDeserializerException("illegal escaped low surrogate unicode sequence");
                    }
                    charBuffer[index++] = c2;
                }
            } else {
                byte escaped = ESCAPE_TABLE[i];
                if(escaped == 0) {
                    throw new JsonDeserializerException("illegal escape sequence : " + escaped);
                }
                charBuffer[index++] = (char) escaped;
            }
            return index;
        }

        @Override
        public String deserializeString(byte firstByte) {
            checkStrStart(firstByte);
            long end = Math.min(option.maxStringBytes(), segment.byteSize() - position) + position;
            int index = copyAscii(end);
            while (position < end) {
                ensureCharBufferCapacity(Math.addExact(index, 2));
                int i = SegmentAccess.getByte(segment, position++) & 0xFF;
                if(i == '"') {
                    return new String(charBuffer, 0, index);
                } else if(i == '\\') {
                    index = deserializeEscapedSequenceFromSegment(index, end);
                } else if(i < 0x20) {
                    throw new JsonDeserializerException("illegal unescaped ascii control byte : " + i);
                } else if(i < 0x80) {
                    charBuffer[index++] = (char) i;
                } else if(i < 0xE0) {
                    charBuffer[index++] = deserializeCharFromSegment2(i);
                } else if(i < 0xF0) {
                    charBuffer[index++] = deserializeCharFromSegment3(i);
                } else {
                    Surr surr = deserializeCharFromSegment4(i);
                    charBuffer[index++] = surr.high();
                    charBuffer[index++] = surr.low();
                }
            }
            throw new JsonDeserializerException("illegal json string, closing quote not found");
        }

        @Override
        public JsonNumberType deserializeJsonNumberType(byte firstByte) {
            JsonNumberUtil.FpRep rep = JsonNumberUtil.readFpFromSegment(firstByte, segment, position, option.maxNumberBytes());
            JsonNumberType r = new JsonNumberType(segment.asSlice(position - 1L, rep.len()).toArray(ValueLayout.JAVA_BYTE));
            position += rep.len() - 1;
            return r;
        }

        @Override
        public void skipNumber(byte firstByte) {
            JsonNumberUtil.FpRep rep = JsonNumberUtil.readFpFromSegment(firstByte, segment, position, option.maxNumberBytes());
            position += rep.len() - 1;
        }

        private void skipAscii(long end, boolean allowUtf) {
            for( ; position <= end - BYTE_SPECIES.length(); position += BYTE_SPECIES.length()) {
                ByteVector byteVector = ByteVector.fromMemorySegment(BYTE_SPECIES, segment, position, ByteOrder.nativeOrder()); // byteOrder will be ignored
                int matched = byteVector.compare(allowUtf ? VectorOperators.ULT : VectorOperators.LT, (byte) 0x20)
                        .or(byteVector.eq((byte) '\\'))
                        .or(byteVector.eq((byte) '"')).firstTrue();
                if(matched != BYTE_SPECIES.length()) {
                    position += matched;
                    return ;
                }
            }
        }

        private void skipEscapedSequence(long end) {
            if(position >= end) {
                throw new JsonDeserializerException("eof reached while reading escaped sequence");
            }
            int i = SegmentAccess.getByte(segment, position++) & 0xFF;
            if(i == 'u') {
                if(position > end - 4L) {
                    throw new JsonDeserializerException("illegal escaped unicode sequence");
                }
                char c = deserializeHexFromSegment();
                if(Character.isHighSurrogate(c)) {
                    if(position > end - 6L || SegmentAccess.getByte(segment, position++) != '\\' || SegmentAccess.getByte(segment, position++) != 'u') {
                        throw new JsonDeserializerException("illegal escaped surrogate unicode sequence");
                    }
                    char c2 = deserializeHexFromSegment();
                    if(!Character.isLowSurrogate(c2)) {
                        throw new JsonDeserializerException("illegal escaped low surrogate unicode sequence");
                    }
                }
            } else {
                byte escaped = ESCAPE_TABLE[i];
                if(escaped == 0) {
                    throw new JsonDeserializerException("illegal escape sequence : " + escaped);
                }
            }
        }

        @SuppressWarnings("StatementWithEmptyBody")
        @Override
        public void skipString(byte firstByte) {
            checkStrStart(firstByte);
            long end = Math.min(option.maxStringBytes(), segment.byteSize() - position) + position;
            skipAscii(end, false);
            while (position < end) {
                int i = SegmentAccess.getByte(segment, position++) & 0xFF;
                if(i == '"') {
                    return ;
                } else if(i == '\\') {
                    skipEscapedSequence(end);
                } else if(i < 0x20) {
                    throw new JsonDeserializerException("illegal unescaped ascii control byte : " + i);
                } else if(i < 0x80) {
                    // single-byte ASCII: already consumed by loop head position++
                } else if(i < 0xE0) {
                    position += 1;
                } else if(i < 0xF0) {
                    position += 2;
                } else {
                    position += 3;
                }
            }
            throw new JsonDeserializerException("illegal json string, closing quote not found");
        }

        @Override
        public MarshallInfo deserializeMarshallInfo(MarshallFacade fc, byte firstByte) {
            checkStrStart(firstByte);
            long start = position;
            long end = Math.min(option.maxStringBytes(), segment.byteSize() - start) + start;
            skipAscii(end, true);
            if(position >= end) {
                throw new JsonDeserializerException("illegal json string, closing quote not found");
            }
            if(SegmentAccess.getByte(segment, position) == (byte) '"') {
                return fc.marshallInfoByMappedName(segment, start, position++);
            }
            return fc.marshallInfoByMappedName(deserializeString(firstByte));
        }

        @Override
        public void commit() {
            segmentReadBuffer.setPosition(position);
        }
    }
}
