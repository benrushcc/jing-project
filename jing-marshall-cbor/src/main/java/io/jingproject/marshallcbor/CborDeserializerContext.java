package io.jingproject.marshallcbor;

import io.jingproject.common.*;
import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.MarshallInfo;
import io.jingproject.marshall.Marshalls;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;

// sealed context with a heap and a segment implementation, mirroring the json
// deserializer. every read primitive consumes the CBOR binary format: integers
// via major 0/1 heads, floats via 0xfa/0xfb plus big-endian payloads, text as
// raw UTF-8 and byte strings via major type 2. container heads (major 4/5)
// carry a declared element count that is kept in the shared count field and
// read by the nodes through declaredCount(); an indefinite-length container is
// signalled by count == -1 and terminated by the 0xff break byte.
public sealed abstract class CborDeserializerContext
        permits CborDeserializerContext.CborDeserializerHeapContext, CborDeserializerContext.CborDeserializerSegmentContext {
    public static final int CHAR_BUFFER_INITIAL_SIZE = 128;
    protected static final int OBJ_ARR_INITIAL_SIZE = 8;
    protected static final Map<Class<?>, CborDeserializeFunc> BUILTIN_DESERIALIZE_OBJ_FUNC_MAP;
    protected static final Map<Class<?>, CborDeserializeFunc> BUILTIN_DESERIALIZE_ARRAY_FUNC_MAP;

    static {
        Map<Class<?>, CborDeserializeFunc> objFuncMap = new HashMap<>();
        objFuncMap.put(Byte.class, (b, c) -> {
            c.setObj(c.deserializeByte(b));
            return CborDeserializeResult.Continue;
        });
        objFuncMap.put(Boolean.class, (b, c) -> {
            c.setObj(c.deserializeBoolean(b));
            return CborDeserializeResult.Continue;
        });
        objFuncMap.put(Short.class, (b, c) -> {
            c.setObj(c.deserializeShort(b));
            return CborDeserializeResult.Continue;
        });
        objFuncMap.put(Character.class, (b, c) -> {
            c.setObj(c.deserializeChar(b));
            return CborDeserializeResult.Continue;
        });
        objFuncMap.put(Integer.class, (b, c) -> {
            c.setObj(c.deserializeInt(b));
            return CborDeserializeResult.Continue;
        });
        objFuncMap.put(Long.class, (b, c) -> {
            c.setObj(c.deserializeLong(b));
            return CborDeserializeResult.Continue;
        });
        objFuncMap.put(Float.class, (b, c) -> {
            c.setObj(c.deserializeFloat(b));
            return CborDeserializeResult.Continue;
        });
        objFuncMap.put(Double.class, (b, c) -> {
            c.setObj(c.deserializeDouble(b));
            return CborDeserializeResult.Continue;
        });
        CborDeserializeFunc strFunc = (b, c) -> {
            c.setObj(c.deserializeString(b));
            return CborDeserializeResult.Continue;
        };
        objFuncMap.put(CharSequence.class, strFunc);
        objFuncMap.put(String.class, strFunc);
        objFuncMap.put(CborPrimitiveType.class, (b, c) -> {
            c.setObj(c.deserializeCborPrimitiveType(b));
            return CborDeserializeResult.Continue;
        });
        objFuncMap.put(CborBoolType.class, (b, c) -> {
            c.setObj(c.deserializeCborBoolType(b));
            return CborDeserializeResult.Continue;
        });
        objFuncMap.put(CborNumberType.class, (b, c) -> {
            c.setObj(c.deserializeCborNumberType(b));
            return CborDeserializeResult.Continue;
        });
        objFuncMap.put(CborStrType.class, (b, c) -> {
            c.setObj(c.deserializeCborStrType(b));
            return CborDeserializeResult.Continue;
        });
        objFuncMap.put(CborBytesType.class, (b, c) -> {
            c.setObj(c.deserializeCborBytesType(b));
            return CborDeserializeResult.Continue;
        });
        BUILTIN_DESERIALIZE_OBJ_FUNC_MAP = Map.copyOf(objFuncMap);
    }

    static {
        Map<Class<?>, CborDeserializeFunc> arrFuncMap = new HashMap<>();
        arrFuncMap.put(byte[].class, (b, c) -> {
            c.setObj(c.deserializeByteArray(b));
            return CborDeserializeResult.Continue;
        });
        arrFuncMap.put(boolean[].class, (b, c) -> {
            c.setObj(c.deserializeBooleanArray(b));
            return CborDeserializeResult.Continue;
        });
        arrFuncMap.put(short[].class, (b, c) -> {
            c.setObj(c.deserializeShortArray(b));
            return CborDeserializeResult.Continue;
        });
        arrFuncMap.put(char[].class, (b, c) -> {
            c.setObj(c.deserializeCharArray(b));
            return CborDeserializeResult.Continue;
        });
        arrFuncMap.put(int[].class, (b, c) -> {
            c.setObj(c.deserializeIntArray(b));
            return CborDeserializeResult.Continue;
        });
        arrFuncMap.put(long[].class, (b, c) -> {
            c.setObj(c.deserializeLongArray(b));
            return CborDeserializeResult.Continue;
        });
        arrFuncMap.put(float[].class, (b, c) -> {
            c.setObj(c.deserializeFloatArray(b));
            return CborDeserializeResult.Continue;
        });
        arrFuncMap.put(double[].class, (b, c) -> {
            c.setObj(c.deserializeDoubleArray(b));
            return CborDeserializeResult.Continue;
        });
        arrFuncMap.put(Byte[].class, (b, c) -> {
            c.setObj(c.deserializeByteWrapperArray(b));
            return CborDeserializeResult.Continue;
        });
        arrFuncMap.put(Boolean[].class, (b, c) -> {
            c.setObj(c.deserializeBooleanWrapperArray(b));
            return CborDeserializeResult.Continue;
        });
        arrFuncMap.put(Short[].class, (b, c) -> {
            c.setObj(c.deserializeShortWrapperArray(b));
            return CborDeserializeResult.Continue;
        });
        arrFuncMap.put(Character[].class, (b, c) -> {
            c.setObj(c.deserializeCharWrapperArray(b));
            return CborDeserializeResult.Continue;
        });
        arrFuncMap.put(Integer[].class, (b, c) -> {
            c.setObj(c.deserializeIntWrapperArray(b));
            return CborDeserializeResult.Continue;
        });
        arrFuncMap.put(Long[].class, (b, c) -> {
            c.setObj(c.deserializeLongWrapperArray(b));
            return CborDeserializeResult.Continue;
        });
        arrFuncMap.put(Float[].class, (b, c) -> {
            c.setObj(c.deserializeFloatWrapperArray(b));
            return CborDeserializeResult.Continue;
        });
        arrFuncMap.put(Double[].class, (b, c) -> {
            c.setObj(c.deserializeDoubleWrapperArray(b));
            return CborDeserializeResult.Continue;
        });
        CborDeserializeFunc strArrayFunc = (b, c) -> {
            c.setObj(c.deserializeStringArray(b));
            return CborDeserializeResult.Continue;
        };
        arrFuncMap.put(CharSequence[].class, strArrayFunc);
        arrFuncMap.put(String[].class, strArrayFunc);
        arrFuncMap.put(CborPrimitiveType[].class, (b, c) -> {
            c.setObj(c.deserializeCborPrimitiveTypeArray(b));
            return CborDeserializeResult.Continue;
        });
        arrFuncMap.put(CborBoolType[].class, (b, c) -> {
            c.setObj(c.deserializeCborBoolTypeArray(b));
            return CborDeserializeResult.Continue;
        });
        arrFuncMap.put(CborNumberType[].class, (b, c) -> {
            c.setObj(c.deserializeCborNumberTypeArray(b));
            return CborDeserializeResult.Continue;
        });
        arrFuncMap.put(CborStrType[].class, (b, c) -> {
            c.setObj(c.deserializeCborStrTypeArray(b));
            return CborDeserializeResult.Continue;
        });
        arrFuncMap.put(CborBytesType[].class, (b, c) -> {
            c.setObj(c.deserializeCborBytesTypeArray(b));
            return CborDeserializeResult.Continue;
        });
        BUILTIN_DESERIALIZE_ARRAY_FUNC_MAP = Map.copyOf(arrFuncMap);
    }

    public static CborDeserializerContext newContext(CborDeserializerOption option, ReadBuffer readBuffer) {
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> new CborDeserializerHeapContext(option, heapReadBuffer);
            case SegmentReadBuffer segmentReadBuffer -> new CborDeserializerSegmentContext(option, segmentReadBuffer);
        };
    }

    protected final CborDeserializerOption option;
    // declared element count of the current container head; -1 means indefinite-length
    protected int count = -1;
    protected Object obj;
    protected Class<?> type;
    protected Object[] arr;

    protected CborDeserializerContext(CborDeserializerOption option) {
        this.option = option;
    }

    public final CborDeserializerOption option() {
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

    public final int declaredCount() {
        return count;
    }

    public abstract byte getByte();

    public abstract void rewind();

    public abstract long remaining();

    public abstract void advance(long length);

    public abstract byte[] copyBytes(int length);

    public abstract String decodeUtf8(int length);

    public abstract MarshallInfo lookupMappedName(MarshallFacade fc, int length);

    abstract short readShortRaw();

    abstract int readIntRaw();

    abstract long readLongRaw();

    public abstract void commit();

    // read the argument of a head: ai 0-23 is the value itself, ai 24-27 reads
    // 1/2/4/8 unsigned big-endian bytes, ai 28-30 is illegal
    private long readArgument(int ai) {
        if (ai < 24) {
            return ai;
        }
        if (ai == 24) {
            return getByte() & 0xFFL;
        }
        if (ai == 25) {
            return readShortRaw() & 0xFFFFL;
        }
        if (ai == 26) {
            return readIntRaw() & 0xFFFFFFFFL;
        }
        if (ai == 27) {
            return readLongRaw();
        }
        throw new CborDeserializerException("unsupported or illegal initial byte");
    }

    // read a length head: like readArgument but ai 31 (indefinite) returns -1
    private long readLength(int ai) {
        if (ai == 31) {
            return -1L;
        }
        return readArgument(ai);
    }

    // a definite array head must stay within maxArrayElements; indefinite is accepted
    public final void checkArrayStart(byte b) {
        if (CborNumberUtil.majorOf(b) != CborNumberUtil.TYPE_ARRAY) {
            throw new CborDeserializerException("not an array start : " + b);
        }
        long declared = readLength(CborNumberUtil.aiOf(b));
        if (declared > option.maxArrayElements()) {
            throw new CborDeserializerException("too many array elements, exceeded limit : " + declared);
        }
        count = (int) declared;
    }

    // a definite map head must stay within maxMapElements; indefinite is accepted
    public final void checkObjStart(byte b) {
        if (CborNumberUtil.majorOf(b) != CborNumberUtil.TYPE_MAP) {
            throw new CborDeserializerException("not an object start : " + b);
        }
        long declared = readLength(CborNumberUtil.aiOf(b));
        if (declared > option.maxMapElements()) {
            throw new CborDeserializerException("too many map elements, exceeded limit : " + declared);
        }
        count = (int) declared;
    }

    public final byte deserializeByte(byte firstByte) {
        int value = deserializeInt(firstByte);
        if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            return (byte) value;
        }
        throw new CborDeserializerException("byte value overflow : " + value);
    }

    public final boolean deserializeBoolean(byte firstByte) {
        if (firstByte == (byte) 0xF5) {
            return true;
        }
        if (firstByte == (byte) 0xF4) {
            return false;
        }
        throw new CborDeserializerException("not a bool start : " + firstByte);
    }

    public final short deserializeShort(byte firstByte) {
        int value = deserializeInt(firstByte);
        if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            return (short) value;
        }
        throw new CborDeserializerException("short value overflow : " + value);
    }

    public final char deserializeChar(byte firstByte) {
        String r = deserializeString(firstByte);
        if (r.length() != 1) {
            throw new CborDeserializerException("not a single char");
        }
        return r.charAt(0);
    }

    public final int deserializeInt(byte firstByte) {
        long value = deserializeLong(firstByte);
        if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
            return (int) value;
        }
        throw new CborDeserializerException("int value overflow : " + value);
    }

    public final long deserializeLong(byte firstByte) {
        int major = CborNumberUtil.majorOf(firstByte);
        if (major == CborNumberUtil.TYPE_UNSIGNED) {
            long arg = readArgument(CborNumberUtil.aiOf(firstByte));
            if ((arg & Long.MIN_VALUE) != 0) {
                throw new CborDeserializerException("long value overflow : " + Long.toUnsignedString(arg));
            }
            return arg;
        }
        if (major == CborNumberUtil.TYPE_NEGATIVE) {
            long arg = readArgument(CborNumberUtil.aiOf(firstByte));
            if ((arg & Long.MIN_VALUE) != 0) {
                throw new CborDeserializerException("long value overflow : " + Long.toUnsignedString(arg));
            }
            return -1L - arg;
        }
        throw new CborDeserializerException("not an integer : " + firstByte);
    }

    // float16/float32 are widened to float; float64 must fit into float precision
    public final float deserializeFloat(byte firstByte) {
        if (CborNumberUtil.majorOf(firstByte) != CborNumberUtil.TYPE_SIMPLE) {
            throw new CborDeserializerException("not a float value : " + firstByte);
        }
        int ai = CborNumberUtil.aiOf(firstByte);
        if (ai == 25) {
            return CborNumberUtil.halfToFloat(readShortRaw());
        }
        if (ai == 26) {
            return Float.intBitsToFloat(readIntRaw());
        }
        if (ai == 27) {
            double v = Double.longBitsToDouble(readLongRaw());
            float r = (float) v;
            if (Float.isInfinite(r) && !Double.isInfinite(v)) {
                throw new CborDeserializerException("float value overflow : " + v);
            }
            return r;
        }
        throw new CborDeserializerException("not a float value : " + firstByte);
    }

    public final double deserializeDouble(byte firstByte) {
        if (CborNumberUtil.majorOf(firstByte) != CborNumberUtil.TYPE_SIMPLE) {
            throw new CborDeserializerException("not a double value : " + firstByte);
        }
        int ai = CborNumberUtil.aiOf(firstByte);
        if (ai == 25) {
            return CborNumberUtil.halfToFloat(readShortRaw());
        }
        if (ai == 26) {
            return Float.intBitsToFloat(readIntRaw());
        }
        if (ai == 27) {
            return Double.longBitsToDouble(readLongRaw());
        }
        throw new CborDeserializerException("not a double value : " + firstByte);
    }

    public final String deserializeString(byte firstByte) {
        if (CborNumberUtil.majorOf(firstByte) != CborNumberUtil.TYPE_TEXT) {
            throw new CborDeserializerException("not a string start : " + firstByte);
        }
        long length = readLength(CborNumberUtil.aiOf(firstByte));
        if (length < 0L) {
            throw new CborDeserializerException("indefinite length not supported");
        }
        if (length > option.maxStringBytes()) {
            throw new CborDeserializerException("string length exceeds limit : " + length);
        }
        return decodeUtf8(Math.toIntExact(length));
    }

    // a byte string (major type 2) with a length head and raw bytes
    public final byte[] deserializeBytes(byte firstByte) {
        if (CborNumberUtil.majorOf(firstByte) != CborNumberUtil.TYPE_BYTES) {
            throw new CborDeserializerException("not a byte string start : " + firstByte);
        }
        long length = readLength(CborNumberUtil.aiOf(firstByte));
        if (length < 0L) {
            throw new CborDeserializerException("indefinite length not supported");
        }
        if (length > option.maxBytesBytes()) {
            throw new CborDeserializerException("bytes length exceeds limit : " + length);
        }
        return copyBytes(Math.toIntExact(length));
    }

    public final byte[] deserializeByteArray(byte firstByte) {
        return deserializeBytes(firstByte);
    }

    public final boolean[] deserializeBooleanArray(byte firstByte) {
        checkArrayStart(firstByte);
        final int maxArrayElements = option.maxArrayElements();
        if (count == 0) {
            return Utils.emptyBooleanArray();
        }
        if (count > 0) {
            boolean[] r = new boolean[count];
            for (int i = 0; i < count; i++) {
                r[i] = deserializeBoolean(getByte());
            }
            return r;
        }
        boolean[] r = new boolean[OBJ_ARR_INITIAL_SIZE];
        for (int i = 0; i < maxArrayElements; ) {
            byte b = getByte();
            if (b == (byte) 0xFF) {
                return i == 0 ? Utils.emptyBooleanArray() : Arrays.copyOf(r, i);
            }
            boolean v = deserializeBoolean(b);
            if (i == r.length) {
                r = Arrays.copyOf(r, Math.multiplyExact(r.length, 2));
            }
            r[i++] = v;
        }
        throw new CborDeserializerException("too many array elements, exceeded limit : " + maxArrayElements);
    }

    public final short[] deserializeShortArray(byte firstByte) {
        checkArrayStart(firstByte);
        final int maxArrayElements = option.maxArrayElements();
        if (count == 0) {
            return Utils.emptyShortArray();
        }
        if (count > 0) {
            short[] r = new short[count];
            for (int i = 0; i < count; i++) {
                r[i] = deserializeShort(getByte());
            }
            return r;
        }
        short[] r = new short[OBJ_ARR_INITIAL_SIZE];
        for (int i = 0; i < maxArrayElements; ) {
            byte b = getByte();
            if (b == (byte) 0xFF) {
                return i == 0 ? Utils.emptyShortArray() : Arrays.copyOf(r, i);
            }
            short v = deserializeShort(b);
            if (i == r.length) {
                r = Arrays.copyOf(r, Math.multiplyExact(r.length, 2));
            }
            r[i++] = v;
        }
        throw new CborDeserializerException("too many array elements, exceeded limit : " + maxArrayElements);
    }

    public final char[] deserializeCharArray(byte firstByte) {
        checkArrayStart(firstByte);
        final int maxArrayElements = option.maxArrayElements();
        if (count == 0) {
            return Utils.emptyCharArray();
        }
        if (count > 0) {
            char[] r = new char[count];
            for (int i = 0; i < count; i++) {
                r[i] = deserializeChar(getByte());
            }
            return r;
        }
        char[] r = new char[OBJ_ARR_INITIAL_SIZE];
        for (int i = 0; i < maxArrayElements; ) {
            byte b = getByte();
            if (b == (byte) 0xFF) {
                return i == 0 ? Utils.emptyCharArray() : Arrays.copyOf(r, i);
            }
            char v = deserializeChar(b);
            if (i == r.length) {
                r = Arrays.copyOf(r, Math.multiplyExact(r.length, 2));
            }
            r[i++] = v;
        }
        throw new CborDeserializerException("too many array elements, exceeded limit : " + maxArrayElements);
    }

    public final int[] deserializeIntArray(byte firstByte) {
        checkArrayStart(firstByte);
        final int maxArrayElements = option.maxArrayElements();
        if (count == 0) {
            return Utils.emptyIntArray();
        }
        if (count > 0) {
            int[] r = new int[count];
            for (int i = 0; i < count; i++) {
                r[i] = deserializeInt(getByte());
            }
            return r;
        }
        int[] r = new int[OBJ_ARR_INITIAL_SIZE];
        for (int i = 0; i < maxArrayElements; ) {
            byte b = getByte();
            if (b == (byte) 0xFF) {
                return i == 0 ? Utils.emptyIntArray() : Arrays.copyOf(r, i);
            }
            int v = deserializeInt(b);
            if (i == r.length) {
                r = Arrays.copyOf(r, Math.multiplyExact(r.length, 2));
            }
            r[i++] = v;
        }
        throw new CborDeserializerException("too many array elements, exceeded limit : " + maxArrayElements);
    }

    public final long[] deserializeLongArray(byte firstByte) {
        checkArrayStart(firstByte);
        final int maxArrayElements = option.maxArrayElements();
        if (count == 0) {
            return Utils.emptyLongArray();
        }
        if (count > 0) {
            long[] r = new long[count];
            for (int i = 0; i < count; i++) {
                r[i] = deserializeLong(getByte());
            }
            return r;
        }
        long[] r = new long[OBJ_ARR_INITIAL_SIZE];
        for (int i = 0; i < maxArrayElements; ) {
            byte b = getByte();
            if (b == (byte) 0xFF) {
                return i == 0 ? Utils.emptyLongArray() : Arrays.copyOf(r, i);
            }
            long v = deserializeLong(b);
            if (i == r.length) {
                r = Arrays.copyOf(r, Math.multiplyExact(r.length, 2));
            }
            r[i++] = v;
        }
        throw new CborDeserializerException("too many array elements, exceeded limit : " + maxArrayElements);
    }

    public final float[] deserializeFloatArray(byte firstByte) {
        checkArrayStart(firstByte);
        final int maxArrayElements = option.maxArrayElements();
        if (count == 0) {
            return Utils.emptyFloatArray();
        }
        if (count > 0) {
            float[] r = new float[count];
            for (int i = 0; i < count; i++) {
                r[i] = deserializeFloat(getByte());
            }
            return r;
        }
        float[] r = new float[OBJ_ARR_INITIAL_SIZE];
        for (int i = 0; i < maxArrayElements; ) {
            byte b = getByte();
            if (b == (byte) 0xFF) {
                return i == 0 ? Utils.emptyFloatArray() : Arrays.copyOf(r, i);
            }
            float v = deserializeFloat(b);
            if (i == r.length) {
                r = Arrays.copyOf(r, Math.multiplyExact(r.length, 2));
            }
            r[i++] = v;
        }
        throw new CborDeserializerException("too many array elements, exceeded limit : " + maxArrayElements);
    }

    public final double[] deserializeDoubleArray(byte firstByte) {
        checkArrayStart(firstByte);
        final int maxArrayElements = option.maxArrayElements();
        if (count == 0) {
            return Utils.emptyDoubleArray();
        }
        if (count > 0) {
            double[] r = new double[count];
            for (int i = 0; i < count; i++) {
                r[i] = deserializeDouble(getByte());
            }
            return r;
        }
        double[] r = new double[OBJ_ARR_INITIAL_SIZE];
        for (int i = 0; i < maxArrayElements; ) {
            byte b = getByte();
            if (b == (byte) 0xFF) {
                return i == 0 ? Utils.emptyDoubleArray() : Arrays.copyOf(r, i);
            }
            double v = deserializeDouble(b);
            if (i == r.length) {
                r = Arrays.copyOf(r, Math.multiplyExact(r.length, 2));
            }
            r[i++] = v;
        }
        throw new CborDeserializerException("too many array elements, exceeded limit : " + maxArrayElements);
    }

    private <T> T[] deserializeObjArray(ElementDeserializer<T> deserializer, IntFunction<T[]> arrayFactory) {
        final int maxArrayElements = option.maxArrayElements();
        if (count == 0) {
            return arrayFactory.apply(0);
        }
        if (count > 0) {
            T[] r = arrayFactory.apply(count);
            for (int i = 0; i < count; i++) {
                byte b = getByte();
                if (b == (byte) 0xF6) {
                    r[i] = null;
                } else {
                    r[i] = deserializer.deserialize(this, b);
                }
            }
            return r;
        }
        if (arr == null) {
            arr = new Object[OBJ_ARR_INITIAL_SIZE];
        }
        int length = 0;
        for ( ; ; ) {
            if (length >= maxArrayElements) {
                throw new CborDeserializerException("too many array elements, limit : " + maxArrayElements);
            }
            if (length >= arr.length) {
                Object[] newArr = new Object[Math.addExact(arr.length, arr.length)];
                System.arraycopy(arr, 0, newArr, 0, arr.length);
                arr = newArr;
            }
            byte b = getByte();
            if (b == (byte) 0xFF) {
                T[] r = arrayFactory.apply(length);
                //noinspection SuspiciousSystemArraycopy
                System.arraycopy(arr, 0, r, 0, length);
                return r;
            }
            if (b == (byte) 0xF6) {
                arr[length++] = null;
            } else {
                arr[length++] = deserializer.deserialize(this, b);
            }
        }
    }

    public final Byte[] deserializeByteWrapperArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(CborDeserializerContext::deserializeByte, Byte[]::new);
    }

    public final Boolean[] deserializeBooleanWrapperArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(CborDeserializerContext::deserializeBoolean, Boolean[]::new);
    }

    public final Short[] deserializeShortWrapperArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(CborDeserializerContext::deserializeShort, Short[]::new);
    }

    public final Character[] deserializeCharWrapperArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(CborDeserializerContext::deserializeChar, Character[]::new);
    }

    public final Integer[] deserializeIntWrapperArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(CborDeserializerContext::deserializeInt, Integer[]::new);
    }

    public final Long[] deserializeLongWrapperArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(CborDeserializerContext::deserializeLong, Long[]::new);
    }

    public final Float[] deserializeFloatWrapperArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(CborDeserializerContext::deserializeFloat, Float[]::new);
    }

    public final Double[] deserializeDoubleWrapperArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(CborDeserializerContext::deserializeDouble, Double[]::new);
    }

    public final String[] deserializeStringArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(CborDeserializerContext::deserializeString, String[]::new);
    }

    public final CborPrimitiveType deserializeCborPrimitiveType(byte firstByte) {
        return switch (CborNumberUtil.majorOf(firstByte)) {
            case CborNumberUtil.TYPE_TEXT -> deserializeCborStrType(firstByte);
            case CborNumberUtil.TYPE_BYTES -> deserializeCborBytesType(firstByte);
            case CborNumberUtil.TYPE_SIMPLE -> {
                int ai = CborNumberUtil.aiOf(firstByte);
                if (ai == 20 || ai == 21) {
                    yield deserializeCborBoolType(firstByte);
                }
                throw new CborDeserializerException("unknown cbor primitive type : " + firstByte);
            }
            default -> deserializeCborNumberType(firstByte);
        };
    }

    public final CborPrimitiveType[] deserializeCborPrimitiveTypeArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(CborDeserializerContext::deserializeCborPrimitiveType, CborPrimitiveType[]::new);
    }

    public final CborBoolType deserializeCborBoolType(byte firstByte) {
        return new CborBoolType(deserializeBoolean(firstByte));
    }

    public final CborBoolType[] deserializeCborBoolTypeArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(CborDeserializerContext::deserializeCborBoolType, CborBoolType[]::new);
    }

    // CborNumberType carries integer semantics only, matching the serializer lane
    public final CborNumberType deserializeCborNumberType(byte firstByte) {
        return new CborNumberType(deserializeLong(firstByte));
    }

    public final CborNumberType[] deserializeCborNumberTypeArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(CborDeserializerContext::deserializeCborNumberType, CborNumberType[]::new);
    }

    public final CborStrType deserializeCborStrType(byte firstByte) {
        return new CborStrType(deserializeString(firstByte));
    }

    public final CborStrType[] deserializeCborStrTypeArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(CborDeserializerContext::deserializeCborStrType, CborStrType[]::new);
    }

    public final CborBytesType deserializeCborBytesType(byte firstByte) {
        return new CborBytesType(deserializeBytes(firstByte));
    }

    public final CborBytesType[] deserializeCborBytesTypeArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(CborDeserializerContext::deserializeCborBytesType, CborBytesType[]::new);
    }

    public final void skipNumber(byte firstByte) {
        int major = CborNumberUtil.majorOf(firstByte);
        if (major == CborNumberUtil.TYPE_UNSIGNED || major == CborNumberUtil.TYPE_NEGATIVE) {
            readArgument(CborNumberUtil.aiOf(firstByte));
            return;
        }
        if (major == CborNumberUtil.TYPE_SIMPLE) {
            int ai = CborNumberUtil.aiOf(firstByte);
            if (ai == 24) {
                getByte();
            } else if (ai == 25 || ai == 26 || ai == 27) {
                advance(1L << (ai - 25));
            } else {
                throw new CborDeserializerException("not a number : " + firstByte);
            }
            return;
        }
        throw new CborDeserializerException("not a number : " + firstByte);
    }

    public final void skipString(byte firstByte) {
        if (CborNumberUtil.majorOf(firstByte) != CborNumberUtil.TYPE_TEXT) {
            throw new CborDeserializerException("not a string start : " + firstByte);
        }
        long length = readLength(CborNumberUtil.aiOf(firstByte));
        if (length < 0L) {
            throw new CborDeserializerException("indefinite length not supported");
        }
        if (length > option.maxStringBytes()) {
            throw new CborDeserializerException("string length exceeds limit : " + length);
        }
        advance(length);
    }

    public final void skipBytes(byte firstByte) {
        if (CborNumberUtil.majorOf(firstByte) != CborNumberUtil.TYPE_BYTES) {
            throw new CborDeserializerException("not a byte string start : " + firstByte);
        }
        long length = readLength(CborNumberUtil.aiOf(firstByte));
        if (length < 0L) {
            throw new CborDeserializerException("indefinite length not supported");
        }
        if (length > option.maxBytesBytes()) {
            throw new CborDeserializerException("bytes length exceeds limit : " + length);
        }
        advance(length);
    }

    // a mapped name is hashed and compared against the raw bytes directly; the
    // result is cached by the facade, so no UTF-8 validation is performed here
    public final MarshallInfo deserializeMarshallInfo(MarshallFacade fc, byte firstByte) {
        if (CborNumberUtil.majorOf(firstByte) != CborNumberUtil.TYPE_TEXT) {
            throw new CborDeserializerException("not a string start : " + firstByte);
        }
        long length = readLength(CborNumberUtil.aiOf(firstByte));
        if (length < 0L) {
            throw new CborDeserializerException("indefinite length not supported");
        }
        if (length > option.maxStringBytes()) {
            throw new CborDeserializerException("string length exceeds limit : " + length);
        }
        return lookupMappedName(fc, Math.toIntExact(length));
    }

    public final Enum<?> deserializeEnum(Class<?> enumType, byte firstByte) {
        Enum<?>[] enumConstants = (Enum<?>[]) enumType.getEnumConstants();
        MarshallFacade fc = Marshalls.enumMarshallFacade(enumType);
        if (fc == null) {
            String name = deserializeString(firstByte);
            for (Enum<?> e : enumConstants) {
                if (e.name().equals(name)) {
                    return e;
                }
            }
            throw new CborDeserializerException("enum item not found for item : " + name);
        }
        MarshallInfo inf = deserializeMarshallInfo(fc, firstByte);
        if (inf == null) {
            throw new CborDeserializerException("enum item not found for type : " + enumType.getName());
        }
        return enumConstants[inf.index()];
    }

    public final Enum<?>[] deserializeEnumArray(Class<?> enumType, byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray((c, b) -> c.deserializeEnum(enumType, b), Enum<?>[]::new);
    }

    public static CborDeserializeFunc builtinDeserializeObjFunc(Class<?> rawType) {
        return BUILTIN_DESERIALIZE_OBJ_FUNC_MAP.get(rawType);
    }

    public static CborDeserializeFunc builtinDeserializeArrayFunc(Class<?> rawType) {
        return BUILTIN_DESERIALIZE_ARRAY_FUNC_MAP.get(rawType);
    }

    // builtin type has the highest priority
    // then check if current type could be overridden by option
    // enum must be specially treated
    // finally assuming marshallable
    public final CborDeserializeFunc valueDeserializeFunc(Class<?> rawType) {
        if (rawType.isArray()) {
            CborDeserializeFunc builtinDeserializeArrFunc = builtinDeserializeArrayFunc(rawType);
            if (builtinDeserializeArrFunc != null) {
                return builtinDeserializeArrFunc;
            }
            CborDeserializeFunc customArrFunc = option.customArrFunc(rawType);
            if (customArrFunc != null) {
                return customArrFunc;
            }
            Class<?> componentType = rawType.componentType();
            if (componentType.isEnum()) {
                return (b, c) -> {
                    c.setObj(c.deserializeEnumArray(componentType, b));
                    return CborDeserializeResult.Continue;
                };
            }
            return (b, c) -> {
                c.checkArrayStart(b);
                c.setType(componentType);
                return CborDeserializeResult.NewArr;
            };
        }
        CborDeserializeFunc builtinDeserializeObjFunc = builtinDeserializeObjFunc(rawType);
        if (builtinDeserializeObjFunc != null) {
            return builtinDeserializeObjFunc;
        }
        CborDeserializeFunc customFunc = option.customFunc(rawType);
        if (customFunc != null) {
            return customFunc;
        }
        if (rawType.isEnum()) {
            return (b, c) -> {
                c.setObj(c.deserializeEnum(rawType, b));
                return CborDeserializeResult.Continue;
            };
        }
        return (b, c) -> {
            c.checkObjStart(b);
            c.setType(rawType);
            return CborDeserializeResult.NewMarshallable;
        };
    }

    @FunctionalInterface
    public interface ElementDeserializer<T> {
        T deserialize(CborDeserializerContext c, byte firstByte);
    }

    public static final class CborDeserializerHeapContext extends CborDeserializerContext {
        private final HeapReadBuffer heapReadBuffer;
        private final byte[] bytes;
        private int position;

        CborDeserializerHeapContext(CborDeserializerOption option, HeapReadBuffer heapReadBuffer) {
            super(option);
            this.heapReadBuffer = heapReadBuffer;
            this.bytes = heapReadBuffer.rawByteArray();
            this.position = heapReadBuffer.intPosition();
        }

        @Override
        public byte getByte() {
            if (position >= bytes.length) {
                throw new CborDeserializerException("unexpected end of input");
            }
            return bytes[position++];
        }

        @Override
        public void rewind() {
            position--;
        }

        @Override
        public long remaining() {
            return bytes.length - position;
        }

        @Override
        public void advance(long length) {
            int advanceLength = Math.toIntExact(length);
            checkLength(advanceLength);
            position += advanceLength;
        }

        @Override
        public byte[] copyBytes(int length) {
            checkLength(length);
            byte[] r = Arrays.copyOfRange(bytes, position, position + length);
            position += length;
            return r;
        }

        @Override
        public String decodeUtf8(int length) {
            checkLength(length);
            if (!CborUtf8Validator.validateHeap(bytes, position, position + length)) {
                throw new CborDeserializerException("not valid utf-8 content");
            }
            String r = new String(bytes, position, length, StandardCharsets.UTF_8);
            position += length;
            return r;
        }

        @Override
        public MarshallInfo lookupMappedName(MarshallFacade fc, int length) {
            checkLength(length);
            MarshallInfo r = fc.marshallInfoByMappedName(bytes, position, position + length);
            position += length;
            return r;
        }

        @Override
        short readShortRaw() {
            checkLength(2);
            short r = ArrayAccess.getShort(bytes, position, ByteOrder.BIG_ENDIAN);
            position += 2;
            return r;
        }

        @Override
        int readIntRaw() {
            checkLength(4);
            int r = ArrayAccess.getInt(bytes, position, ByteOrder.BIG_ENDIAN);
            position += 4;
            return r;
        }

        @Override
        long readLongRaw() {
            checkLength(8);
            long r = ArrayAccess.getLong(bytes, position, ByteOrder.BIG_ENDIAN);
            position += 8;
            return r;
        }

        @Override
        public void commit() {
            heapReadBuffer.setPosition(position);
        }

        private void checkLength(int length) {
            if (length < 0 || position > bytes.length - length) {
                throw new CborDeserializerException("unexpected end of input");
            }
        }
    }

    public static final class CborDeserializerSegmentContext extends CborDeserializerContext {
        private final SegmentReadBuffer segmentReadBuffer;
        private final MemorySegment segment;
        private long position;

        CborDeserializerSegmentContext(CborDeserializerOption option, SegmentReadBuffer segmentReadBuffer) {
            super(option);
            this.segmentReadBuffer = segmentReadBuffer;
            this.segment = segmentReadBuffer.rawSegment();
            this.position = segmentReadBuffer.longPosition();
        }

        @Override
        public byte getByte() {
            if (position >= segment.byteSize()) {
                throw new CborDeserializerException("unexpected end of input");
            }
            return SegmentAccess.getByte(segment, position++);
        }

        @Override
        public void rewind() {
            position--;
        }

        @Override
        public long remaining() {
            return segment.byteSize() - position;
        }

        @Override
        public void advance(long length) {
            if (length < 0L || position > segment.byteSize() - length) {
                throw new CborDeserializerException("unexpected end of input");
            }
            position += length;
        }

        @Override
        public byte[] copyBytes(int length) {
            checkLength(length);
            byte[] r = segment.asSlice(position, length).toArray(ValueLayout.JAVA_BYTE);
            position += length;
            return r;
        }

        @Override
        public String decodeUtf8(int length) {
            checkLength(length);
            byte[] bytes = segment.asSlice(position, length).toArray(ValueLayout.JAVA_BYTE);
            if (!CborUtf8Validator.validateHeap(bytes, 0, length)) {
                throw new CborDeserializerException("not valid utf-8 content");
            }
            position += length;
            return new String(bytes, StandardCharsets.UTF_8);
        }

        @Override
        public MarshallInfo lookupMappedName(MarshallFacade fc, int length) {
            checkLength(length);
            MarshallInfo r = fc.marshallInfoByMappedName(segment, position, position + length);
            position += length;
            return r;
        }

        @Override
        short readShortRaw() {
            checkLength(2);
            short r = SegmentAccess.getShort(segment, position, ByteOrder.BIG_ENDIAN);
            position += 2;
            return r;
        }

        @Override
        int readIntRaw() {
            checkLength(4);
            int r = SegmentAccess.getInt(segment, position, ByteOrder.BIG_ENDIAN);
            position += 4;
            return r;
        }

        @Override
        long readLongRaw() {
            checkLength(8);
            long r = SegmentAccess.getLong(segment, position, ByteOrder.BIG_ENDIAN);
            position += 8;
            return r;
        }

        @Override
        public void commit() {
            segmentReadBuffer.setPosition(position);
        }

        private void checkLength(int length) {
            if (length < 0 || position > segment.byteSize() - length) {
                throw new CborDeserializerException("unexpected end of input");
            }
        }
    }
}