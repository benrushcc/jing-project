package io.jingproject.marshalljson;

import io.jingproject.common.WriteBuffer;

import java.util.Collection;
import java.util.Map;

public final class JsonSerializer {
    private final JsonSerializerOption option;

    public JsonSerializer(JsonSerializerOption option) {
        this.option = option;
    }

    public void serializeByteArray(byte[] arr, WriteBuffer writeBuffer) {
        JsonSerializeUtil.serializeByteArray(arr, 1, option.indentationLevel(), writeBuffer);
    }

    public void serializeBooleanArray(boolean[] arr, WriteBuffer writeBuffer) {
        JsonSerializeUtil.serializeBooleanArray(arr, 1, option.indentationLevel(), writeBuffer);
    }

    public void serializeShortArray(short[] arr, WriteBuffer writeBuffer) {
        JsonSerializeUtil.serializeShortArray(arr, 1, option.indentationLevel(), writeBuffer);
    }

    public void serializeCharArray(char[] arr, WriteBuffer writeBuffer) {
        JsonSerializeUtil.serializeCharArray(arr, 1, option.indentationLevel(), writeBuffer);
    }

    public void serializeIntArray(int[] arr, WriteBuffer writeBuffer) {
        JsonSerializeUtil.serializeIntArray(arr, 1, option.indentationLevel(), writeBuffer);
    }

    public void serializeLongArray(long[] arr, WriteBuffer writeBuffer) {
        JsonSerializeUtil.serializeLongArray(arr, 1, option.indentationLevel(), writeBuffer);
    }

    public void serializeFloatArray(float[] arr, WriteBuffer writeBuffer) {
        JsonSerializeUtil.serializeFloatArray(arr, 1, option.indentationLevel(), writeBuffer);
    }

    public void serializeDoubleArray(double[] arr, WriteBuffer writeBuffer) {
        JsonSerializeUtil.serializeDoubleArray(arr, 1, option.indentationLevel(), writeBuffer);
    }

    public void serializeMarshallableObject(Object instance, WriteBuffer writeBuffer) {
        JsonSerializerState state = new JsonSerializerState(option, writeBuffer);
        state.initMarshallableObject(instance);
        state.process();
    }

    public void serializeArray(Object[] arr, WriteBuffer writeBuffer) {
        JsonSerializerState state = new JsonSerializerState(option, writeBuffer);
        state.initArray(arr);
        state.process();
    }

    public <T> void serializeCollection(Collection<T> collection, Class<T> elementType, WriteBuffer writeBuffer) {
        JsonSerializerState state = new JsonSerializerState(option, writeBuffer);
        state.initCol(collection, elementType);
        state.process();
    }

    public <K, V> void serializeMap(Map<K, V> map, Class<K> keyType, Class<V> valueType, WriteBuffer writeBuffer) {
        JsonSerializerState state = new JsonSerializerState(option, writeBuffer);
        state.initMap(map, keyType, valueType);
        state.process();
    }
}
