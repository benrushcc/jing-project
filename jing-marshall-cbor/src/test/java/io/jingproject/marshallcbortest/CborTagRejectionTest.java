package io.jingproject.marshallcbortest;

import io.jingproject.common.HeapReadBuffer;
import io.jingproject.marshallcbor.CborDeserializer;
import io.jingproject.marshallcbor.CborDeserializerException;
import io.jingproject.marshallcbor.CborDeserializerOption;
import io.jingproject.marshallcbortest.entity.BeanEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

// RFC 8949 tags (major type 6) are not modelled by this format: an unknown
// field value first byte of major 6 throws "tag not supported", while tag heads
// appearing where an array, object or number is expected surface the usual
// type-mismatch messages with the signed byte value.
public class CborTagRejectionTest {
    private static final CborDeserializer DESERIALIZER = new CborDeserializer(CborDeserializerOption.defaultOption());

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

    @Test
    public void testRejectTopLevelTagStarts() {
        // 0xc0 is major type 6 with ai 0; read as a signed byte it is -64
        CborDeserializerException ex = Assertions.assertThrows(CborDeserializerException.class,
                () -> DESERIALIZER.deserializeIntArray(new HeapReadBuffer(hex("c0"))));
        Assertions.assertEquals("not an array start : -64", ex.getMessage());

        ex = Assertions.assertThrows(CborDeserializerException.class,
                () -> DESERIALIZER.deserializeMap(String.class, Integer.class,
                        new HeapReadBuffer(hex("c0")), HashMap::new));
        Assertions.assertEquals("not an object start : -64", ex.getMessage());

        ex = Assertions.assertThrows(CborDeserializerException.class,
                () -> DESERIALIZER.deserializeMarshallableObject(BeanEntity.class, new HeapReadBuffer(hex("c0"))));
        Assertions.assertEquals("not an object start : -64", ex.getMessage());
    }

    @Test
    public void testRejectOtherMajorSixStarts() {
        // tag head variants c1 (ai 1), db (ai 27) and d9 (ai 25) are all major 6
        int[] signedBytes = {-63, -37, -39};
        String[] payloads = {"c1", "db", "d9d9f7"};
        for (int i = 0; i < payloads.length; i++) {
            String payload = payloads[i];
            CborDeserializerException ex = Assertions.assertThrows(CborDeserializerException.class,
                    () -> DESERIALIZER.deserializeIntArray(new HeapReadBuffer(hex(payload))));
            Assertions.assertEquals("not an array start : " + signedBytes[i], ex.getMessage());
        }
    }

    @Test
    public void testRejectTagInUnknownFieldValue() {
        // a1 67 756e6b6e6f776e is a one-entry object with the unknown key
        // "unknown"; the following tag head reaches the dummy value handler
        String[] tagPayloads = {"c0", "c1", "db", "d9d9f7"};
        for (String tag : tagPayloads) {
            CborDeserializerException ex = Assertions.assertThrows(CborDeserializerException.class,
                    () -> DESERIALIZER.deserializeMarshallableObject(BeanEntity.class,
                            new HeapReadBuffer(hex("a167756e6b6e6f776e" + tag))));
            Assertions.assertEquals("tag not supported", ex.getMessage());
        }
    }

    @Test
    public void testRejectNestedTagInArrayElement() {
        // a definite array of 2 whose second element starts with a tag head
        CborDeserializerException ex = Assertions.assertThrows(CborDeserializerException.class,
                () -> DESERIALIZER.deserializeIntArray(new HeapReadBuffer(hex("8201c0"))));
        Assertions.assertEquals("not an integer : -64", ex.getMessage());
    }
}