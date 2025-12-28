package io.jingproject.ffm;

import io.jingproject.common.Os;

import java.io.File;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class SharedLibs {

    private static final List<String> SEARCH_PATH = createSearchPath();

    /**
     *   Critical path could be disabled globally to ensure safepoint is always checked on each downcall
     */
    private static final Boolean JING_CRITICAL = Boolean.parseBoolean(System.getProperty("jing.ffm.critical", "true"));

    private static List<String> createSearchPath() {
        List<String> r = new ArrayList<>();
        String argPath = System.getProperty("jing.library.path");
        if (argPath != null && !argPath.isBlank() && Files.isDirectory(Paths.get(argPath))) {
            r.add(argPath);
        }
        String envPath = System.getenv("JING_LIBRARY_PATH");
        if(envPath != null && !envPath.isBlank() && Files.isDirectory(Paths.get(envPath))) {
            r.add(envPath);
        }
        for (String p : System.getProperty("java.library.path", "").split(File.pathSeparator)) {
            if(!p.isBlank() && Files.isDirectory(Paths.get(p))) {
                r.add(p);
            }
        }
        return List.copyOf(r);
    }

    private static final ConcurrentMap<String, MemorySegment> VM_FUNCTIONS = new ConcurrentHashMap<>();
    private static final Map<String, LibDescriptor> DESCRIPTORS;
    private static final Map<Class<?>, Object> IMPLS;

    record LibDescriptorCache(
            String mappedName,
            SymbolLookup lookup,
            Map<String, MemorySegment> functions
    ) {
        public void registerFunctions(List<String> functionNames) {
            for (String functionName : functionNames) {
                functions.computeIfAbsent(functionName, k -> lookup.find(k).orElse(MemorySegment.NULL));
            }
        }
    }

    static {
        Map<String, LibDescriptorCache> descriptors = new HashMap<>();
        Map<Class<?>, Object> impls = new HashMap<>();
        ServiceLoader<SharedLib> libs = ServiceLoader.load(SharedLib.class);
        for (SharedLib lib : libs) {
            if (impls.put(lib.target(), lib.supplier().get()) != null) {
                throw new ForeignException("SharedLib : " + lib.target() + " already exists");
            }
            if (lib.supportedOS().contains(Os.current())) {
                String libName = lib.libName();
                LibDescriptorCache cache = descriptors.get(libName);
                if (cache == null) {
                    String mappedLibraryName = System.mapLibraryName(libName);
                    Path libPath = SEARCH_PATH.stream().map(p -> Paths.get(p, mappedLibraryName))
                            .filter(Files::exists).findFirst().orElseThrow(() -> new ForeignException("Library : " + libName + " not found"));
                    SymbolLookup lookup = SymbolLookup.libraryLookup(libPath, Arena.global());
                    cache = new LibDescriptorCache(mappedLibraryName, lookup, new HashMap<>());
                    descriptors.put(libName, cache);
                }
                cache.registerFunctions(lib.methodNames());
            }
        }
        Map<String, LibDescriptor> temp = new HashMap<>();
        descriptors.forEach((k, v) -> temp.put(k, new LibDescriptor(k, v.mappedName(), v.lookup(), Map.copyOf(v.functions()))));
        DESCRIPTORS = Map.copyOf(temp);
        IMPLS = Map.copyOf(impls);
    }

    private SharedLibs() {
        throw new UnsupportedOperationException("utility class");
    }

    public static MemorySegment getFunctionAddressFromVM(String functionName) {
        return VM_FUNCTIONS.computeIfAbsent(functionName, k -> {
            Linker linker = Linker.nativeLinker();
            SymbolLookup lookup = linker.defaultLookup();
            return lookup.find(k).orElseThrow(() -> new ForeignException("Function : " + functionName + " not found in vm"));
        });
    }

    public static MethodHandle getMethodHandleFromVM(String functionName, FunctionDescriptor descriptor, boolean critical) {
        Linker linker = Linker.nativeLinker();
        MemorySegment functionAddr = getFunctionAddressFromVM(functionName);
        if(JING_CRITICAL && critical) {
            return linker.downcallHandle(functionAddr, descriptor, Linker.Option.critical(false));
        } else {
            return linker.downcallHandle(functionAddr, descriptor);
        }
    }

    public static MemorySegment getFunctionAddressFromLib(String libName, String functionName) {
        LibDescriptor libDescriptor = DESCRIPTORS.get(libName);
        if (libDescriptor == null) {
            throw new ForeignException("Library : " + libName + " not found");
        }
        MemorySegment segment = libDescriptor.functions().get(functionName);
        if(segment.address() == 0L) {
            throw new ForeignException("Function : " + functionName + " not found");
        }
        return segment;
    }

    public static MethodHandle getMethodHandleFromLib(String libName, String functionName, FunctionDescriptor descriptor, boolean critical) {
        if(libName.equals(FFM.VM)) {
            return getMethodHandleFromVM(functionName, descriptor, critical);
        }
        Linker linker = Linker.nativeLinker();
        MemorySegment segment = getFunctionAddressFromLib(libName, functionName);
        if(JING_CRITICAL && critical) {
            return linker.downcallHandle(segment, descriptor, Linker.Option.critical(false));
        } else {
            return linker.downcallHandle(segment, descriptor);
        }
    }

    public static LibDescriptor getLibDescriptor(String libName) {
        return DESCRIPTORS.get(libName);
    }

    // TODO LazyConstants
    public static <T> T getImpl(Class<T> clazz) {
        Object o = IMPLS.get(clazz);
        if(o == null) {
            throw new ForeignException("Impl for class : " + clazz + " not found");
        }
        return clazz.cast(o);
    }
}
