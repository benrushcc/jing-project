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
        return JsonDeserializeUtil.deserializeByteArray(readBuffer, option);
    }

    public boolean[] deserializeBooleanArray(ReadBuffer readBuffer) {
        return JsonDeserializeUtil.deserializeBooleanArray(readBuffer, option);
    }

    public short[] deserializeShortArray(ReadBuffer readBuffer) {
        return JsonDeserializeUtil.deserializeShortArray(readBuffer, option);
    }

    public char[] deserializeCharArray(ReadBuffer readBuffer) {
        JsonDeserializerContext context = new JsonDeserializerContext();
        return JsonDeserializeUtil.deserializeCharArray(readBuffer, option, context);
    }

    public int[] deserializeIntArray(ReadBuffer readBuffer) {
        return JsonDeserializeUtil.deserializeIntArray(readBuffer, option);
    }

    public long[] deserializeLongArray(ReadBuffer readBuffer) {
        return JsonDeserializeUtil.deserializeLongArray(readBuffer, option);
    }

    public float[] deserializeFloatArray(ReadBuffer readBuffer) {
        return JsonDeserializeUtil.deserializeFloatArray(readBuffer, option);
    }

    public double[] deserializeDoubleArray(ReadBuffer readBuffer) {
        return JsonDeserializeUtil.deserializeDoubleArray(readBuffer, option);
    }

    public <T> T deserializeMarshallableObject(Class<T> type, ReadBuffer readBuffer) {
        return null;
    }

    public <T> List<T> deserializeList(Class<T> type, ReadBuffer readBuffer) {
        return null;
    }

    public <K, V> Map<K, V> deserializeMap(Class<K> keyType, Class<V> valueType, ReadBuffer readBuffer) {
        return null;
    }
}
