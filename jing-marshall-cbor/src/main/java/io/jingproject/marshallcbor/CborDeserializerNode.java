package io.jingproject.marshallcbor;

import io.jingproject.marshall.MarshallBuilder;
import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.MarshallInfo;
import io.jingproject.marshall.MarshallUtil;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// explicit-stack node that processes one container or object at a time, mirroring the json
// deserializer node. a definite-length container keeps the number of entries that are still
// expected, while an indefinite-length container (count == -1) peeks the next byte and rewinds
// unless a 0xff break terminates the container.
@SuppressWarnings({"unchecked", "rawtypes"})
public final class CborDeserializerNode {
    private static final int BITMAP_INITIAL_SIZE = 8;
    private static final int ARR_INITIAL_SIZE = 4;
    private static final byte OBJ = (byte) 0;
    private static final byte ARR = (byte) 1;
    private static final byte COL = (byte) 2;
    private static final byte MAP = (byte) 3;
    private static final byte DUMMY_OBJ = (byte) 4;
    private static final byte DUMMY_COL = (byte) 5;
    private static final Map<Class<?>, CborDeserializerObjFunc> DIRECT_DESERIALIZABLE_FUNC_MAP;
    private static final CborDeserializerObjFunc[] FUNC_TABLE;

    static {
        Map<Class<?>, CborDeserializerObjFunc> r = new HashMap<>();
        r.put(CborValue.class, (b, _, c) -> {
            c.setObj(c.deserializeCborValue(b));
            return CborDeserializeResult.Continue;
        });
        r.put(CborTaggedType.class, (b, _, c) -> {
            c.setObj(c.deserializeCborTaggedType(b));
            return CborDeserializeResult.Continue;
        });
        r.put(CborBytesType.class, (b, _, c) -> {
            c.setObj(c.deserializeCborBytesType(b));
            return CborDeserializeResult.Continue;
        });
        CborDeserializerObjFunc strArrFunc = (b, _, c) -> {
            c.setObj(c.deserializeStringArray(b));
            return CborDeserializeResult.Continue;
        };
        r.put(CharSequence[].class, strArrFunc);
        r.put(String[].class, strArrFunc);

        DIRECT_DESERIALIZABLE_FUNC_MAP = Map.copyOf(r);
    }

    private static CborDeserializerObjFunc directDeserializableFunc(Class<?> clazz) {
        return DIRECT_DESERIALIZABLE_FUNC_MAP.get(clazz);
    }

