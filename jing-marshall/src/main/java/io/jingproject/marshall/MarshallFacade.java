package io.jingproject.marshall;

import io.jingproject.common.anno.ProcessorApi;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.Charset;
import java.util.Objects;

@ProcessorApi
public interface MarshallFacade {

    Class<?> marshallableType();

    int totalElements();

    default int primitiveElements() {
        throw new UnsupportedOperationException();
    }

    /**
     * get marshall info by index, index must be in [0, totalElements),
     * throws IndexOutOfBoundsException if out of range
     */
    MarshallInfo marshallInfoByIndex(int index);

    /**
     * look up info by the original java field name, null if not found
     */
    MarshallInfo marshallInfoByFieldName(String fieldName);

    default MarshallInfo marshallInfoByFieldName(byte[] bytes) {
        return marshallInfoByFieldName(bytes, 0, bytes.length);
    }

    /**
     * look up info by a utf8 byte array slice as field name, null if not found
     */
    MarshallInfo marshallInfoByFieldName(byte[] bytes, int offset, int len);

    default MarshallInfo marshallInfoByFieldName(MemorySegment segment) {
        return marshallInfoByFieldName(segment, 0L, segment.byteSize());
    }

    /**
     * look up info by a utf8 memory segment slice as field name, null if not found
     */
    MarshallInfo marshallInfoByFieldName(MemorySegment segment, long offset, long len);

    default MarshallInfo marshallInfoByFieldName(byte[] bytes, Charset charset) {
        
        String name = new String(bytes, charset);
        return marshallInfoByFieldName(name);
    }

    default MarshallInfo marshallInfoByFieldName(byte[] bytes, int offset, int len, Charset charset) {
        
        String name = new String(bytes, offset, len, charset);
        return marshallInfoByFieldName(name);
    }

    default MarshallInfo marshallInfoByFieldName(MemorySegment segment, Charset charset) {
        
        byte[] bytes = segment.toArray(ValueLayout.JAVA_BYTE);
        String name = new String(bytes, charset);
        return marshallInfoByFieldName(name);
    }

    default MarshallInfo marshallInfoByFieldName(MemorySegment segment, long offset, long len, Charset charset) {
        
        byte[] bytes = segment.asSlice(offset, len).toArray(ValueLayout.JAVA_BYTE);
        String name = new String(bytes, charset);
        return marshallInfoByFieldName(name);
    }

    /**
     * look up info by the original java mapped name, null if not found
     */
    MarshallInfo marshallInfoByMappedName(String mappedName);

    default MarshallInfo marshallInfoByMappedName(byte[] bytes) {
        return marshallInfoByMappedName(bytes, 0, bytes.length);
    }

    /**
     * look up info by a utf8 byte array slice as mapped name, null if not found
     */
    MarshallInfo marshallInfoByMappedName(byte[] bytes, int offset, int len);

    default MarshallInfo marshallInfoByMappedName(MemorySegment segment) {
        return marshallInfoByMappedName(segment, 0L, segment.byteSize());
    }

    /**
     * look up info by a utf8 memory segment slice as mapped name, null if not found
     */
    MarshallInfo marshallInfoByMappedName(MemorySegment segment, long offset, long len);

    default MarshallInfo marshallInfoByMappedName(byte[] bytes, Charset charset) {
        
        String mappedName = new String(bytes, charset);
        return marshallInfoByMappedName(mappedName);
    }

    default MarshallInfo marshallInfoByMappedName(byte[] bytes, int offset, int len, Charset charset) {
        
        String mappedName = new String(bytes, offset, len, charset);
        return marshallInfoByMappedName(mappedName);
    }

    default MarshallInfo marshallInfoByMappedName(MemorySegment segment, Charset charset) {
        
        byte[] bytes = segment.toArray(ValueLayout.JAVA_BYTE);
        String mappedName = new String(bytes, charset);
        return marshallInfoByMappedName(mappedName);
    }

    default MarshallInfo marshallInfoByMappedName(MemorySegment segment, long offset, long len, Charset charset) {
        
        byte[] bytes = segment.asSlice(offset, len).toArray(ValueLayout.JAVA_BYTE);
        String mappedName = new String(bytes, charset);
        return marshallInfoByMappedName(mappedName);
    }

    /**
     * create a reader for extracting values from the given target,
     * not supported for enum types, throw IllegalArgumentException if target type mismatch
     */
    default MarshallReader newReader(Object target) {
        throw new UnsupportedOperationException();
    }

    /**
     * create a new empty writer for building an object,
     * not supported for enum types
     */
    default MarshallWriter newWriter() {
        throw new UnsupportedOperationException();
    }

    /**
     * construct a new instance from the writer's data,
     * not supported for enum types, throw IllegalArgumentException if writer type mismatch
     */
    default Object construct(MarshallWriter writer) {
        throw new UnsupportedOperationException();
    }

}
