package io.jingproject.marshallprocessor;

import java.util.List;

public record MarshallTypeInfo (
        Class<?> type,
        List<MarshallFieldInfo> fieldInfos
) {
}
