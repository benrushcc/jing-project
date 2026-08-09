package io.jingproject.marshalljson;

import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.MarshallInfo;
import io.jingproject.marshall.MarshallUtil;
import io.jingproject.marshall.MarshallWriter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class JsonDeserializerObjNode extends JsonDeserializerNode {
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
        FUNC_TABLE[MarshallUtil.CHARSEQUENCE_TYPE] = FUNC_TABLE[MarshallUtil.STRING_TYPE] = (b, _, c) -> {
            c.setObj(c.deserializeString(b));
            return JsonDeserializeResult.Continue;
        };
        // array
        FUNC_TABLE[MarshallUtil.ARRAY_TYPE] = (b, inf, c) -> {
            JsonDeserializerObjFunc directDeserializableFunc = directDeserializableFunc(inf.rawType());
            if (directDeserializableFunc != null) {
                return directDeserializableFunc.deserialize(b, inf, c);
            }
            // TODO
            return JsonDeserializeResult.Trivial;
        };
        // enum
        FUNC_TABLE[MarshallUtil.ENUM_TYPE] = (b, inf, c) -> {
            // TODO
            return JsonDeserializeResult.Trivial;
        };
        // collection interface
        FUNC_TABLE[MarshallUtil.COLLECTION_INTERFACE_TYPE] = (b, inf, c) -> {
            // TODO
            return JsonDeserializeResult.Trivial;
        };
        // collection impl
        FUNC_TABLE[MarshallUtil.COLLECTION_IMPL_TYPE] = (b, inf, c) -> {
            // TODO
            return JsonDeserializeResult.Trivial;
        };
        // map interface
        FUNC_TABLE[MarshallUtil.MAP_INTERFACE_TYPE] = (b, inf, c) -> {
            // TODO
            return JsonDeserializeResult.Trivial;
        };
        // map impl
        FUNC_TABLE[MarshallUtil.MAP_IMPL_TYPE] = (b, inf, c) -> {
            // TODO
            return JsonDeserializeResult.Trivial;
        };
    }

    private MarshallFacade marshallFacade;
    private MarshallWriter marshallWriter;
    private int bitmapIndex;
    private int marshallIndex;
    private int dummyIndex;
    private int count;

    private static JsonDeserializerObjFunc directDeserializableFunc(Class<?> clazz) {
        return DIRECT_DESERIALIZABLE_FUNC_MAP.get(clazz);
    }

    private static boolean finishDeserializing(MarshallFacade fc, MarshallWriter wr, int cnt, int total,
                                               int bIndex, boolean allPresent, JsonDeserializerContext c) {
        byte firstByte = c.nextFirstValuableByte();
        if (firstByte == (byte) '}') {
            // check if we have deserialized all the target fields
            if (cnt != total) {
                throw new JsonDeserializerException("missing field : " + c.filter(bIndex, total, allPresent, fc));
            }
            // restore bitmap index, and construct our deserialized result
            c.rewind(bIndex);
            c.setObj(fc.construct(wr));
            return true;
        } else if (firstByte == (byte) ',') {
            return false;
        } else {
            throw new JsonDeserializerException("illegal separator, got : " + firstByte);
        }
    }

    private static void deserializePritimiveValue(byte firstByte, MarshallWriter wr, int mIndex,
                                                  int type, JsonDeserializerContext c) {
        switch (type) {
            case MarshallUtil.BYTE_TYPE -> wr.setByte(mIndex, c.deserializeByte(firstByte));
            case MarshallUtil.BOOLEAN_TYPE -> wr.setBoolean(mIndex, c.deserializeBoolean(firstByte));
            case MarshallUtil.SHORT_TYPE -> wr.setShort(mIndex, c.deserializeShort(firstByte));
            case MarshallUtil.CHAR_TYPE -> wr.setChar(mIndex, c.deserializeChar(firstByte));
            case MarshallUtil.INT_TYPE -> wr.setInt(mIndex, c.deserializeInt(firstByte));
            case MarshallUtil.LONG_TYPE -> wr.setLong(mIndex, c.deserializeLong(firstByte));
            case MarshallUtil.FLOAT_TYPE -> wr.setFloat(mIndex, c.deserializeFloat(firstByte));
            case MarshallUtil.DOUBLE_TYPE -> wr.setDouble(mIndex, c.deserializeDouble(firstByte));
            default -> throw new AssertionError();
        }
    }

    public void init(MarshallFacade fc, JsonDeserializerContext context) {
        this.marshallFacade = fc;
        this.marshallWriter = fc.newWriter();
        this.bitmapIndex = context.bitmapIndex(fc.totalElements());
        this.marshallIndex = -1;
        this.dummyIndex = 0;
        this.count = 0;
    }

    @Override
    protected JsonDeserializeResult process(JsonDeserializerContext c, JsonDeserializeResult last) {
        final JsonDeserializerOption op = c.option();
        final boolean allPresent = op.ensureAllFieldsPresent();
        final int maxDummyElements = op.maxDummyElements();
        final MarshallFacade fc = marshallFacade;
        final int total = allPresent ? fc.totalElements() : fc.primitiveElements();
        final MarshallWriter wr = marshallWriter;
        final int bIndex = bitmapIndex;
        int di = dummyIndex;
        int cnt = count;
        if (last == JsonDeserializeResult.Finish) {
            wr.setObject(marshallIndex, c.obj());
        }
        if (last != JsonDeserializeResult.Start && finishDeserializing(fc, wr, cnt, total, bIndex, allPresent, c)) {
            return JsonDeserializeResult.Finish;
        }
        for (int idx = cnt; idx < total; ) {
            byte firstByte = c.nextFirstValuableByte();
            if (firstByte != (byte) '"') {
                throw new JsonDeserializerException("illegal key start, got : " + firstByte);
            }
            c.parseStringIntoBytes(firstByte);
            MarshallInfo inf = c.asMarshallInfo(fc);
            if (inf == null) {
                // we need to skip the next value
                if (++di > maxDummyElements) {
                    throw new JsonDeserializerException("exceeded max dummy elements limit : " + maxDummyElements);
                }
                firstByte = c.skipColon();
                if (c.skipAnyValue(firstByte)) {
                    if (finishDeserializing(fc, wr, cnt, total, bIndex, allPresent, c)) {
                        return JsonDeserializeResult.Finish;
                    }
                    continue;
                } else if (firstByte == (byte) '{') {
                    return JsonDeserializeResult.NewDummyObj;
                } else if (firstByte == (byte) '[') {
                    return JsonDeserializeResult.NewDummyArr;
                } else {
                    throw new JsonDeserializerException("illegal value start, got : " + firstByte);
                }
            }
            idx++;
            final int mIndex = inf.index();
            if (c.assign(bIndex, mIndex)) {
                throw new JsonDeserializerException("duplicate key : " + inf.mappedName());
            }
            firstByte = c.skipColon();
            final int type = inf.type() & MarshallUtil.TYPE_MASK;
            if (type <= MarshallUtil.DOUBLE_TYPE) {
                deserializePritimiveValue(firstByte, wr, mIndex, type, c);
                cnt++;
            } else if (firstByte == (byte) 'n') {
                c.deserializeNull(firstByte);
                if (allPresent) {
                    cnt++;
                }
            } else {
                JsonDeserializeResult r = FUNC_TABLE[type].deserialize(firstByte, inf, c);
                if (allPresent) {
                    cnt++;
                }
                if (r.isNested()) {
                    // save the scene and return
                    marshallIndex = mIndex;
                    dummyIndex = di;
                    count = cnt;
                    return r;
                }
            }
            if (finishDeserializing(fc, wr, cnt, total, bIndex, allPresent, c)) {
                return JsonDeserializeResult.Finish;
            }
        }
        throw new JsonDeserializerException("exceeded max dummy elements limit");
    }

    @FunctionalInterface
    interface JsonDeserializerObjFunc {
        JsonDeserializeResult deserialize(byte firstByte, MarshallInfo inf, JsonDeserializerContext c);
    }
}
