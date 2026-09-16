package io.jingproject.marshallcbortest;

import io.jingproject.common.HeapReadBuffer;
import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.marshallcbor.CborDeserializer;
import io.jingproject.marshallcbor.CborDeserializerOption;
import io.jingproject.marshallcbor.CborSerializer;
import io.jingproject.marshallcbor.CborSerializerOption;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

// text string (major 3) and byte string (major 2) tests with exact head byte
// assertions; text heads use 0x60|length and byte-string heads use 0x40|length
// for lengths of 23 or fewer bytes.
public class CborStringSerializationTest {
    private static final CborSerializer SERIALIZER = new CborSerializer(CborSerializerOption.defaultOption());
    private static final CborDeserializer DESERIALIZER = new CborDeserializer(CborDeserializerOption.defaultOption());
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

    @Test
    public void testSerializeStringArrayAscii() {
        String[] arr = {"hello", "world"};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeArray(arr, writeBuffer);
        Assertions.assertArrayEquals(hex("826568656c6c6f65776f726c64"), writeBuffer.toByteArray());
    }

    @Test
    public void testSerializeStringArrayChinese() {
        String[] arr = {"中", "你好"};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeArray(arr, writeBuffer);
        Assertions.assertArrayEquals(hex("8263e4b8ad66e4bda0e5a5bd"), writeBuffer.toByteArray());
    }

    @Test
    public void testSerializeStringArrayEmoji() {
        String[] arr = {"😀", "€"};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeArray(arr, writeBuffer);
        Assertions.assertArrayEquals(hex("8264f09f988063e282ac"), writeBuffer.toByteArray());
    }

    @Test
    public void testSerializeStringArrayMixed() {
        String[] arr = {"abc", "中", "", "😀"};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeArray(arr, writeBuffer);
        Assertions.assertArrayEquals(hex("846361626363e4b8ad6064f09f9880"), writeBuffer.toByteArray());
    }

    @Test
    public void testSerializeStringArrayEmptyElements() {
        String[] arr = {"", ""};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeArray(arr, writeBuffer);
        Assertions.assertArrayEquals(hex("826060"), writeBuffer.toByteArray());
    }

    @Test
    public void testSerializeLongStringHead() {
        // 24-byte text needs the ai 24 head: 0x78 followed by a 1-byte length
        String[] arr = {"x".repeat(24)};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeArray(arr, writeBuffer);
        Assertions.assertArrayEquals(hex("817818" + "78".repeat(24)), writeBuffer.toByteArray());
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
    public void testSerializeLongByteArrayHead() {
        // 24-byte byte string needs the ai 24 head: 0x58 followed by a 1-byte length
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeByteArray(new byte[24], writeBuffer);
        Assertions.assertArrayEquals(hex("5818" + "00".repeat(24)), writeBuffer.toByteArray());
    }

    @Test
    public void testDeserializeStringArray() {
        String[] actual = DESERIALIZER.deserializeArray(String.class,
                new HeapReadBuffer(hex("846361626363e4b8ad6064f09f9880")));
        Assertions.assertArrayEquals(new String[]{"abc", "中", "", "😀"}, actual);
    }

    @Test
    public void testDeserializeByteArray() {
        byte[] actual = DESERIALIZER.deserializeByteArray(new HeapReadBuffer(hex("4400ff7f80")));
        Assertions.assertArrayEquals(new byte[]{0, -1, 127, -128}, actual);

        actual = DESERIALIZER.deserializeByteArray(new HeapReadBuffer(hex("4b68656c6c6f20776f726c64")));
        Assertions.assertArrayEquals("hello world".getBytes(StandardCharsets.UTF_8), actual);

        actual = DESERIALIZER.deserializeByteArray(new HeapReadBuffer(hex("5818" + "00".repeat(24))));
        Assertions.assertArrayEquals(new byte[24], actual);
    }

    @Test
    public void testStringRoundTrip() {
        String[] expected = {"中文", "你好世界 🎉", "", "abc中文", "😀"};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeArray(expected, writeBuffer);
        String[] actual = DESERIALIZER.deserializeArray(String.class, new HeapReadBuffer(writeBuffer.toByteArray()));
        Assertions.assertArrayEquals(expected, actual);
    }

    @Test
    public void testByteArrayRoundTrip() {
        byte[] expected = {0, -1, 1, 127, -128, 0};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeByteArray(expected, writeBuffer);
        byte[] actual = DESERIALIZER.deserializeByteArray(new HeapReadBuffer(writeBuffer.toByteArray()));
        Assertions.assertArrayEquals(expected, actual);
    }
}