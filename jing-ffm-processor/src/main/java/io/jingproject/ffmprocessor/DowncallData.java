package io.jingproject.ffmprocessor;

import io.jingproject.ffm.Downcall;

import javax.lang.model.element.ExecutableElement;

public record DowncallData(
        ExecutableElement executableElement,
        Downcall downcall
) {
}
