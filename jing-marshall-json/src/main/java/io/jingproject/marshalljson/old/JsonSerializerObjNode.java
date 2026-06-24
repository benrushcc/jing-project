package io.jingproject.marshalljson.old;

import io.jingproject.common.WriteBuffer;
import io.jingproject.marshall.*;
import io.jingproject.marshalljson.JsonSerializerException;

public final class JsonSerializerObjNode extends JsonSerializerNode {
    private final MarshallFacade fc;
    private final MarshallReader reader;

    public JsonSerializerObjNode(JsonSerializerOption option, WriteBuffer writeBuffer, int indent,
                                 Object instance, MarshallFacade fc) {
        super(option, writeBuffer, indent);
        this.fc = fc;
        this.reader = fc.newReader(instance);
    }

    @Override
    protected int capacity() {
        return fc.totalElements();
    }

    @Override
    protected void init() {
        JsonSerializeUtil.serializeObjStart(writeBuffer);
    }

    @Override
    protected JsonSerializerNode process(int index) {
        MarshallInfo marshallInfo = fc.marshallInfoByIndex(index);
        if(marshallInfo.skipSerializing()) {
            return null;
        }
        int z = marshallInfo.flagType();
        if(z >= MarshallUtil.BYTE_TYPE && z <= MarshallUtil.DOUBLE_TYPE) {
            serializeKey(marshallInfo);
            serializePrimitiveValue(index, z);
            return null;
        }
        Object o = reader.getObject(index);
        if(o == null) {
            if(option.serializeNullInObjOrMap()) {
                serializeKey(marshallInfo);
                JsonSerializeUtil.serializeNull(writeBuffer);
            }
            return null;
        }
        serializeKey(marshallInfo);
        return valueSerializer(marshallInfo, z)
                .serialize(o, option, writeBuffer, indent);
    }

    @Override
    protected void end() {
        JsonSerializeUtil.serializeObjEnd(writeBuffer);
    }

    private void serializeKey(MarshallInfo marshallInfo) {
        serializeSep();
        if(marshallInfo.mappedNameSimple()) {
            JsonSerializeUtil.serializeQuote(writeBuffer);
            writeBuffer.writeBytes(marshallInfo.mappedNameUtf8Bytes());
            JsonSerializeUtil.serializeQuote(writeBuffer);
        } else {
            JsonSerializeUtil.serializeEscapedUtf8Bytes(marshallInfo.mappedNameUtf8Bytes(), writeBuffer);
        }
        JsonSerializeUtil.serializeKvSep(writeBuffer);
    }

