package io.jingproject.bindingstest.entity;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public final class DemoBindingImpl implements DemoBinding {

    @Override
    public int singleInt() {
        return 7355608;
    }

    @Override
    public int computeAdd(int a, int b) {
        return a + b;
    }

    @Override
    public int computePointer(MemorySegment a, MemorySegment b) {
        return a.get(ValueLayout.JAVA_INT, 0L) - b.get(ValueLayout.JAVA_INT, 0L);
    }

}
