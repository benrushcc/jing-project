package io.jingproject.bindingstest.entity;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;

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

    @Override
    public long strToLong(MemorySegment str) {
        String s = str.getString(0L, StandardCharsets.UTF_8);
        return Long.parseLong(s);
    }

    @Override
    public double strToDouble(MemorySegment str) {
        String s = str.getString(0L, StandardCharsets.UTF_8);
        return Double.parseDouble(s);
    }

    @Override
    public int longToStr(long var, MemorySegment str, int len) {
        String s = String.valueOf(var);
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        MemorySegment.copy(bytes, 0, str, ValueLayout.JAVA_BYTE, 0L, bytes.length);
        return bytes.length;
    }

    @Override
    public int doubleToStr(double var, MemorySegment str, int len) {
        String s = String.valueOf(var);
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        MemorySegment.copy(bytes, 0, str, ValueLayout.JAVA_BYTE, 0L, bytes.length);
        return bytes.length;
    }

    @Override
    public void nonexist() {
        throw new UnsupportedOperationException("not exist");
    }
}
