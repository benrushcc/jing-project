package io.jingproject.marshallprocessor;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

public record MarshallFieldInfo(
        TypeElement typeElement,
        int typeIndex,
        Element fieldElement,
        String fieldName,
        String mappedName,
        int marshallIndex,
        int fieldNameOffset,
        byte[] fieldNameUtf8Bytes,
        int mappedNameOffset,
        byte[] mappedNameUtf8Bytes,
        boolean skipSerializing,
        boolean skipDeserializing
) {
}
