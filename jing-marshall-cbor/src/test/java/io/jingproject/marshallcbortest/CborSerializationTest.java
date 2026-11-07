package io.jingproject.marshallcbortest;

import io.jingproject.common.HeapReadBuffer;
import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.common.SegmentWriteBuffer;
import io.jingproject.marshallcbor.*;
import io.jingproject.marshallcbortest.entity.BeanEntity;
import io.jingproject.marshallcbortest.entity.EnumEntity;
import io.jingproject.marshallcbortest.entity.RecordEntity;
import io.jingproject.marshallcbortest.entity.RecursiveEntity;
import io.jingproject.marshallcbortest.transformers.BigDecimalTransformer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CborSerializationTest {
    private static final CborSerializer SERIALIZER = new CborSerializer(CborSerializerOption.defaultOption());
    private static final CborSerializer NULL_SERIALIZER = new CborSerializer(
            CborSerializerOption.builder().setSerializeNullInObjOrMap(true).build());
    // default encoding streams objects and maps with indefinite-length heads, so the
    // round trip also exercises indefinite-length container decoding
    private static final CborSerializer ROUND_TRIP_SERIALIZER = new CborSerializer(
            CborSerializerOption.builder().setTransformerClasses(BigDecimalTransformer.class).build());
    private static final CborDeserializer ROUND_TRIP_DESERIALIZER = new CborDeserializer(
            CborDeserializerOption.builder().setTransformerClasses(BigDecimalTransformer.class).build());
    private static final int SIZE = 1024;

    private static byte[] hex(String expected) {
        int length = expected.length() / 2;
        byte[] r = new byte[length];
        for (int i = 0; i < length; i++) {
            int hi = Character.digit(expected.charAt(i * 2), 16);
            int lo = Character.digit(expected.charAt(i * 2 + 1), 16);
            r[i] = (byte) ((hi << 4) | lo);
        }
        return r;
    }

    private static BeanEntity createBeanEntity() {
        BeanEntity entity = new BeanEntity();
        entity.setIntValue(42);
        entity.setLongValue(100L);
        entity.setStringValue("jing");
        entity.setEnumValue(EnumEntity.ENUM_ENTITY3);
        entity.setStringArray(new String[]{"hello", "world", "test"});
        List<CborValue> values = new ArrayList<>();
        values.add(new CborBool(true));
        values.add(new CborText("jing"));
        values.add(new CborInteger(42L));
        entity.setCborValueList(values);
        Map<String, BeanEntity> innerMap = new LinkedHashMap<>();
        BeanEntity k1 = new BeanEntity();
        k1.setIntValue(1);
        BeanEntity k2 = new BeanEntity();
        k2.setIntValue(2);
        innerMap.put("key1", k1);
        innerMap.put("key2", k2);
        entity.setBeanEntityMap(innerMap);
        entity.setDecimalValue(new BigDecimal("12345678901234567890.12345678901234567890123456789"));
        return entity;
    }

    private static BeanEntity roundTripBeanEntity(BeanEntity entity) {
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        ROUND_TRIP_SERIALIZER.serializeMarshallableObject(entity, writeBuffer);
        return ROUND_TRIP_DESERIALIZER.deserializeMarshallableObject(
                BeanEntity.class, new HeapReadBuffer(writeBuffer.toByteArray()));
    }

    @Test
    public void testSerializeByteArray() {
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeByteArray("hello world".getBytes(StandardCharsets.UTF_8), writeBuffer);
        Assertions.assertArrayEquals(hex("4b68656c6c6f20776f726c64"), writeBuffer.toByteArray());

        writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeByteArray(new byte[0], writeBuffer);
        Assertions.assertArrayEquals(hex("40"), writeBuffer.toByteArray());

        byte[] special = {0, -1, 127, -128};
        writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeByteArray(special, writeBuffer);
        Assertions.assertArrayEquals(hex("4400ff7f80"), writeBuffer.toByteArray());
    }

    @Test
    public void testSerializeBooleanArray() {
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeBooleanArray(new boolean[]{true, false, true, false}, writeBuffer);
        Assertions.assertArrayEquals(hex("84f5f4f5f4"), writeBuffer.toByteArray());

        writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeBooleanArray(new boolean[0], writeBuffer);
        Assertions.assertArrayEquals(hex("80"), writeBuffer.toByteArray());
    }

    @Test
    public void testSerializeCharArray() {
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeCharArray("hello world".toCharArray(), writeBuffer);
        Assertions.assertArrayEquals(
                hex("8b61686165616c616c616f61206177616f6172616c6164"), writeBuffer.toByteArray());

        writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeCharArray(new char[0], writeBuffer);
        Assertions.assertArrayEquals(hex("80"), writeBuffer.toByteArray());

        char[] special = {'中', '€'};
        writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeCharArray(special, writeBuffer);
        Assertions.assertArrayEquals(hex("8263e4b8ad63e282ac"), writeBuffer.toByteArray());
    }

    @Test
    public void testSerializeShortArray() {
        short[] arr = {1, 2, 3, 4, 5};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeShortArray(arr, writeBuffer);
        Assertions.assertArrayEquals(hex("850102030405"), writeBuffer.toByteArray());

        writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeShortArray(new short[0], writeBuffer);
        Assertions.assertArrayEquals(hex("80"), writeBuffer.toByteArray());

        short[] special = {Short.MIN_VALUE, -1, 0, Short.MAX_VALUE};
        writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeShortArray(special, writeBuffer);
        Assertions.assertArrayEquals(hex("84397fff2000197fff"), writeBuffer.toByteArray());
    }

    @Test
    public void testSerializeIntArray() {
        int[] arr = {10, 20, 30, 40, 50};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeIntArray(arr, writeBuffer);
        Assertions.assertArrayEquals(hex("850a14181e18281832"), writeBuffer.toByteArray());

        writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeIntArray(new int[0], writeBuffer);
        Assertions.assertArrayEquals(hex("80"), writeBuffer.toByteArray());

        int[] special = {Integer.MIN_VALUE, -999, 0, 999, Integer.MAX_VALUE};
        writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeIntArray(special, writeBuffer);
        Assertions.assertArrayEquals(hex("853a7fffffff3903e6001903e71a7fffffff"), writeBuffer.toByteArray());
    }

    @Test
    public void testSerializeIntArraySegment() {
        try (Arena arena = Arena.ofConfined()) {
            SegmentWriteBuffer writeBuffer = new SegmentWriteBuffer(arena, 32);
            SERIALIZER.serializeIntArray(new int[]{10, 20, 30, 40, 50}, writeBuffer);
            Assertions.assertArrayEquals(hex("850a14181e18281832"), writeBuffer.toByteArray());
        }
    }

    @Test
    public void testSerializeLongArray() {
        long[] arr = {100L, 200L, 300L, 400L, 500L};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeLongArray(arr, writeBuffer);
        Assertions.assertArrayEquals(hex("85186418c819012c1901901901f4"), writeBuffer.toByteArray());

        writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeLongArray(new long[0], writeBuffer);
        Assertions.assertArrayEquals(hex("80"), writeBuffer.toByteArray());

        long[] special = {Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE};
        writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeLongArray(special, writeBuffer);
        Assertions.assertArrayEquals(hex("853b7fffffffffffffff2000011b7fffffffffffffff"), writeBuffer.toByteArray());
    }

    @Test
    public void testSerializeFloatArray() {
        float[] arr = {1.1f, 2.2f, 3.3f};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeFloatArray(arr, writeBuffer);
        ByteBuffer expectedFloat = ByteBuffer.allocate(1 + arr.length * 5);
        expectedFloat.put((byte) (0x80 | arr.length));
        for (float value : arr) {
            expectedFloat.put((byte) 0xFA);
            expectedFloat.putInt(Float.floatToRawIntBits(value));
        }
        Assertions.assertArrayEquals(expectedFloat.array(), writeBuffer.toByteArray());

        writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeFloatArray(new float[0], writeBuffer);
        Assertions.assertArrayEquals(hex("80"), writeBuffer.toByteArray());
    }

    @Test
    public void testSerializeDoubleArray() {
        double[] arr = {1.11, 2.22, 3.33};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeDoubleArray(arr, writeBuffer);
        ByteBuffer expectedDouble = ByteBuffer.allocate(1 + arr.length * 9);
        expectedDouble.put((byte) (0x80 | arr.length));
        for (double value : arr) {
            expectedDouble.put((byte) 0xFB);
            expectedDouble.putLong(Double.doubleToRawLongBits(value));
        }
        Assertions.assertArrayEquals(expectedDouble.array(), writeBuffer.toByteArray());

        writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeDoubleArray(new double[0], writeBuffer);
        Assertions.assertArrayEquals(hex("80"), writeBuffer.toByteArray());
    }

    @Test
    public void testSerializeBeanEntity() {
        BeanEntity entity = createBeanEntity();
        Assertions.assertEquals(entity, roundTripBeanEntity(entity));
    }

    @Test
    public void testSerializeRecordEntity() {
        Map<String, RecordEntity> innerMap = new LinkedHashMap<>();
        RecordEntity k1 = new RecordEntity(1, null, null, null, null, null, null);
        RecordEntity k2 = new RecordEntity(2, null, null, null, null, null, null);
        innerMap.put("key1", k1);
        innerMap.put("key2", k2);
        RecordEntity entity = new RecordEntity(
                42,
                100L,
                "jing",
                EnumEntity.ENUM_ENTITY3,
                new String[]{"hello", "world", "test"},
                new ArrayList<>(List.of(new CborBool(true), new CborText("jing"), new CborInteger(42L))),
                innerMap
        );
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        ROUND_TRIP_SERIALIZER.serializeMarshallableObject(entity, writeBuffer);
        RecordEntity roundTripped = ROUND_TRIP_DESERIALIZER.deserializeMarshallableObject(
                RecordEntity.class, new HeapReadBuffer(writeBuffer.toByteArray()));
        Assertions.assertEquals(entity, roundTripped);
    }

    @Test
    public void testSerializeSimpleMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("k1", 1);
        map.put("k2", 2);
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeMap(map, String.class, Integer.class, writeBuffer);
        Assertions.assertArrayEquals(hex("bf626b3101626b3202ff"), writeBuffer.toByteArray());
    }

    @Test
    public void testSerializeBooleanMap() {
        Map<String, Boolean> map = new LinkedHashMap<>();
        for (int i = 0; i < 10; i++) {
            map.put(i + "", i % 2 == 0);
        }
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeMap(map, String.class, Boolean.class, writeBuffer);
        Assertions.assertArrayEquals(
                hex("bf6130f56131f46132f56133f46134f56135f46136f56137f46138f56139f4ff"), writeBuffer.toByteArray());
    }

    @Test
    public void testSerializeMapWithNullValue() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", null);
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeMap(map, String.class, Integer.class, writeBuffer);
        Assertions.assertArrayEquals(hex("bf616101ff"), writeBuffer.toByteArray());

        writeBuffer = new HeapWriteBuffer(SIZE);
        NULL_SERIALIZER.serializeMap(map, String.class, Integer.class, writeBuffer);
        Assertions.assertArrayEquals(hex("a26161016162f6"), writeBuffer.toByteArray());
    }

    @Test
    public void testSerializeSimpleArray() {
        String[] strings = {"abc", "你好", "hello world"};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeArray(strings, writeBuffer);
        Assertions.assertArrayEquals(
                hex("836361626366e4bda0e5a5bd6b68656c6c6f20776f726c64"), writeBuffer.toByteArray());
    }

    @Test
    public void testSerializeEmptyArray() {
        String[] strings = {"", "", ""};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeArray(strings, writeBuffer);
        Assertions.assertArrayEquals(hex("83606060"), writeBuffer.toByteArray());
    }

    @Test
    public void testCborValueRoundTrip() {
        CborValue value = new CborArray(List.of(
                new CborBool(true),
                new CborInteger(-7),
                CborFloat.ofDouble(1.5, CborFloatWidth.DOUBLE),
                new CborText("中文"),
                new CborBytesType(new byte[]{0, -1}),
                new CborNull(),
                new CborTaggedType(1000, new CborText("tagged")),
                new CborMap(List.of(new CborMapEntry(new CborText("k"), new CborInteger(1))))
        ));
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeCborValue(value, writeBuffer);
        CborValue actual = ROUND_TRIP_DESERIALIZER.deserializeCborValue(
                new HeapReadBuffer(writeBuffer.toByteArray()));
        Assertions.assertEquals(value, actual);
    }

    @Test
    public void testCborValueFloatPrecision() {
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> CborFloat.ofDouble(0.1, CborFloatWidth.SINGLE));
        Assertions.assertEquals("value not exactly representable as single : 0.1", ex.getMessage());

        // a double NaN is allowed only when its payload is exactly representable
        ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> CborFloat.ofDouble(Double.longBitsToDouble(0x7FF8000000000001L), CborFloatWidth.SINGLE));
        Assertions.assertEquals("value not exactly representable as single : NaN", ex.getMessage());

        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        CborFloat half = CborFloat.ofDouble(1.5, CborFloatWidth.HALF);
        SERIALIZER.serializeCborValue(half, writeBuffer);
        Assertions.assertEquals(half,
                ROUND_TRIP_DESERIALIZER.deserializeCborValue(new HeapReadBuffer(writeBuffer.toByteArray())));

        // canonical NaN and infinity are exactly representable in every width
        CborFloat canonicalNaN = CborFloat.ofDouble(Double.NaN, CborFloatWidth.SINGLE);
        writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeCborValue(canonicalNaN, writeBuffer);
        Assertions.assertEquals(canonicalNaN,
                ROUND_TRIP_DESERIALIZER.deserializeCborValue(new HeapReadBuffer(writeBuffer.toByteArray())));

        CborFloat infinity = CborFloat.ofDouble(Double.POSITIVE_INFINITY, CborFloatWidth.HALF);
        writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeCborValue(infinity, writeBuffer);
        Assertions.assertEquals(infinity,
                ROUND_TRIP_DESERIALIZER.deserializeCborValue(new HeapReadBuffer(writeBuffer.toByteArray())));

        // a decoded nan payload is kept bit-for-bit, even when it is not canonical
        CborFloat payloadNaN = new CborFloat(0x7FA00001L, CborFloatWidth.SINGLE);
        writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeCborValue(payloadNaN, writeBuffer);
        Assertions.assertEquals(payloadNaN,
                ROUND_TRIP_DESERIALIZER.deserializeCborValue(new HeapReadBuffer(writeBuffer.toByteArray())));
    }

    @Test
    public void testSerializeCollection() {
        List<Integer> list = new ArrayList<>(List.of(1, 2, 3));
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeCollection(list, Integer.class, writeBuffer);
        Assertions.assertArrayEquals(hex("83010203"), writeBuffer.toByteArray());

        LinkedHashSet<Integer> set = new LinkedHashSet<>(List.of(1, 2, 3));
        writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeCollection(set, Integer.class, writeBuffer);
        Assertions.assertArrayEquals(hex("9f010203ff"), writeBuffer.toByteArray());

        writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeCollection(new LinkedHashSet<Integer>(), Integer.class, writeBuffer);
        Assertions.assertArrayEquals(hex("9fff"), writeBuffer.toByteArray());
    }

    @Test
    public void testSerializeRecursiveObject() {
        RecursiveEntity recursiveEntity = RecursiveEntity.createRecursiveEntity(3);
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        ROUND_TRIP_SERIALIZER.serializeMarshallableObject(recursiveEntity, writeBuffer);
        RecursiveEntity roundTripped = ROUND_TRIP_DESERIALIZER.deserializeMarshallableObject(
                RecursiveEntity.class, new HeapReadBuffer(writeBuffer.toByteArray()));
        Assertions.assertEquals(recursiveEntity, roundTripped);
    }

    @Test
    public void testBeanEntityDefaultBytes() {
        BeanEntity entity = new BeanEntity();
        entity.setIntValue(1);
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeMarshallableObject(entity, writeBuffer);
        Assertions.assertArrayEquals(hex("bf68696e7456616c756501ff"), writeBuffer.toByteArray());
    }

    @Test
    public void testRecursiveEntityDefaultBytes() {
        RecursiveEntity recursiveEntity = RecursiveEntity.createRecursiveEntity(1);
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeMarshallableObject(recursiveEntity, writeBuffer);
        Assertions.assertArrayEquals(hex("bf6576616c756501ff"), writeBuffer.toByteArray());
    }
}