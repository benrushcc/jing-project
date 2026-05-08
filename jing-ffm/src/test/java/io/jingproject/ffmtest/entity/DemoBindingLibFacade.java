package io.jingproject.ffmtest.entity;

import io.jingproject.common.Os;
import io.jingproject.ffm.LibFacade;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public final class DemoBindingLibFacade implements LibFacade {
    private static final AtomicBoolean GUARD = new AtomicBoolean(false);

    public DemoBindingLibFacade() {
        if (!GUARD.compareAndSet(false, true)) {
            throw new IllegalStateException();
        }
    }

    @Override
    public Class<?> target() {
        return DemoBinding.class;
    }

    @Override
    public List<Os> supportedOS() {
        return List.of(Os.WINDOWS, Os.LINUX, Os.MACOS);
    }

    @Override
    public String libName() {
        return "demo";
    }

    @Override
    public List<String> methodNames() {
        return List.of("single_int", "compute_add", "compute_pointer");
    }

    @Override
    public Supplier<?> supplier() {
        return DemoBindingLibImpl::new;
    }
}
