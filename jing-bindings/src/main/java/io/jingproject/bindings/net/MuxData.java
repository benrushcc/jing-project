package io.jingproject.bindings.net;

import io.jingproject.common.Descriptor;

public record MuxData(
        Descriptor descriptor,
        int flag
) {
}
