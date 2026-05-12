package io.jingproject.bindings.net;

import io.jingproject.bindings.LinuxBindings;
import io.jingproject.bindings.PosixBindings;
import io.jingproject.common.Descriptor;
import io.jingproject.common.anno.Fragile;
import io.jingproject.ffm.ForeignException;
import io.jingproject.ffm.Libs;

import java.lang.foreign.MemorySegment;

@Fragile
public final class EpollMux implements Mux {
    private static final LinuxBindings LINUX_BINDINGS = Libs.getImpl(LinuxBindings.class);
    private static final PosixBindings SYS_POSIX_BINDINGS = Libs.getImpl(PosixBindings.class);
    private int epfd = 0;

    static {
        if (LINUX_BINDINGS == null) {
            throw new ExceptionInInitializerError("cannot initialize epoll bindings");
        }
        if (SYS_POSIX_BINDINGS == null) {
            throw new ExceptionInInitializerError("cannot initialize sys_posix bindings");
        }
    }

    @Override
    public void init() {
        if (epfd == Integer.MIN_VALUE) {
            throw new IllegalStateException("epollMux already closed");
        }
        if (epfd > 0) {
            throw new IllegalStateException("epollMux already initialized");
        }
        int v = LINUX_BINDINGS.epollCreate();
        if (v < 0) {
            int err = Math.abs(v);
            throw new ForeignException("failed to create epoll instance, err : " + err);
        }
        epfd = v;
    }

    private static int getOp(int from, int to) {
        assert from != to;
        if (from == Mux.MUX_NONE_FLAG) {
            return LINUX_BINDINGS.epollAdd();
        } else if (to == Mux.MUX_NONE_FLAG) {
            return LINUX_BINDINGS.epollDel();
        } else {
            return LINUX_BINDINGS.epollMod();
        }
    }

    private static int getEventTypes(int op, int to) {
        if (op == LINUX_BINDINGS.epollDel()) {
            return Integer.MIN_VALUE; // safely ignored
        }
        int r = 0;
        if ((to & Mux.MUX_READABLE_FLAG) != 0) {
            r |= LINUX_BINDINGS.epollIn();
        }
        if ((to & Mux.MUX_WRITEABLE_FLAG) != 0) {
            r |= LINUX_BINDINGS.epollOut();
        }
        return r;
    }

    @Override
    public void ctl(Descriptor descriptor, int from, int to, int data) {
        if (epfd == Integer.MIN_VALUE) {
            throw new IllegalStateException("epollMux already closed");
        }
        if (epfd == 0) {
            throw new IllegalStateException("epollMux not initialized");
        }
        int op = getOp(from, to);
        int eventTypes = getEventTypes(op, to);
        int err = LINUX_BINDINGS.epollCtl(epfd, descriptor.asInt(), op, eventTypes, data);
        if (err > 0) {
            throw new ForeignException("failed to ctl epoll instance, err : " + err);
        }
    }

    @Override
    public void poll(MemorySegment events, int maxEvents, int timeout) {

    }

    @Override
    public void close() {
        if (epfd == Integer.MIN_VALUE) {
            throw new IllegalStateException("EpollMux already closed");
        }
        if (epfd == 0) {
            return;
        }
        int err = SYS_POSIX_BINDINGS.posixClose(epfd);
        if (err > 0) {
            throw new ForeignException("failed to close epoll instance, err : " + err);
        }
    }
}
