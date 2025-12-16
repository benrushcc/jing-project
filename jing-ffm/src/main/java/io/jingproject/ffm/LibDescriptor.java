package io.jingproject.ffm;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.util.Map;

public record LibDescriptor(
        String libName,
        String mappedName,
        SymbolLookup lookup,
        Map<String, MemorySegment> functions
) {
}
