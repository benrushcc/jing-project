package io.jingproject.marshall;

import io.jingproject.common.anno.ProcessorApi;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

@ProcessorApi
public interface MarshallFacade {

    Class<?> marshallableType();

    /**
     * returns all marshall infos.
     * The size of the returned list is equal to {@link #totalElements()}.
     */
    List<MarshallInfo> marshallInfos();

    default int totalElements() {
        return marshallInfos().size();
    }

    default int primitiveElements() {
        throw new UnsupportedOperationException();
    }

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
    MarshallInfo marshallInfoByFieldName(byte[] bytes, int from, int to);

    default MarshallInfo marshallInfoByFieldName(MemorySegment segment) {
        return marshallInfoByFieldName(segment, 0L, segment.byteSize());
    }

    /**
     * look up info by a utf8 memory segment slice as field name, null if not found
     */
    MarshallInfo marshallInfoByFieldName(MemorySegment segment, long from, long to);

    default MarshallInfo marshallInfoByFieldName(byte[] bytes, Charset charset) {
        String name = new String(bytes, charset);
        return marshallInfoByFieldName(name);
    }

    default MarshallInfo marshallInfoByFieldName(byte[] bytes, int from, int to, Charset charset) {
        Objects.checkFromToIndex(from, to, bytes.length);
        String name = new String(bytes, from, to - from, charset);
        return marshallInfoByFieldName(name);
    }

    default MarshallInfo marshallInfoByFieldName(MemorySegment segment, Charset charset) {
        byte[] bytes = segment.toArray(ValueLayout.JAVA_BYTE);
        String name = new String(bytes, charset);
        return marshallInfoByFieldName(name);
    }

    default MarshallInfo marshallInfoByFieldName(MemorySegment segment, long from, long to, Charset charset) {
        Objects.checkFromToIndex(from, to, segment.byteSize());
        byte[] bytes = segment.asSlice(from, to - from).toArray(ValueLayout.JAVA_BYTE);
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
    MarshallInfo marshallInfoByMappedName(byte[] bytes, int from, int to);

    default MarshallInfo marshallInfoByMappedName(MemorySegment segment) {
        return marshallInfoByMappedName(segment, 0L, segment.byteSize());
    }

    /**
     * look up info by a utf8 memory segment slice as mapped name, null if not found
     */
    MarshallInfo marshallInfoByMappedName(MemorySegment segment, long from, long to);

    default MarshallInfo marshallInfoByMappedName(byte[] bytes, Charset charset) {
        String mappedName = new String(bytes, charset);
        return marshallInfoByMappedName(mappedName);
    }

    default MarshallInfo marshallInfoByMappedName(byte[] bytes, int from, int to, Charset charset) {
        Objects.checkFromToIndex(from, to, bytes.length);
        String mappedName = new String(bytes, from, to - from, charset);
        return marshallInfoByMappedName(mappedName);
    }

    default MarshallInfo marshallInfoByMappedName(MemorySegment segment, Charset charset) {
        byte[] bytes = segment.toArray(ValueLayout.JAVA_BYTE);
        String mappedName = new String(bytes, charset);
        return marshallInfoByMappedName(mappedName);
    }

    default MarshallInfo marshallInfoByMappedName(MemorySegment segment, long from, long to, Charset charset) {
        Objects.checkFromToIndex(from, to, segment.byteSize());
        byte[] bytes = segment.asSlice(from, to - from).toArray(ValueLayout.JAVA_BYTE);
        String mappedName = new String(bytes, charset);
        return marshallInfoByMappedName(mappedName);
    }

    default boolean readBoolean(Object instance, int index) {
        throw new UnsupportedOperationException();
    }

    default byte readByte(Object instance, int index) {
        throw new UnsupportedOperationException();
    }

    default short readShort(Object instance, int index) {
        throw new UnsupportedOperationException();
    }

    default char readChar(Object instance, int index) {
        throw new UnsupportedOperationException();
    }

    default int readInt(Object instance, int index) {
        throw new UnsupportedOperationException();
    }

    default long readLong(Object instance, int index) {
        throw new UnsupportedOperationException();
    }

    default float readFloat(Object instance, int index) {
        throw new UnsupportedOperationException();
    }

    default double readDouble(Object instance, int index) {
        throw new UnsupportedOperationException();
    }

    default Object readObject(Object instance, int index) {
        throw new UnsupportedOperationException();
    }

    /**
     * create a new empty builder for building an object,
     * not supported for enum types
     */
    default MarshallBuilder newBuilder() {
        throw new UnsupportedOperationException();
    }

    /**
     * construct a new instance from the writer's data,
     * not supported for enum types, throws IllegalArgumentException if writer type mismatch
     */
    default Object construct(MarshallBuilder writer) {
        throw new UnsupportedOperationException();
    }

}
