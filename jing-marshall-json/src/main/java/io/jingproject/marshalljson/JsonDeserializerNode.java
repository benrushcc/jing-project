package io.jingproject.marshalljson;

import io.jingproject.common.ReadBuffer;
import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.MarshallInfo;
import io.jingproject.marshall.MarshallUtil;
import io.jingproject.marshall.MarshallWriter;

import java.lang.reflect.Array;
import java.util.*;

public final class JsonDeserializerNode {
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
    private Object firstVal;
    private Object secondVal;
    private int index;
    private JsonDeserializeFunc func;

    public void initObj(MarshallFacade fc, JsonDeserializerContext c) {
        this.type = OBJ;
        this.firstVal = fc;
        this.secondVal = fc.newWriter();
        this.index = c.alloc(fc.totalElements());
    }

    public void initArr(Class<?> componentType, JsonDeserializeFunc func) {
        this.type = ARR;
        this.firstVal = componentType;
        this.func = func;
        this.index = 0;
    }

    public void initCol(Collection<?> col, JsonDeserializeFunc func) {
        this.type = COL;
        this.firstVal = col;
        this.func = func;
    }

    public void initMap(Map<?, ?> map, JsonDeserializeFunc func) {
        this.type = MAP;
        this.firstVal = map;
        this.func = func;
    }

    public void initDummyObj() {
        this.type = DUMMY_OBJ;
        this.index = 0;
    }

    public void initDummyCol() {
        this.type = DUMMY_COL;
        this.index = 0;
    }

    public JsonDeserializeResult process(JsonDeserializerContext c, boolean hasValue) {
        return switch (type) {
            case OBJ -> processObj(c, hasValue);
            case ARR -> processArr(c, hasValue);
            case COL -> processCol(c, hasValue);
            case MAP -> processMap(c, hasValue);
            case DUMMY_OBJ -> processDummyObj(c, hasValue);
            case DUMMY_COL -> processDummyCol(c, hasValue);
            default -> throw new AssertionError();
        };
    }

    private JsonDeserializeResult processObj(JsonDeserializerContext c, boolean hasValue) {
        final MarshallFacade fc = (MarshallFacade) firstVal;
        final MarshallWriter wr = (MarshallWriter) secondVal;
        final int contextIndex = index;
        final boolean ensureAllFieldsPresent = c.option().ensureAllFieldsPresent();
        final int maxDummyElements = c.option().maxDummyElements();
        final int total = ensureAllFieldsPresent ? fc.totalElements() : fc.primitiveElements();
        int matchedIndex = c.matchedIndex(contextIndex);
        int dummyIndex = c.dummyIndex(contextIndex);
        if(hasValue) {
            Object lastValue = c.obj();
            if(lastValue != null) {
                wr.setObject(c.marshallIndex(contextIndex), lastValue);
            }
        }
        byte b = c.nextValuableByte();
        if(b == (byte) '}') {
            if(matchedIndex != total) {
                throw new JsonDeserializerException("missing field : " + c.filter(contextIndex, fc));
            }
            c.rewind(contextIndex);
            c.setObj(fc.construct(wr));
            return JsonDeserializeResult.Finish;
        }
        alignSep(hasValue, b, c);
        for( ; ; ) {
            b = c.nextValuableByte();
            MarshallInfo inf = c.deserializeMarshallInfo(fc, b);
            if(inf == null) {
                if(++dummyIndex > maxDummyElements) {
                    throw new JsonDeserializerException("exceeded max dummy elements limit : " + dummyIndex);
                }
                b = c.skipColon();
                if(c.skipAnyValue(b)) {
                    b = c.nextValuableByte();
                    if(b == (byte) '}') {
                        if(matchedIndex != total) {
                            throw new JsonDeserializerException("missing field : " + c.filter(contextIndex, fc));
                        }
                        c.rewind(contextIndex);
                        c.setObj(fc.construct(wr));
                        return JsonDeserializeResult.Finish;
                    } else if(b == (byte) ',') {
                        continue ;
                    } else {
                        throw new JsonDeserializerException("illegal separator, got : " + b);
                    }
                } else if (b == (byte) '{') {
                    c.store(contextIndex, 0, dummyIndex, matchedIndex);
                    return JsonDeserializeResult.NewDummyObj;
                } else if (b == (byte) '[') {
                    c.store(contextIndex, 0, dummyIndex, matchedIndex);
                    return JsonDeserializeResult.NewDummyCol;
                } else {
                    throw new JsonDeserializerException("illegal value start, got : " + b);
                }
            }
            final int marshallIndex = inf.index();
            if(c.assign(contextIndex, marshallIndex)) {
                throw new JsonDeserializerException("duplicate key : " + inf.mappedName());
            }
            b = c.skipColon();
            final int type = inf.type() & MarshallUtil.TYPE_MASK;
            if(type <= MarshallUtil.DOUBLE_TYPE) {
                deserializePritimiveValue(b, wr, marshallIndex, type, c);
                matchedIndex++;
            } else if(b == (byte) 'n') {
                c.deserializeFollowingNull();
                if(ensureAllFieldsPresent) {
                    matchedIndex++;
                }
            } else {
                JsonDeserializeResult r = FUNC_TABLE[type].deserialize(b, inf, c);
                if (ensureAllFieldsPresent) {
                    matchedIndex++;
                }
                if(r == JsonDeserializeResult.Continue) {
                    wr.setObject(inf.index(), c.obj());
                } else {
                    c.store(contextIndex, marshallIndex, dummyIndex, matchedIndex);
                    return r;
                }
            }
            b = c.nextValuableByte();
            if(b == (byte) '}') {
                if(matchedIndex != total) {
                    throw new JsonDeserializerException("missing field : " + c.filter(contextIndex, fc));
                }
                c.rewind(contextIndex);
                c.setObj(fc.construct(wr));
                return JsonDeserializeResult.Finish;
            } else if(b != (byte) ',') {
                throw new JsonDeserializerException("illegal separator, got : " + b);
            }
        }
    }

