package io.jingproject.logtest;

import io.jingproject.bindings.alloc.Allocator;
import io.jingproject.log.RegionOut;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

// utf-8 encoder tests: a lone surrogate must render as the U+FFFD replacement
// char exactly like append(...) does, while a valid surrogate pair keeps its
// 4-byte form
public class RegionOutTest {
    private static byte[] written(RegionOut out, MemorySegment region) {
        byte[] bytes = new byte[(int) out.written()];
        MemorySegment.copy(region, ValueLayout.JAVA_BYTE, 0, bytes, 0, bytes.length);
        return bytes;
    }

    @Test
    public void testWriteUtf8ReplacesLoneSurrogates() {
        try (Allocator alloc = Allocator.newAlloc()) {
            MemorySegment region = alloc.allocate(32);
            RegionOut out = new RegionOut(region);
            out.writeUtf8("\uD83D");
            out.writeUtf8("\uDE00");
            Assertions.assertArrayEquals(
                    new byte[]{(byte) 0xEF, (byte) 0xBF, (byte) 0xBD, (byte) 0xEF, (byte) 0xBF, (byte) 0xBD},
                    written(out, region));
        }
    }

    @Test
    public void testWriteUtf8PreservesValidSurrogatePair() {
        try (Allocator alloc = Allocator.newAlloc()) {
            MemorySegment region = alloc.allocate(32);
            RegionOut out = new RegionOut(region);
            out.writeUtf8("\uD83D\uDE80");
            Assertions.assertArrayEquals(
                    new byte[]{(byte) 0xF0, (byte) 0x9F, (byte) 0x9A, (byte) 0x80},
                    written(out, region));
        }
    }
}