package io.jingproject.marshallprocessor;

import java.util.List;

public record MarshallHashInfo(
        int hash,
        List<MarshallFieldInfo> fieldInfos
) {
}
