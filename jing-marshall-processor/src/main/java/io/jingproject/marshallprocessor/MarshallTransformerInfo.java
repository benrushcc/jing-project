package io.jingproject.marshallprocessor;

import javax.lang.model.element.TypeElement;

public record MarshallTransformerInfo(
        TypeElement typeElement,
        TypeElement fromElement,
        TypeElement toElement
) {
}
