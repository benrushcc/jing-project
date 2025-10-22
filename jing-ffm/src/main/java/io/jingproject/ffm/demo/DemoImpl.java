package io.jingproject.ffm.demo;

import io.jingproject.ffm.SharedLibs;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

public final class DemoImpl implements DemoFacade {

    private static final String LIB_NAME = System.getProperty("jing.ffm.demo.libname", "demo");

    @Override
    public void empty() {
        class Holder {
            static final MethodHandle MH = SharedLibs.getMethodHandleFromLib(LIB_NAME, "empty", FunctionDescriptor.ofVoid(), Linker.Option.critical(false));
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
            static final MethodHandle MH = SharedLibs.getMethodHandleFromLib(LIB_NAME, "constant", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT), Linker.Option.critical(false));
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
    public void address(long addr, long size) {
        class Holder {
            static final MethodHandle MH = SharedLibs.getMethodHandleFromLib(LIB_NAME, "address", FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG), Linker.Option.critical(false));
        }
        try {
            Holder.MH.invokeExact(addr, size);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to invoke address method", t);
        }
    }
}
