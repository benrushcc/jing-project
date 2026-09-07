package io.jingproject.marshallprocessor;

import java.util.List;

public record MarshallSwitchInfo (
        int hash,
        List<MarshallFieldInfo> fieldInfos
) {
}
