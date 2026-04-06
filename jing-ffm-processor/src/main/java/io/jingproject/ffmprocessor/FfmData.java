package io.jingproject.ffmprocessor;

import io.jingproject.ffm.FFM;

import javax.lang.model.element.TypeElement;
import java.util.List;

public record FfmData(
        TypeElement typeElement,
        FFM ffm,
        List<DowncallData> downcallDataList
) {
}
