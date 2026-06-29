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
        return null;
    }

    public boolean[] deserializeBooleanArray(ReadBuffer readBuffer) {
        return null;
    }

    public short[] deserializeShortArray(ReadBuffer readBuffer) {
        return null;
    }

    public char[] deserializeCharArray(ReadBuffer readBuffer) {
        return null;
    }

    public int[] deserializeIntArray(ReadBuffer readBuffer) {
        return null;
    }

    public long[] deserializeLongArray(ReadBuffer readBuffer) {
        return null;
    }

    public float[] deserializeFloatArray(ReadBuffer readBuffer) {
        return null;
    }

    public double[] deserializeDoubleArray(ReadBuffer readBuffer) {
        return null;
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
