package io.jingproject.bindingstest;

import io.jingproject.bindings.net.Mux;
import io.jingproject.bindings.net.Waker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

@Tag("require-native-library")
public class WakerTest {

    private static void testWaker(Waker waker) {
        Mux mux = Mux.newMux();
        mux.init();
        waker.init();
        try(Arena arena = Arena.ofConfined()) {
            int count = 1;
            int[] muxEvents = new int[count * 2];
            MemorySegment seg = arena.allocate(mux.layout(), count);
            mux.ctl(waker.socket(), Mux.MUX_NONE_FLAG, Mux.MUX_READABLE_FLAG, -1);
            waker.trigger().accept(1);
            int r = mux.poll(seg.address(), 1, -1, muxEvents);
            Assertions.assertEquals(1, r);
            Assertions.assertEquals(1, waker.consumer().getAsInt());
            Assertions.assertEquals(-1, muxEvents[0]);
            Assertions.assertEquals(Mux.MUX_READABLE_FLAG, muxEvents[1]);
        } finally {
            waker.close();
            mux.close();
        }
    }

    @Test
    @EnabledOnOs({OS.WINDOWS, OS.LINUX, OS.MAC})
    public void testUdsWaker() {
        testWaker(Waker.newUdsWaker());
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    public void testSpairWaker() {
        testWaker(Waker.newSpairWaker());
    }

    @Test
    @EnabledOnOs({OS.LINUX})
    public void testEfdWaker() {
        testWaker(Waker.newEfdWaker());
    }
}