    private void serializePrimitiveValue(int index, int z) {
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

    private JsonValueSerializer valueSerializer(MarshallInfo marshallInfo, int z) {
        assert z >= MarshallUtil.BYTE_TYPE && z <= MarshallUtil.MAP_IMPL_TYPE;
        Class<?> rawType = marshallInfo.rawType();
        JsonValueSerializer builtinSerializer = switch (z) {
            // builtin supported wrapper types
            case MarshallUtil.BYTE_WRAPPER_TYPE          -> JsonSerializeUtil.BYTE_SERIALIZER;
            case MarshallUtil.BOOLEAN_WRAPPER_TYPE       -> JsonSerializeUtil.BOOLEAN_SERIALIZER;
            case MarshallUtil.SHORT_WRAPPER_TYPE         -> JsonSerializeUtil.SHORT_SERIALIZER;
            case MarshallUtil.CHAR_WRAPPER_TYPE          -> JsonSerializeUtil.CHARACTER_SERIALIZER;
            case MarshallUtil.INT_WRAPPER_TYPE           -> JsonSerializeUtil.INTEGER_SERIALIZER;
            case MarshallUtil.LONG_WRAPPER_TYPE          -> JsonSerializeUtil.LONG_SERIALIZER;
            case MarshallUtil.FLOAT_WRAPPER_TYPE         -> JsonSerializeUtil.FLOAT_SERIALIZER;
            case MarshallUtil.DOUBLE_WRAPPER_TYPE        -> JsonSerializeUtil.DOUBLE_SERIALIZER;
            // builtin supported primitive array types
            case MarshallUtil.BYTE_ARRAY_TYPE            -> JsonSerializeUtil.BYTE_ARRAY_SERIALIZER;
            case MarshallUtil.BOOLEAN_ARRAY_TYPE         -> JsonSerializeUtil.BOOLEAN_ARRAY_SERIALIZER;
            case MarshallUtil.SHORT_ARRAY_TYPE           -> JsonSerializeUtil.SHORT_ARRAY_SERIALIZER;
            case MarshallUtil.CHAR_ARRAY_TYPE            -> JsonSerializeUtil.CHAR_ARRAY_SERIALIZER;
            case MarshallUtil.INT_ARRAY_TYPE             -> JsonSerializeUtil.INT_ARRAY_SERIALIZER;
            case MarshallUtil.LONG_ARRAY_TYPE            -> JsonSerializeUtil.LONG_ARRAY_SERIALIZER;
            case MarshallUtil.FLOAT_ARRAY_TYPE           -> JsonSerializeUtil.FLOAT_ARRAY_SERIALIZER;
            case MarshallUtil.DOUBLE_ARRAY_TYPE          -> JsonSerializeUtil.DOUBLE_ARRAY_SERIALIZER;
            // builtin supported wrapper array types
            case MarshallUtil.BYTE_WRAPPER_ARRAY_TYPE    -> JsonSerializeUtil.BYTE_WRAPPER_ARRAY_SERIALIZER;
            case MarshallUtil.BOOLEAN_WRAPPER_ARRAY_TYPE -> JsonSerializeUtil.BOOLEAN_WRAPPER_ARRAY_SERIALIZER;
            case MarshallUtil.SHORT_WRAPPER_ARRAY_TYPE   -> JsonSerializeUtil.SHORT_WRAPPER_ARRAY_SERIALIZER;
            case MarshallUtil.CHAR_WRAPPER_ARRAY_TYPE    -> JsonSerializeUtil.CHARACTER_WRAPPER_ARRAY_SERIALIZER;
            case MarshallUtil.INT_WRAPPER_ARRAY_TYPE     -> JsonSerializeUtil.INTEGER_WRAPPER_ARRAY_SERIALIZER;
            case MarshallUtil.LONG_WRAPPER_ARRAY_TYPE    -> JsonSerializeUtil.LONG_WRAPPER_ARRAY_SERIALIZER;
            case MarshallUtil.FLOAT_WRAPPER_ARRAY_TYPE   -> JsonSerializeUtil.FLOAT_WRAPPER_ARRAY_SERIALIZER;
            case MarshallUtil.DOUBLE_WRAPPER_ARRAY_TYPE  -> JsonSerializeUtil.DOUBLE_WRAPPER_ARRAY_SERIALIZER;
            // builtin supported str types (String is also treated as a CharSequence)
            case MarshallUtil.CHARSEQUENCE_TYPE,
                 MarshallUtil.STRING_TYPE                 -> JsonSerializeUtil.CHAR_SEQUENCE_SERIALIZER;
            // The following types are intentionally processed out of the spec‑defined
            // order. However, thanks to the marshalling metadata, they are dispatched
            // via disjoint paths, so this deviation is safe and correct.
            case MarshallUtil.ARRAY_TYPE -> JsonSerializeUtil.arraySerializer(option, rawType);
            case MarshallUtil.ENUM_TYPE -> {
                JsonValueSerializer customSerializer = option.getCustomSerializer(rawType);
                if(customSerializer != null) {
                    yield customSerializer;
                }
                yield JsonSerializeUtil.enumSerializer(rawType);
            }
            case MarshallUtil.COLLECTION_INTERFACE_TYPE,
                 MarshallUtil.COLLECTION_IMPL_TYPE -> {
                Class<?> firstGenericType = marshallInfo.firstGenericType();
                JsonValueSerializer valueSerializer = firstGenericType.isArray() ?
                        JsonSerializeUtil.arraySerializer(option, firstGenericType) :
                        JsonSerializeUtil.rawSerializer(option, firstGenericType);
                yield JsonSerializeUtil.makeCollectionSerializer(valueSerializer);
            }
            case MarshallUtil.MAP_INTERFACE_TYPE,
                 MarshallUtil.MAP_IMPL_TYPE -> {
                Class<?> firstGenericType = marshallInfo.firstGenericType();
                if(firstGenericType != String.class && firstGenericType != CharSequence.class) {
                    throw new JsonSerializerException("unsupported key type : " + firstGenericType);
                }
                Class<?> secondGenericType = marshallInfo.secondGenericType();
                JsonValueSerializer valueSerializer = secondGenericType.isArray() ?
                        JsonSerializeUtil.arraySerializer(option, secondGenericType) :
                        JsonSerializeUtil.rawSerializer(option, secondGenericType);
                yield JsonSerializeUtil.makeMapSerializer(valueSerializer);
            }
            default -> null;
        };
        if(builtinSerializer != null) {
            return builtinSerializer;
        }
        // exclude generic types
        if(z >= MarshallUtil.ONE_GENERIC_TYPE) {
            throw new JsonSerializerException("unsupported generic type : " + marshallInfo);
        }
        // matching JsonPrimitiveType
        JsonValueSerializer builtinObjSerializer = JsonSerializeUtil.builtinObjSerializer(rawType);
        if(builtinObjSerializer != null) {
            return builtinObjSerializer;
        }
        // check if current type could be override by option
        JsonValueSerializer customSerializer = option.getCustomSerializer(rawType);
        if(customSerializer != null) {
            return customSerializer;
        }
        // assuming marshallable
        return JsonSerializeUtil.marshallableSerializer(rawType);
    }
}
