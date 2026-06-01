package io.jingproject.marshallprocessor;

import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Map;

public record MarshallProcessorInfo (
        List<TypeElement> typeElements,
        List<MarshallFieldInfo> fieldInfos,
        Map<Class<?>, List<MarshallFieldInfo>> fieldTypeInfo,
        Map<Integer, List<MarshallFieldInfo>> fieldHashInfo,
        Map<Integer, List<MarshallFieldInfo>> mappedHashInfo
) {
}
