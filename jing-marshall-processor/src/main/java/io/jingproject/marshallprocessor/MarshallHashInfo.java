package io.jingproject.marshallprocessor;

import java.util.List;
import java.util.Map;

public record MarshallHashInfo (
        Map<Integer, List<MarshallFieldInfo>> fieldHashMap,
        Map<Integer, List<MarshallFieldInfo>> mappedHashMap
) {
}
