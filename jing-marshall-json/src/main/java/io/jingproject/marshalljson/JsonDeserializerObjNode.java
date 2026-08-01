package io.jingproject.marshalljson;

import io.jingproject.common.ReadBuffer;
import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.MarshallInfo;
import io.jingproject.marshall.MarshallUtil;
import io.jingproject.marshall.MarshallWriter;

public final class JsonDeserializerObjNode extends JsonDeserializerNode {
    private MarshallFacade fc;
    private MarshallWriter writer;
    private MarshallInfo info;
    private int dummyIndex;
    private int bitmapIndex;
    private int primitiveCount;
    private int objectCount;

    public void init(MarshallFacade fc, JsonDeserializerOption option, JsonDeserializerContext context) {
        this.fc = fc;
        this.writer = fc.newWriter();
        this.dummyIndex = 0;
        this.bitmapIndex = context.bitmapIndex(fc.totalElements());
        this.primitiveCount = 0;
        this.objectCount = 0;
    }

    @Override
    protected JsonDeserializeResult process(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, Object v) {
        byte firstByte;
        if(v != null) {
            if(info != null) {
                writer.setObject(info.index(), v);
                info = null;
            }
            JsonDeserializeResult r = deserializeSeparator(option, readBuffer, context, fc, writer, bitmapIndex);
            if(r != JsonDeserializeResult.Continue) {
                return r;
            }
        }
        for( ; ; ) {
            firstByte = JsonDeserializeUtil.nextFirstValuableByte(option, readBuffer);
            if(!JsonDeserializeUtil.validateJsonStringStart(firstByte)) {
                throw new JsonDeserializerException("illegal key start, got : " + firstByte);
            }
            JsonDeserializeUtil.parseStringIntoBytes(option, readBuffer, context, firstByte);
            MarshallInfo marshallInfo = context.asMarshallInfo(fc);
            if(marshallInfo == null) {
                // we need to skip the next value
                if(++dummyIndex > option.maxDummyElements()) {
                    throw new JsonDeserializerException("exceeded max dummy elements limit");
                }
                JsonDeserializeUtil.skipColon(option, readBuffer);
                firstByte = JsonDeserializeUtil.nextFirstValuableByte(option, readBuffer);
                if(JsonDeserializeUtil.skipAnyValue(option, readBuffer, firstByte)) {
                    JsonDeserializeResult r = deserializeSeparator(option, readBuffer, context, fc, writer, bitmapIndex);
                    if(r == JsonDeserializeResult.Continue) {
                        continue ;
                    }
                    return r;
                }else if(firstByte == (byte) '{') {
                    return JsonDeserializeResult.NewDummyObj;
                } else if(firstByte == (byte) '[') {
                    return JsonDeserializeResult.NewDummyArr;
                } else {
                    throw new JsonDeserializerException("illegal value start, got : " + firstByte);
                }
            }
            if (context.assign(bitmapIndex, marshallInfo.index())) {
                throw new JsonDeserializerException("key already assigned");
            }
            JsonDeserializeUtil.skipColon(option, readBuffer);
            firstByte = JsonDeserializeUtil.nextFirstValuableByte(option, readBuffer);
            int type = marshallInfo.type() & MarshallUtil.TYPE_MASK;
            if(type <= MarshallUtil.DOUBLE_TYPE) {
                deserializePritimiveValue(option, readBuffer, context, firstByte, writer, marshallInfo, type);
                primitiveCount++;
                JsonDeserializeResult r = deserializeSeparator(option, readBuffer, context, fc, writer, bitmapIndex);
                if(r == JsonDeserializeResult.Continue) {
                    continue ;
                }
                return r;
            }
            if(JsonDeserializeUtil.validateJsonNullStart(firstByte)) {
                JsonDeserializeUtil.deserializeNull(readBuffer, firstByte);
                objectCount++;
                JsonDeserializeResult r = deserializeSeparator(option, readBuffer, context, fc, writer, bitmapIndex);
                if(r == JsonDeserializeResult.Continue) {
                    continue ;
                }
                return r;
            }
            JsonDeserializeResult r = deserializeObjValue(option, readBuffer, context, firstByte, fc, writer, marshallInfo, type);
        }
    }

