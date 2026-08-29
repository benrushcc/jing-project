package io.jingproject.marshalljsontest.test;

import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.marshalljson.*;
import io.jingproject.marshalljsontest.entity.BeanEntity;
import io.jingproject.marshalljsontest.entity.EnumEntity;
import io.jingproject.marshalljsontest.entity.RecursiveEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonSerializationTest {
    private static final JsonSerializer JSON_SERIALIZER = new JsonSerializer(JsonSerializerOption.defaultOption());
    private static final int SIZE = 1024;

    @Test
    public void testSerializeByteArray() {
        String s = "hello world";
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializeByteArray(s.getBytes(StandardCharsets.UTF_8), writeBuffer);
        String json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        String expected = "[104,101,108,108,111,32,119,111,114,108,100]";
        Assertions.assertEquals(expected, json);

        writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializeByteArray(new byte[0], writeBuffer);
        json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        Assertions.assertEquals("[]", json);

        byte[] special = {0, -1, 127, -128};
        writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializeByteArray(special, writeBuffer);
        json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        Assertions.assertEquals("[0,-1,127,-128]", json);
    }

    @Test
    public void testSerializeBooleanArray() {
        boolean[] arr = {true, false, true, false};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializeBooleanArray(arr, writeBuffer);
        String json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        String expected = "[true,false,true,false]";
        Assertions.assertEquals(expected, json);

        writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializeBooleanArray(new boolean[0], writeBuffer);
        json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        Assertions.assertEquals("[]", json);
    }

    @Test
    public void testSerializeCharArray() {
        String s = "hello world";
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializeCharArray(s.toCharArray(), writeBuffer);
        String json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        String expected = "[\"h\",\"e\",\"l\",\"l\",\"o\",\" \",\"w\",\"o\",\"r\",\"l\",\"d\"]";
        Assertions.assertEquals(expected, json);

        writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializeCharArray(new char[0], writeBuffer);
        json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        Assertions.assertEquals("[]", json);

        char[] special = {'"', '\\', '\n'};
        writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializeCharArray(special, writeBuffer);
        json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        Assertions.assertEquals("[\"\\\"\",\"\\\\\",\"\\n\"]", json);
    }

    @Test
    public void testSerializeShortArray() {
        short[] arr = {1, 2, 3, 4, 5};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializeShortArray(arr, writeBuffer);
        String json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        String expected = "[1,2,3,4,5]";
        Assertions.assertEquals(expected, json);

        writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializeShortArray(new short[0], writeBuffer);
        json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        Assertions.assertEquals("[]", json);

        short[] special = {Short.MIN_VALUE, -1, 0, Short.MAX_VALUE};
        writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializeShortArray(special, writeBuffer);
        json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        Assertions.assertEquals("[-32768,-1,0,32767]", json);
    }

    @Test
    public void testSerializeIntArray() {
        int[] arr = {10, 20, 30, 40, 50};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializeIntArray(arr, writeBuffer);
        String json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        String expected = "[10,20,30,40,50]";
        Assertions.assertEquals(expected, json);

        writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializeIntArray(new int[0], writeBuffer);
        json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        Assertions.assertEquals("[]", json);

        int[] special = {Integer.MIN_VALUE, -999, 0, 999, Integer.MAX_VALUE};
        writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializeIntArray(special, writeBuffer);
        json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        Assertions.assertEquals("[-2147483648,-999,0,999,2147483647]", json);
    }

    @Test
    public void testSerializeLongArray() {
        long[] arr = {100L, 200L, 300L, 400L, 500L};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializeLongArray(arr, writeBuffer);
        String json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        String expected = "[100,200,300,400,500]";
        Assertions.assertEquals(expected, json);

        writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializeLongArray(new long[0], writeBuffer);
        json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        Assertions.assertEquals("[]", json);

        long[] special = {Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE};
        writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializeLongArray(special, writeBuffer);
        json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        Assertions.assertEquals("[-9223372036854775808,-1,0,1,9223372036854775807]", json);
    }

    @Test
    public void testSerializeFloatArray() {
        float[] arr = {1.1f, 2.2f, 3.3f};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializeFloatArray(arr, writeBuffer);
        String json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        String expected = "[1.1,2.2,3.3]";
        Assertions.assertEquals(expected, json);

        writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializeFloatArray(new float[0], writeBuffer);
        json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        Assertions.assertEquals("[]", json);
    }

    @Test
    public void testSerializeDoubleArray() {
        double[] arr = {1.11, 2.22, 3.33};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializeDoubleArray(arr, writeBuffer);
        String json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        String expected = "[1.11,2.22,3.33]";
        Assertions.assertEquals(expected, json);

        writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializeDoubleArray(new double[0], writeBuffer);
        json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        Assertions.assertEquals("[]", json);
    }

    @Test
    public void testSerializeSimpleObject() {
        BeanEntity entity = new BeanEntity();
        entity.setIntValue(42);
        entity.setLongValue(100L);
        entity.setStringValue("");
        entity.setEnumValue(EnumEntity.ENUM_ENTITY3);
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
        JSON_SERIALIZER.serializeMarshallableObject(entity, writeBuffer);
        String json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        String expected = "{\"intValue\": 42,\"longValue\": 100,\"stringValue\": \"\",\"enumValue\": \"\\\"ENUM_ESCAPE\\\"\",\"stringArray\": [\"hello\",\"world\",\"test\"],\"jsonPrimitiveTypeList\": [true,false,\"jing\"],\"beanEntityMap\": {\"key1\": {\"intValue\": 1},\"key2\": {\"intValue\": 2}}}";
        Assertions.assertEquals(expected, json);
    }

    @Test
    public void testSerializeSimpleMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("k1", 1);
        map.put("k2", 2);
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializeMap(map, String.class, Integer.class, writeBuffer);
        String json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        String expected = "{\"k1\": 1,\"k2\": 2}";
        Assertions.assertEquals(expected, json);
    }

    @Test
    public void testSerializeBooleanMap() {
        Map<String, Boolean> map = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            map.put(i + "", i % 2 == 0);
        }
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializeMap(map, String.class, Boolean.class, writeBuffer);
        String json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        String expected = "{\"0\": true,\"1\": false,\"2\": true,\"3\": false,\"4\": true,\"5\": false,\"6\": true,\"7\": false,\"8\": true,\"9\": false}";
        Assertions.assertEquals(expected, json);
    }

    @Test
    public void testSerializeSimpleArray() {
        String[] strings = {"abc", "你好", "hello world"};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializeArray(strings, writeBuffer);
        String json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        String expected = "[\"abc\",\"你好\",\"hello world\"]";
        Assertions.assertEquals(expected, json);
    }

    @Test
    public void testSerializeEmptyArray() {
        String[] strings = {"", "", ""};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializeArray(strings, writeBuffer);
        String json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        String expected = "[\"\",\"\",\"\"]";
        Assertions.assertEquals(expected, json);
    }

    @Test
    public void testSerializeRecursiveObject() {
        RecursiveEntity recursiveEntity = RecursiveEntity.createRecursiveEntity(3);
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        JSON_SERIALIZER.serializeMarshallableObject(recursiveEntity, writeBuffer);
        String json = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
        String expected = "{\"value\": 1,\"left\": {\"value\": 2,\"left\": {\"value\": 4},\"right\": {\"value\": 5}},\"right\": {\"value\": 3,\"left\": {\"value\": 6},\"right\": {\"value\": 7}}}";
        Assertions.assertEquals(expected, json);
    }
}
