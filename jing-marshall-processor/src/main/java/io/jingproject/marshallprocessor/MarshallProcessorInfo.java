package io.jingproject.marshallprocessor;

import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Map;

public record MarshallProcessorInfo (
        List<TypeElement> typeElements,
        List<MarshallFieldInfo> fieldInfos,
        Map<Class<?>, List<MarshallFieldInfo>> fieldTypeInfo,
        int fieldHashIndex,
        Map<Integer, List<MarshallFieldInfo>> fieldHashInfo,
        int mappedHashIndex,
        Map<Integer, List<MarshallFieldInfo>> mappedHashInfo
) {
}
