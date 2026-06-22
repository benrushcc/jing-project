package io.jingproject.marshalljsontest.test;

import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.marshalljson.*;
import io.jingproject.marshalljsontest.entity.BeanEntity;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag("view-output")
public class JsonSerializationTest {
    private static final JsonSerializer JSON_SERIALIZER = new JsonSerializer(JsonSerializerOption.defaultOption());
    private static final int SIZE = 128;

    @Test
    public void testSerializeByteArray() {
        String s = "hello world";
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializePrimitiveArray(s.getBytes(StandardCharsets.UTF_8), writeBuffer);
        String json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        System.out.println(json);
    }

    @Test
    public void testSerializeBooleanArray() {
        boolean[] arr = {true, false, true, false};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializePrimitiveArray(arr, writeBuffer);
        String json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        System.out.println(json);
    }

    @Test
    public void testSerializeCharArray() {
        String s = "hello world";
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializePrimitiveArray(s.toCharArray(), writeBuffer);
        String json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        System.out.println(json);
    }

    @Test
    public void testSerializeShortArray() {
        short[] arr = {1, 2, 3, 4, 5};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializePrimitiveArray(arr, writeBuffer);
        String json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        System.out.println(json);
    }

    @Test
    public void testSerializeIntArray() {
        int[] arr = {10, 20, 30};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializePrimitiveArray(arr, writeBuffer);
        String json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        System.out.println(json);
    }

    @Test
    public void testSerializeLongArray() {
        long[] arr = {100L, 200L, 300L};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializePrimitiveArray(arr, writeBuffer);
        String json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        System.out.println(json);
    }

    @Test
    public void testSerializeFloatArray() {
        float[] arr = {1.1f, 2.2f, 3.3f};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializePrimitiveArray(arr, writeBuffer);
        String json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        System.out.println(json);
    }

    @Test
    public void testSerializeDoubleArray() {
        double[] arr = {1.11, 2.22, 3.33};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializePrimitiveArray(arr, writeBuffer);
        String json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        System.out.println(json);
    }

    @Test
    public void testSerializeObject() {
        BeanEntity entity = new BeanEntity();
        entity.setIntValue(42);
        entity.setLongValue(100L);
        entity.setStringArray(new String[]{"hello", "world", "test"});
        List<JsonPrimitiveType> jsonPrimitiveTypes = new ArrayList<>();
        jsonPrimitiveTypes.add(new JsonBoolType(true));
        jsonPrimitiveTypes.add(new JsonBoolType(false));
        jsonPrimitiveTypes.add(new JsonStrType("jing"));
        entity.setJsonPrimitiveTypeList(jsonPrimitiveTypes);
        Map<String, BeanEntity> innerMap = new HashMap<>();
        BeanEntity k1 = new BeanEntity();
        k1.setIntValue(1);
        BeanEntity k2 = new BeanEntity();
        k2.setIntValue(2);
        innerMap.put("key1", k1);
        innerMap.put("key2", k2);
        entity.setBeanEntityMap(innerMap);
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializeMarshallableObject(entity, BeanEntity.class, writeBuffer);
        String json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        System.out.println(json);
    }
}
