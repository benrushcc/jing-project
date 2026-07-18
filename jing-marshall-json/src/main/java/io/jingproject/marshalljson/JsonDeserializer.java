package io.jingproject.marshalljson;

import io.jingproject.common.ReadBuffer;

import java.util.List;
import java.util.Map;

public final class JsonDeserializer {
    private final JsonDeserializerOption option;

    public JsonDeserializer(JsonDeserializerOption option) {
        this.option = option;
    }

    public byte[] deserializeByteArray(ReadBuffer readBuffer) {
        byte firstByte = JsonDeserializeUtil.nextFirstValuableByte(option, readBuffer);
        if(!JsonDeserializeUtil.validateJsonArrayStart(firstByte)) {
            throw new JsonDeserializerException("array start not found, got : " + firstByte);
        }
        return JsonDeserializeUtil.deserializeByteArray(option, readBuffer, firstByte);
    }

    public boolean[] deserializeBooleanArray(ReadBuffer readBuffer) {
        byte firstByte = JsonDeserializeUtil.nextFirstValuableByte(option, readBuffer);
        if(!JsonDeserializeUtil.validateJsonArrayStart(firstByte)) {
            throw new JsonDeserializerException("array start not found, got : " + firstByte);
        }
        return JsonDeserializeUtil.deserializeBooleanArray(option, readBuffer, firstByte);
    }

    public short[] deserializeShortArray(ReadBuffer readBuffer) {
        byte firstByte = JsonDeserializeUtil.nextFirstValuableByte(option, readBuffer);
        if(!JsonDeserializeUtil.validateJsonArrayStart(firstByte)) {
            throw new JsonDeserializerException("array start not found, got : " + firstByte);
        }
        return JsonDeserializeUtil.deserializeShortArray(option, readBuffer, firstByte);
    }

    public char[] deserializeCharArray(ReadBuffer readBuffer) {
        JsonDeserializerContext context = new JsonDeserializerContext(option);
        byte firstByte = JsonDeserializeUtil.nextFirstValuableByte(option, readBuffer);
        if(!JsonDeserializeUtil.validateJsonArrayStart(firstByte)) {
            throw new JsonDeserializerException("array start not found, got : " + firstByte);
        }
        return JsonDeserializeUtil.deserializeCharArray(option, readBuffer, context, firstByte);
    }

    public int[] deserializeIntArray(ReadBuffer readBuffer) {
        byte firstByte = JsonDeserializeUtil.nextFirstValuableByte(option, readBuffer);
        if(!JsonDeserializeUtil.validateJsonArrayStart(firstByte)) {
            throw new JsonDeserializerException("array start not found, got : " + firstByte);
        }
        return JsonDeserializeUtil.deserializeIntArray(option, readBuffer, firstByte);
    }

    public long[] deserializeLongArray(ReadBuffer readBuffer) {
        byte firstByte = JsonDeserializeUtil.nextFirstValuableByte(option, readBuffer);
        if(!JsonDeserializeUtil.validateJsonArrayStart(firstByte)) {
            throw new JsonDeserializerException("array start not found, got : " + firstByte);
        }
        return JsonDeserializeUtil.deserializeLongArray(option, readBuffer, firstByte);
    }

    public float[] deserializeFloatArray(ReadBuffer readBuffer) {
        byte firstByte = JsonDeserializeUtil.nextFirstValuableByte(option, readBuffer);
        if(!JsonDeserializeUtil.validateJsonArrayStart(firstByte)) {
            throw new JsonDeserializerException("array start not found, got : " + firstByte);
        }
        return JsonDeserializeUtil.deserializeFloatArray(option, readBuffer, firstByte);
    }

    public double[] deserializeDoubleArray(ReadBuffer readBuffer) {
        byte firstByte = JsonDeserializeUtil.nextFirstValuableByte(option, readBuffer);
        if(!JsonDeserializeUtil.validateJsonArrayStart(firstByte)) {
            throw new JsonDeserializerException("array start not found, got : " + firstByte);
        }
        return JsonDeserializeUtil.deserializeDoubleArray(option, readBuffer, firstByte);
    }

    @SuppressWarnings("unchecked")
    public <T> T deserializeMarshallableObject(Class<T> type, ReadBuffer readBuffer) {
        JsonDeserializerState state = new JsonDeserializerState(option, readBuffer);
        state.initMarshallableType(type);
        return (T) state.process();
    }

    public <T> List<T> deserializeList(Class<T> type, ReadBuffer readBuffer) {
        return null;
    }

    public <K, V> Map<K, V> deserializeMap(Class<K> keyType, Class<V> valueType, ReadBuffer readBuffer) {
        return null;
    }
}
