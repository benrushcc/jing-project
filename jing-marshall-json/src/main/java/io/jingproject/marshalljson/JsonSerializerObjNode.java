package io.jingproject.marshalljson;

import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.MarshallInfo;
import io.jingproject.marshall.MarshallReader;
import io.jingproject.marshall.MarshallUtil;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class JsonSerializerObjNode extends JsonSerializerNode {
    private static final Map<Class<?>, JsonSerializerObjFunc> DIRECT_SERIALIZABLE_FUNC_MAP;
    private static final JsonSerializerObjFunc[] FUNC_TABLE;
    private MarshallFacade fc;
    private MarshallReader reader;
    
    @FunctionalInterface
    interface JsonSerializerObjFunc {
        JsonSerializeResult serialize(Object fieldValue, MarshallInfo marshallInfo, int indent, JsonSerializerContext context);
    }

    static {
        Map<Class<?>, JsonSerializerObjFunc> r = new HashMap<>();
        r.put(JsonPrimitiveType.class, (o, _, _, c) -> {
            c.serializeJsonPrimitiveType((JsonPrimitiveType) o);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonBoolType.class, (o, _, _, c) -> {
            c.serializeJsonBoolType((JsonBoolType) o);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonNumberType.class, (o, _, _, c) -> {
            c.serializeJsonNumberType((JsonNumberType) o);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonStrType.class, (o, _, _, c) -> {
            c.serializeJsonStrType((JsonStrType) o);
            return JsonSerializeResult.Continue;
        });
        r.put(CharSequence[].class, (o, _, i, c) -> {
            c.serializeEscapedCharSequenceArray((CharSequence[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(String[].class, (o, _, i, c) -> {
            c.serializeEscapedStringArray((String[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonPrimitiveType[].class, (o, _, i, c) -> {
            c.serializeJsonPrimitiveTypeArray((JsonPrimitiveType[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonBoolType[].class, (o, _, i, c) -> {
            c.serializeJsonBoolTypeArray((JsonBoolType[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonNumberType[].class, (o, _, i, c) -> {
            c.serializeJsonNumberTypeArray((JsonNumberType[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonStrType[].class, (o, _, i, c) -> {
            c.serializeJsonStrTypeArray((JsonStrType[]) o, i);
            return JsonSerializeResult.Continue;
        });
        DIRECT_SERIALIZABLE_FUNC_MAP = Map.copyOf(r);
    }

    private static JsonSerializerObjFunc directSerializableFunc(Class<?> rawType) {
        return DIRECT_SERIALIZABLE_FUNC_MAP.get(rawType);
    }
    
    static {
        FUNC_TABLE = new JsonSerializerObjFunc[MarshallUtil.TYPE_SIZE];
        JsonSerializerObjFunc defaultFunc = (o, inf, i, c) -> {
            // exclude generic types
            if(inf.firstGenericType() != null || inf.secondGenericType() != null) {
                throw new JsonSerializerException("unsupported generic type : " + inf);
            }
            // matching direct serializable value
            Class<?> rawType = inf.rawType();
            JsonSerializerObjFunc directSerializableFunc = directSerializableFunc(rawType);
            if(directSerializableFunc != null) {
                return directSerializableFunc.serialize(o, inf, i, c);
            }
            // check if current type could be override by option
            JsonSerializeFunc customFunc = c.option().customFunc(rawType);
            if(customFunc != null) {
                customFunc.serialize(o, i, c);
                return JsonSerializeResult.Continue;
            }
            // assuming marshallable
            c.setObj(o);
            return JsonSerializeResult.NewMarshallable;
        };
        Arrays.fill(FUNC_TABLE, defaultFunc);
        // builtin supported wrapper types
        FUNC_TABLE[MarshallUtil.BYTE_WRAPPER_TYPE] = (o, _, _, c) -> {
            c.serializeByte((byte) o);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.BOOLEAN_WRAPPER_TYPE] = (o, _, _, c) -> {
            c.serializeBoolean((boolean) o);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.SHORT_WRAPPER_TYPE] = (o, _, _, c) -> {
            c.serializeShort((short) o);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.CHAR_WRAPPER_TYPE] = (o, _, _, c) -> {
            c.serializeChar((char) o);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.INT_WRAPPER_TYPE] = (o, _, _, c) -> {
            c.serializeInt((int) o);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.LONG_WRAPPER_TYPE] = (o, _, _, c) -> {
            c.serializeLong((long) o);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.FLOAT_WRAPPER_TYPE] = (o, _, _, c) -> {
            c.serializeFloat((float) o);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.DOUBLE_WRAPPER_TYPE] = (o, _, _, c) -> {
            c.serializeDouble((double) o);
            return JsonSerializeResult.Continue;
        };
        // builtin supported primitive array types
        FUNC_TABLE[MarshallUtil.BYTE_ARRAY_TYPE] = (o, _, i, c) -> {
            c.serializeByteArray((byte[]) o, i);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.BOOLEAN_ARRAY_TYPE] = (o, _, i, c) -> {
            c.serializeBooleanArray((boolean[]) o, i);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.SHORT_ARRAY_TYPE] = (o, _, i, c) -> {
            c.serializeShortArray((short[]) o, i);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.CHAR_ARRAY_TYPE] = (o, _, i, c) -> {
            c.serializeCharArray((char[]) o, i);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.INT_ARRAY_TYPE] = (o, _, i, c) -> {
            c.serializeIntArray((int[]) o, i);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.LONG_ARRAY_TYPE] = (o, _, i, c) -> {
            c.serializeLongArray((long[]) o, i);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.FLOAT_ARRAY_TYPE] = (o, _, i, c) -> {
            c.serializeFloatArray((float[]) o, i);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.DOUBLE_ARRAY_TYPE] = (o, _, i, c) -> {
            c.serializeDoubleArray((double[]) o, i);
            return JsonSerializeResult.Continue;
        };
        // builtin supported wrapper array types
        FUNC_TABLE[MarshallUtil.BYTE_WRAPPER_ARRAY_TYPE] = (o, _, i, c) -> {
            c.serializeByteWrapperArray((Byte[]) o, i);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.BOOLEAN_WRAPPER_ARRAY_TYPE] = (o, _, i, c) -> {
            c.serializeBooleanWrapperArray((Boolean[]) o, i);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.SHORT_WRAPPER_ARRAY_TYPE] = (o, _, i, c) -> {
            c.serializeShortWrapperArray((Short[]) o, i);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.CHAR_WRAPPER_ARRAY_TYPE] = (o, _, i, c) -> {
            c.serializeCharWrapperArray((Character[]) o, i);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.INT_WRAPPER_ARRAY_TYPE] = (o, _, i, c) -> {
            c.serializeIntWrapperArray((Integer[]) o, i);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.LONG_WRAPPER_ARRAY_TYPE] = (o, _, i, c) -> {
            c.serializeLongWrapperArray((Long[]) o, i);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.FLOAT_WRAPPER_ARRAY_TYPE] = (o, _, i, c) -> {
            c.serializeFloatWrapperArray((Float[]) o, i);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.DOUBLE_WRAPPER_ARRAY_TYPE] = (o, _, i, c) -> {
            c.serializeDoubleWrapperArray((Double[]) o, i);
            return JsonSerializeResult.Continue;
        };
        // charsequence and string
        FUNC_TABLE[MarshallUtil.CHARSEQUENCE_TYPE] = (o, _, _, c) -> {
            c.serializeEscapedCharSequence((CharSequence) o);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.STRING_TYPE] = (o, _, _, c) -> {
            c.serializeEscapedString((String) o);
            return JsonSerializeResult.Continue;
        };
        // array
        FUNC_TABLE[MarshallUtil.ARRAY_TYPE] = (o, _, _, c) -> {
            c.setArr((Object[]) o);
            return JsonSerializeResult.NewArray;
        };
        // enum will be viewed as strings
        FUNC_TABLE[MarshallUtil.ENUM_TYPE] = (o, inf, i, c) -> {
            Class<?> rawType = inf.rawType();
            JsonSerializeFunc customFunc = c.option().customFunc(rawType);
            if(customFunc != null) {
                customFunc.serialize(o, i, c);
            } else {
                c.serializeEnum((Enum<?>) o);
            }
            return JsonSerializeResult.Continue;
        };
        // collection type
        FUNC_TABLE[MarshallUtil.COLLECTION_INTERFACE_TYPE] = FUNC_TABLE[MarshallUtil.COLLECTION_IMPL_TYPE] = (o, inf, _, c) -> {
            c.setCol((Collection<?>) o);
            c.setFirstType(inf.firstGenericType());
            return JsonSerializeResult.NewCollection;
        };
        // map type
        FUNC_TABLE[MarshallUtil.MAP_INTERFACE_TYPE] = FUNC_TABLE[MarshallUtil.MAP_IMPL_TYPE] = (o, inf, _, c) -> {
            c.setMap((Map<?, ?>) o);
            c.setFirstType(inf.firstGenericType());
            c.setSecondType(inf.secondGenericType());
            return JsonSerializeResult.NewMap;
        };
    }

    public void init(MarshallFacade fc, Object marshallable, int indent) {
        this.fc = fc;
        this.reader = fc.newReader(marshallable);
        this.indent = indent;
        this.index = 0;
        this.written = false;
    }

    @Override
    protected JsonSerializeResult process(JsonSerializerContext c) {
        final int size = fc.totalElements();
        for(int i = index; i < size; i++) {
            MarshallInfo inf = fc.marshallInfoByIndex(i);
            if(inf.skipSerializing()) {
                continue ;
            }
            int type = inf.type() & MarshallUtil.TYPE_MASK;
            if(type <= MarshallUtil.DOUBLE_TYPE) {
                serializeKey(inf, c);
                serializePrimitiveValue(reader, i, type, c);
                continue ;
            }
            Object fieldValue = reader.getObject(i);
            if(fieldValue == null) {
                if(c.option().serializeNullInObjOrMap()) {
                    serializeKey(inf, c);
                    c.serializeNull();
                }
                continue ;
            }
            serializeKey(inf, c);
            JsonSerializeResult r = FUNC_TABLE[type].serialize(fieldValue, inf, indent, c);
            if(r == JsonSerializeResult.Continue) {
                continue ;
            }
            index = i + 1;
            return r;
        }
        c.writeBuffer().writeByte((byte) '}');
        return JsonSerializeResult.Finished;
    }

    private void serializeKey(MarshallInfo marshallInfo, JsonSerializerContext c) {
        serializeSep(c);
        byte[] mappedNameUtf8Bytes = marshallInfo.mappedNameUtf8Bytes();
        if(marshallInfo.mappedNameSimple()) {
            c.serializeNonEscapedUtf8Bytes(mappedNameUtf8Bytes);
        } else {
            c.serializeEscapedUtf8Bytes(mappedNameUtf8Bytes);
        }
        c.writeBuffer().writeBytes((byte) ':', (byte) ' ');
    }

    private static void serializePrimitiveValue(MarshallReader reader, int index, int type, JsonSerializerContext c) {
        switch (type) {
            case MarshallUtil.BYTE_TYPE    -> c.serializeByte(reader.getByte(index));
            case MarshallUtil.BOOLEAN_TYPE -> c.serializeBoolean(reader.getBoolean(index));
            case MarshallUtil.SHORT_TYPE   -> c.serializeShort(reader.getShort(index));
            case MarshallUtil.CHAR_TYPE    -> c.serializeChar(reader.getChar(index));
            case MarshallUtil.INT_TYPE     -> c.serializeInt(reader.getInt(index));
            case MarshallUtil.LONG_TYPE    -> c.serializeLong(reader.getLong(index));
            case MarshallUtil.FLOAT_TYPE   -> c.serializeFloat(reader.getFloat(index));
            case MarshallUtil.DOUBLE_TYPE  -> c.serializeDouble(reader.getDouble(index));
            default -> throw new AssertionError();
        }
    }

}
