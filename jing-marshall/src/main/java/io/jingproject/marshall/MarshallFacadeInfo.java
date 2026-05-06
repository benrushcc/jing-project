package io.jingproject.marshall;

import io.jingproject.common.anno.ProcessorApi;
import io.jingproject.marshall.hash.HashUtil;
import io.jingproject.marshall.hash.Hasher;

import java.lang.foreign.MemorySegment;
import java.util.Arrays;
import java.util.List;

@ProcessorApi
public record MarshallFacadeInfo(
        List<MarshallInfo> infos,
        Hasher fieldNameHasher,
        byte[] fieldNameBytes,
        MemorySegment fieldNameSegment,
        Hasher mappedNameHasher,
        byte[] mappedNameBytes,
        MemorySegment mappedNameSegment
) {

    public MarshallFacadeInfo(List<MarshallInfo> infos) {
        List<String> fieldNames = infos.stream().map(MarshallInfo::fieldName).toList();
        Hasher fh = HashUtil.calcHasher(fieldNames);
        byte[] fb = HashUtil.calcBytes(fieldNames);
        MemorySegment fs = MemorySegment.ofArray(fb).asReadOnly();
        List<String> mappedNames = infos.stream().map(MarshallInfo::mappedName).toList();
        Hasher mh = HashUtil.calcHasher(mappedNames);
        byte[] mb = HashUtil.calcBytes(mappedNames);
        MemorySegment ms = MemorySegment.ofArray(mb).asReadOnly();
        this(infos, fh, fb, fs, mh, mb, ms);
    }

    public boolean fieldNameEquals(int fieldOffset, int fieldLen, byte[] bytes, int offset, int len) {
        return Arrays.equals(fieldNameBytes, fieldOffset, fieldLen, bytes, offset, len);
    }

    public boolean fieldNameEquals(long fieldOffset, long fieldLen, MemorySegment segment, long offset, long len) {
        return MemorySegment.mismatch(fieldNameSegment, fieldOffset, fieldLen, segment, offset, len) == -1L;
    }

    public boolean mappedNameEquals(int mappedOffset, int mappedLen, byte[] bytes, int offset, int len) {
        return Arrays.equals(mappedNameBytes, mappedOffset, mappedLen, bytes, offset, len);
    }

    public boolean mappedNameEquals(long fieldOffset, long fieldLen, MemorySegment segment, long offset, long len) {
        return MemorySegment.mismatch(mappedNameSegment, fieldOffset, fieldLen, segment, offset, len) == -1L;
    }
}
