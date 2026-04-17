package io.jingproject.ffmtest.test;

import io.jingproject.ffmtest.entity.DemoBinding;
import io.jingproject.ffmtest.entity.DemoBindingJavaImpl;
import io.jingproject.ffmtest.entity.DemoLibs;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Tag("require-native-library")
public class DemoTest {
    private static final DemoBinding NATIVE_IMPL = Objects.requireNonNull(DemoLibs.getImpl(DemoBinding.class), "Failed to load jing_demo library");
    private static final DemoBinding JAVA_IMPL = new DemoBindingJavaImpl();

    @Test
    public void testSingleInt() {
        Assertions.assertEquals(JAVA_IMPL.singleInt(), NATIVE_IMPL.singleInt());
    }

    @Test
    public void testComputeAdd() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for(int i = 0; i < 1000; i++) {
            int a = random.nextInt(0, Integer.MAX_VALUE);
            int b = random.nextInt(-Integer.MAX_VALUE, 0);
            Assertions.assertEquals(JAVA_IMPL.computeAdd(a, b), NATIVE_IMPL.computeAdd(a, b));
        }
    }

    @Test
    public void testComputePointer() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for(int i = 0; i < 1000; i++) {
            int a = random.nextInt(0, Integer.MAX_VALUE);
            int b = random.nextInt(0, Integer.MAX_VALUE);
            MemorySegment m1 = Arena.ofAuto().allocateFrom(ValueLayout.JAVA_INT, a);
            MemorySegment m2 = Arena.ofAuto().allocateFrom(ValueLayout.JAVA_INT, b);
            Assertions.assertEquals(JAVA_IMPL.computePointer(m1, m2), NATIVE_IMPL.computePointer(m1, m2));
        }
    }
}
