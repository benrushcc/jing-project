package io.jingproject.marshall;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.Charset;
import java.util.Objects;

public interface MarshallFacade {
    int totalElements();

    int objectElements();

    int primitiveElements();

    int primitiveBytes();

    MarshallInfo elementInfoByIndex(int index);

    MarshallInfo elementInfoByName(String name);

    default MarshallInfo elementInfoByUtf8Bytes(byte[] bytes) {
        return elementInfoByUtf8Bytes(bytes, 0, bytes.length);
    }

    MarshallInfo elementInfoByUtf8Bytes(byte[] bytes, int offset, int len);

    default MarshallInfo elementInfoByUtf8Segment(MemorySegment segment) {
        return elementInfoByUtf8Segment(segment, 0L, segment.byteSize());
    }

    MarshallInfo elementInfoByUtf8Segment(MemorySegment segment, long offset, long len);

    default MarshallInfo elementInfoByBytesWithCharset(byte[] bytes, Charset charset) {
        assert bytes != null && charset != null;
        String name = new String(bytes, charset);
        return elementInfoByName(name);
    }

    default MarshallInfo elementInfoByBytesWithCharset(byte[] bytes, int offset, int len, Charset charset) {
        assert bytes != null && Objects.checkFromIndexSize(offset, bytes.length, len) >= 0 && charset != null;
        String name = new String(bytes, offset, len, charset);
        return elementInfoByName(name);
    }

    default MarshallInfo elementInfoBySegmentWithCharset(MemorySegment segment, Charset charset) {
        assert segment != null && charset != null;
        byte[] bytes = segment.toArray(ValueLayout.JAVA_BYTE);
        String name = new String(bytes, charset);
        return elementInfoByName(name);
    }

    default MarshallInfo elementInfoBySegmentWithCharset(MemorySegment segment, long offset, long len, Charset charset) {
        assert segment != null && Objects.checkFromIndexSize(offset, segment.byteSize(), len) >= 0L && charset != null;
        byte[] bytes = segment.asSlice(offset, len).toArray(ValueLayout.JAVA_BYTE);
        String name = new String(bytes, charset);
        return elementInfoByName(name);
    }
}
