package io.jingproject.marshalljson;

import io.jingproject.common.WriteBuffer;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public final class JsonSerializer {
    private final JsonSerializerOption option;

    public JsonSerializer(JsonSerializerOption option) {
        this.option = Objects.requireNonNull(option, "option must not be null");
    }

    public void serializeByteArray(byte[] arr, WriteBuffer writeBuffer) {
        Objects.requireNonNull(arr, "arr must not be null");
        Objects.requireNonNull(writeBuffer, "writeBuffer must not be null");
        new JsonSerializerContext(option, writeBuffer).serializeByteArray(arr, 1);
    }

    public void serializeBooleanArray(boolean[] arr, WriteBuffer writeBuffer) {
        Objects.requireNonNull(arr, "arr must not be null");
        Objects.requireNonNull(writeBuffer, "writeBuffer must not be null");
        new JsonSerializerContext(option, writeBuffer).serializeBooleanArray(arr, 1);
    }

    public void serializeShortArray(short[] arr, WriteBuffer writeBuffer) {
        Objects.requireNonNull(arr, "arr must not be null");
        Objects.requireNonNull(writeBuffer, "writeBuffer must not be null");
        new JsonSerializerContext(option, writeBuffer).serializeShortArray(arr, 1);
    }

    public void serializeCharArray(char[] arr, WriteBuffer writeBuffer) {
        Objects.requireNonNull(arr, "arr must not be null");
        Objects.requireNonNull(writeBuffer, "writeBuffer must not be null");
        new JsonSerializerContext(option, writeBuffer).serializeCharArray(arr, 1);
    }

    public void serializeIntArray(int[] arr, WriteBuffer writeBuffer) {
        Objects.requireNonNull(arr, "arr must not be null");
        Objects.requireNonNull(writeBuffer, "writeBuffer must not be null");
        new JsonSerializerContext(option, writeBuffer).serializeIntArray(arr, 1);
    }

    public void serializeLongArray(long[] arr, WriteBuffer writeBuffer) {
        Objects.requireNonNull(arr, "arr must not be null");
        Objects.requireNonNull(writeBuffer, "writeBuffer must not be null");
        new JsonSerializerContext(option, writeBuffer).serializeLongArray(arr, 1);
    }

    public void serializeFloatArray(float[] arr, WriteBuffer writeBuffer) {
        Objects.requireNonNull(arr, "arr must not be null");
        Objects.requireNonNull(writeBuffer, "writeBuffer must not be null");
        new JsonSerializerContext(option, writeBuffer).serializeFloatArray(arr, 1);
    }

    public void serializeDoubleArray(double[] arr, WriteBuffer writeBuffer) {
        Objects.requireNonNull(arr, "arr must not be null");
        Objects.requireNonNull(writeBuffer, "writeBuffer must not be null");
        new JsonSerializerContext(option, writeBuffer).serializeDoubleArray(arr, 1);
    }

    public void serializeMarshallableObject(Object instance, WriteBuffer writeBuffer) {
        Objects.requireNonNull(instance, "instance must not be null");
        Objects.requireNonNull(writeBuffer, "writeBuffer must not be null");
        JsonSerializerState state = new JsonSerializerState(option, writeBuffer, instance);
        state.process();
    }

    public void serializeArray(Object[] arr, WriteBuffer writeBuffer) {
        Objects.requireNonNull(arr, "arr must not be null");
        Objects.requireNonNull(writeBuffer, "writeBuffer must not be null");
        JsonSerializerState state = new JsonSerializerState(option, writeBuffer, arr);
        state.process();
    }

    public <T> void serializeCollection(Collection<T> collection, Class<T> elementType, WriteBuffer writeBuffer) {
        Objects.requireNonNull(collection, "collection must not be null");
        Objects.requireNonNull(elementType, "elementType must not be null");
        Objects.requireNonNull(writeBuffer, "writeBuffer must not be null");
        JsonSerializerState state = new JsonSerializerState(option, writeBuffer, collection, elementType);
        state.process();
    }

    public <K, V> void serializeMap(Map<K, V> map, Class<K> keyType, Class<V> valueType, WriteBuffer writeBuffer) {
        Objects.requireNonNull(map, "map must not be null");
        Objects.requireNonNull(keyType, "keyType must not be null");
        Objects.requireNonNull(valueType, "valueType must not be null");
        Objects.requireNonNull(writeBuffer, "writeBuffer must not be null");
        JsonSerializerState state = new JsonSerializerState(option, writeBuffer, map, keyType, valueType);
        state.process();
    }
}