    private JsonDeserializeResult processArr(JsonDeserializerContext c, boolean hasValue) {
        final Class<?> componentType = (Class<?>) firstVal;
        Object[] buf = (Object[]) secondVal;
        if(buf == null) {
            buf = new Object[ARR_INITIAL_SIZE];
        }
        int i = index;
        final int maxArrayElements = c.option().maxArrayElements();
        if(hasValue) {
            Object lastValue = c.obj();
            if(lastValue != null) {
                if(i == maxArrayElements) {
                    throw new JsonDeserializerException("too many array elements : " + maxArrayElements);
                }
                if(i == buf.length) {
                    buf = Arrays.copyOf(buf, Math.addExact(buf.length, buf.length));
                }
                buf[i++] = lastValue;
            }
        }
        byte b = c.nextValuableByte();
        if(b == (byte) ']') {
            Object arr = Array.newInstance(componentType, i);
            //noinspection SuspiciousSystemArraycopy
            System.arraycopy(buf, 0, arr, 0, i);
            c.setObj(arr);
            return JsonDeserializeResult.Finish;
        }
        alignSep(hasValue, b, c);
        for( ; ; ) {
            b = c.nextValuableByte();
            JsonDeserializeResult r = func.deserialize(b, c);
            if (r == JsonDeserializeResult.Continue) {
                if(i == maxArrayElements) {
                    throw new JsonDeserializerException("too many array elements : " + maxArrayElements);
                }
                if(i == buf.length) {
                    buf = Arrays.copyOf(buf, Math.addExact(buf.length, buf.length));
                }
                buf[i++] = c.obj();
                b = c.nextValuableByte();
                if(b == (byte) ']') {
                    Object arr = Array.newInstance(componentType, i);
                    //noinspection SuspiciousSystemArraycopy
                    System.arraycopy(buf, 0, arr, 0, i);
                    c.setObj(arr);
                    return JsonDeserializeResult.Finish;
                } else if(b == (byte) ',') {
                    continue ;
                } else {
                    throw new JsonDeserializerException("illegal separator, got : " + b);
                }
            }
            secondVal = buf;
            index = i;
            return r;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private JsonDeserializeResult processCol(JsonDeserializerContext c, boolean hasValue) {
        final Collection col = (Collection) firstVal;
        final int maxArrayElements = c.option().maxArrayElements();
        if(hasValue) {
            Object lastValue = c.obj();
            if(lastValue != null) {
                if(col.size() == maxArrayElements) {
                    throw new JsonDeserializerException("too many array elements : " + maxArrayElements);
                }
                col.add(lastValue);
            }
        }
        byte b = c.nextValuableByte();
        if(b == (byte) ']') {
            c.setObj(col);
            return JsonDeserializeResult.Finish;
        }
        alignSep(hasValue, b, c);
        for( ; ; ) {
            b = c.nextValuableByte();
            JsonDeserializeResult r = func.deserialize(b, c);
            if (r == JsonDeserializeResult.Continue) {
                if(col.size() == maxArrayElements) {
                    throw new JsonDeserializerException("too many array elements : " + maxArrayElements);
                }
                col.add(c.obj());
                b = c.nextValuableByte();
                if(b == (byte) ']') {
                    c.setObj(col);
                    return JsonDeserializeResult.Finish;
                } else if(b == (byte) ',') {
                    continue ;
                } else {
                    throw new JsonDeserializerException("illegal separator, got : " + b);
                }
            }
            return r;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private JsonDeserializeResult processMap(JsonDeserializerContext c, boolean hasValue) {
        final Map map = (Map) firstVal;
        final int maxMapElements = c.option().maxMapElements();
        if(hasValue) {
            Object lastValue = c.obj();
            if(lastValue != null) {
                if(map.size() == maxMapElements) {
                    throw new JsonDeserializerException("too many map elements : " + maxMapElements);
                }
                map.put(secondVal, lastValue);
            }
        }
        byte b = c.nextValuableByte();
        if(b == (byte) '}') {
            c.setObj(map);
            return JsonDeserializeResult.Finish;
        }
        alignSep(hasValue, b, c);
        for( ; ; ) {
            String k = c.deserializeString(c.nextValuableByte());
            b = c.skipColon();
            if(b == (byte) 'n') {
                throw new JsonDeserializerException("map value can't be null");
            }
            JsonDeserializeResult r = func.deserialize(b, c);
            if (r == JsonDeserializeResult.Continue) {
                if(map.size() == maxMapElements) {
                    throw new JsonDeserializerException("too many map elements : " + maxMapElements);
                }
                map.put(k, c.obj());
                b = c.nextValuableByte();
                if(b == (byte) '}') {
                    c.setObj(map);
                    return JsonDeserializeResult.Finish;
                } else if(b == (byte) ',') {
                    continue ;
                } else {
                    throw new JsonDeserializerException("illegal separator, got : " + b);
                }
            }
            secondVal = k;
            return r;
        }
    }

    private JsonDeserializeResult processDummyObj(JsonDeserializerContext c, boolean hasValue) {
        final int maxDummyElements = c.option().maxDummyElements();
        int i = index;
        if(hasValue) {
            if(i++ == maxDummyElements) {
                throw new JsonDeserializerException("too many dummy elements : " + maxDummyElements);
            }
        }
        byte b = c.nextValuableByte();
        if(b == (byte) '}') {
            c.setObj(null);
            return JsonDeserializeResult.Finish;
        }
        alignSep(hasValue, b, c);
        while (i < maxDummyElements) {
            b = c.nextValuableByte();
            if (b != (byte) '"') {
                throw new JsonDeserializerException("illegal key start, got : " + b);
            }
            c.skipStringValue();
            b = c.skipColon();
            if (c.skipAnyValue(b)) {
                i++;
            } else if (b == (byte) '{') {
                index = i + 1;
                return JsonDeserializeResult.NewDummyObj;
            } else if (b == (byte) '[') {
                index = i + 1;
                return JsonDeserializeResult.NewDummyCol;
            } else {
                throw new JsonDeserializerException("illegal value start, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many elements in dummy object");
    }

    private JsonDeserializeResult processDummyCol(JsonDeserializerContext c, boolean hasValue) {
        final int maxDummyArrayElements = c.option().maxDummyArrayElements();
        int i = index;
        if(hasValue) {
            if(i++ == maxDummyArrayElements) {
                throw new JsonDeserializerException("too many dummy elements : " + maxDummyArrayElements);
            }
        }
        byte b = c.nextValuableByte();
        if(b == (byte) ']') {
            c.setObj(null);
            return JsonDeserializeResult.Finish;
        }
        alignSep(hasValue, b, c);
        while (i < maxDummyArrayElements) {
            b = c.nextValuableByte();
            if (c.skipAnyValue(b)) {
                i++;
            } else if (b == (byte) '{') {
                index = i + 1;
                return JsonDeserializeResult.NewDummyObj;
            } else if (b == (byte) '[') {
                index = i + 1;
                return JsonDeserializeResult.NewDummyCol;
            } else {
                throw new JsonDeserializerException("illegal value start, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many elements in dummy array");
    }

    private static void alignSep(boolean hasValue, byte b, JsonDeserializerContext c) {
        if(hasValue) {
            if(b != (byte) ',') {
                throw new JsonDeserializerException("illegal separator, got : " + b);
            }
        } else {
            ReadBuffer readBuffer = c.readBuffer();
            readBuffer.setPosition(readBuffer.intPosition() - 1);
        }
    }

    private static void deserializePritimiveValue(byte b, MarshallWriter wr, int marshallIndex,
                                                  int type, JsonDeserializerContext c) {
        switch (type) {
            case MarshallUtil.BYTE_TYPE -> wr.setByte(marshallIndex, c.deserializeByte(b));
            case MarshallUtil.BOOLEAN_TYPE -> wr.setBoolean(marshallIndex, c.deserializeBoolean(b));
            case MarshallUtil.SHORT_TYPE -> wr.setShort(marshallIndex, c.deserializeShort(b));
            case MarshallUtil.CHAR_TYPE -> wr.setChar(marshallIndex, c.deserializeChar(b));
            case MarshallUtil.INT_TYPE -> wr.setInt(marshallIndex, c.deserializeInt(b));
            case MarshallUtil.LONG_TYPE -> wr.setLong(marshallIndex, c.deserializeLong(b));
            case MarshallUtil.FLOAT_TYPE -> wr.setFloat(marshallIndex, c.deserializeFloat(b));
            case MarshallUtil.DOUBLE_TYPE -> wr.setDouble(marshallIndex, c.deserializeDouble(b));
            default -> throw new AssertionError();
        }
    }

    @FunctionalInterface
    interface JsonDeserializerObjFunc {
        JsonDeserializeResult deserialize(byte firstByte, MarshallInfo inf, JsonDeserializerContext c);
    }
}
