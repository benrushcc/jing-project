package io.jingproject.ffm;

import java.io.File;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public final class SharedLibs {

    private static final List<String> SEARCH_PATH = createSearchPath();

    private static List<String> createSearchPath() {
        List<String> r = new ArrayList<>();
        String argPath = System.getProperty("jing.library.path");
        if (argPath != null && !argPath.isBlank()) {
            r.add(argPath);
        }
        String envPath = System.getenv("JING_LIBRARY_PATH");
        if(envPath != null && !envPath.isBlank()) {
            r.add(envPath);
        }
        for (String p : System.getProperty("java.library.path", "").split(File.pathSeparator)) {
            if(!p.isBlank()) {
                r.add(p);
            }
        }
        return List.copyOf(r);
    }

    private static final Map<String, Map<String, MemorySegment>> SYMBOLS;
    private static final Map<Class<?>, Object> IMPLS;

    static {
        Map<String, Map<String, MemorySegment>> symbols = new HashMap<>();
        Map<Class<?>, Object> impls = new HashMap<>();
        ServiceLoader<SharedLib> libs = ServiceLoader.load(SharedLib.class);
        for (SharedLib lib : libs) {
            if(OsType.enabled(lib.supportedOsType())) {
                String libName = lib.libName();
                String mappedLibraryName = System.mapLibraryName(libName);
                Path libPath = SEARCH_PATH.stream().map(p -> Paths.get(p, mappedLibraryName))
                        .filter(Files::exists).findFirst().orElseThrow(() -> new ForeignException("Library : " + libName + " not found"));
                SymbolLookup lookup = SymbolLookup.libraryLookup(libPath, Arena.global());
                List<String> methodNames = lib.methodNames();
                Map<String, MemorySegment> methodMap = symbols.getOrDefault(libName, new HashMap<>());
                for (String methodName : methodNames) {
                    methodMap.computeIfAbsent(methodName, k -> lookup.find(k).orElse(MemorySegment.NULL));
                }
                symbols.putIfAbsent(libName, methodMap);
                impls.put(lib.target(), lib.supplier().get());
            }
        }
        symbols.replaceAll((_, v) -> Map.copyOf(v));
        SYMBOLS = Map.copyOf(symbols);
        IMPLS = Map.copyOf(impls);
    }

    private SharedLibs() {
        throw new UnsupportedOperationException("utility class");
    }

    public static MethodHandle getMethodHandleFromVM(String functionName, FunctionDescriptor descriptor, Linker.Option... options) {
        Linker linker = Linker.nativeLinker();
        SymbolLookup lookup = linker.defaultLookup();
        MemorySegment functionAddr = lookup.find(functionName).orElseThrow(() -> new ForeignException("Function : " + functionName + " not found in vm"));
        return linker.downcallHandle(functionAddr, descriptor, options);
    }

    public static MethodHandle getMethodHandleFromLib(String libName, String functionName, FunctionDescriptor descriptor, Linker.Option... options) {
        if(libName.equals(FFM.VM)) {
            return getMethodHandleFromVM(functionName, descriptor, options);
        }
        Linker linker = Linker.nativeLinker();
        MemorySegment segment = getFunctionAddressFromLib(libName, functionName);
        return linker.downcallHandle(segment, descriptor, options);
    }

    public static MemorySegment getFunctionAddressFromLib(String libName, String functionName) {
        Map<String, MemorySegment> m = SYMBOLS.get(libName);
        if (m == null || m.isEmpty()) {
            throw new ForeignException("Library : " + libName + " not found");
        }
        MemorySegment segment = m.get(functionName);
        if(segment.address() == 0L) {
            throw new ForeignException("Function : " + functionName + " not found");
        }
        return segment;
    }

    public static <T> T getImpl(Class<T> clazz) {
        Object o = IMPLS.get(clazz);
        if(o == null) {
            throw new ForeignException("Impl for class : " + clazz + " not found");
        }
        return clazz.cast(o);
    }
}
