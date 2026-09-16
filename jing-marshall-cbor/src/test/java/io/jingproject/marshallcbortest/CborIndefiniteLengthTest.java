package io.jingproject.marshallcbortest;

import io.jingproject.common.HeapReadBuffer;
import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.marshallcbor.CborDeserializer;
import io.jingproject.marshallcbor.CborDeserializerException;
import io.jingproject.marshallcbor.CborDeserializerOption;
import io.jingproject.marshallcbor.CborSerializer;
import io.jingproject.marshallcbor.CborSerializerOption;
import io.jingproject.marshallcbortest.entity.BeanEntity;
import io.jingproject.marshallcbortest.entity.EnumEntity;
import io.jingproject.marshallcbortest.transformers.BigDecimalTransformer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

// indefinite-length containers (0x9f/0xbf ... 0xff) are accepted for primitive
// arrays and empty maps, while indefinite text/byte strings are rejected and
// the round-based map/collection path mis-consumes the first key/element.
public class CborIndefiniteLengthTest {
    private static final CborDeserializer DESERIALIZER = new CborDeserializer(CborDeserializerOption.defaultOption());
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
    public void testDeserializeIndefiniteArray() {
        int[] array = DESERIALIZER.deserializeIntArray(new HeapReadBuffer(hex("9f010203ff")));
        Assertions.assertArrayEquals(new int[]{1, 2, 3}, array);

        array = DESERIALIZER.deserializeIntArray(new HeapReadBuffer(hex("9f0001020304ff")));
        Assertions.assertArrayEquals(new int[]{0, 1, 2, 3, 4}, array);

        array = DESERIALIZER.deserializeIntArray(new HeapReadBuffer(hex("9fff")));
        Assertions.assertArrayEquals(new int[0], array);
    }

    @Test
    public void testDeserializeIndefiniteLongArray() {
        long[] array = DESERIALIZER.deserializeLongArray(new HeapReadBuffer(hex("9f18641903e8ff")));
        Assertions.assertArrayEquals(new long[]{100L, 1000L}, array);
    }

    @Test
    public void testSerializeIndefiniteCollectionRoundTrips() {
        // a non-List Collection is always written as an indefinite array even in
        // default mode, and the primitive-array fast path can read it back
        LinkedHashSet<Integer> set = new LinkedHashSet<>(List.of(1, 2, 3));
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeCollection(set, Integer.class, writeBuffer);
        Assertions.assertArrayEquals(hex("9f010203ff"), writeBuffer.toByteArray());
        Assertions.assertArrayEquals(new int[]{1, 2, 3},
                DESERIALIZER.deserializeIntArray(new HeapReadBuffer(writeBuffer.toByteArray())));

        LinkedHashSet<Integer> emptySet = new LinkedHashSet<>();
        writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeCollection(emptySet, Integer.class, writeBuffer);
        Assertions.assertArrayEquals(hex("9fff"), writeBuffer.toByteArray());
        Assertions.assertArrayEquals(new int[0],
                DESERIALIZER.deserializeIntArray(new HeapReadBuffer(writeBuffer.toByteArray())));
    }

    @Test
    public void testDeserializeEmptyIndefiniteMap() {
        Map<String, Integer> map = DESERIALIZER.deserializeMap(String.class, Integer.class,
                new HeapReadBuffer(hex("bfff")), HashMap::new);
        Assertions.assertEquals(new HashMap<>(), map);
    }

    @Test
    public void testDeserializeNonEmptyIndefiniteMapRejected() {
        // bf 61 61 01 ff encodes one entry "a" -> 1; the round-based map node
        // swallows the first text head during the round, then treats the key
        // content 0x61 as a new head, mis-aligning the stream on the 0xff break
        CborDeserializerException ex = Assertions.assertThrows(CborDeserializerException.class,
                () -> DESERIALIZER.deserializeMap(String.class, Integer.class,
                        new HeapReadBuffer(hex("bf616101ff")), HashMap::new));
        Assertions.assertEquals("not an integer : -1", ex.getMessage());
    }

    @Test
    public void testDeserializeIndefiniteTextAndByteStringRejected() {
        // 0x7f is an indefinite-length text string inside a definite array
        CborDeserializerException ex = Assertions.assertThrows(CborDeserializerException.class,
                () -> DESERIALIZER.deserializeArray(String.class, new HeapReadBuffer(hex("817f6161ff"))));
        Assertions.assertEquals("indefinite length not supported", ex.getMessage());

        // 0x5f is an indefinite-length byte string
        ex = Assertions.assertThrows(CborDeserializerException.class,
                () -> DESERIALIZER.deserializeByteArray(new HeapReadBuffer(hex("5f4400ff"))));
        Assertions.assertEquals("indefinite length not supported", ex.getMessage());
    }

    @Test
    public void testOrphanBreakInDefiniteContainer() {
        // a stray 0xff break cannot terminate a definite-length array
        CborDeserializerException ex = Assertions.assertThrows(CborDeserializerException.class,
                () -> DESERIALIZER.deserializeIntArray(new HeapReadBuffer(hex("8201ff"))));
        Assertions.assertEquals("not an integer : -1", ex.getMessage());

        // the stray 0xff appears where the second map key must start
        ex = Assertions.assertThrows(CborDeserializerException.class,
                () -> DESERIALIZER.deserializeMap(String.class, Integer.class,
                        new HeapReadBuffer(hex("a2616101ff")), HashMap::new));
        Assertions.assertEquals("not a string start : -1", ex.getMessage());
    }

    @Test
    public void testMultiLevelContainerRoundTrip() {
        // multi-level indefinite-length containers cannot be round-tripped today
        // (the map/collection node defect above), so the round trip relies on
        // deterministic definite-length output
        BeanEntity entity = createBeanEntity();
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        DETERMINISTIC_SERIALIZER.serializeMarshallableObject(entity, writeBuffer);
        BeanEntity roundTripped = DETERMINISTIC_DESERIALIZER.deserializeMarshallableObject(
                BeanEntity.class, new HeapReadBuffer(writeBuffer.toByteArray()));
        Assertions.assertEquals(entity, roundTripped);
    }
}
