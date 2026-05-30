package io.jingproject.ffm;

import io.jingproject.common.Os;
import io.jingproject.common.anno.ProcessorApi;

import java.io.File;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Libs is a unified entry for managing dynamic library loading, providing a standardized implementation across different operating systems.
 * The dynamic library search path is first checked using the property specified by '-D' as jing.library.path.
 * Then, it looks for directories specified by the JING_LIBRARY_PATH environment variable.
 * Finally, it searches the default directories specified by java.library.path, which is the default loadLibrary() behavior of the JDK.
 * If a library or function is not found, the program can still start normally as long as the invalid functions are not invoked.
 */
@ProcessorApi
@SuppressWarnings("unused")
public final class Libs {

    /**
     *  all the dynamic library search directories ordered by priority
     */
    private static final List<String> SEARCH_PATH = createSearchPath();

    /**
     * critical path could be disabled globally to ensure safepoint is always checked on each downcall
     */
    private static final boolean JING_CRITICAL = Boolean.parseBoolean(System.getProperty("jing.ffm.critical", "true"));

    /**
     * @return the available dynamic library serach paths on current machine
     */
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

    /**
     * @return the first searched path for given library name after mapping, or {@code null} if not found
     */
    private static Path searchLibrary(String mappedLibraryName) {
        for (String searchPath : SEARCH_PATH) {
            Path p = Paths.get(searchPath, mappedLibraryName);
            if (Files.isRegularFile(p)) {
                return p;
            }
        }
        return null;
    }

    private static final Map<Class<?>, LibDescriptor<?>> DESCRIPTORS;

