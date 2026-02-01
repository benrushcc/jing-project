package io.jingproject.marshall;

import java.lang.foreign.MemorySegment;

public interface MarshallFacade {
    int totalElements();

    int objectElements();

    int primitiveElements();

    int primitiveBytes();

    MarshallInfo elementInfoByIndex(int index);

    MarshallInfo elementInfoByName(String name);

    MarshallInfo elementInfoByUtf8Bytes(byte[] bytes);

    MarshallInfo elementInfoByUtf8Bytes(byte[] bytes, int offset, int len);

    MarshallInfo elementInfoBySegment(MemorySegment segment);

    MarshallInfo elementInfoBySegment(MemorySegment segment, long offset, long len);
}
