package io.jingproject.bindingstest.test;

import io.jingproject.bindingstest.entity.DemoBinding;
import io.jingproject.bindingstest.entity.DemoBindingImpl;
import io.jingproject.ffm.ForeignException;
import io.jingproject.ffm.Libs;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Tag("require-native-library")
public class DemoTest {
    private static final DemoBinding NATIVE_IMPL = Objects.requireNonNull(Libs.getImpl(DemoBinding.class), "Failed to load jing_demo library");
    private static final DemoBinding JAVA_IMPL = new DemoBindingImpl();
    private static final int BATCH = 10000;

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
}
