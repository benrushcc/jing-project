package io.jingproject.bindings.net;

import io.jingproject.bindings.WinBindings;
import io.jingproject.common.Descriptor;
import io.jingproject.common.anno.Fragile;
import io.jingproject.ffm.ForeignException;
import io.jingproject.ffm.Libs;
import io.jingproject.ffm.NativeSegmentAccess;

import java.lang.foreign.MemorySegment;

@Fragile
public final class WepollMux implements Mux {
    private static final WinBindings WIN_BINDINGS = Libs.getImpl(WinBindings.class);
    private MemorySegment epfd = MemorySegment.NULL;

    static {
        if (WIN_BINDINGS == null) {
            throw new ExceptionInInitializerError("cannot initialize WepollMux");
        }
    }

    @Override
    public void init() {
        if (epfd == null) {
            throw new IllegalStateException("wepollMux already closed");
        }
        if (epfd.address() != 0L) {
            throw new IllegalStateException("wepollMux already initialized");
        }
        MemorySegment fd = WIN_BINDINGS.wepollCreate();
        if (NativeSegmentAccess.isErrPtr(fd)) {
            int err = NativeSegmentAccess.errCode(fd);
            throw new ForeignException("failed to create wepoll instance, err : " + err);
        }
        epfd = fd;
    }

    private static int getOp(int from, int to) {
        assert from != to;
        if (from == Mux.MUX_NONE_FLAG) {
            return WIN_BINDINGS.wepollAdd();
        } else if (to == Mux.MUX_NONE_FLAG) {
            return WIN_BINDINGS.wepollDel();
        } else {
            return WIN_BINDINGS.wepollMod();
        }
    }

    private static int getEventTypes(int op, int to) {
        if (op == WIN_BINDINGS.wepollDel()) {
            return Integer.MIN_VALUE; // safely ignored
        }
        int r = 0;
        if ((to & Mux.MUX_READABLE_FLAG) != 0) {
            r |= WIN_BINDINGS.wepollIn();
        }
        if ((to & Mux.MUX_WRITEABLE_FLAG) != 0) {
            r |= WIN_BINDINGS.wepollOut();
        }
        return r;
    }

    @Override
    public void ctl(Descriptor descriptor, int from, int to, int data) {
        if (epfd == null) {
            throw new IllegalStateException("wepollMux already closed");
        }
        if (epfd.address() == 0L) {
            throw new IllegalStateException("wepollMux not initialized");
        }
        int op = getOp(from, to);
        int eventTypes = getEventTypes(op, to);
        int err = WIN_BINDINGS.wepollCtl(epfd, descriptor.asLong(), op, eventTypes, data);
        if (err > 0) {
            throw new ForeignException("failed to ctl wepoll instance, err : " + err);
        }
    }

    @Override
    public void poll(MemorySegment events, int maxEvents, int timeout) {

    }

    @Override
    public void close() {
        if (epfd == null) {
            throw new IllegalStateException("WepollMux already closed");
        }
        if (epfd.address() == 0L) {
            return;
        }
        int err = WIN_BINDINGS.wepollClose(epfd);
        if (err != 0) {
            throw new ForeignException("failed to close wepoll instance, err : " + err);
        }
    }
}
