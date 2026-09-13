package io.jingproject.bindingstest.test;

import io.jingproject.bindingstest.entity.DemoBinding;
import io.jingproject.bindingstest.entity.DemoBindingImpl;
import io.jingproject.common.Os;
import io.jingproject.ffm.ForeignException;
import io.jingproject.ffm.Libs;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

@Tag("require-native-library")
public class DemoTest {
    private static final DemoBinding NATIVE_IMPL = Libs.impl(DemoBinding.class);
    private static final DemoBinding JAVA_IMPL = new DemoBindingImpl();
    private static final int BATCH = 10000;

    static {
        if(NATIVE_IMPL == null) {
            throw new ExceptionInInitializerError("native library not available");
        }
    }

    @Test
    public void testSingleInt() {
        Assertions.assertEquals(JAVA_IMPL.singleInt(), NATIVE_IMPL.singleInt());
    }

    @Test
    public void testComputeAdd() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < BATCH; i++) {
            int a = random.nextInt(0, Integer.MAX_VALUE);
            int b = random.nextInt(-Integer.MAX_VALUE, 0);
            Assertions.assertEquals(JAVA_IMPL.computeAdd(a, b), NATIVE_IMPL.computeAdd(a, b));
        }
    }

    @Test
    public void testComputePointer() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < BATCH; i++) {
            int a = random.nextInt(0, Integer.MAX_VALUE);
            int b = random.nextInt(0, Integer.MAX_VALUE);
            MemorySegment m1 = Arena.ofAuto().allocateFrom(ValueLayout.JAVA_INT, a);
            MemorySegment m2 = Arena.ofAuto().allocateFrom(ValueLayout.JAVA_INT, b);
            Assertions.assertEquals(JAVA_IMPL.computePointer(m1, m2), NATIVE_IMPL.computePointer(m1, m2));
        }
    }

    @Test
    public void testStrToLong() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < BATCH; i++) {
            long v = random.nextLong();
            String str = String.valueOf(v);
            MemorySegment segment = Arena.ofAuto().allocateFrom(str, StandardCharsets.UTF_8);
            long v1 = JAVA_IMPL.strToLong(segment);
            long v2 = NATIVE_IMPL.strToLong(segment);
            Assertions.assertEquals(v1, v2);
        }
    }

    @Test
    public void testLongToStr() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < BATCH; i++) {
            long v = random.nextLong();
            MemorySegment s1 = Arena.ofAuto().allocate(ValueLayout.JAVA_BYTE, 64);
            int i1 = JAVA_IMPL.longToStr(v, s1, 64);
            MemorySegment s2 = Arena.ofAuto().allocate(ValueLayout.JAVA_BYTE, 64);
            int i2 = JAVA_IMPL.longToStr(v, s2, 64);
            Assertions.assertEquals(i1, i2);
        }
    }

    @Test
    public void testNonExist() {
        Assertions.assertThrows(UnsupportedOperationException.class, JAVA_IMPL::nonexist);
        Assertions.assertThrows(ForeignException.class, NATIVE_IMPL::nonexist);
    }

    @Test
    public void testBoolean() {
        Assertions.assertEquals(JAVA_IMPL.boolNot(true), NATIVE_IMPL.boolNot(true));
        Assertions.assertEquals(JAVA_IMPL.boolNot(false), NATIVE_IMPL.boolNot(false));
        Assertions.assertEquals(JAVA_IMPL.boolAnd(true, true), NATIVE_IMPL.boolAnd(true, true));
        Assertions.assertEquals(JAVA_IMPL.boolAnd(true, false), NATIVE_IMPL.boolAnd(true, false));
        Assertions.assertEquals(JAVA_IMPL.boolAnd(false, false), NATIVE_IMPL.boolAnd(false, false));
        Assertions.assertTrue(NATIVE_IMPL.boolTrue());
        Assertions.assertFalse(NATIVE_IMPL.boolFalse());
        Assertions.assertEquals(JAVA_IMPL.intToBool(1), NATIVE_IMPL.intToBool(1));
        Assertions.assertEquals(JAVA_IMPL.intToBool(0), NATIVE_IMPL.intToBool(0));
        Assertions.assertEquals(JAVA_IMPL.intToBool(-5), NATIVE_IMPL.intToBool(-5));
        Assertions.assertEquals(JAVA_IMPL.boolToInt(true), NATIVE_IMPL.boolToInt(true));
        Assertions.assertEquals(JAVA_IMPL.boolToInt(false), NATIVE_IMPL.boolToInt(false));
    }

    @Test
    public void testByteShortCharFloat() {
        Assertions.assertEquals(JAVA_IMPL.byteAdd((byte) 1, (byte) 2), NATIVE_IMPL.byteAdd((byte) 1, (byte) 2));
        Assertions.assertEquals(JAVA_IMPL.byteAdd(Byte.MAX_VALUE, (byte) 1), NATIVE_IMPL.byteAdd(Byte.MAX_VALUE, (byte) 1));
        Assertions.assertEquals(JAVA_IMPL.byteAdd(Byte.MIN_VALUE, (byte) -1), NATIVE_IMPL.byteAdd(Byte.MIN_VALUE, (byte) -1));
        Assertions.assertEquals(JAVA_IMPL.shortAdd((short) 100, (short) 200), NATIVE_IMPL.shortAdd((short) 100, (short) 200));
        Assertions.assertEquals(JAVA_IMPL.shortAdd(Short.MAX_VALUE, (short) 1), NATIVE_IMPL.shortAdd(Short.MAX_VALUE, (short) 1));
        Assertions.assertEquals(JAVA_IMPL.charUpper('a'), NATIVE_IMPL.charUpper('a'));
        Assertions.assertEquals(JAVA_IMPL.charUpper('z'), NATIVE_IMPL.charUpper('z'));
        Assertions.assertEquals(JAVA_IMPL.charUpper('A'), NATIVE_IMPL.charUpper('A'));
        Assertions.assertEquals(JAVA_IMPL.charUpper('1'), NATIVE_IMPL.charUpper('1'));
        Assertions.assertEquals(JAVA_IMPL.floatAdd(1.5f, 2.25f), NATIVE_IMPL.floatAdd(1.5f, 2.25f), 0.0001f);
        Assertions.assertEquals(JAVA_IMPL.floatAdd(-1.0f, 1.0f), NATIVE_IMPL.floatAdd(-1.0f, 1.0f), 0.0001f);
    }

    @Test
    public void testCommonCTypes() {
        Assertions.assertEquals(JAVA_IMPL.longAdd(100L, 200L), NATIVE_IMPL.longAdd(100L, 200L));
        Assertions.assertEquals(JAVA_IMPL.longAdd(Long.MAX_VALUE, 1L), NATIVE_IMPL.longAdd(Long.MAX_VALUE, 1L));
        Assertions.assertEquals(JAVA_IMPL.longAdd(Long.MIN_VALUE, -1L), NATIVE_IMPL.longAdd(Long.MIN_VALUE, -1L));
        Assertions.assertEquals(JAVA_IMPL.longLongAdd(100L, 200L), NATIVE_IMPL.longLongAdd(100L, 200L));
        Assertions.assertEquals(JAVA_IMPL.longLongAdd(Long.MAX_VALUE, 1L), NATIVE_IMPL.longLongAdd(Long.MAX_VALUE, 1L));
        Assertions.assertEquals(JAVA_IMPL.sizeTAdd(0L, 0L), NATIVE_IMPL.sizeTAdd(0L, 0L));
        Assertions.assertEquals(JAVA_IMPL.sizeTAdd(1000L, 2000L), NATIVE_IMPL.sizeTAdd(1000L, 2000L));
        Assertions.assertEquals(JAVA_IMPL.sizeTAdd(Long.MAX_VALUE, 1L), NATIVE_IMPL.sizeTAdd(Long.MAX_VALUE, 1L));
        Assertions.assertEquals(JAVA_IMPL.unsignedIntAdd(-1, -1), NATIVE_IMPL.unsignedIntAdd(-1, -1));
        Assertions.assertEquals(JAVA_IMPL.unsignedIntAdd(-1, 1), NATIVE_IMPL.unsignedIntAdd(-1, 1));
        Assertions.assertEquals(JAVA_IMPL.unsignedLongAdd(-1L, 1L), NATIVE_IMPL.unsignedLongAdd(-1L, 1L));
        Assertions.assertEquals(JAVA_IMPL.unsignedLongAdd(-1L, -1L), NATIVE_IMPL.unsignedLongAdd(-1L, -1L));
    }

    @Test
    public void testStrLenAndPointers() {
        MemorySegment s = Arena.ofAuto().allocateFrom("hello", StandardCharsets.UTF_8);
        Assertions.assertEquals(JAVA_IMPL.strLen(s), NATIVE_IMPL.strLen(s));
        MemorySegment p = Arena.ofAuto().allocate(ValueLayout.JAVA_INT, 1);
        Assertions.assertEquals(p.address(), NATIVE_IMPL.voidPtrIdentity(p).address());
        Assertions.assertEquals(p.address(), NATIVE_IMPL.pointerIdentity(p).address());
    }

    @Test
    public void testVoidNoop() {
        NATIVE_IMPL.voidNoop();
        JAVA_IMPL.voidNoop();
    }

    @Test
    public void testSizeOf() {
        Assertions.assertEquals(4, NATIVE_IMPL.sizeofInt());
        Assertions.assertEquals(Os.current() == Os.WINDOWS ? 4 : 8, NATIVE_IMPL.sizeofLong());
        Assertions.assertEquals(8, NATIVE_IMPL.sizeofSizeT());
        Assertions.assertEquals(8, NATIVE_IMPL.sizeofPointer());
    }
}
