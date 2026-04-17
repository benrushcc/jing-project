package io.jingproject.ffmtest.entity;

import io.jingproject.ffm.ForeignException;
import io.jingproject.ffm.LibDescriptor;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

// Same source as io.jingproject.ffm.Libs built only for testing purpose
public final class DemoLibs {
    private static final Map<Class<?>, LibDescriptor<?>> DESCRIPTORS;

    static {
        Path buildPath = Paths.get(System.getenv("JING_LIBRARY_PATH"));
        String libName = "jing_demo";
        String mappedName = System.mapLibraryName(libName);
        Path libPath = buildPath.resolve(mappedName);
        SymbolLookup lookup = SymbolLookup.libraryLookup(libPath, Arena.global());
        String s1 = "single_int";
        MemorySegment m1 = lookup.findOrThrow(s1);
        String s2 = "compute_add";
        MemorySegment m2 = lookup.findOrThrow(s2);
        String s3 = "compute_pointer";
        MemorySegment m3 = lookup.findOrThrow(s3);
        DESCRIPTORS = Map.of(DemoBinding.class, new LibDescriptor<>(
                libName, mappedName, lookup, libPath, Map.of(s1, m1, s2, m2, s3, m3), new DemoBindingLibImpl()
        ));
    }

    public static MemorySegment addrFromLib(Class<?> libType, String functionName) {
        LibDescriptor<?> libDescriptor = DESCRIPTORS.get(libType);
        if (libDescriptor == null) {
            return MemorySegment.NULL;
        }
        MemorySegment segment = libDescriptor.functions().get(functionName);
        if (segment == null || segment.address() == 0L) {
            return MemorySegment.NULL;
        }
        return segment;
    }

    public static MethodHandle mhFromLib(Class<?> libType, String functionName, List<Class<?>> types, boolean critical, boolean constant) {
        MemorySegment addr = addrFromLib(libType, functionName);
        return makeDowncallMethodHandle(addr, types, critical, constant);
    }

    private static MethodHandle makeDowncallMethodHandle(MemorySegment funcAddr, List<Class<?>> types, boolean critical, boolean constant) {
        if(types == null || types.isEmpty()) {
            throw new IllegalArgumentException("types cannot be null or empty");
        }
        if(constant && types.size() > 1) {
            throw new IllegalArgumentException("constant method cannot have parameters");
        }
        if(funcAddr.address() == 0L) {
            return MethodHandles.throwException(types.getFirst(), ForeignException.class).bindTo(new ForeignException("function address not found"));
        }
        FunctionDescriptor descriptor = castFunctionDescriptor(types);
        Linker linker = Linker.nativeLinker();
        MethodHandle mh = (critical) ?
                linker.downcallHandle(funcAddr, descriptor, Linker.Option.critical(false)) :
                linker.downcallHandle(funcAddr, descriptor);
        if(constant) {
            return makeConstantMethodHandle(types, mh);
        }
        return mh;
    }

    private static MethodHandle makeConstantMethodHandle(List<Class<?>> types, MethodHandle mh) {
        try {
            Class<?> returnType = types.getFirst();
            if(returnType.equals(byte.class)) {
                byte r = (byte) mh.invokeExact();
                return MethodHandles.constant(byte.class, r);
            } else if (boolean.class.equals(returnType)) {
                boolean r = (boolean) mh.invokeExact();
                return MethodHandles.constant(boolean.class, r);
            } else if (short.class.equals(returnType)) {
                short r = (short) mh.invokeExact();
                return MethodHandles.constant(short.class, r);
            } else if (char.class.equals(returnType)) {
                char r = (char) mh.invokeExact();
                return MethodHandles.constant(char.class, r);
            } else if (int.class.equals(returnType)) {
                int r = (int) mh.invokeExact();
                return MethodHandles.constant(int.class, r);
            } else if (long.class.equals(returnType)) {
                long r = (long) mh.invokeExact();
                return MethodHandles.constant(long.class, r);
            } else if (float.class.equals(returnType)) {
                float r = (float) mh.invokeExact();
                return MethodHandles.constant(float.class, r);
            } else if (double.class.equals(returnType)) {
                double r = (double) mh.invokeExact();
                return MethodHandles.constant(double.class, r);
            } else if (MemorySegment.class.equals(returnType)) {
                MemorySegment r = (MemorySegment) mh.invokeExact();
                return MethodHandles.constant(MemorySegment.class, r);
            } else {
                throw new ForeignException("unsupported constant foreign method return type: " + returnType);
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

    @SuppressWarnings("unchecked")
    public static <T> LibDescriptor<T> getLibDescriptor(Class<T> type) {
        return (LibDescriptor<T>) DESCRIPTORS.get(type);
    }

    public static <T> T getImpl(Class<T> type) {
        LibDescriptor<T> libDescriptor = getLibDescriptor(type);
        if (libDescriptor == null) {
            return null;
        }
        return libDescriptor.impl();
    }
}
