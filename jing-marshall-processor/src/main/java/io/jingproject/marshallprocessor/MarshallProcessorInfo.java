package io.jingproject.marshallprocessor;

import javax.lang.model.element.TypeElement;
import java.util.List;

public record MarshallProcessorInfo(
        List<TypeElement> typeElements,
        List<MarshallFieldInfo> fieldInfos,
        List<MarshallTypeInfo> typeInfos,
        int fieldHashIndex,
        List<MarshallSwitchInfo> fieldHashInfos,
        int mappedHashIndex,
        List<MarshallSwitchInfo> mappedHashInfos
) {
}
