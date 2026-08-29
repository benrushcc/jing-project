package io.jingproject.marshalljsontest.test;

import io.jingproject.common.HeapReadBuffer;
import io.jingproject.common.ReadBuffer;
import io.jingproject.marshalljson.*;
import io.jingproject.marshalljsontest.entity.BeanEntity;
import io.jingproject.marshalljsontest.entity.EnumEntity;
import io.jingproject.marshalljsontest.entity.RecursiveEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag("view-output")
public class JsonDeserializationTest {
    private static final JsonDeserializer JSON_DESERIALIZER = new JsonDeserializer(JsonDeserializerOption.defaultOption());

    @Test
    public void testDeserializeByteArray() {
        String s = "[1,2,3,4,5]";
        ReadBuffer readBuffer = new HeapReadBuffer(s.getBytes(StandardCharsets.UTF_8));
        byte[] bytes = JSON_DESERIALIZER.deserializeByteArray(readBuffer);
        Assertions.assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, bytes);

        s = "[]";
        readBuffer = new HeapReadBuffer(s.getBytes(StandardCharsets.UTF_8));
        bytes = JSON_DESERIALIZER.deserializeByteArray(readBuffer);
        Assertions.assertArrayEquals(new byte[0], bytes);

        s = "[42]";
        readBuffer = new HeapReadBuffer(s.getBytes(StandardCharsets.UTF_8));
        bytes = JSON_DESERIALIZER.deserializeByteArray(readBuffer);
        Assertions.assertArrayEquals(new byte[]{42}, bytes);
    }

    @Test
    public void testDeserializeBooleanArray() {
        String s = "[true, false, true, false, true]";
        ReadBuffer readBuffer = new HeapReadBuffer(s.getBytes(StandardCharsets.UTF_8));
        boolean[] booleans = JSON_DESERIALIZER.deserializeBooleanArray(readBuffer);
        Assertions.assertArrayEquals(new boolean[]{true, false, true, false, true}, booleans);

        s = "[]";
        readBuffer = new HeapReadBuffer(s.getBytes(StandardCharsets.UTF_8));
        booleans = JSON_DESERIALIZER.deserializeBooleanArray(readBuffer);
        Assertions.assertArrayEquals(new boolean[0], booleans);

        s = "[true]";
        readBuffer = new HeapReadBuffer(s.getBytes(StandardCharsets.UTF_8));
        booleans = JSON_DESERIALIZER.deserializeBooleanArray(readBuffer);
        Assertions.assertArrayEquals(new boolean[]{true}, booleans);
    }

    @Test
    public void testDeserializeShortArray() {
        String s = "[123,234,345,456,567]";
        ReadBuffer readBuffer = new HeapReadBuffer(s.getBytes(StandardCharsets.UTF_8));
        short[] shorts = JSON_DESERIALIZER.deserializeShortArray(readBuffer);
        Assertions.assertArrayEquals(new short[]{123, 234, 345, 456, 567}, shorts);

        s = "[]";
        readBuffer = new HeapReadBuffer(s.getBytes(StandardCharsets.UTF_8));
        shorts = JSON_DESERIALIZER.deserializeShortArray(readBuffer);
        Assertions.assertArrayEquals(new short[0], shorts);

        s = "[123]";
        readBuffer = new HeapReadBuffer(s.getBytes(StandardCharsets.UTF_8));
        shorts = JSON_DESERIALIZER.deserializeShortArray(readBuffer);
        Assertions.assertArrayEquals(new short[]{123}, shorts);
    }

    @Test
    public void testDeserializeCharArray() {
        String s = "[\"a\",\"b\",\"c\",\"d\",\"e\"]";
        ReadBuffer readBuffer = new HeapReadBuffer(s.getBytes(StandardCharsets.UTF_8));
        char[] chars = JSON_DESERIALIZER.deserializeCharArray(readBuffer);
        Assertions.assertArrayEquals(new char[]{'a', 'b', 'c', 'd', 'e'}, chars);

        s = "[]";
        readBuffer = new HeapReadBuffer(s.getBytes(StandardCharsets.UTF_8));
        chars = JSON_DESERIALIZER.deserializeCharArray(readBuffer);
        Assertions.assertArrayEquals(new char[0], chars);

        s = "[\"z\"]";
        readBuffer = new HeapReadBuffer(s.getBytes(StandardCharsets.UTF_8));
        chars = JSON_DESERIALIZER.deserializeCharArray(readBuffer);
        Assertions.assertArrayEquals(new char[]{'z'}, chars);
    }

    @Test
    public void testDeserializeIntArray() {
        String s = "[123,234,345,456,567]";
        ReadBuffer readBuffer = new HeapReadBuffer(s.getBytes(StandardCharsets.UTF_8));
        int[] ints = JSON_DESERIALIZER.deserializeIntArray(readBuffer);
        Assertions.assertArrayEquals(new int[]{123, 234, 345, 456, 567}, ints);

        s = "[]";
        readBuffer = new HeapReadBuffer(s.getBytes(StandardCharsets.UTF_8));
        ints = JSON_DESERIALIZER.deserializeIntArray(readBuffer);
        Assertions.assertArrayEquals(new int[0], ints);

        s = "[999]";
        readBuffer = new HeapReadBuffer(s.getBytes(StandardCharsets.UTF_8));
        ints = JSON_DESERIALIZER.deserializeIntArray(readBuffer);
        Assertions.assertArrayEquals(new int[]{999}, ints);
    }

    @Test
    public void testDeserializeLongArray() {
        String s = "[123,234,345,456,567]";
        ReadBuffer readBuffer = new HeapReadBuffer(s.getBytes(StandardCharsets.UTF_8));
        long[] longs = JSON_DESERIALIZER.deserializeLongArray(readBuffer);
        Assertions.assertArrayEquals(new long[]{123, 234, 345, 456, 567}, longs);

        s = "[]";
        readBuffer = new HeapReadBuffer(s.getBytes(StandardCharsets.UTF_8));
        longs = JSON_DESERIALIZER.deserializeLongArray(readBuffer);
        Assertions.assertArrayEquals(new long[0], longs);

        s = "[8888888888]";
        readBuffer = new HeapReadBuffer(s.getBytes(StandardCharsets.UTF_8));
        longs = JSON_DESERIALIZER.deserializeLongArray(readBuffer);
        Assertions.assertArrayEquals(new long[]{8888888888L}, longs);
    }

    @Test
    public void testDeserializeFloatArray() {
        String s = "[1.23,2.34,3.45,4.56,5.67]";
        ReadBuffer readBuffer = new HeapReadBuffer(s.getBytes(StandardCharsets.UTF_8));
        float[] floats = JSON_DESERIALIZER.deserializeFloatArray(readBuffer);
        Assertions.assertArrayEquals(new float[]{1.23f, 2.34f, 3.45f, 4.56f, 5.67f}, floats);

        s = "[]";
        readBuffer = new HeapReadBuffer(s.getBytes(StandardCharsets.UTF_8));
        floats = JSON_DESERIALIZER.deserializeFloatArray(readBuffer);
        Assertions.assertArrayEquals(new float[0], floats);

        s = "[9.99]";
        readBuffer = new HeapReadBuffer(s.getBytes(StandardCharsets.UTF_8));
        floats = JSON_DESERIALIZER.deserializeFloatArray(readBuffer);
        Assertions.assertArrayEquals(new float[]{9.99f}, floats);
    }

    @Test
    public void testDeserializeDoubleArray() {
        String s = "[1.23,2.34,3.45,4.56,5.67]";
        ReadBuffer readBuffer = new HeapReadBuffer(s.getBytes(StandardCharsets.UTF_8));
        double[] doubles = JSON_DESERIALIZER.deserializeDoubleArray(readBuffer);
        Assertions.assertArrayEquals(new double[]{1.23, 2.34, 3.45, 4.56, 5.67}, doubles);

        s = "[]";
        readBuffer = new HeapReadBuffer(s.getBytes(StandardCharsets.UTF_8));
        doubles = JSON_DESERIALIZER.deserializeDoubleArray(readBuffer);
        Assertions.assertArrayEquals(new double[0], doubles);

        s = "[3.14159]";
        readBuffer = new HeapReadBuffer(s.getBytes(StandardCharsets.UTF_8));
        doubles = JSON_DESERIALIZER.deserializeDoubleArray(readBuffer);
        Assertions.assertArrayEquals(new double[]{3.14159}, doubles);
    }

    @Test
    public void testDeserializeSimpleObject() {
        BeanEntity entity = new BeanEntity();
        entity.setIntValue(42);
        entity.setLongValue(100L);
        entity.setStringValue("");
        entity.setEnumValue(EnumEntity.ENUM_ENTITY3);
        entity.setStringArray(new String[]{"hello", "world", "test"});
        List<JsonPrimitiveType> expectedList = new ArrayList<>();
        expectedList.add(new JsonBoolType(true));
        expectedList.add(new JsonBoolType(false));
        expectedList.add(new JsonStrType("jing"));
        entity.setJsonPrimitiveTypeList(expectedList);
        Map<String, BeanEntity> expectedMap = new HashMap<>();
        BeanEntity k1 = new BeanEntity();
        k1.setIntValue(1);
        BeanEntity k2 = new BeanEntity();
        k2.setIntValue(2);
        expectedMap.put("key1", k1);
        expectedMap.put("key2", k2);
        entity.setBeanEntityMap(expectedMap);
        String json = "{\"intValue\": 42,\"longValue\": 100,\"stringValue\": \"\",\"enumValue\": \"\\\"ENUM_ESCAPE\\\"\",\"stringArray\": [\"hello\",\"world\",\"test\"],\"jsonPrimitiveTypeList\": [true,false,\"jing\"],\"beanEntityMap\": {\"key1\": {\"intValue\": 1},\"key2\": {\"intValue\": 2}}}";
        ReadBuffer readBuffer = new HeapReadBuffer(json.getBytes(StandardCharsets.UTF_8));
        BeanEntity actual = JSON_DESERIALIZER.deserializeMarshallableObject(BeanEntity.class, readBuffer);
        Assertions.assertEquals(entity, actual);
    }

    @Test
    public void testDeserializeSimpleMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("k1", 1);
        map.put("k2", 2);
        String json = "{\"k1\": 1,\"k2\": 2}";
        ReadBuffer readBuffer = new HeapReadBuffer(json.getBytes(StandardCharsets.UTF_8));
        Map<String, Integer> actual = JSON_DESERIALIZER.deserializeMap(String.class, Integer.class, readBuffer, HashMap::new);
        Assertions.assertEquals(map, actual);
    }

    @Test
    public void testDeserializeBooleanMap() {
        Map<String, Boolean> expected = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            expected.put(i + "", i % 2 == 0);
        }
        String json = "{\"0\": true,\"1\": false,\"2\": true,\"3\": false,\"4\": true,\"5\": false,\"6\": true,\"7\": false,\"8\": true,\"9\": false}";
        ReadBuffer readBuffer = new HeapReadBuffer(json.getBytes(StandardCharsets.UTF_8));
        Map<String, Boolean> actual = JSON_DESERIALIZER.deserializeMap(String.class, Boolean.class, readBuffer, HashMap::new);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testDeserializeSimpleArray() {
        String[] expected = {"abc", "你好", "hello world"};
        String json = "[\"abc\",\"你好\",\"hello world\"]";
        ReadBuffer readBuffer = new HeapReadBuffer(json.getBytes(StandardCharsets.UTF_8));
        String[] actual = JSON_DESERIALIZER.deserializeArray(String.class, readBuffer);
        Assertions.assertArrayEquals(expected, actual);
    }

    @Test
    public void testDeserializeEmptyArray() {
        String[] expected = {"", "", ""};
        String json = "[\"\",\"\",\"\"]";
        ReadBuffer readBuffer = new HeapReadBuffer(json.getBytes(StandardCharsets.UTF_8));
        String[] actual = JSON_DESERIALIZER.deserializeArray(String.class, readBuffer);
        Assertions.assertArrayEquals(expected, actual);
    }

    @Test
    public void testDeserializeRecursiveObject() {
        RecursiveEntity expected = RecursiveEntity.createRecursiveEntity(3);
        String json = "{\"value\": 1,\"left\": {\"value\": 2,\"left\": {\"value\": 4},\"right\": {\"value\": 5}},\"right\": {\"value\": 3,\"left\": {\"value\": 6},\"right\": {\"value\": 7}}}";
        ReadBuffer readBuffer = new HeapReadBuffer(json.getBytes(StandardCharsets.UTF_8));
        RecursiveEntity actual = JSON_DESERIALIZER.deserializeMarshallableObject(RecursiveEntity.class, readBuffer);
        Assertions.assertEquals(expected, actual);
    }
}
