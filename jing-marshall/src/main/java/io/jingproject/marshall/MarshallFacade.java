package io.jingproject.marshall;

import io.jingproject.common.Utils;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.Charset;
import java.util.Objects;

public interface MarshallFacade {

    String marshallableName();

    MethodHandle constructor();

    int objectElements();

    int primitiveElements();

    default int totalElements() {
        return Math.addExact(objectElements(), primitiveElements());
    }

    int primitiveBytes();

    MarshallInfo marshallInfoByIndex(int index);

    MarshallInfo marshallInfoByFieldName(String fieldName);

    default MarshallInfo marshallInfoByFieldName(byte[] bytes) {
        return marshallInfoByFieldName(bytes, 0, bytes.length);
    }

    MarshallInfo marshallInfoByFieldName(byte[] bytes, int offset, int len);

    default MarshallInfo marshallInfoByFieldName(MemorySegment segment) {
        return marshallInfoByFieldName(segment, 0L, segment.byteSize());
    }

    MarshallInfo marshallInfoByFieldName(MemorySegment segment, long offset, long len);

    default MarshallInfo marshallInfoByFieldName(byte[] bytes, Charset charset) {
        assert bytes != null && charset != null;
        String name = new String(bytes, charset);
        return marshallInfoByFieldName(name);
    }

    default MarshallInfo marshallInfoByFieldName(byte[] bytes, int offset, int len, Charset charset) {
        assert bytes != null && Objects.checkFromIndexSize(offset, len, bytes.length) >= 0 && charset != null;
        String name = new String(bytes, offset, len, charset);
        return marshallInfoByFieldName(name);
    }

    default MarshallInfo marshallInfoByFieldName(MemorySegment segment, Charset charset) {
        assert segment != null && charset != null;
        byte[] bytes = segment.toArray(ValueLayout.JAVA_BYTE);
        String name = new String(bytes, charset);
        return marshallInfoByFieldName(name);
    }

    default MarshallInfo marshallInfoByFieldName(MemorySegment segment, long offset, long len, Charset charset) {
        assert segment != null && Objects.checkFromIndexSize(offset, len, segment.byteSize()) >= 0L && charset != null;
        byte[] bytes = segment.asSlice(offset, len).toArray(ValueLayout.JAVA_BYTE);
        String name = new String(bytes, charset);
        return marshallInfoByFieldName(name);
    }

    MarshallInfo marshallInfoByMappedName(String mappedName);

    default MarshallInfo marshallInfoByMappedName(byte[] bytes) {
        return marshallInfoByMappedName(bytes, 0, bytes.length);
    }

    MarshallInfo marshallInfoByMappedName(byte[] bytes, int offset, int len);

    default MarshallInfo marshallInfoByMappedName(MemorySegment segment) {
        return marshallInfoByMappedName(segment, 0L, segment.byteSize());
    }

    MarshallInfo marshallInfoByMappedName(MemorySegment segment, long offset, long len);

    default MarshallInfo marshallInfoByMappedName(byte[] bytes, Charset charset) {
        assert bytes != null && charset != null;
        String mappedName = new String(bytes, charset);
        return marshallInfoByMappedName(mappedName);
    }

    default MarshallInfo marshallInfoByMappedName(byte[] bytes, int offset, int len, Charset charset) {
        assert bytes != null && Objects.checkFromIndexSize(offset, len, bytes.length) >= 0 && charset != null;
        String mappedName = new String(bytes, offset, len, charset);
        return marshallInfoByMappedName(mappedName);
    }

    default MarshallInfo marshallInfoByMappedName(MemorySegment segment, Charset charset) {
        assert segment != null && charset != null;
        byte[] bytes = segment.toArray(ValueLayout.JAVA_BYTE);
        String mappedName = new String(bytes, charset);
        return marshallInfoByMappedName(mappedName);
    }

    default MarshallInfo marshallInfoByMappedName(MemorySegment segment, long offset, long len, Charset charset) {
        assert segment != null && Objects.checkFromIndexSize(offset, len, segment.byteSize()) >= 0L && charset != null;
        byte[] bytes = segment.asSlice(offset, len).toArray(ValueLayout.JAVA_BYTE);
        String mappedName = new String(bytes, charset);
        return marshallInfoByMappedName(mappedName);
    }

    default MarshallSchema newSchema() {
        Object[] objectArray = Utils.emptyObjectArray();
        int objectElements = objectElements();
        if(objectElements > 0) {
            objectArray = new Object[objectElements];
        }
        byte[] primitiveArray = Utils.emptyByteArray();
        int primitiveElements = primitiveElements();
        if(primitiveElements > 0) {
            primitiveArray = new byte[primitiveElements];
        }
        return new MarshallSchema(objectArray, primitiveArray, 0, 0, objectElements, primitiveElements);
    }

}
