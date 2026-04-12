package io.jingproject.marshallprocessor;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

public record MarshallFieldInfo (
        TypeElement typeElement,
        int typeIndex,
        VariableElement fieldElement,
        String fieldName,
        String mappedName,
        int marshallIndex,
        int fieldNameStartIndex,
        int fieldNameEndIndex,
        int mappedNameStartIndex,
        int mappedNameEndIndex,
        boolean skipSerializing,
        boolean skipDeserializing
) {
}
