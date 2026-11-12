package io.jingproject.ffm;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record LibDescriptor (
        Arena arena,
        String libName,
        String mappedName,
        SymbolLookup libLookup,
        List<SymbolLookup> depLookups,
        Path libPath,
        List<Path> depPaths,
        Map<String, MemorySegment> functions,
        Object impl
) {
}
