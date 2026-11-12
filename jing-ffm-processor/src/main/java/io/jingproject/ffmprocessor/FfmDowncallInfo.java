package io.jingproject.ffmprocessor;

import javax.lang.model.element.ExecutableElement;

public record FfmDowncallInfo(
        int index,
        ExecutableElement element,
        String methodName,
        boolean critical
) {
}
