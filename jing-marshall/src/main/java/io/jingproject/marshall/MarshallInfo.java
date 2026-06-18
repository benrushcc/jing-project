package io.jingproject.marshall;

import io.jingproject.common.anno.ProcessorApi;

import java.nio.charset.StandardCharsets;

@ProcessorApi
public record MarshallInfo(
        Class<?> rawType,
        Class<?> firstGenericType,
        Class<?> secondGenericType,
        int index,
        String fieldName,
        byte[] fieldNameUtf8Bytes,
        String mappedName,
        byte[] mappedNameUtf8Bytes,
        Enum<?> enumValue,
        long flags // currently 48 + 4 = 52 bits used
) {

    public MarshallInfo {
        if(rawType == null) {
            throw new IllegalArgumentException("rawType cannot be null");
        }
        if(rawType == void.class || firstGenericType == void.class || secondGenericType == void.class) {
            throw new IllegalArgumentException("type cannot be void");
        }
        if(fieldName == null || fieldName.isBlank() || fieldNameUtf8Bytes == null || fieldNameUtf8Bytes.length == 0) {
            throw new IllegalArgumentException("fieldName cannot be blank");
        }
        if(mappedName == null || mappedName.isBlank() || mappedNameUtf8Bytes == null || mappedNameUtf8Bytes.length == 0) {
            throw new IllegalArgumentException("mappedName cannot be blank");
        }
    }

    public MarshallInfo(Class<?> rawType,
                        Class<?> firstGenericType,
                        Class<?> secondGenericType,
                        int index,
                        String fieldName,
                        String mappedName,
                        Enum<?> enumValue,
                        boolean skipSerializing,
                        boolean skipDeserializing) {
        long flags = MarshallUtil.makeFlags(rawType, firstGenericType, secondGenericType,
                fieldName, mappedName, skipSerializing, skipDeserializing);
        byte[] fieldNameUtf8Bytes = fieldName.getBytes(StandardCharsets.UTF_8);
        byte[] mappedNameUtf8Bytes = mappedName.getBytes(StandardCharsets.UTF_8);
        this(rawType, firstGenericType, secondGenericType, index,
                fieldName, fieldNameUtf8Bytes, mappedName, mappedNameUtf8Bytes, enumValue, flags);
    }

    public int flagType() {
        return Long.numberOfLeadingZeros(flags);
    }

    public boolean fieldNameSimple() {
        return (flags & MarshallUtil.FIELD_NAME_SIMPLE_MASK) != 0;
    }

    public boolean mappedNameSimple() {
        return (flags & MarshallUtil.MAPPED_NAME_SIMPLE_MASK) != 0;
    }

    public boolean skipSerializing() {
        return (flags & MarshallUtil.SKIP_SERIALIZATION_MASK) != 0;
    }

    public boolean skipDeserializing() {
        return (flags & MarshallUtil.SKIP_DESERIALIZATION_MASK) != 0;
    }
}
