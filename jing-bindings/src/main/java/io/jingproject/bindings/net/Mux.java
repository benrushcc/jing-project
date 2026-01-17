package io.jingproject.bindings.net;

import io.jingproject.common.Descriptor;

import java.lang.foreign.MemorySegment;

public interface Mux {
    int MUX_NONE_FLAG = 0x0000;
    int MUX_READABLE_FLAG = 0x0001;
    int MUX_WRITEABLE_FLAG = 0x0002;
    int MUX_READABLE_AND_WRITEABLE_FLAG = MUX_READABLE_FLAG | MUX_WRITEABLE_FLAG;

    void init();

    // data在创建之后就不允许发生变化，否则是ub
    void ctl(Descriptor descriptor, int from, int to, int data);

    void poll(MemorySegment events, int maxEvents, int timeout);

    void close();
}
