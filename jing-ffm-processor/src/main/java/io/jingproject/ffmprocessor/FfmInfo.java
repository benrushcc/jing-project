package io.jingproject.ffmprocessor;

import io.jingproject.common.Os;

import javax.lang.model.element.TypeElement;
import java.util.List;

public record FfmInfo (
        TypeElement element,
        String libraryName,
        List<Os> supportedOS,
        List<DowncallInfo> downcallInfos
) {
}
