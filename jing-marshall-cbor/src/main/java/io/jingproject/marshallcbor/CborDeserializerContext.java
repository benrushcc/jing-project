package io.jingproject.marshallcbor;

import io.jingproject.common.*;
import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.MarshallInfo;
import io.jingproject.marshall.Marshalls;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

// sealed context with a heap and a segment implementation, mirroring the json
// deserializer. every read primitive consumes the CBOR binary format: integers
// via major 0/1 heads, floats via 0xf9/0xfa/0xfb plus big-endian payloads, text
// as raw UTF-8 and byte strings via major type 2. container heads (major 4/5)
// carry a declared element count that is kept in the shared count field and
// read by the nodes through declaredCount(); an indefinite-length container is
// signalled by count == -1 and terminated by the 0xff break byte.
public sealed abstract class CborDeserializerContext permits
        CborDeserializerContext.CborDeserializerHeapContext,
        CborDeserializerContext.CborDeserializerSegmentContext {
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
        objFuncMap.put(CborValue.class, (b, c) -> {
            c.setObj(c.deserializeCborValue(b));
            return CborDeserializeResult.Continue;
        });
        objFuncMap.put(CborTaggedType.class, (b, c) -> {
            c.setObj(c.deserializeCborTaggedType(b));
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
    protected char[] charBuffer;

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

    public abstract void advance(long length);

    public abstract byte[] copyBytes(int length);

    public abstract String decodeUtf8(int length);

    // validates the utf-8 bytes of the next text payload without consuming them
    public abstract boolean validateUtf8(int length);

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

    // read a length head: like readArgument but ai 31 (indefinite) returns -1.
    // a 64-bit argument with the sign bit set cannot be represented as a java
    // length and is rejected instead of being mistaken for the indefinite marker.
    private long readLength(int ai) {
        if (ai == 31) {
            return -1L;
        }
        long length = readArgument(ai);
        if (length < 0L) {
            throw new CborDeserializerException("length exceeds limit : " + Long.toUnsignedString(length));
        }
        return length;
    }

    // the format does not model tags, and ai 24 in major type 7 carries a
    // one-byte simple value; reject both consistently instead of reporting a
    // type mismatch whose message depends on the expected java type
    private static void checkInitialByte(byte b) {
        int major = CborNumberUtil.majorOf(b);
        if (major == CborNumberUtil.TYPE_TAG) {
            throw new CborDeserializerException("tag not supported");
        }
        if (major == CborNumberUtil.TYPE_SIMPLE && CborNumberUtil.aiOf(b) == 24) {
            throw new CborDeserializerException("unsupported or illegal initial byte");
        }
    }

    // a definite array head must stay within maxArrayElements; indefinite is accepted
    public final void checkArrayStart(byte b) {
        checkInitialByte(b);
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
        checkInitialByte(b);
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
        checkInitialByte(firstByte);
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
        checkInitialByte(firstByte);
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

    // float16/float32 are widened to float; float64 may be narrowed only when
    // the value is exactly representable as a float, so no precision is lost
    public final float deserializeFloat(byte firstByte) {
        checkInitialByte(firstByte);
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
            double value = Double.longBitsToDouble(readLongRaw());
            float result = (float) value;
            if (Double.doubleToRawLongBits((double) result) != Double.doubleToRawLongBits(value)) {
                throw new CborDeserializerException("float value loses precision : " + value);
            }
            return result;
        }
        throw new CborDeserializerException("not a float value : " + firstByte);
    }

    public final double deserializeDouble(byte firstByte) {
        checkInitialByte(firstByte);
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
        checkInitialByte(firstByte);
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
        checkInitialByte(firstByte);
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

    public final CborBytesType deserializeCborBytesType(byte firstByte) {
        return new CborBytesType(deserializeBytes(firstByte));
    }

    public final CborTaggedType deserializeCborTaggedType(byte firstByte) {
        CborValue value = deserializeCborValue(firstByte);
        if (value instanceof CborTaggedType taggedType) {
            return taggedType;
        }
        throw new CborDeserializerException("not a tagged type : " + firstByte);
    }

    // decodes a complete value tree without recursion: every container pushes a
    // frame, and a completed child bubbles up until a parent frame is satisfied
    public final CborValue deserializeCborValue(byte firstByte) {
        Deque<CborValueFrame> frames = new ArrayDeque<>();
        CborValue value = startCborValue(firstByte, frames);
        for (; ; ) {
            if (value != null) {
                for (; ; ) {
                    if (frames.isEmpty()) {
                        return value;
                    }
                    CborValueFrame frame = frames.peek();
                    CborValue completed = frame.accept(value);
                    if (completed == null) {
                        break;
                    }
                    frames.pop();
                    value = completed;
                }
            }
            CborValueFrame frame = frames.peek();
            if (frame.indefinite() && frame.atElementStart()) {
                byte b = getByte();
                if (b == (byte) 0xFF) {
                    frames.pop();
                    value = frame.finish();
                    continue;
                }
                rewind();
            }
            byte b = getByte();
            value = startCborValue(b, frames);
        }
    }

    private CborValue startCborValue(byte firstByte, Deque<CborValueFrame> frames) {
        int major = CborNumberUtil.majorOf(firstByte);
        if (major == CborNumberUtil.TYPE_UNSIGNED || major == CborNumberUtil.TYPE_NEGATIVE) {
            return new CborInteger(deserializeLong(firstByte));
        }
        if (major == CborNumberUtil.TYPE_BYTES) {
            return deserializeCborBytesType(firstByte);
        }
        if (major == CborNumberUtil.TYPE_TEXT) {
            return new CborText(deserializeString(firstByte));
        }
        if (major == CborNumberUtil.TYPE_ARRAY) {
            checkArrayStart(firstByte);
            int arrayCount = count;
            if (arrayCount == 0) {
                return new CborArray(List.of());
            }
            pushFrame(frames, new CborArrayFrame(arrayCount, option.maxArrayElements()));
            return null;
        }
        if (major == CborNumberUtil.TYPE_MAP) {
            checkObjStart(firstByte);
            int mapCount = count;
            if (mapCount == 0) {
                return new CborMap(List.of());
            }
            pushFrame(frames, new CborMapFrame(mapCount, option.maxMapElements()));
            return null;
        }
        if (major == CborNumberUtil.TYPE_TAG) {
            pushFrame(frames, new CborTagFrame(readArgument(CborNumberUtil.aiOf(firstByte))));
            return null;
        }
        if (major == CborNumberUtil.TYPE_SIMPLE) {
            int ai = CborNumberUtil.aiOf(firstByte);
            if (ai == 20 || ai == 21) {
                return new CborBool(ai == 21);
            }
            if (ai == 22) {
                return new CborNull();
            }
            if (ai == 25) {
                return new CborFloat(readShortRaw() & 0xFFFFL, CborFloatWidth.HALF);
            }
            if (ai == 26) {
                return new CborFloat(readIntRaw() & 0xFFFFFFFFL, CborFloatWidth.SINGLE);
            }
            if (ai == 27) {
                return new CborFloat(readLongRaw(), CborFloatWidth.DOUBLE);
            }
        }
        throw new CborDeserializerException("unsupported or illegal initial byte");
    }

    private void pushFrame(Deque<CborValueFrame> frames, CborValueFrame frame) {
        if (frames.size() >= option.maxNestedSize()) {
            throw new CborDeserializerException("exceeded maximum nested size : " + option.maxNestedSize());
        }
        frames.push(frame);
    }

    private abstract static class CborValueFrame {
        abstract boolean indefinite();

        abstract boolean atElementStart();

        abstract CborValue accept(CborValue value);

        abstract CborValue finish();
    }

    private static final class CborArrayFrame extends CborValueFrame {
        private final List<CborValue> elements = new ArrayList<>();
        private final int maxElements;
        private int remaining;

        CborArrayFrame(int remaining, int maxElements) {
            this.remaining = remaining;
            this.maxElements = maxElements;
        }

        @Override
        boolean indefinite() {
            return remaining < 0;
        }

        @Override
        boolean atElementStart() {
            return true;
        }

        @Override
        CborValue accept(CborValue value) {
            elements.add(value);
            if (elements.size() > maxElements) {
                throw new CborDeserializerException("too many array elements : " + elements.size());
            }
            if (remaining > 0) {
                remaining--;
                if (remaining == 0) {
                    return new CborArray(elements);
                }
            }
            return null;
        }

        @Override
        CborValue finish() {
            return new CborArray(elements);
        }
    }

    private static final class CborMapFrame extends CborValueFrame {
        private final List<CborMapEntry> entries = new ArrayList<>();
        private final int maxEntries;
        private int remaining;
        private CborValue key;
        private boolean expectingKey = true;

        CborMapFrame(int remaining, int maxEntries) {
            this.remaining = remaining;
            this.maxEntries = maxEntries;
        }

        @Override
        boolean indefinite() {
            return remaining < 0;
        }

        @Override
        boolean atElementStart() {
            return expectingKey;
        }

        @Override
        CborValue accept(CborValue value) {
            if (expectingKey) {
                key = value;
                expectingKey = false;
                return null;
            }
            entries.add(new CborMapEntry(key, value));
            key = null;
            expectingKey = true;
            if (entries.size() > maxEntries) {
                throw new CborDeserializerException("too many map elements : " + entries.size());
            }
            if (remaining > 0) {
                remaining--;
                if (remaining == 0) {
                    return new CborMap(entries);
                }
            }
            return null;
        }

        @Override
        CborValue finish() {
            return new CborMap(entries);
        }
    }

    private static final class CborTagFrame extends CborValueFrame {
        private final long tag;

        CborTagFrame(long tag) {
            this.tag = tag;
        }

        @Override
        boolean indefinite() {
            return false;
        }

        @Override
        boolean atElementStart() {
            return true;
        }

        @Override
        CborValue accept(CborValue value) {
            return new CborTaggedType(tag, value);
        }

        @Override
        CborValue finish() {
            throw new AssertionError();
        }
    }

    public final void skipNumber(byte firstByte) {
        checkInitialByte(firstByte);
        int major = CborNumberUtil.majorOf(firstByte);
        if (major == CborNumberUtil.TYPE_UNSIGNED || major == CborNumberUtil.TYPE_NEGATIVE) {
            readArgument(CborNumberUtil.aiOf(firstByte));
            return;
        }
        if (major == CborNumberUtil.TYPE_SIMPLE) {
            int ai = CborNumberUtil.aiOf(firstByte);
            if (ai == 25) {
                advance(2L);
            } else if (ai == 26) {
                advance(4L);
            } else if (ai == 27) {
                advance(8L);
            } else {
                // ai 0-24 are simple values, not numbers; ai 28-31 are reserved
                throw new CborDeserializerException("not a number : " + firstByte);
            }
            return;
        }
        throw new CborDeserializerException("not a number : " + firstByte);
    }

    public final void skipString(byte firstByte) {
        checkInitialByte(firstByte);
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
        int intLength = Math.toIntExact(length);
        if (!validateUtf8(intLength)) {
            throw new CborDeserializerException("not valid utf-8 content");
        }
        advance(intLength);
    }

    public final void skipBytes(byte firstByte) {
        checkInitialByte(firstByte);
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

    // a mapped name is hashed and compared against the raw bytes directly, so
    // the lookup itself allocates nothing; the implementation validates UTF-8
    // only when the bytes do not match a known mapped name
    public final MarshallInfo deserializeMarshallInfo(MarshallFacade fc, byte firstByte) {
        checkInitialByte(firstByte);
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
            if (length == 0) {
                return "";
            }
            if (!CborUtf8Validator.validateHeap(bytes, position, position + length)) {
                throw new CborDeserializerException("not valid utf-8 content");
            }
            String r = new String(bytes, position, length, StandardCharsets.UTF_8);
            position += length;
            return r;
        }

        @Override
        public boolean validateUtf8(int length) {
            checkLength(length);
            return CborUtf8Validator.validateHeap(bytes, position, position + length);
        }

        @Override
        public MarshallInfo lookupMappedName(MarshallFacade fc, int length) {
            checkLength(length);
            MarshallInfo r = fc.marshallInfoByMappedName(bytes, position, position + length);
            if (r == null && !CborUtf8Validator.validateHeap(bytes, position, position + length)) {
                throw new CborDeserializerException("not valid utf-8 content");
            }
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
            if (length == 0) {
                return "";
            }
            if (!CborUtf8Validator.validateSegment(segment, position, position + length)) {
                throw new CborDeserializerException("not valid utf-8 content");
            }
            if (charBuffer == null) {
                charBuffer = new char[Math.max(length, option.charBufferSize())];
            } else if (length > charBuffer.length) {
                charBuffer = new char[Math.max(length, Math.addExact(charBuffer.length, charBuffer.length))];
            }
            int charCount = 0;
            long end = position + length;
            while (position < end) {
                int b0 = SegmentAccess.getByte(segment, position++) & 0xFF;
                if (b0 < 0x80) {
                    charBuffer[charCount++] = (char) b0;
                } else if (b0 < 0xE0) {
                    int b1 = SegmentAccess.getByte(segment, position++) & 0x3F;
                    charBuffer[charCount++] = (char) (((b0 & 0x1F) << 6) | b1);
                } else if (b0 < 0xF0) {
                    int b1 = SegmentAccess.getByte(segment, position++) & 0x3F;
                    int b2 = SegmentAccess.getByte(segment, position++) & 0x3F;
                    charBuffer[charCount++] = (char) (((b0 & 0x0F) << 12) | (b1 << 6) | b2);
                } else {
                    int b1 = SegmentAccess.getByte(segment, position++) & 0x3F;
                    int b2 = SegmentAccess.getByte(segment, position++) & 0x3F;
                    int b3 = SegmentAccess.getByte(segment, position++) & 0x3F;
                    int codePoint = (((b0 & 0x07) << 18) | (b1 << 12) | (b2 << 6) | b3) - 0x10000;
                    charBuffer[charCount++] = (char) (0xD800 | (codePoint >> 10));
                    charBuffer[charCount++] = (char) (0xDC00 | (codePoint & 0x3FF));
                }
            }
            return new String(charBuffer, 0, charCount);
        }

        @Override
        public boolean validateUtf8(int length) {
            checkLength(length);
            return CborUtf8Validator.validateSegment(segment, position, position + length);
        }

        @Override
        public MarshallInfo lookupMappedName(MarshallFacade fc, int length) {
            checkLength(length);
            MarshallInfo r = fc.marshallInfoByMappedName(segment, position, position + length);
            if (r == null && !CborUtf8Validator.validateSegment(segment, position, position + length)) {
                throw new CborDeserializerException("not valid utf-8 content");
            }
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