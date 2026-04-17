package io.jingproject.ffmtest.entity;

import io.jingproject.ffm.ForeignException;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.util.List;

public final class DemoBindingLibImpl implements DemoBinding {
    private static final List<MethodHandle> MHS = List.ofLazy(3, DemoBindingLibImpl::makeMHS);

    private static MethodHandle makeMHS(int index) {
        return switch (index) {
            case 0 -> DemoLibs.mhFromLib(DemoBinding.class, "single_int", List.of(int.class), true, true);
            case 1 -> DemoLibs.mhFromLib(DemoBinding.class, "compute_add", List.of(int.class, int.class, int.class), true, false);
            case 2 -> DemoLibs.mhFromLib(DemoBinding.class, "compute_pointer", List.of(int.class, MemorySegment.class, MemorySegment.class), true, false);
            default -> throw new AssertionError();
        };
    }

    @Override
    public int singleInt() {
        try {
            return (int) MHS.get(0).invokeExact();
        } catch (Throwable t) {
            throw new ForeignException("Failed to invoke single_int method", t);
        }
    }

    @Override
    public int computeAdd(int a, int b) {
        try {
            return (int) MHS.get(1).invokeExact(a, b);
        } catch (Throwable t) {
            throw new ForeignException("Failed to invoke compute_add method", t);
        }
    }

    @Override
    public int computePointer(MemorySegment a, MemorySegment b) {
        try {
            return (int) MHS.get(2).invokeExact(a, b);
        } catch (Throwable t) {
            throw new ForeignException("Failed to invoke compute_pointer method", t);
        }
    }
}
