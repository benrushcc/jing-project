package io.jingproject.marshallprocessor;

import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Map;

public record MarshallGenInfo (
        List<TypeElement> typeElements,
        List<MarshallFieldInfo> fieldInfos,
        Map<Integer, List<MarshallFieldInfo>> fieldHash,
        Map<Integer, List<MarshallFieldInfo>> mappedHash
) {
}
