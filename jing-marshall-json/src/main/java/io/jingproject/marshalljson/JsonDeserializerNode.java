package io.jingproject.marshalljson;

import io.jingproject.marshall.*;

import java.lang.reflect.Array;
import java.util.*;

@SuppressWarnings({"unchecked", "rawtypes"})
public final class JsonDeserializerNode {
    private static final int BITMAP_INITIAL_SIZE = 8;
    private static final int ARR_INITIAL_SIZE = 4;
    private static final byte OBJ = (byte) 0;
    private static final byte ARR = (byte) 1;
    private static final byte COL = (byte) 2;
    private static final byte MAP = (byte) 3;
    private static final byte DUMMY_OBJ = (byte) 4;
    private static final byte DUMMY_COL = (byte) 5;
    private static final Map<Class<?>, JsonDeserializerObjFunc> DIRECT_DESERIALIZABLE_FUNC_MAP;
    private static final JsonDeserializerObjFunc[] FUNC_TABLE;

    static {
        Map<Class<?>, JsonDeserializerObjFunc> r = new HashMap<>();
        r.put(JsonPrimitiveType.class, (b, _, c) -> {
            c.setObj(c.deserializeJsonPrimitiveType(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(JsonBoolType.class, (b, _, c) -> {
            c.setObj(c.deserializeJsonBoolType(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(JsonNumberType.class, (b, _, c) -> {
            c.setObj(c.deserializeJsonNumberType(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(JsonStrType.class, (b, _, c) -> {
            c.setObj(c.deserializeJsonStrType(b));
            return JsonDeserializeResult.Continue;
        });
        JsonDeserializerObjFunc strArrFunc = (b, _, c) -> {
            c.setObj(c.deserializeStringArray(b));
            return JsonDeserializeResult.Continue;
        };
        r.put(CharSequence[].class, strArrFunc);
        r.put(String[].class, strArrFunc);
        r.put(JsonPrimitiveType[].class, (b, _, c) -> {
            c.setObj(c.deserializeJsonPrimitiveTypeArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(JsonBoolType[].class, (b, _, c) -> {
            c.setObj(c.deserializeJsonBoolTypeArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(JsonNumberType[].class, (b, _, c) -> {
            c.setObj(c.deserializeJsonNumberTypeArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(JsonStrType[].class, (b, _, c) -> {
            c.setObj(c.deserializeJsonStrTypeArray(b));
            return JsonDeserializeResult.Continue;
        });
        DIRECT_DESERIALIZABLE_FUNC_MAP = Map.copyOf(r);
    }

    private static JsonDeserializerObjFunc directDeserializableFunc(Class<?> clazz) {
        return DIRECT_DESERIALIZABLE_FUNC_MAP.get(clazz);
    }

    static {
        FUNC_TABLE = new JsonDeserializerObjFunc[MarshallUtil.TYPE_SIZE];
        JsonDeserializerObjFunc defaultFunc = (b, inf, c) -> {
            // exclude generic types
            if (inf.firstGenericType() != null || inf.secondGenericType() != null) {
                throw new JsonSerializerException("unsupported generic type : " + inf);
            }
            // matching direct deserializable value
            Class<?> rawType = inf.rawType();
            JsonDeserializerObjFunc directDeserializableFunc = directDeserializableFunc(rawType);
            if (directDeserializableFunc != null) {
                return directDeserializableFunc.deserialize(b, inf, c);
            }
            // check if current type could be override by option
            JsonDeserializeFunc customFunc = c.option().customFunc(rawType);
            if (customFunc != null) {
                return customFunc.deserialize(b, c);
            }
            // assuming marshallable
            JsonDeserializerContext.checkObjStart(b);
            c.setType(rawType);
            return JsonDeserializeResult.NewMarshallable;
        };
        Arrays.fill(FUNC_TABLE, defaultFunc);
        // builtin supported wrapper types
        FUNC_TABLE[MarshallUtil.BYTE_WRAPPER_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeByte(b));
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.BOOLEAN_WRAPPER_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeBoolean(b));
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.SHORT_WRAPPER_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeShort(b));
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.CHAR_WRAPPER_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeChar(b));
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.INT_WRAPPER_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeInt(b));
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.LONG_WRAPPER_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeLong(b));
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.FLOAT_WRAPPER_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeFloat(b));
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.DOUBLE_WRAPPER_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeDouble(b));
            return JsonDeserializeResult.Continue;
        };
        // builtin supported primitive array types
        FUNC_TABLE[MarshallUtil.BYTE_ARRAY_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeByteArray(b));
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.BOOLEAN_ARRAY_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeBooleanArray(b));
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.SHORT_ARRAY_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeShortArray(b));
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.CHAR_ARRAY_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeCharArray(b));
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.INT_ARRAY_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeIntArray(b));
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.LONG_ARRAY_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeLongArray(b));
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.FLOAT_ARRAY_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeFloatArray(b));
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.DOUBLE_ARRAY_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeDoubleArray(b));
            return JsonDeserializeResult.Continue;
        };
        // builtin supported wrapper array types
        FUNC_TABLE[MarshallUtil.BYTE_WRAPPER_ARRAY_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeByteWrapperArray(b));
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.BOOLEAN_WRAPPER_ARRAY_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeBooleanWrapperArray(b));
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.SHORT_WRAPPER_ARRAY_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeShortWrapperArray(b));
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.CHAR_WRAPPER_ARRAY_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeCharWrapperArray(b));
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.INT_WRAPPER_ARRAY_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeIntWrapperArray(b));
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.LONG_WRAPPER_ARRAY_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeLongWrapperArray(b));
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.FLOAT_WRAPPER_ARRAY_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeFloatWrapperArray(b));
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.DOUBLE_WRAPPER_ARRAY_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeDoubleWrapperArray(b));
            return JsonDeserializeResult.Continue;
        };
        // str
        FUNC_TABLE[MarshallUtil.CHARSEQUENCE_TYPE] = FUNC_TABLE[MarshallUtil.STRING_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeString(b));
            return JsonDeserializeResult.Continue;
        };
        // array
        FUNC_TABLE[MarshallUtil.ARRAY_TYPE] = (b, inf, c) -> {
            Class<?> arrType = inf.rawType();
            JsonDeserializerObjFunc directDeserializableFunc = directDeserializableFunc(arrType);
            if (directDeserializableFunc != null) {
                return directDeserializableFunc.deserialize(b, inf, c);
            }
            JsonDeserializeFunc customArrFunc = c.option().customArrFunc(arrType);
            if(customArrFunc != null) {
                return customArrFunc.deserialize(b, c);
            }
            Class<?> componentType = arrType.componentType();
            if(componentType.isEnum()) {
                c.setObj(c.deserializeEnumArray(componentType, b));
                return JsonDeserializeResult.Continue;
            }
            JsonDeserializerContext.checkArrayStart(b);
            c.setType(arrType);
            return JsonDeserializeResult.NewArr;
        };
        // enum
        FUNC_TABLE[MarshallUtil.ENUM_TYPE] = (b, inf, c) -> {
            Class<?> rawType = inf.rawType();
            JsonDeserializeFunc customFunc = c.option().customFunc(rawType);
            if (customFunc != null) {
                return customFunc.deserialize(b, c);
            }
            c.setObj(c.deserializeEnum(rawType, b));
            return JsonDeserializeResult.Continue;
        };
        // collection interface
        FUNC_TABLE[MarshallUtil.COLLECTION_INTERFACE_TYPE] = (b, inf, c) -> {
            JsonDeserializerContext.checkArrayStart(b);
            c.setObj(MarshallUtil.newCollectionInterface(inf.rawType()));
            c.setType(inf.firstGenericType());
            return JsonDeserializeResult.NewCol;
        };
        // collection impl
        FUNC_TABLE[MarshallUtil.COLLECTION_IMPL_TYPE] = (b, inf, c) -> {
            JsonDeserializerContext.checkArrayStart(b);
            c.setObj(MarshallUtil.newCollectionImpl(inf.rawType()));
            c.setType(inf.firstGenericType());
            return JsonDeserializeResult.NewCol;
        };
        // map interface
        FUNC_TABLE[MarshallUtil.MAP_INTERFACE_TYPE] = (b, inf, c) -> {
            JsonDeserializerContext.checkObjStart(b);
            c.setObj(MarshallUtil.newMapInterface(inf.rawType()));
            c.setType(inf.secondGenericType());
            return JsonDeserializeResult.NewMap;
        };
        // map impl
        FUNC_TABLE[MarshallUtil.MAP_IMPL_TYPE] = (b, inf, c) -> {
            JsonDeserializerContext.checkObjStart(b);
            c.setObj(MarshallUtil.newMapImpl(inf.rawType()));
            c.setType(inf.secondGenericType());
            return JsonDeserializeResult.NewMap;
        };
    }

    private byte type;
    private MarshallFacade fc;
    private MarshallBuilder builder;
    private byte[] bitmap;
    private int index;
    private int marshallIndex;
    private int dummyIndex;
    private Object[] arr;
    private Class componentType;
    private Collection col;
    private Map map;
    private String key;
    private JsonDeserializeFunc func;

    public void initObj(MarshallFacade fc) {
        this.type = OBJ;
        this.fc = fc;
        this.builder = fc.newBuilder();
        this.index = 0;
        this.marshallIndex = 0;
        this.dummyIndex = 0;
        initBitmap(fc);
    }

    public void initArr(Class<?> componentType, JsonDeserializeFunc func) {
        this.type = ARR;
        this.index = 0;
        this.componentType = componentType;
        this.func = func;
        initArr();
    }

    public void initCol(Collection<?> col, JsonDeserializeFunc func) {
        this.type = COL;
        this.col = col;
        this.func = func;
    }

    public void initMap(Map<?, ?> map, JsonDeserializeFunc func) {
        this.type = MAP;
        this.map = map;
        this.func = func;
    }

    public void initDummyObj() {
        this.type = DUMMY_OBJ;
        this.dummyIndex = 0;
    }

    public void initDummyCol() {
        this.type = DUMMY_COL;
        this.dummyIndex = 0;
    }

    public JsonDeserializeResult process(boolean hasValue, JsonDeserializerContext c) {
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

    private void initBitmap(MarshallFacade fc) {
        int requiredBytes = (fc.totalElements() + 7) >> 3; // no overflow
        if(bitmap == null || bitmap.length < requiredBytes) {
            bitmap = new byte[Math.max(BITMAP_INITIAL_SIZE, requiredBytes)];
        } else {
            Arrays.fill(bitmap, 0, requiredBytes, (byte) 0);
        }
    }

    private boolean assignBitmap(int marshallIndex) {
        int byteOffset = marshallIndex >> 3;
        int bitOffset = marshallIndex & 0x7;
        byte mask = (byte) (1 << bitOffset);
        byte val = bitmap[byteOffset];
        bitmap[byteOffset] = (byte) (val | mask);
        return (val & mask) != 0;
    }

    private void initArr() {
        if(arr == null) {
            arr = new Object[ARR_INITIAL_SIZE];
        }
    }

    private JsonDeserializeResult dummyResult(byte firstByte, JsonDeserializerContext c) {
        switch (firstByte) {
            case (byte) '{' -> {
                return JsonDeserializeResult.NewDummyObj;
            }
            case (byte) '[' -> {
                return JsonDeserializeResult.NewDummyCol;
            }
            case (byte) 'n' -> c.deserializeNull();
            case (byte) 't', (byte) 'f' -> c.deserializeBoolean(firstByte);
            case (byte) '"' -> c.deserializeString(firstByte);
            default -> c.skipNumber(firstByte);
        }
        if(dummyIndex == c.option().maxDummyElements()) {
            throw new JsonDeserializerException("exceeded max dummy elements limit : " + dummyIndex);
        }
        dummyIndex++;
        return JsonDeserializeResult.Continue;
    }

    private void appendObjValue(Object value) {
        builder.writeObject(marshallIndex, value);
    }

    private void setObjValue(JsonDeserializerContext c) {
        int required = c.option().ensureAllFieldsPresent() ? fc.totalElements() : fc.primitiveElements();
        if(index != required) {
            List<MarshallInfo> marshallInfos = fc.marshallInfos();
            for(int i = 0; i < fc.totalElements(); i++) {
                MarshallInfo inf = marshallInfos.get(i);
                if(!assignBitmap(i) && (c.option().ensureAllFieldsPresent() || inf.rawType().isPrimitive())) {
                    throw new JsonDeserializerException("missing field : " + inf.fieldName());
                }
            }
        }
        c.setObj(fc.construct(builder));
    }

    private JsonDeserializeResult objRoundResult(boolean hasValue, JsonDeserializerContext c) {
        byte firstByte = c.nextValuableByte();
        if(hasValue) {
            Object lastValue = c.obj();
            if(lastValue != null) {
                appendObjValue(lastValue);
            } else {
                if(dummyIndex == c.option().maxDummyElements()) {
                    throw new JsonDeserializerException("exceeded max dummy elements limit : " + dummyIndex);
                }
                dummyIndex++;
            }
        }
        if(firstByte == (byte) '}') {
            setObjValue(c);
            return JsonDeserializeResult.Finish;
        }
        if(hasValue) {
            if(firstByte != (byte) ',') {
                throw new JsonDeserializerException("illegal separator, got : " + firstByte);
            }
        } else {
            c.rewind();
        }
        return JsonDeserializeResult.Continue;
    }

    private JsonDeserializeResult objSepResult(JsonDeserializerContext c) {
        byte b = c.nextValuableByte();
        if(b == (byte) '}') {
            setObjValue(c);
            return JsonDeserializeResult.Finish;
        } else if(b == (byte) ',') {
            return JsonDeserializeResult.Continue;
        } else {
            throw new JsonDeserializerException("illegal separator, got : " + b);
        }
    }

    private JsonDeserializeResult objValueResult(byte firstByte, MarshallInfo inf, JsonDeserializerContext c) {
        marshallIndex = inf.index();
        if(assignBitmap(marshallIndex)) {
            throw new JsonDeserializerException("duplicate key : " + inf.mappedName());
        }
        int type = inf.type() & MarshallUtil.TYPE_MASK;
        if(type <= MarshallUtil.DOUBLE_TYPE) {
            deserializePritimiveValue(firstByte, type, c);
            index++;
            return JsonDeserializeResult.Continue;
        }
        if(firstByte == (byte) 'n') {
            c.deserializeNull();
            if(c.option().ensureAllFieldsPresent()) {
                index++;
            }
            return JsonDeserializeResult.Continue;
        }
        JsonDeserializeResult r = FUNC_TABLE[type].deserialize(firstByte, inf, c);
        if (c.option().ensureAllFieldsPresent()) {
            index++;
        }
        if(r == JsonDeserializeResult.Continue) {
            appendObjValue(c.obj());
        }
        return r;
    }

    private JsonDeserializeResult processObj(boolean hasValue, JsonDeserializerContext c) {
        JsonDeserializeResult r = objRoundResult(hasValue, c);
        if (r != JsonDeserializeResult.Continue) {
            return r;
        }
        for( ; ; ) {
            byte firstByte = c.nextValuableByte();
            MarshallInfo inf = c.deserializeMarshallInfo(fc, firstByte);
            firstByte = c.skipColon();
            r = inf == null ? dummyResult(firstByte, c) : objValueResult(firstByte, inf, c);
            if(r != JsonDeserializeResult.Continue) {
                return r;
            }
            r = objSepResult(c);
            if(r != JsonDeserializeResult.Continue) {
                return r;
            }
        }
    }

    private void appendArrValue(Object value, JsonDeserializerContext c) {
        if(index == c.option().maxArrayElements()) {
            throw new JsonDeserializerException("too many array elements : " + index);
        }
        if(index == arr.length) {
            Object[] newArr = new Object[Math.addExact(arr.length, arr.length)];
            System.arraycopy(arr, 0, newArr, 0, arr.length);
            arr = newArr;
        }
        arr[index++] = value;
    }

    private void setArrValue(JsonDeserializerContext c) {
        Object r = Array.newInstance(componentType, index);
        //noinspection SuspiciousSystemArraycopy
        System.arraycopy(arr, 0, r, 0, index);
        c.setObj(arr);
    }

    private JsonDeserializeResult arrRoundResult(boolean hasValue, JsonDeserializerContext c) {
        byte firstByte = c.nextValuableByte();
        if(hasValue) {
            Object lastValue = c.obj();
            if(lastValue != null) {
                appendArrValue(lastValue, c);
            }
        }
        if(firstByte == (byte) ']') {
            setArrValue(c);
            return JsonDeserializeResult.Finish;
        }
        if(hasValue) {
            if(firstByte != (byte) ',') {
                throw new JsonDeserializerException("illegal separator, got : " + firstByte);
            }
        } else {
            c.rewind();
        }
        return JsonDeserializeResult.Continue;
    }

    private JsonDeserializeResult arrSepResult(JsonDeserializerContext c) {
        byte b = c.nextValuableByte();
        if(b == (byte) ']') {
            setArrValue(c);
            return JsonDeserializeResult.Finish;
        } else if(b == (byte) ',') {
            return JsonDeserializeResult.Continue;
        } else {
            throw new JsonDeserializerException("illegal separator, got : " + b);
        }
    }

    private JsonDeserializeResult processArr(boolean hasValue, JsonDeserializerContext c) {
        JsonDeserializeResult r = arrRoundResult(hasValue, c);
        if(r != JsonDeserializeResult.Continue) {
            return r;
        }
        for( ; ; ) {
            byte firstByte = c.nextValuableByte();
            r = func.deserialize(firstByte, c);
            if(r != JsonDeserializeResult.Continue) {
                return r;
            }
            Object value = c.obj();
            appendArrValue(value, c);
            r = arrSepResult(c);
            if(r != JsonDeserializeResult.Continue) {
                return r;
            }
        }
    }

    private void appendColValue(Object value, JsonDeserializerContext c) {
        if(col.size() == c.option().maxArrayElements()) {
            throw new JsonDeserializerException("too many array elements : " + col.size());
        }
        col.add(value);
    }

    private JsonDeserializeResult colRoundResult(boolean hasValue, JsonDeserializerContext c) {
        byte firstByte = c.nextValuableByte();
        if(hasValue) {
            Object lastValue = c.obj();
            if(lastValue != null) {
                appendColValue(lastValue, c);
            }
        }
        if(firstByte == (byte) '}') {
            c.setObj(col);
            return JsonDeserializeResult.Finish;
        }
        if(hasValue) {
            if(firstByte != (byte) ',') {
                throw new JsonDeserializerException("illegal separator, got : " + firstByte);
            }
        } else {
            c.rewind();
        }
        return JsonDeserializeResult.Continue;
    }

    private JsonDeserializeResult colSepResult(JsonDeserializerContext c) {
        byte b = c.nextValuableByte();
        if(b == (byte) ']') {
            c.setObj(col);
            return JsonDeserializeResult.Finish;
        } else if(b == (byte) ',') {
            return JsonDeserializeResult.Continue;
        } else {
            throw new JsonDeserializerException("illegal separator, got : " + b);
        }
    }

    private JsonDeserializeResult processCol(boolean hasValue, JsonDeserializerContext c) {
        JsonDeserializeResult r = colRoundResult(hasValue, c);
        if(r != JsonDeserializeResult.Continue) {
            return r;
        }
        for( ; ; ) {
            byte firstByte = c.nextValuableByte();
            r = func.deserialize(firstByte, c);
            if(r != JsonDeserializeResult.Continue) {
                return r;
            }
            appendColValue(c.obj(), c);
            r = colSepResult(c);
            if(r != JsonDeserializeResult.Continue) {
                return r;
            }
        }
    }

    private void appendMapValue(Object value, JsonDeserializerContext c) {
        if(map.size() == c.option().maxMapElements()) {
            throw new JsonDeserializerException("too many map elements : " + map.size());
        }
        map.put(key, value);
    }

    private JsonDeserializeResult mapRoundResult(boolean hasValue, JsonDeserializerContext c) {
        byte firstByte = c.nextValuableByte();
        if(hasValue) {
            Object lastValue = c.obj();
            if(lastValue != null) {
                appendMapValue(lastValue, c);
            }
        }
        if(firstByte == (byte) '}') {
            c.setObj(map);
            return JsonDeserializeResult.Finish;
        }
        if(hasValue) {
            if(firstByte != (byte) ',') {
                throw new JsonDeserializerException("illegal separator, got : " + firstByte);
            }
        } else {
            c.rewind();
        }
        return JsonDeserializeResult.Continue;
    }

    private JsonDeserializeResult mapSepResult(JsonDeserializerContext c) {
        byte b = c.nextValuableByte();
        if(b == (byte) '}') {
            c.setObj(map);
            return JsonDeserializeResult.Finish;
        } else if(b == (byte) ',') {
            return JsonDeserializeResult.Continue;
        } else {
            throw new JsonDeserializerException("illegal separator, got : " + b);
        }
    }

    private JsonDeserializeResult processMap(boolean hasValue, JsonDeserializerContext c) {
        JsonDeserializeResult r = mapRoundResult(hasValue, c);
        if (r != JsonDeserializeResult.Continue) {
            return r;
        }
        for( ; ; ) {
            byte firstByte = c.nextValuableByte();
            key = c.deserializeString(firstByte);
            r = func.deserialize(c.skipColon(), c);
            if (r != JsonDeserializeResult.Continue) {
                return r;
            }
            appendMapValue(c.obj(), c);
            r = mapSepResult(c);
            if(r != JsonDeserializeResult.Continue) {
                return r;
            }
        }
    }

    private JsonDeserializeResult dummyRoundResult(boolean hasValue, boolean isObj, JsonDeserializerContext c) {
        byte firstByte = c.nextValuableByte();
        if(hasValue) {
            if(dummyIndex == c.option().maxDummyElements()) {
                throw new JsonDeserializerException("too many dummy obj elements : " + dummyIndex);
            }
            dummyIndex++;
        }
        if(firstByte == (isObj ? (byte) '}' : (byte) ']')) {
            c.setObj(null);
            return JsonDeserializeResult.Finish;
        }
        if(hasValue) {
            if(firstByte != (byte) ',') {
                throw new JsonDeserializerException("illegal separator, got : " + firstByte);
            }
        } else {
            c.rewind();
        }
        return JsonDeserializeResult.Continue;
    }

    private JsonDeserializeResult dummySepResult(boolean isObj, JsonDeserializerContext c) {
        byte b = c.nextValuableByte();
        if(b == (isObj ? (byte) '}' : (byte) ']')) {
            c.setObj(null);
            return JsonDeserializeResult.Finish;
        } else if(b == (byte) ',') {
            return JsonDeserializeResult.Continue;
        } else {
            throw new JsonDeserializerException("illegal separator, got : " + b);
        }
    }

    private JsonDeserializeResult processDummyObj(boolean hasValue, JsonDeserializerContext c) {
        JsonDeserializeResult r = dummyRoundResult(hasValue, true, c);
        if (r != JsonDeserializeResult.Continue) {
            return r;
        }
        for( ; ; ) {
            byte firstByte = c.nextValuableByte();
            c.skipString(firstByte);
            firstByte = c.skipColon();
            r = dummyResult(firstByte, c);
            if(r != JsonDeserializeResult.Continue) {
                return r;
            }
            r = dummySepResult(true, c);
            if(r != JsonDeserializeResult.Continue) {
                return r;
            }
        }
    }

    private JsonDeserializeResult processDummyCol(boolean hasValue, JsonDeserializerContext c) {
        JsonDeserializeResult r = dummyRoundResult(hasValue, false, c);
        if (r != JsonDeserializeResult.Continue) {
            return r;
        }
        for( ; ; ) {
            byte firstByte = c.nextValuableByte();
            r = dummyResult(firstByte, c);
            if(r != JsonDeserializeResult.Continue) {
                return r;
            }
            r = dummySepResult(false, c);
            if(r != JsonDeserializeResult.Continue) {
                return r;
            }
        }
    }

    private void deserializePritimiveValue(byte firstByte, int type, JsonDeserializerContext c) {
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

    @FunctionalInterface
    interface JsonDeserializerObjFunc {
        JsonDeserializeResult deserialize(byte firstByte, MarshallInfo inf, JsonDeserializerContext c);
    }
}