    static {
        FUNC_TABLE = new CborDeserializerObjFunc[MarshallUtil.TYPE_SIZE];
        CborDeserializerObjFunc defaultFunc = (b, inf, c) -> {
            // exclude generic types
            if (inf.firstGenericType() != null || inf.secondGenericType() != null) {
                throw new CborDeserializerException("unsupported generic type : " + inf);
            }
            // matching direct deserializable value
            Class<?> rawType = inf.rawType();
            CborDeserializerObjFunc directDeserializableFunc = directDeserializableFunc(rawType);
            if (directDeserializableFunc != null) {
                return directDeserializableFunc.deserialize(b, inf, c);
            }
            // check if current type could be overridden by option
            CborDeserializeFunc customFunc = c.option().customFunc(rawType);
            if (customFunc != null) {
                return customFunc.deserialize(b, c);
            }
            // assuming marshallable
            c.checkObjStart(b);
            c.setType(rawType);
            return CborDeserializeResult.NewMarshallable;
        };
        Arrays.fill(FUNC_TABLE, defaultFunc);
        // builtin supported wrapper types
        FUNC_TABLE[MarshallUtil.BYTE_WRAPPER_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeByte(b));
            return CborDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.BOOLEAN_WRAPPER_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeBoolean(b));
            return CborDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.SHORT_WRAPPER_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeShort(b));
            return CborDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.CHAR_WRAPPER_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeChar(b));
            return CborDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.INT_WRAPPER_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeInt(b));
            return CborDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.LONG_WRAPPER_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeLong(b));
            return CborDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.FLOAT_WRAPPER_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeFloat(b));
            return CborDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.DOUBLE_WRAPPER_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeDouble(b));
            return CborDeserializeResult.Continue;
        };
        // builtin supported primitive array types
        FUNC_TABLE[MarshallUtil.BYTE_ARRAY_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeByteArray(b));
            return CborDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.BOOLEAN_ARRAY_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeBooleanArray(b));
            return CborDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.SHORT_ARRAY_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeShortArray(b));
            return CborDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.CHAR_ARRAY_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeCharArray(b));
            return CborDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.INT_ARRAY_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeIntArray(b));
            return CborDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.LONG_ARRAY_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeLongArray(b));
            return CborDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.FLOAT_ARRAY_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeFloatArray(b));
            return CborDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.DOUBLE_ARRAY_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeDoubleArray(b));
            return CborDeserializeResult.Continue;
        };
        // builtin supported wrapper array types
        FUNC_TABLE[MarshallUtil.BYTE_WRAPPER_ARRAY_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeByteWrapperArray(b));
            return CborDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.BOOLEAN_WRAPPER_ARRAY_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeBooleanWrapperArray(b));
            return CborDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.SHORT_WRAPPER_ARRAY_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeShortWrapperArray(b));
            return CborDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.CHAR_WRAPPER_ARRAY_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeCharWrapperArray(b));
            return CborDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.INT_WRAPPER_ARRAY_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeIntWrapperArray(b));
            return CborDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.LONG_WRAPPER_ARRAY_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeLongWrapperArray(b));
            return CborDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.FLOAT_WRAPPER_ARRAY_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeFloatWrapperArray(b));
            return CborDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.DOUBLE_WRAPPER_ARRAY_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeDoubleWrapperArray(b));
            return CborDeserializeResult.Continue;
        };
        // str
        FUNC_TABLE[MarshallUtil.CHARSEQUENCE_TYPE] = FUNC_TABLE[MarshallUtil.STRING_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeString(b));
            return CborDeserializeResult.Continue;
        };
        // array
        FUNC_TABLE[MarshallUtil.ARRAY_TYPE] = (b, inf, c) -> {
            Class<?> arrType = inf.rawType();
            CborDeserializerObjFunc directDeserializableFunc = directDeserializableFunc(arrType);
            if (directDeserializableFunc != null) {
                return directDeserializableFunc.deserialize(b, inf, c);
            }
            Class<?> componentType = arrType.componentType();
            if (componentType.isEnum()) {
                c.setObj(c.deserializeEnumArray(componentType, b));
                return CborDeserializeResult.Continue;
            }
            c.checkArrayStart(b);
            c.setType(componentType);
            return CborDeserializeResult.NewArr;
        };
        // enum
        FUNC_TABLE[MarshallUtil.ENUM_TYPE] = (b, inf, c) -> {
            Class<?> rawType = inf.rawType();
            CborDeserializeFunc customFunc = c.option().customFunc(rawType);
            if (customFunc != null) {
                return customFunc.deserialize(b, c);
            }
            c.setObj(c.deserializeEnum(rawType, b));
            return CborDeserializeResult.Continue;
        };
        // collection interface
        FUNC_TABLE[MarshallUtil.COLLECTION_INTERFACE_TYPE] = (b, inf, c) -> {
            c.checkArrayStart(b);
            c.setObj(MarshallUtil.newCollectionInterface(inf.rawType()));
            c.setType(inf.firstGenericType());
            return CborDeserializeResult.NewCol;
        };
        // collection impl
        FUNC_TABLE[MarshallUtil.COLLECTION_IMPL_TYPE] = (b, inf, c) -> {
            c.checkArrayStart(b);
            c.setObj(MarshallUtil.newCollectionImpl(inf.rawType()));
            c.setType(inf.firstGenericType());
            return CborDeserializeResult.NewCol;
        };
        // map interface
        FUNC_TABLE[MarshallUtil.MAP_INTERFACE_TYPE] = (b, inf, c) -> {
            c.checkObjStart(b);
            c.setObj(MarshallUtil.newMapInterface(inf.rawType()));
            c.setType(inf.secondGenericType());
            return CborDeserializeResult.NewMap;
        };
        // map impl
        FUNC_TABLE[MarshallUtil.MAP_IMPL_TYPE] = (b, inf, c) -> {
            c.checkObjStart(b);
            c.setObj(MarshallUtil.newMapImpl(inf.rawType()));
            c.setType(inf.secondGenericType());
            return CborDeserializeResult.NewMap;
        };
    }

    private byte type;
    private MarshallFacade fc;
    private MarshallBuilder builder;
    private byte[] bitmap;
    // remaining entries of a definite-length container; -1 for indefinite-length
    private int count = -1;
    private int index;
    private int marshallIndex;
    private int dummyIndex;
    private boolean pendingValueSkipped;
    private Object[] arr;
    private Class<?> componentType;
    private Class<?> elementType;
    private Class<?> valueType;
    private Collection col;
    private Map map;
    private String key;
    private CborDeserializeFunc func;

    public void initObj(MarshallFacade fc, int count) {
        this.type = OBJ;
        this.fc = fc;
        this.builder = fc.newBuilder();
        this.count = count;
        this.index = 0;
        this.marshallIndex = 0;
        this.dummyIndex = 0;
        this.pendingValueSkipped = false;
        int requiredBytes = (fc.totalElements() + 7) >> 3;
        if (bitmap == null || bitmap.length < requiredBytes) {
            bitmap = new byte[Math.max(BITMAP_INITIAL_SIZE, requiredBytes)];
        } else {
            Arrays.fill(bitmap, 0, requiredBytes, (byte) 0);
        }
    }

    public void initArr(Class<?> componentType, CborDeserializeFunc func, int count) {
        this.type = ARR;
        this.count = count;
        this.index = 0;
        this.componentType = componentType;
        this.func = func;
        if (count > 0) {
            arr = new Object[count];
        } else if (arr == null) {
            arr = new Object[ARR_INITIAL_SIZE];
        }
    }

    public void initCol(Collection<?> col, Class<?> elementType, CborDeserializeFunc func, int count) {
        this.type = COL;
        this.count = count;
        this.col = col;
        this.elementType = elementType;
        this.func = func;
    }

    public void initMap(Map<?, ?> map, Class<?> valueType, CborDeserializeFunc func, int count) {
        this.type = MAP;
        this.count = count;
        this.map = map;
        this.valueType = valueType;
        this.func = func;
    }

    public void initDummyObj(int count) {
        this.type = DUMMY_OBJ;
        this.count = count;
        this.dummyIndex = 0;
    }

    public void initDummyCol(int count) {
        this.type = DUMMY_COL;
        this.count = count;
        this.dummyIndex = 0;
    }

    public CborDeserializeResult process(boolean hasValue, CborDeserializerContext c) {
        return switch (type) {
            case OBJ -> processObj(hasValue, c);
            case ARR -> processArr(hasValue, c);
            case COL -> processCol(hasValue, c);
            case MAP -> processMap(hasValue, c);
            case DUMMY_OBJ -> processDummyObj(hasValue, c);
            case DUMMY_COL -> processDummyCol(hasValue, c);
            default -> throw new AssertionError();
        };
    }

    private boolean assignBitmap(int marshallIndex) {
        int byteOffset = marshallIndex >> 3;
        int bitOffset = marshallIndex & 0x7;
        byte mask = (byte) (1 << bitOffset);
        byte val = bitmap[byteOffset];
        bitmap[byteOffset] = (byte) (val | mask);
        return (val & mask) != 0;
    }

    // a definite-length container is exhausted when no entry is left; an
    // indefinite-length container is exhausted when its next byte is the break
    private boolean prepareNext(CborDeserializerContext c) {
        if (count >= 0) {
            return count != 0;
        }
        byte b = c.getByte();
        if (b == (byte) 0xFF) {
            return false;
        }
        c.rewind();
        return true;
    }

    private void consumeEntry() {
        if (count > 0) {
            count--;
        }
    }

    private void incrementDummyIndex(String message, CborDeserializerContext c) {
        if (dummyIndex >= c.option().maxDummyElements()) {
            throw new CborDeserializerException(message + dummyIndex);
        }
        dummyIndex++;
    }

    private CborDeserializeResult dummyResult(byte firstByte, CborDeserializerContext c) {
        switch (CborNumberUtil.majorOf(firstByte)) {
            case CborNumberUtil.TYPE_UNSIGNED, CborNumberUtil.TYPE_NEGATIVE -> c.skipNumber(firstByte);
            case CborNumberUtil.TYPE_BYTES -> c.skipBytes(firstByte);
            case CborNumberUtil.TYPE_TEXT -> c.skipString(firstByte);
            case CborNumberUtil.TYPE_ARRAY -> {
                c.checkArrayStart(firstByte);
                return CborDeserializeResult.NewDummyCol;
            }
            case CborNumberUtil.TYPE_MAP -> {
                c.checkObjStart(firstByte);
                return CborDeserializeResult.NewDummyObj;
            }
            case CborNumberUtil.TYPE_TAG -> throw new CborDeserializerException("tag not supported");
            case CborNumberUtil.TYPE_SIMPLE -> {
                int ai = CborNumberUtil.aiOf(firstByte);
                if (ai == 25) {
                    c.advance(2L);
                } else if (ai == 26) {
                    c.advance(4L);
                } else if (ai == 27) {
                    c.advance(8L);
                } else if (ai == 24 || ai >= 28) {
                    // ai 24 is a simple value with a payload, ai 28-30 are
                    // reserved and ai 31 is the break byte; none are supported
                    throw new CborDeserializerException("unsupported or illegal initial byte");
                }
                // ai 0-23 simple values (incl. bool/null/undefined) carry no payload
            }
            default -> throw new CborDeserializerException("unsupported or illegal initial byte");
        }
        incrementDummyIndex("exceeded max dummy elements limit : ", c);
        return CborDeserializeResult.Continue;
    }

    private void appendObjValue(Object value) {
        builder.writeObject(marshallIndex, value);
    }

    private void setObjValue(CborDeserializerContext c) {
        int required = 0;
        List<MarshallInfo> marshallInfos = fc.marshallInfos();
        for (MarshallInfo inf : marshallInfos) {
            if (!inf.skipDeserializing() && (c.option().ensureAllFieldsPresent() || inf.rawType().isPrimitive())) {
                required++;
            }
        }
        if (index != required) {
            for (int i = 0; i < fc.totalElements(); i++) {
                MarshallInfo inf = marshallInfos.get(i);
                if (!inf.skipDeserializing() && !assignBitmap(i)
                        && (c.option().ensureAllFieldsPresent() || inf.rawType().isPrimitive())) {
                    throw new CborDeserializerException("missing field : " + inf.fieldName());
                }
            }
        }
        c.setObj(fc.construct(builder));
    }

    private CborDeserializeResult processObj(boolean hasValue, CborDeserializerContext c) {
        if (hasValue) {
            Object lastValue = c.obj();
            if (pendingValueSkipped) {
                pendingValueSkipped = false;
            } else if (lastValue != null) {
                appendObjValue(lastValue);
            } else {
                incrementDummyIndex("too many dummy obj elements : ", c);
            }
        }
        for (; ; ) {
            if (!prepareNext(c)) {
                setObjValue(c);
                return CborDeserializeResult.Finish;
            }
            byte firstByte = c.getByte();
            MarshallInfo inf = c.deserializeMarshallInfo(fc, firstByte);
            firstByte = c.getByte();
            CborDeserializeResult r = inf == null ? dummyResult(firstByte, c) : objValueResult(firstByte, inf, c);
            consumeEntry();
            if (r != CborDeserializeResult.Continue) {
                return r;
            }
        }
    }

    private CborDeserializeResult objValueResult(byte firstByte, MarshallInfo inf, CborDeserializerContext c) {
        marshallIndex = inf.index();
        boolean skip = inf.skipDeserializing();
        if (assignBitmap(marshallIndex)) {
            throw new CborDeserializerException("duplicate key : " + inf.mappedName());
        }
        int type = inf.type() & MarshallUtil.TYPE_MASK;
        if (type <= MarshallUtil.DOUBLE_TYPE) {
            if (skip) {
                skipPrimitiveValue(firstByte, type, c);
            } else {
                deserializePrimitiveValue(firstByte, type, c);
                index++;
            }
            return CborDeserializeResult.Continue;
        }
        if (firstByte == (byte) 0xF6 && inf.rawType() != CborValue.class) {
            if (c.option().ensureAllFieldsPresent() && !skip) {
                index++;
            }
            return CborDeserializeResult.Continue;
        }
        CborDeserializeResult r = FUNC_TABLE[type].deserialize(firstByte, inf, c);
        if (c.option().ensureAllFieldsPresent() && !skip) {
            index++;
        }
        if (r == CborDeserializeResult.Continue) {
            if (!skip) {
                appendObjValue(c.obj());
            }
        } else {
            pendingValueSkipped = skip;
        }
        return r;
    }

    private void appendArrValue(Object value, CborDeserializerContext c) {
        if (index == c.option().maxArrayElements()) {
            throw new CborDeserializerException("too many array elements : " + index);
        }
        if (index == arr.length) {
            Object[] newArr = new Object[Math.addExact(arr.length, arr.length)];
            System.arraycopy(arr, 0, newArr, 0, arr.length);
            arr = newArr;
        }
        arr[index++] = value;
    }

    private CborDeserializeResult processArr(boolean hasValue, CborDeserializerContext c) {
        if (hasValue) {
            appendArrValue(c.obj(), c);
        }
        for (; ; ) {
            if (!prepareNext(c)) {
                Object result = Array.newInstance(componentType, index);
                //noinspection SuspiciousSystemArraycopy
                System.arraycopy(arr, 0, result, 0, index);
                c.setObj(result);
                return CborDeserializeResult.Finish;
            }
            byte firstByte = c.getByte();
            if (firstByte == (byte) 0xF6 && componentType != CborValue.class) {
                appendArrValue(null, c);
                consumeEntry();
                continue;
            }
            CborDeserializeResult r = func.deserialize(firstByte, c);
            consumeEntry();
            if (r == CborDeserializeResult.Continue) {
                appendArrValue(c.obj(), c);
            } else {
                return r;
            }
        }
    }

    private void appendColValue(Object value, CborDeserializerContext c) {
        if (col.size() >= c.option().maxArrayElements()) {
            throw new CborDeserializerException("too many array elements : " + col.size());
        }
        col.add(value);
    }

    private CborDeserializeResult processCol(boolean hasValue, CborDeserializerContext c) {
        if (hasValue) {
            appendColValue(c.obj(), c);
        }
        for (; ; ) {
            if (!prepareNext(c)) {
                c.setObj(col);
                return CborDeserializeResult.Finish;
            }
            byte firstByte = c.getByte();
            if (firstByte == (byte) 0xF6 && elementType != CborValue.class) {
                appendColValue(null, c);
                consumeEntry();
                continue;
            }
            CborDeserializeResult r = func.deserialize(firstByte, c);
            consumeEntry();
            if (r == CborDeserializeResult.Continue) {
                appendColValue(c.obj(), c);
            } else {
                return r;
            }
        }
    }

    private void appendMapValue(Object value, CborDeserializerContext c) {
        if (map.size() >= c.option().maxMapElements()) {
            throw new CborDeserializerException("too many map elements : " + map.size());
        }
        map.put(key, value);
    }

    private CborDeserializeResult processMap(boolean hasValue, CborDeserializerContext c) {
        if (hasValue) {
            appendMapValue(c.obj(), c);
        }
        for (; ; ) {
            if (!prepareNext(c)) {
                c.setObj(map);
                return CborDeserializeResult.Finish;
            }
            key = c.deserializeString(c.getByte());
            byte firstByte = c.getByte();
            if (firstByte == (byte) 0xF6 && valueType != CborValue.class) {
                appendMapValue(null, c);
                consumeEntry();
                continue;
            }
            CborDeserializeResult r = func.deserialize(firstByte, c);
            consumeEntry();
            if (r == CborDeserializeResult.Continue) {
                appendMapValue(c.obj(), c);
            } else {
                return r;
            }
        }
    }

    private CborDeserializeResult processDummyObj(boolean hasValue, CborDeserializerContext c) {
        if (hasValue) {
            incrementDummyIndex("too many dummy obj elements : ", c);
        }
        for (; ; ) {
            if (!prepareNext(c)) {
                c.setObj(null);
                return CborDeserializeResult.Finish;
            }
            c.skipString(c.getByte());
            byte firstByte = c.getByte();
            CborDeserializeResult r = dummyResult(firstByte, c);
            consumeEntry();
            if (r != CborDeserializeResult.Continue) {
                return r;
            }
        }
    }

    private CborDeserializeResult processDummyCol(boolean hasValue, CborDeserializerContext c) {
        if (hasValue) {
            incrementDummyIndex("too many dummy col elements : ", c);
        }
        for (; ; ) {
            if (!prepareNext(c)) {
                c.setObj(null);
                return CborDeserializeResult.Finish;
            }
            CborDeserializeResult r = dummyResult(c.getByte(), c);
            consumeEntry();
            if (r != CborDeserializeResult.Continue) {
                return r;
            }
        }
    }

    private void deserializePrimitiveValue(byte firstByte, int type, CborDeserializerContext c) {
        switch (type) {
            case MarshallUtil.BYTE_TYPE -> builder.writeByte(marshallIndex, c.deserializeByte(firstByte));
            case MarshallUtil.BOOLEAN_TYPE -> builder.writeBoolean(marshallIndex, c.deserializeBoolean(firstByte));
            case MarshallUtil.SHORT_TYPE -> builder.writeShort(marshallIndex, c.deserializeShort(firstByte));
            case MarshallUtil.CHAR_TYPE -> builder.writeChar(marshallIndex, c.deserializeChar(firstByte));
            case MarshallUtil.INT_TYPE -> builder.writeInt(marshallIndex, c.deserializeInt(firstByte));
            case MarshallUtil.LONG_TYPE -> builder.writeLong(marshallIndex, c.deserializeLong(firstByte));
            case MarshallUtil.FLOAT_TYPE -> builder.writeFloat(marshallIndex, c.deserializeFloat(firstByte));
            case MarshallUtil.DOUBLE_TYPE -> builder.writeDouble(marshallIndex, c.deserializeDouble(firstByte));
            default -> throw new AssertionError();
        }
    }

    // skip the scalar value of a skipDeserializing primitive field so the wire
    // stays in sync without touching the builder
    private void skipPrimitiveValue(byte firstByte, int type, CborDeserializerContext c) {
        switch (type) {
            case MarshallUtil.BYTE_TYPE, MarshallUtil.SHORT_TYPE, MarshallUtil.INT_TYPE,
                    MarshallUtil.LONG_TYPE, MarshallUtil.FLOAT_TYPE, MarshallUtil.DOUBLE_TYPE ->
                    c.skipNumber(firstByte);
            case MarshallUtil.BOOLEAN_TYPE -> c.deserializeBoolean(firstByte);
            case MarshallUtil.CHAR_TYPE -> c.skipString(firstByte);
            default -> throw new AssertionError();
        }
    }

    @FunctionalInterface
    interface CborDeserializerObjFunc {
        CborDeserializeResult deserialize(byte firstByte, MarshallInfo inf, CborDeserializerContext c);
    }
}
