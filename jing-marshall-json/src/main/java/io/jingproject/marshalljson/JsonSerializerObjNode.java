package io.jingproject.marshalljson;

import io.jingproject.common.WriteBuffer;
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
        JsonSerializeResult serialize(JsonSerializerOption option, WriteBuffer writeBuffer, JsonSerializerContext context, 
                                      Object fieldValue, MarshallInfo marshallInfo, int indent);
    }

    static {
        Map<Class<?>, JsonSerializerObjFunc> r = new HashMap<>();
        r.put(JsonPrimitiveType.class, (_, w, c, fi, _, _) -> {
            JsonSerializeUtil.serializeJsonPrimitiveType((JsonPrimitiveType) fi, w, c);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonBoolType.class, (_, w, _, fi, _, _) -> {
            JsonSerializeUtil.serializeJsonBoolType((JsonBoolType) fi, w);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonNumberType.class, (_, w, _, fi, _, _) -> {
            JsonSerializeUtil.serializeJsonNumberType((JsonNumberType) fi, w);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonStrType.class, (_, w, c, fi, _, _) -> {
            JsonSerializeUtil.serializeJsonStrType((JsonStrType) fi, w, c);
            return JsonSerializeResult.Continue;
        });
        r.put(CharSequence[].class, (op, w, c, fi, _, ind) -> {
            JsonSerializeUtil.serializeEscapedCharSequenceArray((CharSequence[]) fi, ind, op.indentationLevel(), w, c);
            return JsonSerializeResult.Continue;
        });
        r.put(String[].class, (op, w, c, fi, _, ind) -> {
            JsonSerializeUtil.serializeEscapedStringArray((String[]) fi, ind, op.indentationLevel(), w, c);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonPrimitiveType[].class, (op, w, c, fi, _, ind) -> {
            JsonSerializeUtil.serializeJsonPrimitiveTypeArray((JsonPrimitiveType[]) fi, ind, op.indentationLevel(), w, c);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonBoolType[].class, (op, w, _, fi, _, ind) -> {
            JsonSerializeUtil.serializeJsonBoolTypeArray((JsonBoolType[]) fi, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonNumberType[].class, (op, w, _, fi, _, ind) -> {
            JsonSerializeUtil.serializeJsonNumberTypeArray((JsonNumberType[]) fi, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonStrType[].class, (op, w, c, fi, _, ind) -> {
            JsonSerializeUtil.serializeJsonStrTypeArray((JsonStrType[]) fi, ind, op.indentationLevel(), w, c);
            return JsonSerializeResult.Continue;
        });
        DIRECT_SERIALIZABLE_FUNC_MAP = Map.copyOf(r);
    }

    private static JsonSerializerObjFunc directSerializableFunc(Class<?> rawType) {
        return DIRECT_SERIALIZABLE_FUNC_MAP.get(rawType);
    }
    
    static {
        FUNC_TABLE = new JsonSerializerObjFunc[MarshallUtil.TYPE_SIZE];
        JsonSerializerObjFunc defaultFunc = (op, w, c, fi, inf, ind) -> {
            // exclude generic types
            if(inf.firstGenericType() != null || inf.secondGenericType() != null) {
                throw new JsonSerializerException("unsupported generic type : " + inf);
            }
            // matching direct serializable value
            Class<?> rawType = inf.rawType();
            JsonSerializerObjFunc directSerializableFunc = directSerializableFunc(rawType);
            if(directSerializableFunc != null) {
                return directSerializableFunc.serialize(op, w, c, fi, inf, ind);
            }
            // check if current type could be override by option
            JsonSerializeFunc customFunc = op.customFunc(rawType);
            if(customFunc != null) {
                customFunc.serialize(op, w, c, fi, ind);
                return JsonSerializeResult.Continue;
            }
            // assuming marshallable
            c.setObj(fi);
            return JsonSerializeResult.NewMarshallable;
        };
        Arrays.fill(FUNC_TABLE, defaultFunc);
        // builtin supported wrapper types
        FUNC_TABLE[MarshallUtil.BYTE_WRAPPER_TYPE] = (_, w, _, fi, _, _) -> {
            JsonSerializeUtil.serializeByte((byte) fi, w);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.BOOLEAN_WRAPPER_TYPE] = (_, w, _, fi, _, _) -> {
            JsonSerializeUtil.serializeBoolean((boolean) fi, w);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.SHORT_WRAPPER_TYPE] = (_, w, _, fi, _, _) -> {
            JsonSerializeUtil.serializeShort((short) fi, w);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.CHAR_WRAPPER_TYPE] = (_, w, _, fi, _, _) -> {
            JsonSerializeUtil.serializeChar((char) fi, w);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.INT_WRAPPER_TYPE] = (_, w, _, fi, _, _) -> {
            JsonSerializeUtil.serializeInt((int) fi, w);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.LONG_WRAPPER_TYPE] = (_, w, _, fi, _, _) -> {
            JsonSerializeUtil.serializeLong((long) fi, w);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.FLOAT_WRAPPER_TYPE] = (_, w, _, fi, _, _) -> {
            JsonSerializeUtil.serializeFloat((float) fi, w);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.DOUBLE_WRAPPER_TYPE] = (_, w, _, fi, _, _) -> {
            JsonSerializeUtil.serializeDouble((double) fi, w);
            return JsonSerializeResult.Continue;
        };
        // builtin supported primitive array types
        FUNC_TABLE[MarshallUtil.BYTE_ARRAY_TYPE] = (op, w, _, fi, _, ind) -> {
            JsonSerializeUtil.serializeByteArray((byte[]) fi, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.BOOLEAN_ARRAY_TYPE] = (op, w, _, fi, _, ind) -> {
            JsonSerializeUtil.serializeBooleanArray((boolean[]) fi, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.SHORT_ARRAY_TYPE] = (op, w, _, fi, _, ind) -> {
            JsonSerializeUtil.serializeShortArray((short[]) fi, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.CHAR_ARRAY_TYPE] = (op, w, _, fi, _, ind) -> {
            JsonSerializeUtil.serializeCharArray((char[]) fi, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.INT_ARRAY_TYPE] = (op, w, _, fi, _, ind) -> {
            JsonSerializeUtil.serializeIntArray((int[]) fi, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.LONG_ARRAY_TYPE] = (op, w, _, fi, _, ind) -> {
            JsonSerializeUtil.serializeLongArray((long[]) fi, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.FLOAT_ARRAY_TYPE] = (op, w, _, fi, _, ind) -> {
            JsonSerializeUtil.serializeFloatArray((float[]) fi, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.DOUBLE_ARRAY_TYPE] = (op, w, _, fi, _, ind) -> {
            JsonSerializeUtil.serializeDoubleArray((double[]) fi, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        };
        // builtin supported wrapper array types
        FUNC_TABLE[MarshallUtil.BYTE_WRAPPER_ARRAY_TYPE] = (op, w, _, fi, _, ind) -> {
            JsonSerializeUtil.serializeByteWrapperArray((Byte[]) fi, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.BOOLEAN_WRAPPER_ARRAY_TYPE] = (op, w, _, fi, _, ind) -> {
            JsonSerializeUtil.serializeBooleanWrapperArray((Boolean[]) fi, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.SHORT_WRAPPER_ARRAY_TYPE] = (op, w, _, fi, _, ind) -> {
            JsonSerializeUtil.serializeShortWrapperArray((Short[]) fi, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.CHAR_WRAPPER_ARRAY_TYPE] = (op, w, _, fi, _, ind) -> {
            JsonSerializeUtil.serializeCharWrapperArray((Character[]) fi, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.INT_WRAPPER_ARRAY_TYPE] = (op, w, _, fi, _, ind) -> {
            JsonSerializeUtil.serializeIntWrapperArray((Integer[]) fi, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.LONG_WRAPPER_ARRAY_TYPE] = (op, w, _, fi, _, ind) -> {
            JsonSerializeUtil.serializeLongWrapperArray((Long[]) fi, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.FLOAT_WRAPPER_ARRAY_TYPE] = (op, w, _, fi, _, ind) -> {
            JsonSerializeUtil.serializeFloatWrapperArray((Float[]) fi, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.DOUBLE_WRAPPER_ARRAY_TYPE] = (op, w, _, fi, _, ind) -> {
            JsonSerializeUtil.serializeDoubleWrapperArray((Double[]) fi, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        };
        // charsequence and string
        FUNC_TABLE[MarshallUtil.CHARSEQUENCE_TYPE] = (_, w, context, fi, _, _) -> {
            JsonSerializeUtil.serializeEscapedCharSequence((CharSequence) fi, w, context);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.STRING_TYPE] = (_, w, context, fi, _, _) -> {
            JsonSerializeUtil.serializeEscapedString((String) fi, w, context);
            return JsonSerializeResult.Continue;
        };
        // array
        FUNC_TABLE[MarshallUtil.ARRAY_TYPE] = (_, _, context, fi, _, _) -> {
            context.setArr((Object[]) fi);
            return JsonSerializeResult.NewArray;
        };
        // enum will be viewed as strings
        FUNC_TABLE[MarshallUtil.ENUM_TYPE] = (op, w, context, fi, inf, ind) -> {
            Class<?> rawType = inf.rawType();
            JsonSerializeFunc customFunc = op.customFunc(rawType);
            if(customFunc != null) {
                customFunc.serialize(op, w, context, fi, ind);
            } else {
                JsonSerializeUtil.serializeEnum((Enum<?>) fi, rawType, w, context);
            }
            return JsonSerializeResult.Continue;
        };
        // collection type
        FUNC_TABLE[MarshallUtil.COLLECTION_INTERFACE_TYPE] = FUNC_TABLE[MarshallUtil.COLLECTION_IMPL_TYPE] = (_, _, context, fi, inf, _) -> {
            context.setCol((Collection<?>) fi);
            context.setFirstType(inf.firstGenericType());
            return JsonSerializeResult.NewCollection;
        };
        // map type
        FUNC_TABLE[MarshallUtil.MAP_INTERFACE_TYPE] = FUNC_TABLE[MarshallUtil.MAP_IMPL_TYPE] = (_, _, context, fi, inf, _) -> {
            context.setMap((Map<?, ?>) fi);
            context.setFirstType(inf.firstGenericType());
            context.setSecondType(inf.secondGenericType());
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
    protected JsonSerializeResult process(JsonSerializerOption option, WriteBuffer writeBuffer, JsonSerializerContext context) {
        final int size = fc.totalElements();
        for(int i = index; i < size; i++) {
            MarshallInfo marshallInfo = fc.marshallInfoByIndex(i);
            if(marshallInfo.skipSerializing()) {
                continue ;
            }
            int type = marshallInfo.type() & MarshallUtil.TYPE_MASK;
            if(type <= MarshallUtil.DOUBLE_TYPE) {
                serializeKey(option, writeBuffer, marshallInfo);
                serializePrimitiveValue(reader, writeBuffer, i, type);
                continue ;
            }
            Object fieldValue = reader.getObject(i);
            if(fieldValue == null) {
                if(option.serializeNullInObjOrMap()) {
                    serializeKey(option, writeBuffer, marshallInfo);
                    JsonSerializeUtil.serializeNull(writeBuffer);
                }
                continue ;
            }
            serializeKey(option, writeBuffer, marshallInfo);
            JsonSerializeResult r = FUNC_TABLE[type].serialize(option, writeBuffer, context, fieldValue, marshallInfo, indent);
            if(r == JsonSerializeResult.Continue) {
                continue ;
            }
            index = i + 1;
            return r;
        }
        JsonSerializeUtil.serializeObjEnd(writeBuffer);
        return JsonSerializeResult.Finished;
    }

    private void serializeKey(JsonSerializerOption option, WriteBuffer writeBuffer, MarshallInfo marshallInfo) {
        serializeSep(option, writeBuffer);
        byte[] mappedNameUtf8Bytes = marshallInfo.mappedNameUtf8Bytes();
        if(marshallInfo.mappedNameSimple()) {
            JsonSerializeUtil.serializeNonEscapedUtf8Bytes(mappedNameUtf8Bytes, writeBuffer);
        } else {
            JsonSerializeUtil.serializeEscapedUtf8Bytes(mappedNameUtf8Bytes, writeBuffer);
        }
        JsonSerializeUtil.serializeKvSep(writeBuffer);
    }

    private static void serializePrimitiveValue(MarshallReader reader, WriteBuffer writeBuffer, int index, int z) {
        switch (z) {
            case MarshallUtil.BYTE_TYPE    -> JsonSerializeUtil.serializeByte(reader.getByte(index), writeBuffer);
            case MarshallUtil.BOOLEAN_TYPE -> JsonSerializeUtil.serializeBoolean(reader.getBoolean(index), writeBuffer);
            case MarshallUtil.SHORT_TYPE   -> JsonSerializeUtil.serializeShort(reader.getShort(index), writeBuffer);
            case MarshallUtil.CHAR_TYPE    -> JsonSerializeUtil.serializeChar(reader.getChar(index), writeBuffer);
            case MarshallUtil.INT_TYPE     -> JsonSerializeUtil.serializeInt(reader.getInt(index), writeBuffer);
            case MarshallUtil.LONG_TYPE    -> JsonSerializeUtil.serializeLong(reader.getLong(index), writeBuffer);
            case MarshallUtil.FLOAT_TYPE   -> JsonSerializeUtil.serializeFloat(reader.getFloat(index), writeBuffer);
            case MarshallUtil.DOUBLE_TYPE  -> JsonSerializeUtil.serializeDouble(reader.getDouble(index), writeBuffer);
            default -> throw new AssertionError();
        }
    }

}
