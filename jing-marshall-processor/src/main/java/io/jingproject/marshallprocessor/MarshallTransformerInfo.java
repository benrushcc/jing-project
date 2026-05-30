package io.jingproject.marshallprocessor;

import javax.lang.model.element.TypeElement;

public record MarshallTransformerInfo(
        TypeElement typeElement,
        TypeElement customTypeElement,
        TypeElement builtInElement
) {
}
