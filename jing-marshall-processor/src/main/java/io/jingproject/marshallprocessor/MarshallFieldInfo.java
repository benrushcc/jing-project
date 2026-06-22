package io.jingproject.marshallprocessor;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

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
