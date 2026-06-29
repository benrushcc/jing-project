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
        int fieldNameLen,
        int mappedNameOffset,
        int mappedNameLen,
        boolean skipSerializing,
        boolean skipDeserializing
) {
}
