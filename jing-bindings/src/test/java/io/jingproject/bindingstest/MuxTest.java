package io.jingproject.bindingstest;

import io.jingproject.bindings.net.Mux;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

@Tag("require-native-library")
public class MuxTest {

    @Test
    public void muxImmediatePollTest() {
        try(Mux mux = Mux.newMux()) {
            int count = 32;
            int[] muxEvents = new int[count * 2];
            mux.init();
            try(Arena arena = Arena.ofConfined()) {
                MemorySegment seg = arena.allocate(mux.layout(), count);
                int r = mux.poll(seg.address(), count, 0, muxEvents);
                Assertions.assertEquals(0, r);
            }
        }
    }
}
