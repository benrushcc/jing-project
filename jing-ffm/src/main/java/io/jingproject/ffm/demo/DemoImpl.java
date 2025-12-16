package io.jingproject.ffm.demo;

import io.jingproject.ffm.SharedLibs;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

public final class DemoImpl implements DemoFacade {

    private static final String LIB_NAME = System.getProperty("jing.ffm.demo.libname", "demo");

    @Override
    public void empty() {
        class Holder {
            static final MethodHandle MH = SharedLibs.getMethodHandleFromLib(LIB_NAME, "empty", FunctionDescriptor.ofVoid(), false);
        }
        try {
            Holder.MH.invokeExact();
        } catch (Throwable t) {
            throw new RuntimeException("Failed to invoke empty method", t);
        }
    }

    @Override
    public int constant() {
        class Holder {
            static final MethodHandle MH = SharedLibs.getMethodHandleFromLib(LIB_NAME, "constant", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT), false);
            static final int CACHED;
            static {
                try {
                    CACHED = (int) MH.invokeExact();
                } catch (Throwable e) {
                    throw new RuntimeException("Failed to invoke constant method", e);
                }
            }
        }
        return Holder.CACHED;
    }

    @Override
    public MemorySegment address(MemorySegment addr, long size) {
        class Holder {
            static final MethodHandle MH = SharedLibs.getMethodHandleFromLib(LIB_NAME, "address", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG), false);
        }
        try {
            return (MemorySegment) Holder.MH.invokeExact(addr, size);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to invoke address method", t);
        }
    }
}
