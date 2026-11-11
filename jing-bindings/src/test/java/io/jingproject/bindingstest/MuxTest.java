package io.jingproject.bindingstest;

import io.jingproject.bindings.net.Mux;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

@Tag("require-native-library")
public class MuxTest {

    private static void testMuxAccess(Mux mux) {
        int count = 32;
        int[] muxEvents = new int[count * 2];
        mux.init();
        try(Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocate(mux.layout(), count);
            int r = mux.poll(seg.address(), count, 0, muxEvents);
            Assertions.assertEquals(0, r);
        } finally {
            mux.close();
        }
    }

    @Test
    @EnabledOnOs({OS.WINDOWS})
    public void testMuxForWin() {
        testMuxAccess(Mux.newWepollMux());
    }

    @Test
    @EnabledOnOs({OS.LINUX})
    public void testMuxForLinux() {
        testMuxAccess(Mux.newEpollMux());
    }

    @Test
    @EnabledOnOs({OS.MAC})
    public void testMuxForMacos() {
        testMuxAccess(Mux.newKqueueMux());
    }
}
