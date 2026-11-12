package io.jingproject.ffm;

import io.jingproject.common.Os;
import io.jingproject.common.anno.ProcessorApi;

import java.io.File;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

// Libs is a unified entry for managing dynamic library loading,
// providing a standardized implementation across different operating systems.
// the search path is first checked using the property specified by '-D'
// as jing.library.path.
// then, it looks for directories specified by the JING_LIBRARY_PATH
// environment variable.
// finally, it searches the default directories specified by java.library.path,
// which is the default loadLibrary() behavior of the JDK.
// if a library or function is not found, the program can still start normally
// as long as the invalid functions are not invoked.
@ProcessorApi
public final class Libs {

    // all the dynamic library search directories ordered by priority
    private static final List<String> SEARCH_PATH = createSearchPath();

    // critical path could be disabled globally to ensure safepoint is always checked on each downcall.
    // enabling critical is strongly recommended, the improvement is significant for very short calls.
    // only disable it when problems caused by critical calls are actually encountered,
    // to trade a little performance for stability.
    private static final boolean JING_CRITICAL = Boolean.parseBoolean(System.getProperty("jing.ffm.critical", "true"));
    private static final Map<Class<?>, LibDescriptor> LIB_MAPPINGS;

    static {
        List<LibFacade> facades = ServiceLoader.load(LibFacade.class).stream().map(ServiceLoader.Provider::get).toList();
        Map<Class<?>, LibDescriptor> libMappings = new HashMap<>();
        outer : for (LibFacade facade : facades) {
            if (!facade.supportedOS().contains(Os.current())) {
                continue ;
            }
            Arena arena = Arena.ofShared();
            String libName = facade.libName();
            List<Path> depPaths = new ArrayList<>();
            for (String dep : facade.dependsOn()) {
                String depMappedName = System.mapLibraryName(dep);
                Path depPath = searchLibrary(depMappedName);
                if(depPath == null) {
                    continue outer;
                }
                depPaths.add(depPath);
            }
            List<SymbolLookup> depLookups = new ArrayList<>();
            for (Path depPath : depPaths) {
                try {
                    depLookups.add(SymbolLookup.libraryLookup(depPath, arena));
                } catch (Throwable t) {
                    arena.close();
                    continue outer;
                }
            }
            String mappedName = System.mapLibraryName(libName);
            Path libPath = searchLibrary(mappedName);
            if (libPath == null) {
                arena.close();
                continue;
            }
            SymbolLookup libLookup;
            try {
                libLookup = SymbolLookup.libraryLookup(libPath, arena);
            } catch (Throwable t) {
                arena.close();
                continue;
            }
            Map<String, MemorySegment> functions = new HashMap<>();
            for (String methodName : facade.methodNames()) {
                MemorySegment addr = libLookup.find(methodName).orElse(MemorySegment.NULL);
                if (functions.put(methodName, addr) != null) {
                    arena.close();
                    throw new ExceptionInInitializerError("duplicate method exposed in ffm library : " + libName);
                }
            }
            LibDescriptor libDescriptor = new LibDescriptor(arena, libName, mappedName, libLookup, List.copyOf(depLookups), libPath, List.copyOf(depPaths), Map.copyOf(functions), facade.impl());
            libMappings.put(facade.target(), libDescriptor);
        }
        LIB_MAPPINGS = Map.copyOf(libMappings);
    }

    private Libs() {
        throw new UnsupportedOperationException("utility class");
    }

    // library search paths ordered by priority, from most specific to least:
    //   1. the 'jing.library.path' system property (-Djing.library.path),
    //      an explicit per-process override.
    //   2. the JING_LIBRARY_PATH environment variable, a machine-wide fallback.
    //   3. java.library.path, the jvm default search path.
    // only existing directories are collected, so the caller (searchLibrary)
    // can simply probe each path in order and stop at the first hit.
    private static List<String> createSearchPath() {
        List<String> r = new ArrayList<>();
        String argPath = System.getProperty("jing.library.path");
        if (argPath != null && !argPath.isBlank() && Files.isDirectory(Paths.get(argPath))) {
            r.add(argPath);
        }
        String envPath = System.getenv("JING_LIBRARY_PATH");
        if (envPath != null && !envPath.isBlank() && Files.isDirectory(Paths.get(envPath))) {
            r.add(envPath);
        }
        for (String p : System.getProperty("java.library.path", "").split(File.pathSeparator)) {
            if (!p.isBlank() && Files.isDirectory(Paths.get(p))) {
                r.add(p);
            }
        }
        if (r.isEmpty()) {
            throw new ExceptionInInitializerError("cannot initialize library search path");
        }
        return List.copyOf(r);
    }

