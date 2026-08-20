package io.jingproject.marshalljson;

import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.MarshallInfo;
import io.jingproject.marshall.MarshallUtil;
import io.jingproject.marshall.MarshallWriter;

import java.util.*;

public final class JsonDeserializerNode {
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
            c.setObj(c.deserializeJsonPrimitiveType(b)); // self guarded
            return JsonDeserializeResult.Continue;
        });
        r.put(JsonBoolType.class, (b, _, c) -> {
            JsonDeserializerContext.checkBoolStart(b);
            c.setObj(c.deserializeJsonBoolType(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(JsonNumberType.class, (b, _, c) -> {
            JsonDeserializerContext.checkNumStart(b);
            c.setObj(c.deserializeJsonNumberType(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(JsonStrType.class, (b, _, c) -> {
            JsonDeserializerContext.checkStrStart(b);
            c.setObj(c.deserializeJsonStrType());
            return JsonDeserializeResult.Continue;
        });
        JsonDeserializerObjFunc strArrFunc = (b, _, c) -> {
            JsonDeserializerContext.checkArrayStart(b);
            c.setObj(c.deserializeStringArray());
            return JsonDeserializeResult.Continue;
        };
        r.put(CharSequence[].class, strArrFunc);
        r.put(String[].class, strArrFunc);
        r.put(JsonPrimitiveType[].class, (b, _, c) -> {
            JsonDeserializerContext.checkArrayStart(b);
            c.setObj(c.deserializeJsonPrimitiveTypeArray());
            return JsonDeserializeResult.Continue;
        });
        r.put(JsonBoolType[].class, (b, _, c) -> {
            JsonDeserializerContext.checkArrayStart(b);
            c.setObj(c.deserializeJsonBoolTypeArray());
            return JsonDeserializeResult.Continue;
        });
        r.put(JsonNumberType[].class, (b, _, c) -> {
            JsonDeserializerContext.checkArrayStart(b);
            c.setObj(c.deserializeJsonNumberTypeArray());
            return JsonDeserializeResult.Continue;
        });
        r.put(JsonStrType[].class, (b, _, c) -> {
            JsonDeserializerContext.checkArrayStart(b);
            c.setObj(c.deserializeJsonStrTypeArray());
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
            return JsonDeserializeResult.NewMarshallable;
        };
        Arrays.fill(FUNC_TABLE, defaultFunc);
        // builtin supported wrapper types
        FUNC_TABLE[MarshallUtil.BYTE_WRAPPER_TYPE] = (b, _, c) -> {
            JsonDeserializerContext.checkNumStart(b);
            c.setObj(c.deserializeByte(b));
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.BOOLEAN_WRAPPER_TYPE] = (b, _, c) -> {
            JsonDeserializerContext.checkBoolStart(b);
            c.setObj(c.deserializeBoolean(b));
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.SHORT_WRAPPER_TYPE] = (b, _, c) -> {
            JsonDeserializerContext.checkNumStart(b);
            c.setObj(c.deserializeShort(b));
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.CHAR_WRAPPER_TYPE] = (b, _, c) -> {
            JsonDeserializerContext.checkStrStart(b);
            c.setObj(c.deserializeChar());
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.INT_WRAPPER_TYPE] = (b, _, c) -> {
            JsonDeserializerContext.checkNumStart(b);
            c.setObj(c.deserializeInt(b));
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.LONG_WRAPPER_TYPE] = (b, _, c) -> {
            JsonDeserializerContext.checkNumStart(b);
            c.setObj(c.deserializeLong(b));
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.FLOAT_WRAPPER_TYPE] = (b, _, c) -> {
            JsonDeserializerContext.checkNumStart(b);
            c.setObj(c.deserializeFloat(b));
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.DOUBLE_WRAPPER_TYPE] = (b, _, c) -> {
            JsonDeserializerContext.checkNumStart(b);
            c.setObj(c.deserializeDouble(b));
            return JsonDeserializeResult.Continue;
        };
        // builtin supported primitive array types
        FUNC_TABLE[MarshallUtil.BYTE_ARRAY_TYPE] = (b, _, c) -> {
            JsonDeserializerContext.checkArrayStart(b);
            c.setObj(c.deserializeByteArray());
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.BOOLEAN_ARRAY_TYPE] = (b, _, c) -> {
            JsonDeserializerContext.checkArrayStart(b);
            c.setObj(c.deserializeBooleanArray());
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.SHORT_ARRAY_TYPE] = (b, _, c) -> {
            JsonDeserializerContext.checkArrayStart(b);
            c.setObj(c.deserializeShortArray());
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.CHAR_ARRAY_TYPE] = (b, _, c) -> {
            JsonDeserializerContext.checkArrayStart(b);
            c.setObj(c.deserializeCharArray());
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.INT_ARRAY_TYPE] = (b, _, c) -> {
            JsonDeserializerContext.checkArrayStart(b);
            c.setObj(c.deserializeIntArray());
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.LONG_ARRAY_TYPE] = (b, _, c) -> {
            JsonDeserializerContext.checkArrayStart(b);
            c.setObj(c.deserializeLongArray());
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.FLOAT_ARRAY_TYPE] = (b, _, c) -> {
            JsonDeserializerContext.checkArrayStart(b);
            c.setObj(c.deserializeFloatArray());
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.DOUBLE_ARRAY_TYPE] = (b, _, c) -> {
            JsonDeserializerContext.checkArrayStart(b);
            c.setObj(c.deserializeDoubleArray());
            return JsonDeserializeResult.Continue;
        };
        // builtin supported wrapper array types
        FUNC_TABLE[MarshallUtil.BYTE_WRAPPER_ARRAY_TYPE] = (b, _, c) -> {
            JsonDeserializerContext.checkArrayStart(b);
            c.setObj(c.deserializeByteWrapperArray());
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.BOOLEAN_WRAPPER_ARRAY_TYPE] = (b, _, c) -> {
            JsonDeserializerContext.checkArrayStart(b);
            c.setObj(c.deserializeBooleanWrapperArray());
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.SHORT_WRAPPER_ARRAY_TYPE] = (b, _, c) -> {
            JsonDeserializerContext.checkArrayStart(b);
            c.setObj(c.deserializeShortWrapperArray());
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.CHAR_WRAPPER_ARRAY_TYPE] = (b, _, c) -> {
            JsonDeserializerContext.checkArrayStart(b);
            c.setObj(c.deserializeCharWrapperArray());
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.INT_WRAPPER_ARRAY_TYPE] = (b, _, c) -> {
            JsonDeserializerContext.checkArrayStart(b);
            c.setObj(c.deserializeIntWrapperArray());
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.LONG_WRAPPER_ARRAY_TYPE] = (b, _, c) -> {
            JsonDeserializerContext.checkArrayStart(b);
            c.setObj(c.deserializeLongWrapperArray());
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.FLOAT_WRAPPER_ARRAY_TYPE] = (b, _, c) -> {
            JsonDeserializerContext.checkArrayStart(b);
            c.setObj(c.deserializeFloatWrapperArray());
            return JsonDeserializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.DOUBLE_WRAPPER_ARRAY_TYPE] = (b, _, c) -> {
            JsonDeserializerContext.checkArrayStart(b);
            c.setObj(c.deserializeDoubleWrapperArray());
            return JsonDeserializeResult.Continue;
        };
        // str
        FUNC_TABLE[MarshallUtil.CHARSEQUENCE_TYPE] = FUNC_TABLE[MarshallUtil.STRING_TYPE] = (b, _, c) -> {
            JsonDeserializerContext.checkStrStart(b);
            c.setObj(c.deserializeString());
            return JsonDeserializeResult.Continue;
        };
        // array
        FUNC_TABLE[MarshallUtil.ARRAY_TYPE] = (b, inf, c) -> {
            JsonDeserializerContext.checkArrayStart(b);
            Class<?> arrType = inf.rawType();
            JsonDeserializerObjFunc directDeserializableFunc = directDeserializableFunc(arrType);
            if (directDeserializableFunc != null) {
                return directDeserializableFunc.deserialize(b, inf, c);
            }
            c.setObj(arrType);
            return JsonDeserializeResult.NewArr;
        };
        // enum
        FUNC_TABLE[MarshallUtil.ENUM_TYPE] = (b, inf, c) -> {
            Class<?> rawType = inf.rawType();
            JsonDeserializeFunc customFunc = c.option().customFunc(rawType);
            if (customFunc != null) {
                return customFunc.deserialize(b, c);
            }
            JsonDeserializerContext.checkStrStart(b);
            c.setObj(c.deserializeEnum(rawType));
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
            return JsonDeserializeResult.Trivial;
        };
        // map impl
        FUNC_TABLE[MarshallUtil.MAP_IMPL_TYPE] = (b, inf, c) -> {
            JsonDeserializerContext.checkObjStart(b);
            c.setObj(MarshallUtil.newMapImpl(inf.rawType()));
            c.setType(inf.secondGenericType());
            return JsonDeserializeResult.Trivial;
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

    public void initCol(boolean isArray, Collection<Object> col, JsonDeserializeFunc func, JsonDeserializerContext c) {
        this.type = isArray ? ARR : COL;
        this.firstVal = col;
        this.func = func;
        JsonDeserializerContext.checkArrayStart(c.nextValuableByte(true));
    }

    public void initMap(Map<Object, Object> map, JsonDeserializeFunc func, JsonDeserializerContext c) {
        this.type = MAP;
        this.firstVal = map;
        this.func = func;
        JsonDeserializerContext.checkObjStart(c.nextValuableByte(true));
    }

    public void initDummyObj(JsonDeserializerContext c) {
        this.type = DUMMY_OBJ;
        this.index = 0;
        JsonDeserializerContext.checkObjStart(c.nextValuableByte(true));
    }

    public void initDummyCol(JsonDeserializerContext c) {
        this.type = DUMMY_COL;
        this.index = 0;
        JsonDeserializerContext.checkArrayStart(c.nextValuableByte(true));
    }

    public JsonDeserializeResult process(JsonDeserializerContext c, JsonDeserializeResult last) {
        return switch (type) {
            case OBJ -> processObj(c, last);
            case ARR, COL -> processCol(c, last);
            case MAP -> processMap(c, last);
            case DUMMY_OBJ -> processDummyObj(c, last);
            case DUMMY_COL -> processDummyCol(c, last);
            default -> throw new AssertionError();
        };
    }

    private JsonDeserializeResult processObj(JsonDeserializerContext c, JsonDeserializeResult last) {
        final MarshallFacade fc = (MarshallFacade) firstVal;
        final MarshallWriter wr = (MarshallWriter) secondVal;
        final int contextIndex = index;
        final boolean ensureAllFieldsPresent = c.option().ensureAllFieldsPresent();
        final int maxDummyElements = c.option().maxDummyElements();
        final int total = ensureAllFieldsPresent ? fc.totalElements() : fc.primitiveElements();
        int matchedIndex = c.matchedIndex(contextIndex);
        int dummyIndex = c.dummyIndex(contextIndex);
        if(last == JsonDeserializeResult.Finish) {
            wr.setObject(c.marshallIndex(contextIndex), c.obj());
        }
        byte b = c.nextValuableByte(last != JsonDeserializeResult.Start);
        if(b == (byte) '}') {
            if(matchedIndex != total) {
                throw new JsonDeserializerException("missing field : " + c.filter(contextIndex, fc));
            }
            c.reset(contextIndex);
            c.setObj(fc.construct(wr));
            return JsonDeserializeResult.Finish;
        }
        if(last != JsonDeserializeResult.Start && b != (byte) ',') {
            throw new JsonDeserializerException("illegal separator, got : " + b);
        }
        for( ; ; ) {
            b = c.nextValuableByte(true);
            JsonDeserializerContext.checkStrStart(b);
            MarshallInfo inf = c.deserializeMarshallInfo(fc);
            if(inf == null) {
                if(++dummyIndex > maxDummyElements) {
                    throw new JsonDeserializerException("exceeded max dummy elements limit : " + dummyIndex);
                }
                b = c.skipColon();
                if(c.skipAnyValue(b)) {
                    b = c.nextValuableByte(true);
                    if(b == (byte) '}') {
                        if(matchedIndex != total) {
                            throw new JsonDeserializerException("missing field : " + c.filter(contextIndex, fc));
                        }
                        c.reset(contextIndex);
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
                c.deserializeNull();
                if(ensureAllFieldsPresent) {
                    matchedIndex++;
                }
            } else {
                JsonDeserializeResult r = FUNC_TABLE[type].deserialize(b, inf, c);
                if (ensureAllFieldsPresent) {
                    matchedIndex++;
                }
                if(r.isNested()) {
                    c.store(contextIndex, marshallIndex, dummyIndex, matchedIndex);
                    return r;
                }
            }
            b = c.nextValuableByte(true);
            if(b == (byte) '}') {
                if(matchedIndex != total) {
                    throw new JsonDeserializerException("missing field : " + c.filter(contextIndex, fc));
                }
                c.reset(contextIndex);
                c.setObj(fc.construct(wr));
                return JsonDeserializeResult.Finish;
            } else if(b != (byte) ',') {
                throw new JsonDeserializerException("illegal separator, got : " + b);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private JsonDeserializeResult processCol(JsonDeserializerContext c, JsonDeserializeResult last) {
        final Collection<Object> col = (Collection<Object>) firstVal;
        final int maxArrayElements = c.option().maxArrayElements();
        if(last == JsonDeserializeResult.Finish) {
            if(col.size() == maxArrayElements) {
                throw new JsonDeserializerException("too many array elements : " + maxArrayElements);
            }
            col.add(c.obj());
        }
        byte b = c.nextValuableByte(last != JsonDeserializeResult.Start);
        if(b == (byte) ']') {
            c.setObj(type == ARR ? col.toArray() : col);
            return JsonDeserializeResult.Finish;
        }
        if(last != JsonDeserializeResult.Start && b != (byte) ',') {
            throw new JsonDeserializerException("illegal separator, got : " + b);
        }
        for( ; ; ) {
            b = c.nextValuableByte(true);
                JsonDeserializeResult r = func.deserialize(b, c);
            if (r == JsonDeserializeResult.Continue) {
                if(col.size() == maxArrayElements) {
                    throw new JsonDeserializerException("too many array elements : " + maxArrayElements);
                }
                col.add(c.obj());
                b = c.nextValuableByte(last != JsonDeserializeResult.Start);
                if(b == (byte) ']') {
                    c.setObj(type == ARR ? col.toArray() : col);
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

    @SuppressWarnings("unchecked")
    private JsonDeserializeResult processMap(JsonDeserializerContext c, JsonDeserializeResult last) {
        final Map<Object, Object> map = (Map<Object, Object>) firstVal;
        final int maxMapElements = c.option().maxMapElements();
        if(last == JsonDeserializeResult.Finish) {
            if(map.size() == maxMapElements) {
                throw new JsonDeserializerException("too many map elements : " + maxMapElements);
            }
            map.put(secondVal, c.obj());
        }
        byte b = c.nextValuableByte(last != JsonDeserializeResult.Start);
        if(b == (byte) '}') {
            c.setObj(map);
            return JsonDeserializeResult.Finish;
        }
        if(last != JsonDeserializeResult.Start && b != (byte) ',') {
            throw new JsonDeserializerException("illegal separator, got : " + b);
        }
        for( ; ; ) {
            b = c.nextValuableByte(true);
            if (b != (byte) '"') {
                throw new JsonDeserializerException("illegal key start, got : " + b);
            }
            String k = c.deserializeString();
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
                b = c.nextValuableByte(last != JsonDeserializeResult.Start);
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

    private JsonDeserializeResult processDummyObj(JsonDeserializerContext c, JsonDeserializeResult last) {
        final int maxDummyElements = c.option().maxDummyElements();
        int i = index;
        if(last == JsonDeserializeResult.Trivial) {
            if(i++ == maxDummyElements) {
                throw new JsonDeserializerException("too many dummy elements : " + maxDummyElements);
            }
        }
        byte b = c.nextValuableByte(last != JsonDeserializeResult.Start);
        if(b == (byte) '}') {
            return JsonDeserializeResult.Trivial;
        }
        if(last != JsonDeserializeResult.Start && b != (byte) ',') {
            throw new JsonDeserializerException("illegal separator, got : " + b);
        }
        while (i < maxDummyElements) {
            b = c.nextValuableByte(true);
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

    private JsonDeserializeResult processDummyCol(JsonDeserializerContext c, JsonDeserializeResult last) {
        final int maxDummyArrayElements = c.option().maxDummyArrayElements();
        int i = index;
        if(last == JsonDeserializeResult.Trivial) {
            if(i++ == maxDummyArrayElements) {
                throw new JsonDeserializerException("too many dummy elements : " + maxDummyArrayElements);
            }
        }
        byte b = c.nextValuableByte(last != JsonDeserializeResult.Start);
        if(b == (byte) ']') {
            return JsonDeserializeResult.Trivial;
        }
        if(last != JsonDeserializeResult.Start && b != (byte) ',') {
            throw new JsonDeserializerException("illegal separator, got : " + b);
        }
        while (i < maxDummyArrayElements) {
            b = c.nextValuableByte(true);
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

    private static void deserializePritimiveValue(byte b, MarshallWriter wr, int marshallIndex,
                                                  int type, JsonDeserializerContext c) {
        switch (type) {
            case MarshallUtil.BYTE_TYPE -> {
                JsonDeserializerContext.checkNumStart(b);
                wr.setByte(marshallIndex, c.deserializeByte(b));
            }
            case MarshallUtil.BOOLEAN_TYPE -> {
                JsonDeserializerContext.checkBoolStart(b);
                wr.setBoolean(marshallIndex, c.deserializeBoolean(b));
            }
            case MarshallUtil.SHORT_TYPE -> {
                JsonDeserializerContext.checkNumStart(b);
                wr.setShort(marshallIndex, c.deserializeShort(b));
            }
            case MarshallUtil.CHAR_TYPE -> {
                JsonDeserializerContext.checkStrStart(b);
                wr.setChar(marshallIndex, c.deserializeChar());
            }
            case MarshallUtil.INT_TYPE -> {
                JsonDeserializerContext.checkNumStart(b);
                wr.setInt(marshallIndex, c.deserializeInt(b));
            }
            case MarshallUtil.LONG_TYPE -> {
                JsonDeserializerContext.checkNumStart(b);
                wr.setLong(marshallIndex, c.deserializeLong(b));
            }
            case MarshallUtil.FLOAT_TYPE -> {
                JsonDeserializerContext.checkNumStart(b);
                wr.setFloat(marshallIndex, c.deserializeFloat(b));
            }
            case MarshallUtil.DOUBLE_TYPE -> {
                JsonDeserializerContext.checkNumStart(b);
                wr.setDouble(marshallIndex, c.deserializeDouble(b));
            }
            default -> throw new AssertionError();
        }
    }

    @FunctionalInterface
    interface JsonDeserializerObjFunc {
        JsonDeserializeResult deserialize(byte firstByte, MarshallInfo inf, JsonDeserializerContext c);
    }
}
