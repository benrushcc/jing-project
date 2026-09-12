package io.jingproject.ffm;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.nio.file.Path;
import java.util.Map;

public record LibDescriptor<T> (
        // the original library name declared in the @FFM annotation.
        String libName,
        // the OS-mapped library name (System.mapLibraryName result),
        // including the platform prefix and extension, e.g. "libmy.so".
        String mappedName,
        // the symbol lookup opened over Arena.global().
        SymbolLookup lookup,
        // the resolved library file path.
        Path libPath,
        // the method names resolved to their native addresses,
        // NULL segments stand for missing symbols.
        Map<String, MemorySegment> functions,
        // the singleton implementation instance of the binding.
        T impl
) {
}