    private static JsonDeserializeResult deserializeSeparator(JsonDeserializerOption op, ReadBuffer r, JsonDeserializerContext c,
                                                              MarshallFacade fc, MarshallWriter writer, int bitmapIndex) {
        byte firstByte = JsonDeserializeUtil.nextFirstValuableByte(op, r);
        if(firstByte == (byte) '}') {
            if(op.ensureAllFieldsPresent()) {
                // if all fields are required to be present, we can simply check whether every bit in the bitmap is set
                if (!c.allPresent(bitmapIndex, fc.totalElements())) {
                    throw new JsonDeserializerException("missing field");
                }
            } else {
                // if not all fields are required to be present, we only need to check the presence of primitive fields
                if(!c.allPrimitiveFieldPresent(fc, bitmapIndex, fc.totalElements())) {
                    throw new JsonDeserializerException("missing primitive field");
                }
            }
            c.rewind(bitmapIndex);
            c.setObj(fc.construct(writer));
            return JsonDeserializeResult.Finish;
        } else if(firstByte == (byte) ',') {
            return JsonDeserializeResult.Continue;
        } else {
            throw new JsonDeserializerException("illegal separator, got : " + firstByte);
        }
    }

    private static void deserializePritimiveValue(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, byte firstByte,
                                                  MarshallWriter writer, MarshallInfo marshallInfo, int z) {
        assert readBuffer != null && writer != null && marshallInfo != null && z >= MarshallUtil.BYTE_TYPE && z <= MarshallUtil.DOUBLE_TYPE;
        final int index = marshallInfo.index();
        switch (z) {
            case MarshallUtil.BYTE_TYPE -> writer.setByte(index, JsonDeserializeUtil.deserializeByte(readBuffer, firstByte));
            case MarshallUtil.BOOLEAN_TYPE ->  writer.setBoolean(index, JsonDeserializeUtil.deserializeBoolean(readBuffer, firstByte));
            case MarshallUtil.SHORT_TYPE -> writer.setShort(index, JsonDeserializeUtil.deserializeShort(readBuffer, firstByte));
            case MarshallUtil.CHAR_TYPE -> writer.setChar(index, JsonDeserializeUtil.deserializeChar(option, readBuffer, context, firstByte));
            case MarshallUtil.INT_TYPE ->  writer.setInt(index, JsonDeserializeUtil.deserializeInt(readBuffer, firstByte));
            case MarshallUtil.LONG_TYPE -> writer.setLong(index, JsonDeserializeUtil.deserializeLong(readBuffer, firstByte));
            case MarshallUtil.FLOAT_TYPE -> writer.setFloat(index, JsonDeserializeUtil.deserializeFloat(option, readBuffer, firstByte));
            case MarshallUtil.DOUBLE_TYPE -> writer.setDouble(index, JsonDeserializeUtil.deserializeDouble(option, readBuffer, firstByte));
            default -> throw new AssertionError();
        }
    }