    static {
        List<LibFacade> facades = ServiceLoader.load(LibFacade.class).stream().map(ServiceLoader.Provider::get).toList();
        Map<Class<?>, LibDescriptor<?>> tempDescriptors = new HashMap<>();
        for (LibFacade facade : facades) {
            if (facade.supportedOS().contains(Os.current())) {
                Class<?> target = facade.target();
                String libName = facade.libName();
                LibDescriptor<?> desc = tempDescriptors.get(target);
                if (desc == null) {
                    String mappedName = System.mapLibraryName(libName);
                    Path libPath = searchLibrary(mappedName);
                    if (libPath == null) {
                        continue;
                    }
                    SymbolLookup lookup = SymbolLookup.libraryLookup(libPath, Arena.global());
                    Object impl = facade.supplier().get();
                    desc = new LibDescriptor<>(libName, mappedName, lookup, libPath, new HashMap<>(), impl);
                    tempDescriptors.put(target, desc);
                }
                for (String methodName : facade.methodNames()) {
                    if (!desc.functions().containsKey(methodName)) {
                        MemorySegment methodAddress = desc.lookup().find(methodName).orElse(MemorySegment.NULL);
                        desc.functions().put(methodName, methodAddress);
                    }
                }
            }
        }
        DESCRIPTORS = tempDescriptors.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                entry -> {
                    LibDescriptor<?> desc = entry.getValue();
                    Map<String, MemorySegment> immutableFunctions =
                            Map.copyOf(desc.functions());
                    return new LibDescriptor<>(desc.libName(), desc.mappedName(), desc.lookup(), desc.libPath(), immutableFunctions, desc.impl());
                }
        ));
    }

    private Libs() {
        throw new UnsupportedOperationException("utility class");
    }

    public static MemorySegment addrFromVM(String methodName) {
        if (methodName == null || methodName.isBlank()) {
            throw new IllegalArgumentException("methodName cannot be null or blank");
        }
        Linker linker = Linker.nativeLinker();
        SymbolLookup lookup = linker.defaultLookup();
        return lookup.find(methodName).orElse(MemorySegment.NULL);
    }

    public static MethodHandle mhFromVM(String methodName, List<Class<?>> types, boolean critical, boolean constant) {
        MemorySegment addr = addrFromVM(methodName);
        return makeDowncallMethodHandle(methodName, addr, types, critical, constant);
    }

    public static MemorySegment addrFromLib(Class<?> libType, String methodName) {
        LibDescriptor<?> libDescriptor = DESCRIPTORS.get(libType);
        if (libDescriptor == null) {
            return MemorySegment.NULL;
        }
        MemorySegment segment = libDescriptor.functions().get(methodName);
        if (segment == null || segment.address() == 0L) {
            return MemorySegment.NULL;
        }
        return segment;
    }

    public static MethodHandle mhFromLib(Class<?> libType, String methodName, List<Class<?>> types, boolean critical, boolean constant) {
        MemorySegment addr = addrFromLib(libType, methodName);
        return makeDowncallMethodHandle(methodName, addr, types, critical, constant);
    }

    private static MethodHandle makeDowncallMethodHandle(String methodName, MemorySegment addr, List<Class<?>> types, boolean critical, boolean constant) {
        if (types == null || types.isEmpty()) {
            throw new IllegalArgumentException("types cannot be null or empty");
        }
        if (constant && types.size() > 1) {
            throw new IllegalArgumentException("constant method cannot have parameters");
        }
        if (addr.address() == 0L) {
            return makeErrorMethodHandle(types, methodName);
        }
        FunctionDescriptor descriptor = castFunctionDescriptor(types);
        Linker linker = Linker.nativeLinker();
        MethodHandle mh = (JING_CRITICAL && critical) ?
                linker.downcallHandle(addr, descriptor, Linker.Option.critical(false)) :
                linker.downcallHandle(addr, descriptor);
        if (constant) {
            return makeConstantMethodHandle(types, mh);
        }
        return mh;
    }

    private static MethodHandle makeErrorMethodHandle(List<Class<?>> types, String methodName) {
        Class<?> rType = types.getFirst();
        MethodHandle mh = MethodHandles.throwException(rType, ForeignException.class);
        mh = mh.bindTo(new ForeignException("native method not found : " + methodName));
        if(types.size() > 1) {
            mh = MethodHandles.dropArguments(mh, 0, types.subList(1, types.size()));
        }
        return mh;
    }

    private static MethodHandle makeConstantMethodHandle(List<Class<?>> types, MethodHandle mh) {
        try {
            Class<?> rType = types.getFirst();
            if (rType.equals(byte.class)) {
                byte r = (byte) mh.invokeExact();
                return MethodHandles.constant(byte.class, r);
            } else if (boolean.class.equals(rType)) {
                boolean r = (boolean) mh.invokeExact();
                return MethodHandles.constant(boolean.class, r);
            } else if (short.class.equals(rType)) {
                short r = (short) mh.invokeExact();
                return MethodHandles.constant(short.class, r);
            } else if (char.class.equals(rType)) {
                char r = (char) mh.invokeExact();
                return MethodHandles.constant(char.class, r);
            } else if (int.class.equals(rType)) {
                int r = (int) mh.invokeExact();
                return MethodHandles.constant(int.class, r);
            } else if (long.class.equals(rType)) {
                long r = (long) mh.invokeExact();
                return MethodHandles.constant(long.class, r);
            } else if (float.class.equals(rType)) {
                float r = (float) mh.invokeExact();
                return MethodHandles.constant(float.class, r);
            } else if (double.class.equals(rType)) {
                double r = (double) mh.invokeExact();
                return MethodHandles.constant(double.class, r);
            } else if (MemorySegment.class.equals(rType)) {
                MemorySegment r = (MemorySegment) mh.invokeExact();
                return MethodHandles.constant(MemorySegment.class, r);
            } else {
                throw new ForeignException("unsupported constant foreign method return type: " + rType);
            }
        } catch (Throwable t) {
            throw new ForeignException("failed to invoke constant foreign method", t);
        }
    }

    private static FunctionDescriptor castFunctionDescriptor(List<Class<?>> types) {
        MemoryLayout[] layouts = new MemoryLayout[types.size() - 1];
        for (int i = 1; i < types.size(); i++) {
            layouts[i - 1] = castMemoryLayout(types.get(i));
        }
        Class<?> firstType = types.getFirst();
        if (firstType.equals(void.class)) {
            return FunctionDescriptor.ofVoid(layouts);
        } else {
            MemoryLayout resLayout = castMemoryLayout(firstType);
            return FunctionDescriptor.of(resLayout, layouts);
        }
    }

    private static MemoryLayout castMemoryLayout(Class<?> type) {
        if (byte.class.equals(type)) {
            return ValueLayout.JAVA_BYTE;
        } else if (boolean.class.equals(type)) {
            return ValueLayout.JAVA_BOOLEAN;
        } else if (short.class.equals(type)) {
            return ValueLayout.JAVA_SHORT;
        } else if (char.class.equals(type)) {
            return ValueLayout.JAVA_CHAR;
        } else if (int.class.equals(type)) {
            return ValueLayout.JAVA_INT;
        } else if (long.class.equals(type)) {
            return ValueLayout.JAVA_LONG;
        } else if (float.class.equals(type)) {
            return ValueLayout.JAVA_FLOAT;
        } else if (double.class.equals(type)) {
            return ValueLayout.JAVA_DOUBLE;
        } else if (MemorySegment.class.equals(type)) {
            return ValueLayout.ADDRESS;
        } else {
            throw new ForeignException("unknown type : " + type);
        }
    }

    /**
     * find target libDecriptor by given type
     *
     * @return the library descriptor for the given type, or {@code null} if the library is missing or unsupported on current operating system
     * for optimal performance, callers should store the return value in a {@code static final} field
     */
    @SuppressWarnings("unchecked")
    public static <T> LibDescriptor<T> getLibDescriptor(Class<T> type) {
        return (LibDescriptor<T>) DESCRIPTORS.get(type);
    }

    /**
     * find target impl by given type
     *
     * @return the library impl for the given type, or {@code null} if the library is missing or unsupported on current operating system
     * for optimal performance, callers should store the return value in a {@code static final} field
     */
    public static <T> T getImpl(Class<T> type) {
        LibDescriptor<T> libDescriptor = getLibDescriptor(type);
        if (libDescriptor == null) {
            return null;
        }
        return libDescriptor.impl();
    }
}
