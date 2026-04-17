package io.jingproject.ffmprocessor;

import javax.lang.model.element.ExecutableElement;

public record DowncallInfo (
        int index,
        ExecutableElement element,
        String methodName,
        boolean constant,
        boolean critical
) {
}
