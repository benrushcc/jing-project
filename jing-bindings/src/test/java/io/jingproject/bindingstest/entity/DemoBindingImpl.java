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

    @Override
    public long longAdd(long a, long b) {
        return a + b;
    }

    @Override
    public int longWinLongAdd(int a, int b) {
        return a + b;
    }

    @Override
    public long sizeTAdd(long a, long b) {
        return a + b;
    }

    @Override
    public int unsignedIntAdd(int a, int b) {
        return a + b;
    }

    @Override
    public long unsignedLongAdd(long a, long b) {
        return a + b;
    }

    @Override
    public int unsignedLongWinAdd(int a, int b) {
        return a + b;
    }

    @Override
    public long strLen(MemorySegment str) {
        String s = str.getString(0L, StandardCharsets.UTF_8);
        return s.length();
    }

    @Override
    public MemorySegment voidPtrIdentity(MemorySegment p) {
        return p;
    }

    @Override
    public int sizeofInt() {
        return 4;
    }

    @Override
    public int sizeofLong() {
        return 8;
    }

    @Override
    public int sizeofSizeT() {
        return 8;
    }

    @Override
    public int sizeofPointer() {
        return 8;
    }

    @Override
    public boolean boolNot(boolean v) {
        return !v;
    }

    @Override
    public boolean boolAnd(boolean a, boolean b) {
        return a && b;
    }

    @Override
    public boolean boolTrue() {
        return true;
    }

    @Override
    public boolean boolFalse() {
        return false;
    }

    @Override
    public boolean intToBool(int v) {
        return v != 0;
    }

    @Override
    public int boolToInt(boolean v) {
        return v ? 1 : 0;
    }

    @Override
    public byte byteAdd(byte a, byte b) {
        return (byte) (a + b);
    }

    @Override
    public short shortAdd(short a, short b) {
        return (short) (a + b);
    }

    @Override
    public char charUpper(char c) {
        if (c >= 'a' && c <= 'z') {
            return (char) (c - ('a' - 'A'));
        }
        return c;
    }

    @Override
    public float floatAdd(float a, float b) {
        return a + b;
    }

    @Override
    public void voidNoop() {
    }

    @Override
    public MemorySegment pointerIdentity(MemorySegment p) {
        return p;
    }
}
