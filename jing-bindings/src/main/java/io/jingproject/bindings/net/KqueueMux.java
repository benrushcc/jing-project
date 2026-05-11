package io.jingproject.bindings.net;

import io.jingproject.bindings.KqueueBindings;
import io.jingproject.bindings.PosixBindings;
import io.jingproject.common.Descriptor;
import io.jingproject.common.anno.Fragile;
import io.jingproject.ffm.ForeignException;
import io.jingproject.ffm.Libs;

import java.lang.foreign.MemorySegment;

@Fragile
public final class KqueueMux implements Mux {
    private static final KqueueBindings KQUEUE_BINDINGS = Libs.getImpl(KqueueBindings.class);
    private static final PosixBindings SYS_POSIX_BINDINGS = Libs.getImpl(PosixBindings.class);
    private int kqFd = 0;

    @Override
    public void init() {
        if (kqFd == Integer.MIN_VALUE) {
            throw new IllegalStateException("kqueueMux already closed");
        }
        if (kqFd > 0) {
            throw new IllegalStateException("kqueueMux already initialized");
        }
        int v = KQUEUE_BINDINGS.kqueue();
        if (v < 0) {
            int err = Math.abs(v);
            throw new ForeignException("failed to create kqueue instance, err : " + err);
        }
        kqFd = v;
    }

    private static int mod(int from, int to, int mask) {
        int f = from & mask;
        int t = to & mask;
        return Math.subtractExact(t, f);
    }

    @Override
    public void ctl(Descriptor descriptor, int from, int to, int data) {
        if (kqFd == Integer.MIN_VALUE) {
            throw new IllegalStateException("KqueueMux already closed");
        }
        if (kqFd == 0) {
            throw new IllegalStateException("KqueueMux not initialized");
        }
        int modRead = mod(from, to, Mux.MUX_READABLE_FLAG);
        int modWrite = mod(from, to, Mux.MUX_WRITEABLE_FLAG);
        int err = KQUEUE_BINDINGS.keventCtl(kqFd, descriptor.asInt(), modRead, modWrite, MemorySegment.ofAddress(data));
        if (err > 0) {
            throw new ForeignException("failed to ctl kqueue instance, err : " + err);
        }
    }

    @Override
    public void poll(MemorySegment events, int maxEvents, int timeout) {

    }

    @Override
    public void close() {
        if (kqFd == Integer.MIN_VALUE) {
            throw new IllegalStateException("KqueueMux already closed");
        }
        if (kqFd == 0) {
            return;
        }
        int err = SYS_POSIX_BINDINGS.posixClose(kqFd);
        if (err > 0) {
            throw new ForeignException("failed to close kqueue instance, err : " + err);
        }
    }
}
