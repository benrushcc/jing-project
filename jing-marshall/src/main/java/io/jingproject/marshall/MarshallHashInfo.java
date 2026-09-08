package io.jingproject.marshall;

import io.jingproject.common.anno.ProcessorApi;
import io.jingproject.marshall.hash.HashUtil;
import io.jingproject.marshall.hash.Hasher;

import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@ProcessorApi
public record MarshallHashInfo (
        Hasher fieldNameUtf8Hasher,
        byte[] fieldNameCompactUtf8Bytes,
        MemorySegment fieldNameCompactUtf8Segment,
        Hasher mappedNameUtf8Hasher,
        byte[] mappedNameCompactUtf8Bytes,
        MemorySegment mappedNameCompactUtf8Segment
) {

    public MarshallHashInfo(List<MarshallInfo> infos, int fieldHasherIndex, int mappedHasherIndex) {
        Hasher fh = HashUtil.hasher(fieldHasherIndex);
        byte[] fb = HashUtil.compactUtf8Bytes(infos, marshallInfo -> marshallInfo.fieldName().getBytes(StandardCharsets.UTF_8));
        MemorySegment fs = MemorySegment.ofArray(fb).asReadOnly();
        Hasher mh = HashUtil.hasher(mappedHasherIndex);
        byte[] mb = HashUtil.compactUtf8Bytes(infos, marshallInfo -> marshallInfo.mappedName().getBytes(StandardCharsets.UTF_8));
        MemorySegment ms = MemorySegment.ofArray(mb).asReadOnly();
        this(fh, fb, fs, mh, mb, ms);
    }

    // for test purpose only
    public MarshallHashInfo(List<MarshallInfo> infos) {
        int fieldHasherIndex = HashUtil.selectUtf8Hasher(infos, marshallInfo -> marshallInfo.fieldName().getBytes(StandardCharsets.UTF_8));
        int mappedHasherIndex = HashUtil.selectUtf8Hasher(infos, marshallInfo -> marshallInfo.mappedName().getBytes(StandardCharsets.UTF_8));
        this(infos, fieldHasherIndex, mappedHasherIndex);
    }

    // from and to are guaranteed to be in bounds by the hasher; no additional validation needed.
    public boolean fieldNameEquals(int srcFrom, int srcTo, byte[] dstBytes, int dstFrom, int dstTo) {
        return Arrays.equals(fieldNameCompactUtf8Bytes, srcFrom, srcTo, dstBytes, dstFrom, dstTo);
    }

    public boolean fieldNameEquals(long srcFrom, long srcTo, MemorySegment dstSegment, long dstFrom, long dstTo) {
        return MemorySegment.mismatch(fieldNameCompactUtf8Segment, srcFrom, srcTo, dstSegment, dstFrom, dstTo) == -1L;
    }

    public boolean mappedNameEquals(int srcFrom, int srcTo, byte[] dstBytes, int dstFrom, int dstTo) {
        return Arrays.equals(mappedNameCompactUtf8Bytes, srcFrom, srcTo, dstBytes, dstFrom, dstTo);
    }

    public boolean mappedNameEquals(long srcFrom, long srcTo, MemorySegment dstSegment, long dstFrom, long dstTo) {
        return MemorySegment.mismatch(mappedNameCompactUtf8Segment, srcFrom, srcTo, dstSegment, dstFrom, dstTo) == -1L;
    }
}