    // 后面改成查表法，但是现在先用switch来写
    private static JsonDeserializeResult deserializeObjValue(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, byte firstByte,
                                                             MarshallFacade fc, MarshallWriter writer, MarshallInfo inf, int z) {
        final int index = inf.index();
        switch (z) {
            // builtin supported wrapper types, relies on auto boxing
            case MarshallUtil.BYTE_WRAPPER_TYPE ->
                    writer.setObject(index, JsonDeserializeUtil.deserializeByte(readBuffer, firstByte));
            case MarshallUtil.BOOLEAN_WRAPPER_TYPE ->
                    writer.setObject(index, JsonDeserializeUtil.deserializeBoolean(readBuffer, firstByte));
            case MarshallUtil.SHORT_WRAPPER_TYPE ->
                    writer.setObject(index, JsonDeserializeUtil.deserializeShort(readBuffer, firstByte));
            case MarshallUtil.CHAR_WRAPPER_TYPE ->
                    writer.setObject(index, JsonDeserializeUtil.deserializeChar(option, readBuffer, context, firstByte));
            case MarshallUtil.INT_WRAPPER_TYPE ->
                    writer.setObject(index, JsonDeserializeUtil.deserializeInt(readBuffer, firstByte));
            case MarshallUtil.LONG_WRAPPER_TYPE ->
                    writer.setObject(index, JsonDeserializeUtil.deserializeLong(readBuffer, firstByte));
            case MarshallUtil.FLOAT_WRAPPER_TYPE ->
                    writer.setObject(index, JsonDeserializeUtil.deserializeFloat(option, readBuffer, firstByte));
            case MarshallUtil.DOUBLE_WRAPPER_TYPE ->
                    writer.setObject(index, JsonDeserializeUtil.deserializeDouble(option, readBuffer, firstByte));
            // builtin supported primitive array types
            case MarshallUtil.BYTE_ARRAY_TYPE ->
                    writer.setObject(index, JsonDeserializeUtil.deserializeByteArray(option, readBuffer, context, firstByte));
            case MarshallUtil.BOOLEAN_ARRAY_TYPE ->
                    writer.setObject(index, JsonDeserializeUtil.deserializeBooleanArray(option, readBuffer, context, firstByte));
            case MarshallUtil.SHORT_ARRAY_TYPE ->
                    writer.setObject(index, JsonDeserializeUtil.deserializeShortArray(option, readBuffer, context, firstByte));
            case MarshallUtil.CHAR_ARRAY_TYPE ->
                    writer.setObject(index, JsonDeserializeUtil.deserializeCharArray(option, readBuffer, context, firstByte));
            case MarshallUtil.INT_ARRAY_TYPE ->
                    writer.setObject(index, JsonDeserializeUtil.deserializeIntArray(option, readBuffer, context, firstByte));
            case MarshallUtil.LONG_ARRAY_TYPE ->
                    writer.setObject(index, JsonDeserializeUtil.deserializeLongArray(option, readBuffer, context, firstByte));
            case MarshallUtil.FLOAT_ARRAY_TYPE ->
                    writer.setObject(index, JsonDeserializeUtil.deserializeFloatArray(option, readBuffer, context, firstByte));
            case MarshallUtil.DOUBLE_ARRAY_TYPE ->
                    writer.setObject(index, JsonDeserializeUtil.deserializeDoubleArray(option, readBuffer, context, firstByte));
            // builtin supported wrapper array types
            case MarshallUtil.BYTE_WRAPPER_ARRAY_TYPE ->
                    writer.setObject(index, JsonDeserializeUtil.deserializeByteWrapperArray(option, readBuffer, context, firstByte));
            case MarshallUtil.BOOLEAN_WRAPPER_ARRAY_TYPE ->
                    writer.setObject(index, JsonDeserializeUtil.deserializeBooleanWrapperArray(option, readBuffer, context, firstByte));
            case MarshallUtil.SHORT_WRAPPER_ARRAY_TYPE ->
                    writer.setObject(index, JsonDeserializeUtil.deserializeShortWrapperArray(option, readBuffer, context, firstByte));
            case MarshallUtil.CHAR_WRAPPER_ARRAY_TYPE ->
                    writer.setObject(index, JsonDeserializeUtil.deserializeCharWrapperArray(option, readBuffer, context, firstByte));
            case MarshallUtil.INT_WRAPPER_ARRAY_TYPE ->
                    writer.setObject(index, JsonDeserializeUtil.deserializeIntWrapperArray(option, readBuffer, context, firstByte));
            case MarshallUtil.LONG_WRAPPER_ARRAY_TYPE ->
                    writer.setObject(index, JsonDeserializeUtil.deserializeLongWrapperArray(option, readBuffer, context, firstByte));
            case MarshallUtil.FLOAT_WRAPPER_ARRAY_TYPE ->
                    writer.setObject(index, JsonDeserializeUtil.deserializeFloatWrapperArray(option, readBuffer, context, firstByte));
            case MarshallUtil.DOUBLE_WRAPPER_ARRAY_TYPE ->
                    writer.setObject(index, JsonDeserializeUtil.deserializeDoubleWrapperArray(option, readBuffer, context, firstByte));
            case MarshallUtil.CHARSEQUENCE_TYPE, MarshallUtil.STRING_TYPE ->
                    writer.setObject(index, JsonDeserializeUtil.deserializeString(option, readBuffer, context, firstByte));
            case MarshallUtil.ARRAY_TYPE -> {

            }
            case MarshallUtil.ENUM_TYPE -> {

            }
            case MarshallUtil.COLLECTION_INTERFACE_TYPE -> {

            }
            case MarshallUtil.COLLECTION_IMPL_TYPE -> {

            }
            case MarshallUtil.MAP_INTERFACE_TYPE -> {

            }
            case MarshallUtil.MAP_IMPL_TYPE -> {

            }
            default -> {
                // exclude generic types
                if(inf.firstGenericType() != null || inf.secondGenericType() != null) {
                    throw new JsonSerializerException("unsupported generic type : " + inf);
                }
                // matching direct deserializable value
            }
        }
        return null;
    }
}
