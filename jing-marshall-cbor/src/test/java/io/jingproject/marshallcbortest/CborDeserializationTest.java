package io.jingproject.marshallcbortest;

import io.jingproject.common.HeapReadBuffer;
import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.common.SegmentReadBuffer;
import io.jingproject.marshallcbor.CborBoolType;
import io.jingproject.marshallcbor.CborDeserializer;
import io.jingproject.marshallcbor.CborDeserializerException;
import io.jingproject.marshallcbor.CborDeserializerOption;
import io.jingproject.marshallcbor.CborNumberType;
import io.jingproject.marshallcbor.CborSerializer;
import io.jingproject.marshallcbor.CborSerializerOption;
import io.jingproject.marshallcbor.CborStrType;
import io.jingproject.marshallcbortest.entity.BeanEntity;
import io.jingproject.marshallcbortest.entity.EnumEntity;
import io.jingproject.marshallcbortest.entity.RecursiveEntity;
import io.jingproject.marshallcbortest.transformers.BigDecimalTransformer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class CborDeserializationTest {
    private static final CborDeserializer DESERIALIZER = new CborDeserializer(CborDeserializerOption.defaultOption());
    private static final CborSerializer DETERMINISTIC_SERIALIZER = new CborSerializer(
            CborSerializerOption.builder().setTransformerClasses(BigDecimalTransformer.class)
                    .setDeterministicEncoding(true).build());
    private static final CborDeserializer DETERMINISTIC_DESERIALIZER = new CborDeserializer(
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
        List<io.jingproject.marshallcbor.CborPrimitiveType> primitiveTypes = new ArrayList<>();
        primitiveTypes.add(new CborBoolType(true));
        primitiveTypes.add(new CborStrType("jing"));
        primitiveTypes.add(new CborNumberType(42L));
        entity.setCborPrimitiveTypeList(primitiveTypes);
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
        DETERMINISTIC_SERIALIZER.serializeMarshallableObject(entity, writeBuffer);
        return DETERMINISTIC_DESERIALIZER.deserializeMarshallableObject(
                BeanEntity.class, new HeapReadBuffer(writeBuffer.toByteArray()));
    }

    @Test
    public void testDeserializeByteArray() {
        byte[] bytes = DESERIALIZER.deserializeByteArray(new HeapReadBuffer(hex("4b68656c6c6f20776f726c64")));
        Assertions.assertArrayEquals("hello world".getBytes(java.nio.charset.StandardCharsets.UTF_8), bytes);

        bytes = DESERIALIZER.deserializeByteArray(new HeapReadBuffer(hex("40")));
        Assertions.assertArrayEquals(new byte[0], bytes);

        bytes = DESERIALIZER.deserializeByteArray(new HeapReadBuffer(hex("4400ff7f80")));
        Assertions.assertArrayEquals(new byte[]{0, -1, 127, -128}, bytes);
    }

    @Test
    public void testDeserializeBooleanArray() {
        boolean[] array = DESERIALIZER.deserializeBooleanArray(new HeapReadBuffer(hex("84f5f4f5f4")));
        Assertions.assertArrayEquals(new boolean[]{true, false, true, false}, array);

        array = DESERIALIZER.deserializeBooleanArray(new HeapReadBuffer(hex("80")));
        Assertions.assertArrayEquals(new boolean[0], array);

        array = DESERIALIZER.deserializeBooleanArray(new HeapReadBuffer(hex("81f5")));
        Assertions.assertArrayEquals(new boolean[]{true}, array);
    }

    @Test
    public void testDeserializeShortArray() {
        short[] array = DESERIALIZER.deserializeShortArray(
                new HeapReadBuffer(hex("85187b1900ea1901591901c8190237")));
        Assertions.assertArrayEquals(new short[]{123, 234, 345, 456, 567}, array);

        array = DESERIALIZER.deserializeShortArray(new HeapReadBuffer(hex("80")));
        Assertions.assertArrayEquals(new short[0], array);

        array = DESERIALIZER.deserializeShortArray(new HeapReadBuffer(hex("84397fff2000197fff")));
        Assertions.assertArrayEquals(new short[]{Short.MIN_VALUE, -1, 0, Short.MAX_VALUE}, array);
    }

    @Test
    public void testDeserializeCharArray() {
        char[] array = DESERIALIZER.deserializeCharArray(
                new HeapReadBuffer(hex("8b61686165616c616c616f61206177616f6172616c6164")));
        Assertions.assertArrayEquals("hello world".toCharArray(), array);

        array = DESERIALIZER.deserializeCharArray(new HeapReadBuffer(hex("80")));
        Assertions.assertArrayEquals(new char[0], array);

        array = DESERIALIZER.deserializeCharArray(new HeapReadBuffer(hex("8263e4b8ad63e282ac")));
        Assertions.assertArrayEquals(new char[]{'中', '€'}, array);
    }

    @Test
    public void testDeserializeIntArray() {
        int[] array = DESERIALIZER.deserializeIntArray(
                new HeapReadBuffer(hex("85187b1900ea1901591901c8190237")));
        Assertions.assertArrayEquals(new int[]{123, 234, 345, 456, 567}, array);

        array = DESERIALIZER.deserializeIntArray(new HeapReadBuffer(hex("80")));
        Assertions.assertArrayEquals(new int[0], array);

        array = DESERIALIZER.deserializeIntArray(new HeapReadBuffer(hex("853a7fffffff3903e6001903e71a7fffffff")));
        Assertions.assertArrayEquals(new int[]{Integer.MIN_VALUE, -999, 0, 999, Integer.MAX_VALUE}, array);
    }

    @Test
    public void testDeserializeLongArray() {
        long[] array = DESERIALIZER.deserializeLongArray(
                new HeapReadBuffer(hex("8318641a000186a01b7fffffffffffffff")));
        Assertions.assertArrayEquals(new long[]{100L, 100000L, Long.MAX_VALUE}, array);

        array = DESERIALIZER.deserializeLongArray(new HeapReadBuffer(hex("80")));
        Assertions.assertArrayEquals(new long[0], array);

        array = DESERIALIZER.deserializeLongArray(
                new HeapReadBuffer(hex("853b7fffffffffffffff2000011b7fffffffffffffff")));
        Assertions.assertArrayEquals(new long[]{Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE}, array);
    }

    @Test
    public void testDeserializeFloatArray() {
        float[] array = DESERIALIZER.deserializeFloatArray(
                new HeapReadBuffer(hex("84f93c00fa3f800000fb3ff0000000000000fb4008000000000000")));
        Assertions.assertArrayEquals(new float[]{1.0f, 1.0f, 1.0f, 3.0f}, array);

        array = DESERIALIZER.deserializeFloatArray(new HeapReadBuffer(hex("80")));
        Assertions.assertArrayEquals(new float[0], array);
    }

    @Test
    public void testDeserializeDoubleArray() {
        double[] array = DESERIALIZER.deserializeDoubleArray(
                new HeapReadBuffer(hex("83f93e00fa40400000fb4000000000000000")));
        Assertions.assertArrayEquals(new double[]{1.5, 3.0, 2.0}, array);

        array = DESERIALIZER.deserializeDoubleArray(new HeapReadBuffer(hex("80")));
        Assertions.assertArrayEquals(new double[0], array);
    }

    @Test
    public void testDeserializeStringArray() {
        String[] array = DESERIALIZER.deserializeArray(String.class,
                new HeapReadBuffer(hex("836361626366e4bda0e5a5bd6b68656c6c6f20776f726c64")));
        Assertions.assertArrayEquals(new String[]{"abc", "你好", "hello world"}, array);

        array = DESERIALIZER.deserializeArray(String.class, new HeapReadBuffer(hex("83606060")));
        Assertions.assertArrayEquals(new String[]{"", "", ""}, array);
    }

    @Test
    public void testDeserializeEnumArray() {
        EnumEntity[] expected = {EnumEntity.ENUM_ENTITY3, EnumEntity.ENUM_ENTITY1, EnumEntity.ENUM_ENTITY4};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        DETERMINISTIC_SERIALIZER.serializeArray(expected, writeBuffer);
        EnumEntity[] actual = DESERIALIZER.deserializeArray(EnumEntity.class, new HeapReadBuffer(writeBuffer.toByteArray()));
        Assertions.assertArrayEquals(expected, actual);
    }

    @Test
    public void testDeserializeBeanEntity() {
        BeanEntity entity = createBeanEntity();
        Assertions.assertEquals(entity, roundTripBeanEntity(entity));
    }

    @Test
    public void testDeserializeBeanEntityDefault() {
        BeanEntity actual = DESERIALIZER.deserializeMarshallableObject(BeanEntity.class,
                new HeapReadBuffer(hex("a868696e7456616c75650069656e756d56616c7565f6696c6f6e6756616c7565f6" +
                        "6b737472696e674172726179f66b737472696e6756616c7565f66c646563696d616c56616c7565f6" +
                        "6d6265616e456e746974794d6170f67563626f725072696d6974697665547970654c697374f6")));
        Assertions.assertEquals(new BeanEntity(), actual);
    }

    @Test
    public void testDeserializeMap() {
        Map<String, Integer> map = DESERIALIZER.deserializeMap(String.class, Integer.class,
                new HeapReadBuffer(hex("a2626b3101626b3202")), HashMap::new);
        Map<String, Integer> expected = new HashMap<>();
        expected.put("k1", 1);
        expected.put("k2", 2);
        Assertions.assertEquals(expected, map);
    }

    @Test
    public void testDeserializeBooleanMap() {
        Map<String, Boolean> map = DESERIALIZER.deserializeMap(String.class, Boolean.class,
                new HeapReadBuffer(hex("aa6130f56131f46132f56133f46134f56135f46136f56137f46138f56139f4")),
                HashMap::new);
        Map<String, Boolean> expected = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            expected.put(i + "", i % 2 == 0);
        }
        Assertions.assertEquals(expected, map);
    }

    @Test
    public void testDeserializeCollection() {
        Collection<Integer> list = DESERIALIZER.deserializeCol(Integer.class,
                new HeapReadBuffer(hex("83010203")), ArrayList::new);
        Assertions.assertEquals(List.of(1, 2, 3), list);

        Collection<Integer> set = DESERIALIZER.deserializeCol(Integer.class,
                new HeapReadBuffer(hex("83010203")), LinkedHashSet::new);
        Assertions.assertEquals(new LinkedHashSet<>(List.of(1, 2, 3)), set);
    }

    @Test
    public void testDeserializeUnknownField() {
        BeanEntity actual = DESERIALIZER.deserializeMarshallableObject(BeanEntity.class,
                new HeapReadBuffer(hex("a268696e7456616c75650167756e6b6e6f776e8104")));
        Assertions.assertEquals(1, actual.intValue());
    }

    @Test
    public void testDeserializeUnknownNestedObject() {
        BeanEntity actual = DESERIALIZER.deserializeMarshallableObject(BeanEntity.class,
                new HeapReadBuffer(hex("a268696e7456616c75650167756e6b6e6f776ea1616101")));
        Assertions.assertEquals(1, actual.intValue());
    }

    @Test
    public void testDeserializeDuplicateKey() {
        CborDeserializerException ex = Assertions.assertThrows(CborDeserializerException.class,
                () -> DESERIALIZER.deserializeMarshallableObject(BeanEntity.class,
                        new HeapReadBuffer(hex("a268696e7456616c75650168696e7456616c756502"))));
        Assertions.assertEquals("duplicate key : intValue", ex.getMessage());
    }

    @Test
    public void testDeserializeMissingField() {
        CborDeserializerException ex = Assertions.assertThrows(CborDeserializerException.class,
                () -> DESERIALIZER.deserializeMarshallableObject(BeanEntity.class, new HeapReadBuffer(hex("a0"))));
        Assertions.assertEquals("missing field : intValue", ex.getMessage());
    }

    @Test
    public void testDeserializeRecursiveObject() {
        RecursiveEntity recursiveEntity = RecursiveEntity.createRecursiveEntity(3);
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        DETERMINISTIC_SERIALIZER.serializeMarshallableObject(recursiveEntity, writeBuffer);
        RecursiveEntity roundTripped = DETERMINISTIC_DESERIALIZER.deserializeMarshallableObject(
                RecursiveEntity.class, new HeapReadBuffer(writeBuffer.toByteArray()));
        Assertions.assertEquals(recursiveEntity, roundTripped);
    }

    @Test
    public void testDeserializeLimits() {
        CborDeserializerException ex = Assertions.assertThrows(CborDeserializerException.class,
                () -> DESERIALIZER.deserializeIntArray(new HeapReadBuffer(hex("9907d0"))));
        Assertions.assertEquals("too many array elements, exceeded limit : 2000", ex.getMessage());

        ex = Assertions.assertThrows(CborDeserializerException.class,
                () -> DESERIALIZER.deserializeMap(String.class, Integer.class,
                        new HeapReadBuffer(hex("b900c9")), HashMap::new));
        Assertions.assertEquals("too many map elements, exceeded limit : 201", ex.getMessage());

        ex = Assertions.assertThrows(CborDeserializerException.class,
                () -> DESERIALIZER.deserializeArray(String.class, new HeapReadBuffer(hex("817a00010000"))));
        Assertions.assertEquals("string length exceeds limit : 65536", ex.getMessage());

        ex = Assertions.assertThrows(CborDeserializerException.class,
                () -> DESERIALIZER.deserializeByteArray(new HeapReadBuffer(hex("5a00010000"))));
        Assertions.assertEquals("bytes length exceeds limit : 65536", ex.getMessage());

        CborDeserializer limitDeserializer = new CborDeserializer(
                CborDeserializerOption.builder().setMaxArrayElements(150).build());
        byte[] custom = new byte[152];
        custom[0] = (byte) 0x98;
        custom[1] = (byte) 0x96;
        Arrays.fill(custom, 2, custom.length, (byte) 0x07);
        int[] array = limitDeserializer.deserializeIntArray(new HeapReadBuffer(custom));
        Assertions.assertEquals(150, array.length);
        for (int value : array) {
            Assertions.assertEquals(7, value);
        }
    }

    @Test
    public void testDeserializeSegment() {
        SegmentReadBuffer readBuffer = new SegmentReadBuffer(
                MemorySegment.ofArray(hex("85187b1900ea1901591901c8190237")));
        int[] array = DESERIALIZER.deserializeIntArray(readBuffer);
        Assertions.assertArrayEquals(new int[]{123, 234, 345, 456, 567}, array);
    }
}