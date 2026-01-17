package io.jingproject.ffm.demo;

import io.jingproject.common.Os;
import io.jingproject.ffm.SharedLib;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public final class DemoProvider implements SharedLib {
    private static final String LIB_NAME = System.getProperty("jing.ffm.demo.libname", "demo");

    private static final AtomicBoolean atomicBoolean = new AtomicBoolean(false);

    public DemoProvider() {
        if(!atomicBoolean.compareAndSet(false, true)) {
            throw new IllegalStateException("DemoProvider instance has already been created");
        }
    }

    @Override
    public Class<?> target() {
        return DemoFacade.class;
    }

    @Override
    public List<Os> supportedOS() {
        return List.of(Os.WINDOWS, Os.LINUX, Os.MACOS);
    }

    @Override
    public String libName() {
        return LIB_NAME;
    }

    @Override
    public List<String> methodNames() {
        return List.of("empty", "constant", "address");
    }

    @Override
    public Supplier<?> supplier() {
        class Holder {
            static final DemoFacade INSTANCE = new DemoImpl();
        }
        return () -> Holder.INSTANCE;
    }
}
