package io.jingproject.ffm.demo;

import io.jingproject.ffm.SharedLib;

import java.util.List;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public final class DemoLib implements SharedLib {
    private static final String LIB_NAME = System.getProperty("jing.ffm.demo.libname", "demo");

    @Override
    public Class<?> target() {
        return DemoFacade.class;
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
