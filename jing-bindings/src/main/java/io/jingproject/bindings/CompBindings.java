package io.jingproject.bindings;

import io.jingproject.ffm.Downcall;
import io.jingproject.ffm.FFM;

import java.lang.foreign.MemorySegment;

@FFM(libraryName = "jing_bindings")
public interface CompBindings {
    @Downcall(methodName = "jing_zlib_ng_version", constant = true, critical = true)
    MemorySegment zlibVersion();
}
