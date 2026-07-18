package io.jingproject.marshalljson;

import io.jingproject.common.WriteBuffer;
import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.MarshallInfo;
import io.jingproject.marshall.MarshallReader;
import io.jingproject.marshall.MarshallUtil;

import java.util.Collection;
import java.util.Map;

public final class JsonSerializerObjNode extends JsonSerializerNode {
    private MarshallFacade fc;
    private MarshallReader reader;

    public void init(MarshallFacade fc, Object instance, int indent) {
        this.fc = fc;
        this.reader = fc.newReader(instance);
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
            int z = marshallInfo.flagType();
            if(z >= MarshallUtil.BYTE_TYPE && z <= MarshallUtil.DOUBLE_TYPE) {
                serializeKey(option, writeBuffer, marshallInfo);
                serializePrimitiveValue(reader, writeBuffer, i, z);
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
            JsonSerializeResult r = serializeObjValue(option, writeBuffer, context, fieldValue, marshallInfo, z, indent);
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

    private static boolean trySerializeDirectSerializableValue(JsonSerializerOption option, WriteBuffer writeBuffer, Object fieldValue, Class<?> rawType, int indent, JsonSerializerContext context) {
        if(rawType == JsonPrimitiveType.class) {
            JsonSerializeUtil.serializeJsonPrimitiveType((JsonPrimitiveType) fieldValue, writeBuffer, context);
        } else if(rawType == JsonBoolType.class) {
            JsonSerializeUtil.serializeJsonBoolType((JsonBoolType) fieldValue, writeBuffer);
        } else if(rawType == JsonNumberType.class) {
            JsonSerializeUtil.serializeJsonNumberType((JsonNumberType) fieldValue, writeBuffer);
        } else if(rawType == JsonStrType.class) {
            JsonSerializeUtil.serializeJsonStrType((JsonStrType) fieldValue, writeBuffer, context);
        } else if(rawType == CharSequence[].class) {
            JsonSerializeUtil.serializeEscapedCharSequenceArray((CharSequence[]) fieldValue, indent, option.indentationLevel(), writeBuffer, context);
        } else if(rawType == String[].class) {
            JsonSerializeUtil.serializeEscapedStringArray((String[]) fieldValue, indent, option.indentationLevel(), writeBuffer, context);
        } else if(rawType == JsonPrimitiveType[].class) {
            JsonSerializeUtil.serializeJsonPrimitiveTypeArray((JsonPrimitiveType[]) fieldValue, indent, option.indentationLevel(), writeBuffer, context);
        } else if(rawType == JsonBoolType[].class) {
            JsonSerializeUtil.serializeJsonBoolTypeArray((JsonBoolType[]) fieldValue, indent, option.indentationLevel(), writeBuffer);
        } else if(rawType == JsonNumberType[].class) {
            JsonSerializeUtil.serializeJsonNumberTypeArray((JsonNumberType[]) fieldValue, indent, option.indentationLevel(), writeBuffer);
        } else if(rawType == JsonStrType[].class) {
            JsonSerializeUtil.serializeJsonStrTypeArray((JsonStrType[]) fieldValue, indent, option.indentationLevel(), writeBuffer, context);
        } else {
            return false;
        }
        return true;
    }

    private static JsonSerializeResult serializeObjValue(JsonSerializerOption option, WriteBuffer writeBuffer, JsonSerializerContext context, Object fieldValue, MarshallInfo marshallInfo, int z, int indent) {
        assert z >= MarshallUtil.BYTE_TYPE && z <= MarshallUtil.MAP_IMPL_TYPE;
        Class<?> rawType = marshallInfo.rawType();
        switch (z) {
            // builtin supported wrapper types
            case MarshallUtil.BYTE_WRAPPER_TYPE ->
                    JsonSerializeUtil.serializeByte((byte) fieldValue, writeBuffer);
            case MarshallUtil.BOOLEAN_WRAPPER_TYPE ->
                    JsonSerializeUtil.serializeBoolean((boolean) fieldValue, writeBuffer);
            case MarshallUtil.SHORT_WRAPPER_TYPE ->
                    JsonSerializeUtil.serializeShort((short) fieldValue, writeBuffer);
            case MarshallUtil.CHAR_WRAPPER_TYPE ->
                    JsonSerializeUtil.serializeChar((char) fieldValue, writeBuffer);
            case MarshallUtil.INT_WRAPPER_TYPE ->
                    JsonSerializeUtil.serializeInt((int) fieldValue, writeBuffer);
            case MarshallUtil.LONG_WRAPPER_TYPE ->
                    JsonSerializeUtil.serializeLong((long) fieldValue, writeBuffer);
            case MarshallUtil.FLOAT_WRAPPER_TYPE ->
                    JsonSerializeUtil.serializeFloat((float) fieldValue, writeBuffer);
            case MarshallUtil.DOUBLE_WRAPPER_TYPE ->
                    JsonSerializeUtil.serializeDouble((double) fieldValue, writeBuffer);
            // builtin supported primitive array types
            case MarshallUtil.BYTE_ARRAY_TYPE ->
                    JsonSerializeUtil.serializeByteArray((byte[]) fieldValue, indent, option.indentationLevel(), writeBuffer);
            case MarshallUtil.BOOLEAN_ARRAY_TYPE ->
                    JsonSerializeUtil.serializeBooleanArray((boolean[]) fieldValue, indent, option.indentationLevel(), writeBuffer);
            case MarshallUtil.SHORT_ARRAY_TYPE ->
                    JsonSerializeUtil.serializeShortArray((short[]) fieldValue, indent, option.indentationLevel(), writeBuffer);
            case MarshallUtil.CHAR_ARRAY_TYPE ->
                    JsonSerializeUtil.serializeCharArray((char[]) fieldValue, indent, option.indentationLevel(), writeBuffer);
            case MarshallUtil.INT_ARRAY_TYPE ->
                    JsonSerializeUtil.serializeIntArray((int[]) fieldValue, indent, option.indentationLevel(), writeBuffer);
            case MarshallUtil.LONG_ARRAY_TYPE ->
                    JsonSerializeUtil.serializeLongArray((long[]) fieldValue, indent, option.indentationLevel(), writeBuffer);
            case MarshallUtil.FLOAT_ARRAY_TYPE ->
                    JsonSerializeUtil.serializeFloatArray((float[]) fieldValue, indent, option.indentationLevel(), writeBuffer);
            case MarshallUtil.DOUBLE_ARRAY_TYPE ->
                    JsonSerializeUtil.serializeDoubleArray((double[]) fieldValue, indent, option.indentationLevel(), writeBuffer);
            // builtin supported wrapper array types
            case MarshallUtil.BYTE_WRAPPER_ARRAY_TYPE ->
                    JsonSerializeUtil.serializeByteWrapperArray((Byte[]) fieldValue, indent, option.indentationLevel(), writeBuffer);
            case MarshallUtil.BOOLEAN_WRAPPER_ARRAY_TYPE ->
                    JsonSerializeUtil.serializeBooleanWrapperArray((Boolean[]) fieldValue, indent, option.indentationLevel(), writeBuffer);
            case MarshallUtil.SHORT_WRAPPER_ARRAY_TYPE ->
                    JsonSerializeUtil.serializeShortWrapperArray((Short[]) fieldValue, indent, option.indentationLevel(), writeBuffer);
            case MarshallUtil.CHAR_WRAPPER_ARRAY_TYPE ->
                    JsonSerializeUtil.serializeCharWrapperArray((Character[]) fieldValue, indent, option.indentationLevel(), writeBuffer);
            case MarshallUtil.INT_WRAPPER_ARRAY_TYPE ->
                    JsonSerializeUtil.serializeIntWrapperArray((Integer[]) fieldValue, indent, option.indentationLevel(), writeBuffer);
            case MarshallUtil.LONG_WRAPPER_ARRAY_TYPE ->
                    JsonSerializeUtil.serializeLongWrapperArray((Long[]) fieldValue, indent, option.indentationLevel(), writeBuffer);
            case MarshallUtil.FLOAT_WRAPPER_ARRAY_TYPE ->
                    JsonSerializeUtil.serializeFloatWrapperArray((Float[]) fieldValue, indent, option.indentationLevel(), writeBuffer);
            case MarshallUtil.DOUBLE_WRAPPER_ARRAY_TYPE ->
                    JsonSerializeUtil.serializeDoubleWrapperArray((Double[]) fieldValue, indent, option.indentationLevel(), writeBuffer);
            case MarshallUtil.CHARSEQUENCE_TYPE ->
                    JsonSerializeUtil.serializeEscapedCharSequence((CharSequence) fieldValue, writeBuffer, context);
            case MarshallUtil.STRING_TYPE ->
                JsonSerializeUtil.serializeEscapedString((String) fieldValue, writeBuffer, context);
            case MarshallUtil.ARRAY_TYPE -> {
                context.setArr((Object[]) fieldValue);
                return JsonSerializeResult.NewArray;
            }
            // enum will be viewed as strings
            case MarshallUtil.ENUM_TYPE -> {
                JsonSerializeFunc customFunc = option.customFunc(rawType);
                if(customFunc != null) {
                    customFunc.serialize(option, writeBuffer, context, fieldValue, indent);
                } else {
                    JsonSerializeUtil.serializeEnum((Enum<?>) fieldValue, rawType, writeBuffer, context);
                }
            }
            case MarshallUtil.COLLECTION_INTERFACE_TYPE,
                 MarshallUtil.COLLECTION_IMPL_TYPE -> {
                context.setCol((Collection<?>) fieldValue);
                context.setFirstType(marshallInfo.firstGenericType());
                return JsonSerializeResult.NewCollection;
            }
            case MarshallUtil.MAP_INTERFACE_TYPE,
                 MarshallUtil.MAP_IMPL_TYPE -> {
                context.setMap((Map<?, ?>) fieldValue);
                context.setFirstType(marshallInfo.firstGenericType());
                context.setSecondType(marshallInfo.secondGenericType());
                return JsonSerializeResult.NewMap;
            }
            default -> {
                // exclude generic types
                if(z >= MarshallUtil.ONE_GENERIC_TYPE) {
                    throw new JsonSerializerException("unsupported generic type : " + marshallInfo);
                }
                // matching direct serializable value
                if(trySerializeDirectSerializableValue(option, writeBuffer, fieldValue, rawType, indent, context)) {
                    return JsonSerializeResult.Continue;
                }
                // check if current type could be override by option
                JsonSerializeFunc customFunc = option.customFunc(rawType);
                if(customFunc != null) {
                    customFunc.serialize(option, writeBuffer, context, fieldValue, indent);
                    return JsonSerializeResult.Continue;
                }
                // assuming marshallable
                context.setObj(fieldValue);
                return JsonSerializeResult.NewMarshallable;
            }
        }
        return JsonSerializeResult.Continue;
    }
}
