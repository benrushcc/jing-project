package io.jingproject.marshallcbortest;

import io.jingproject.marshallcbor.CborUtf8Validator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;

// utf-8 validation tests for both the heap and the segment entry points.
// every case is cross-checked against the scalar implementations so vector and
// scalar paths must agree, and both must match the expected verdict.
public class CborUtf8ValidationTest {

    private static void checkBytes(byte[] bytes, boolean expected) {
        checkBytes(bytes, 0, bytes.length, expected);
    }

    private static void checkBytes(byte[] bytes, int from, int to, boolean expected) {
        boolean heap = CborUtf8Validator.validateHeap(bytes, from, to);
        boolean scalarHeap = CborUtf8Validator.scalarValidateHeap(bytes, from, to);
        Assertions.assertEquals(scalarHeap, heap);
        Assertions.assertEquals(expected, heap);

        MemorySegment segment = MemorySegment.ofArray(bytes);
        boolean seg = CborUtf8Validator.validateSegment(segment, from, to);
        boolean scalarSeg = CborUtf8Validator.scalarValidateSegment(segment, from, to);
        Assertions.assertEquals(scalarSeg, seg);
        Assertions.assertEquals(expected, seg);
        Assertions.assertEquals(heap, seg);
    }

    @Test
    public void testValidateHeapValidAscii() {
        checkBytes("hello".getBytes(StandardCharsets.UTF_8), true);
        checkBytes("hello world !@#$%^&*()".getBytes(StandardCharsets.UTF_8), true);
        checkBytes(new byte[0], true);
    }

    @Test
    public void testValidateHeapValidChinese() {
        checkBytes("中".getBytes(StandardCharsets.UTF_8), true);
        checkBytes("你好".getBytes(StandardCharsets.UTF_8), true);
    }

    @Test
    public void testValidateHeapValidEmoji() {
        // the smiley is a supplementary character encoded as a surrogate pair
        checkBytes("😀".getBytes(StandardCharsets.UTF_8), true);
        checkBytes("hello中😀".getBytes(StandardCharsets.UTF_8), true);
    }

    @Test
    public void testValidateHeapInvalidShortLeadAtEnd() {
        checkBytes(new byte[]{(byte) 0x41, (byte) 0xC2}, false);
        checkBytes(new byte[]{(byte) 0xC2}, false);
        checkBytes(new byte[]{(byte) 0x41, (byte) 0x42, (byte) 0xC3}, false);
    }

    @Test
    public void testValidateHeapInvalidOverlong() {
        // U+0000 illegally encoded as 0xc0 0x80
        checkBytes(new byte[]{(byte) 0xC0, (byte) 0x80}, false);
    }

    @Test
    public void testValidateHeapInvalidSurrogate() {
        // isolated surrogate encoded in utf-8 (0xed 0xa0 0x80)
        checkBytes(new byte[]{(byte) 0xED, (byte) 0xA0, (byte) 0x80}, false);
        checkBytes(new byte[]{(byte) 0x41, (byte) 0xED, (byte) 0xA0, (byte) 0x80, (byte) 0x41}, false);
    }

    @Test
    public void testValidateHeapInvalidTooLarge() {
        // codepoint 0x110000 exceeds the utf-8 upper bound 0x10ffff
        checkBytes(new byte[]{(byte) 0xF4, (byte) 0x90, (byte) 0x80, (byte) 0x80}, false);
    }

    @Test
    public void testValidateHeapInvalidMissingContinuation() {
        checkBytes(new byte[]{(byte) 0xE4, (byte) 0xB8}, false);
        checkBytes(new byte[]{(byte) 0xF0, (byte) 0x9F}, false);
    }

    @Test
    public void testValidateRange() {
        // the bad surrogate bytes sit outside the validated sub-range
        byte[] bytes = {(byte) 0xED, (byte) 0xA0, (byte) 0x80, (byte) 0x61, (byte) 0x62};
        checkBytes(bytes, false);
        checkBytes(bytes, 3, 5, true);
    }

    @Test
    public void testValidateSegmentValid() {
        MemorySegment segment = MemorySegment.ofArray("中😀abc".getBytes(StandardCharsets.UTF_8));
        Assertions.assertTrue(CborUtf8Validator.validateSegment(segment, 0L, segment.byteSize()));
        Assertions.assertTrue(CborUtf8Validator.validateSegment(segment, 7L, segment.byteSize()));
    }

    @Test
    public void testValidateSegmentInvalid() {
        MemorySegment segment = MemorySegment.ofArray(new byte[]{(byte) 0x41, (byte) 0xC0, (byte) 0x80});
        Assertions.assertFalse(CborUtf8Validator.validateSegment(segment, 0L, segment.byteSize()));
        MemorySegment bad = MemorySegment.ofArray(new byte[]{(byte) 0x41, (byte) 0xED, (byte) 0xA0, (byte) 0x80});
        Assertions.assertFalse(CborUtf8Validator.validateSegment(bad, 0L, bad.byteSize()));
    }
}