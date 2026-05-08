package io.jingproject.ffm;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.nio.file.Path;
import java.util.Map;

public record LibDescriptor<T>(
        String libName,
        String mappedName,
        SymbolLookup lookup,
        Path libPath,
        Map<String, MemorySegment> functions,
        T impl
) {
}
