package io.jingproject.marshallcbortest;

import io.jingproject.common.HeapReadBuffer;
import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.marshallcbor.CborDeserializer;
import io.jingproject.marshallcbor.CborDeserializerOption;
import io.jingproject.marshallcbor.CborSerializer;
import io.jingproject.marshallcbor.CborSerializerOption;
import io.jingproject.marshallcbortest.entity.BeanEntity;
import io.jingproject.marshallcbortest.entity.EnumEntity;
import io.jingproject.marshallcbortest.transformers.BigDecimalTransformer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

// deterministic encoding writes definite-length container heads, forces null
// entries to be serialized as 0xf6 and sorts map keys by UTF-8 byte length then
// unsigned bytewise order, matching RFC 8949 section 4.2.1 core determinism.
public class CborDeterministicEncodingTest {
    private static final CborSerializer SERIALIZER = new CborSerializer(CborSerializerOption.defaultOption());
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

    private static int countF6(byte[] bytes) {
        int count = 0;
        for (byte b : bytes) {
            if (b == (byte) 0xF6) {
                count++;
            }
        }
        return count;
    }

    private static Map<String, Integer> threeEntryMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("中", 3);
        map.put("a", 1);
        map.put("bb", 2);
        return map;
    }

    private static BeanEntity createBeanEntity() {
        BeanEntity entity = new BeanEntity();
        entity.setIntValue(42);
        entity.setLongValue(100L);
        entity.setStringValue("jing");
        entity.setEnumValue(EnumEntity.ENUM_ENTITY3);
        entity.setStringArray(new String[]{"hello", "world", "test"});
        List<io.jingproject.marshallcbor.CborPrimitiveType> primitiveTypes = new ArrayList<>();
        primitiveTypes.add(new io.jingproject.marshallcbor.CborBoolType(true));
        primitiveTypes.add(new io.jingproject.marshallcbor.CborStrType("jing"));
        primitiveTypes.add(new io.jingproject.marshallcbor.CborNumberType(42L));
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

    @Test
    public void testDeterministicMapKeySortingByUtf8LengthThenByteOrder() {
        // byte lengths are a(1) < bb(2) < 中(3), so the deterministic output
        // reorders the insertion sequence 中, a, bb accordingly
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        DETERMINISTIC_SERIALIZER.serializeMap(threeEntryMap(), String.class, Integer.class, writeBuffer);
        Assertions.assertArrayEquals(hex("a36161016262620263e4b8ad03"), writeBuffer.toByteArray());

        // default encoding keeps insertion order and falls back to an indefinite map
        writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeMap(threeEntryMap(), String.class, Integer.class, writeBuffer);
        Assertions.assertArrayEquals(hex("bf63e4b8ad0361610162626202ff"), writeBuffer.toByteArray());
    }

    @Test
    public void testDeterministicOutputIndependentOfIterationOrder() {
        Map<String, Integer> reversed = new LinkedHashMap<>();
        reversed.put("bb", 2);
        reversed.put("a", 1);
        reversed.put("中", 3);
        HeapWriteBuffer first = new HeapWriteBuffer(SIZE);
        DETERMINISTIC_SERIALIZER.serializeMap(threeEntryMap(), String.class, Integer.class, first);
        HeapWriteBuffer second = new HeapWriteBuffer(SIZE);
        DETERMINISTIC_SERIALIZER.serializeMap(reversed, String.class, Integer.class, second);
        Assertions.assertArrayEquals(first.toByteArray(), second.toByteArray());
        Assertions.assertArrayEquals(hex("a36161016262620263e4b8ad03"), second.toByteArray());
    }

    @Test
    public void testDeterministicContainerHeadsAreDefinite() {
        // a definite map head with 3 entries is 0xa3, not the indefinite 0xbf
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        DETERMINISTIC_SERIALIZER.serializeMap(threeEntryMap(), String.class, Integer.class, writeBuffer);
        byte[] mapBytes = writeBuffer.toByteArray();
        Assertions.assertEquals((byte) 0xA3, mapBytes[0]);

        // a List serializes as a definite array head 0x83 in both modes
        List<Integer> list = new ArrayList<>(List.of(1, 2, 3));
        writeBuffer = new HeapWriteBuffer(SIZE);
        DETERMINISTIC_SERIALIZER.serializeCollection(list, Integer.class, writeBuffer);
        Assertions.assertEquals((byte) 0x83, writeBuffer.toByteArray()[0]);

        // a non-List Collection still streams an indefinite array even in
        // deterministic mode, which deviates from RFC 8949 core determinism
        LinkedHashSet<Integer> set = new LinkedHashSet<>(List.of(1, 2, 3));
        writeBuffer = new HeapWriteBuffer(SIZE);
        DETERMINISTIC_SERIALIZER.serializeCollection(set, Integer.class, writeBuffer);
        Assertions.assertEquals((byte) 0x9F, writeBuffer.toByteArray()[0]);
    }

    @Test
    public void testDeterministicEncodesNullFields() {
        BeanEntity entity = new BeanEntity();
        entity.setIntValue(1);
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        DETERMINISTIC_SERIALIZER.serializeMarshallableObject(entity, writeBuffer);
        byte[] deterministic = writeBuffer.toByteArray();
        Assertions.assertArrayEquals(
                hex("a868696e7456616c75650169656e756d56616c7565f6696c6f6e6756616c7565f6" +
                        "6b737472696e674172726179f66b737472696e6756616c7565f66c646563696d616c56616c7565f6" +
                        "6d6265616e456e746974794d6170f67563626f725072696d6974697665547970654c697374f6"),
                deterministic);
        Assertions.assertEquals(7, countF6(deterministic));

        // default encoding omits null fields, so they disappear entirely
        writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeMarshallableObject(entity, writeBuffer);
        Assertions.assertArrayEquals(hex("bf68696e7456616c756501ff"), writeBuffer.toByteArray());
        Assertions.assertEquals(0, countF6(writeBuffer.toByteArray()));
    }

    @Test
    public void testDeterministicEncodesNullMapValues() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("b", null);
        map.put("a", 1);
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        DETERMINISTIC_SERIALIZER.serializeMap(map, String.class, Integer.class, writeBuffer);
        Assertions.assertArrayEquals(hex("a26161016162f6"), writeBuffer.toByteArray());
    }

    @Test
    public void testDeterministicRoundTripScrambledMapOrderBean() {
        BeanEntity entity = createBeanEntity();
        Map<String, BeanEntity> scrambled = new LinkedHashMap<>();
        BeanEntity k2 = new BeanEntity();
        k2.setIntValue(2);
        BeanEntity k1 = new BeanEntity();
        k1.setIntValue(1);
        scrambled.put("key2", k2);
        scrambled.put("key1", k1);
        entity.setBeanEntityMap(scrambled);
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        DETERMINISTIC_SERIALIZER.serializeMarshallableObject(entity, writeBuffer);
        BeanEntity roundTripped = DETERMINISTIC_DESERIALIZER.deserializeMarshallableObject(
                BeanEntity.class, new HeapReadBuffer(writeBuffer.toByteArray()));
        Assertions.assertEquals(entity, roundTripped);
    }
}