    // returns the first searched path for the given library name after mapping,
    // or {@code null} if not found.
    private static Path searchLibrary(String mappedLibraryName) {
        for (String searchPath : SEARCH_PATH) {
            Path p = Paths.get(searchPath, mappedLibraryName);
            if (Files.isRegularFile(p)) {
                return p;
            }
        }
        return null;
    }

    public static MemorySegment addr(Class<?> libType, String methodName) {
        LibDescriptor libDescriptor = LIB_MAPPINGS.get(libType);
        if (libDescriptor == null) {
            return MemorySegment.NULL;
        }
        MemorySegment segment = libDescriptor.functions().get(methodName);
        if (segment == null) {
            return MemorySegment.NULL;
        }
        return segment;
    }

    public static MethodHandle mh(Class<?> libType, String methodName, Class<?> returnType, List<Class<?>> parameterTypes, boolean critical) {
        if(libType == null || methodName == null || methodName.isBlank() || returnType == null || parameterTypes == null || parameterTypes.isEmpty()) {
            throw new IllegalArgumentException();
        }
        MemorySegment addr = addr(libType, methodName);
        if(addr.address() == 0L) {
            MethodHandle mh = MethodHandles.throwException(returnType, ForeignException.class)
                    .bindTo(new ForeignException("native method not found : " + methodName));
            if (!parameterTypes.isEmpty()) {
                mh = MethodHandles.dropArguments(mh, 0, parameterTypes);
            }
            return mh;
        }
        FunctionDescriptor descriptor;
        if(parameterTypes.isEmpty()) {
            descriptor = returnType == void.class ? FunctionDescriptor.ofVoid() : FunctionDescriptor.of(castMemoryLayout(returnType));
        } else {
            MemoryLayout[] layouts = new MemoryLayout[parameterTypes.size()];
            for (int i = 0; i < parameterTypes.size(); i++) {
                layouts[i] = castMemoryLayout(parameterTypes.get(i));
            }
            descriptor = returnType == void.class ? FunctionDescriptor.ofVoid(layouts) : FunctionDescriptor.of(castMemoryLayout(returnType), layouts);
        }
        Linker linker = Linker.nativeLinker();
        return (JING_CRITICAL && critical) ?
                linker.downcallHandle(addr, descriptor, Linker.Option.critical(false)) :
                linker.downcallHandle(addr, descriptor);
    }

    private static MemoryLayout castMemoryLayout(Class<?> type) {
        if (type == byte.class) {
            return ValueLayout.JAVA_BYTE;
        }
        if (type == boolean.class) {
            return ValueLayout.JAVA_BOOLEAN;
        }
        if (type == short.class) {
            return ValueLayout.JAVA_SHORT;
        }
        if (type == char.class) {
            return ValueLayout.JAVA_CHAR;
        }
        if (type == int.class) {
            return ValueLayout.JAVA_INT;
        }
        if (type == long.class) {
            return ValueLayout.JAVA_LONG;
        }
        if (type == float.class) {
            return ValueLayout.JAVA_FLOAT;
        }
        if (type == double.class) {
            return ValueLayout.JAVA_DOUBLE;
        }
        if (type == MemorySegment.class) {
            return ValueLayout.ADDRESS;
        }
        throw new ForeignException("unknown type : " + type);
    }

    // find the target libDescriptor by the given type.
    // returns the library descriptor for the given type, or {@code null}
    // if the library is missing or unsupported on the current operating system.
    // for optimal performance, callers should store the return value
    // in a {@code static final} field.
    public static <T> LibDescriptor libDescriptor(Class<T> type) {
        return LIB_MAPPINGS.get(type);
    }

    // find the target impl by the given type.
    // returns the library impl for the given type, or {@code null}
    // if the library is missing or unsupported on the current operating system.
    // for optimal performance, callers should store the return value
    // in a {@code static final} field.
    @SuppressWarnings("unchecked")
    public static <T> T impl(Class<T> type) {
        LibDescriptor libDescriptor = libDescriptor(type);
        if(libDescriptor == null) {
            return null;
        }
        return (T) libDescriptor.impl();
    }

    // rethrows Error/RuntimeException as-is; wraps any other Throwable as undeclared.
    public static RuntimeException sanitize(Throwable throwable) {
        if(throwable instanceof Error err) {
            throw err;
        }
        if(throwable instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new UndeclaredThrowableException(throwable);
    }
}
