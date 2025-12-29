package io.jingproject.bindings.net;

import io.jingproject.bindings.EpollBindings;
import io.jingproject.bindings.SysPosixBindings;
import io.jingproject.common.Descriptor;
import io.jingproject.ffm.ForeignException;
import io.jingproject.ffm.SharedLibs;

import java.lang.foreign.MemorySegment;

public final class EpollMux implements Mux {
    private static final EpollBindings EPOLL_BINDINGS = SharedLibs.getImpl(EpollBindings.class);
    private static final SysPosixBindings SYS_POSIX_BINDINGS = SharedLibs.getImpl(SysPosixBindings.class);
    private int epfd = 0;
    @Override
    public void init() {
        if(epfd == Integer.MIN_VALUE) {
            throw new IllegalStateException("EpollMux already closed");
        }
        if(epfd > 0) {
            throw new IllegalStateException("EpollMux already initialized");
        }
        int v = EPOLL_BINDINGS.epollCreate();
        if(v < 0) {
            int err = Math.abs(v);
            throw new ForeignException("Failed to create epoll instance, err : " + err);
        }
        epfd = v;
    }

    private static int getOp(int from, int to) {
        assert from != to;
        if(from == Mux.MUX_NONE_FLAG) {
            return EPOLL_BINDINGS.epollAdd();
        }else if(to == Mux.MUX_NONE_FLAG) {
            return EPOLL_BINDINGS.epollDel();
        }else {
            return EPOLL_BINDINGS.epollMod();
        }
    }

    private static int getEventTypes(int op, int to) {
        if(op == EPOLL_BINDINGS.epollDel()) {
            return Integer.MIN_VALUE; // safely ignored
        }
        int r = 0;
        if((to & Mux.MUX_READABLE_FLAG) != 0) {
            r |= EPOLL_BINDINGS.epollIn();
        }
        if((to & Mux.MUX_WRITEABLE_FLAG) != 0) {
            r |= EPOLL_BINDINGS.epollOut();
        }
        return r;
    }

    @Override
    public void ctl(Descriptor descriptor, int from, int to, int data) {
        if(epfd == Integer.MIN_VALUE) {
            throw new IllegalStateException("EpollMux already closed");
        }
        if(epfd == 0) {
            throw new IllegalStateException("EpollMux not initialized");
        }
        int op = getOp(from, to);
        int eventTypes = getEventTypes(op, to);
        int err = EPOLL_BINDINGS.epollCtl(epfd, descriptor.asInt(), op, eventTypes, data);
        if(err > 0) {
            throw new ForeignException("Failed to ctl epoll instance, err : " + err);
        }
    }

    @Override
    public void poll(MemorySegment events, int maxEvents, int timeout) {

    }

    @Override
    public void close() {
        if(epfd == Integer.MIN_VALUE) {
            throw new IllegalStateException("EpollMux already closed");
        }
        if(epfd == 0) {
            return ;
        }
        int err = SYS_POSIX_BINDINGS.posixClose(epfd);
        if(err > 0) {
            throw new ForeignException("Failed to close epoll instance, err : " + err);
        }
    }
}
