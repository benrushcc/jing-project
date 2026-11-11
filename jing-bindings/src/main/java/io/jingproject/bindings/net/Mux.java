package io.jingproject.bindings.net;

import io.jingproject.bindings.LinuxBindings;
import io.jingproject.bindings.MacosBindings;
import io.jingproject.bindings.PosixBindings;
import io.jingproject.bindings.WinBindings;
import io.jingproject.common.Os;
import io.jingproject.common.anno.Fragile;
import io.jingproject.ffm.ForeignException;
import io.jingproject.ffm.Libs;
import io.jingproject.ffm.NativeSegmentAccess;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.ValueLayout;

@Fragile
public sealed interface Mux permits Mux.WepollMux, Mux.EpollMux, Mux.KqueueMux {
    int MUX_NONE_FLAG = 0x0000;
    int MUX_READABLE_FLAG = 0x0001;
    int MUX_WRITEABLE_FLAG = 0x0002;
    int MUX_READABLE_AND_WRITEABLE_FLAG = MUX_READABLE_FLAG | MUX_WRITEABLE_FLAG;

    MemoryLayout layout();

    void init();

    void ctl(int socket, int from, int to, int data);

    int poll(long events, int maxEvents, int timeout, int[] muxEvents);

    void close();

    static Mux newWepollMux() {
        if(Os.current() != Os.WINDOWS) {
            throw new UnsupportedOperationException("wepoll is only supported on windows operating system");
        }
        return new WepollMux();
    }

    static Mux newEpollMux() {
        if(Os.current() != Os.LINUX) {
            throw new UnsupportedOperationException("epoll is only supported on linux operating system");
        }
        return new EpollMux();
    }

    static Mux newKqueueMux() {
        if(Os.current() != Os.MACOS) {
            throw new UnsupportedOperationException("kqueue is only supported on macos operating system");
        }
        return new KqueueMux();
    }

    static Mux newMux() {
        return switch (Os.current()) {
            case WINDOWS -> new WepollMux();
            case LINUX -> new EpollMux();
            case MACOS -> new KqueueMux();
        };
    }

    @Fragile
    final class EpollMux implements Mux {
        private static final LinuxBindings LINUX_BINDINGS = Libs.impl(LinuxBindings.class);
        private static final PosixBindings POSIX_BINDINGS = Libs.impl(PosixBindings.class);
        private static final MemoryLayout EPOLL_DATA_LAYOUT = MemoryLayout.unionLayout(
                ValueLayout.ADDRESS_UNALIGNED.withName("ptr"),
                ValueLayout.JAVA_INT_UNALIGNED.withName("fd"),
                ValueLayout.JAVA_INT_UNALIGNED.withName("u32"),
                ValueLayout.JAVA_LONG_UNALIGNED.withName("u64")
        );
        private static final MemoryLayout EPOLL_EVENT_LAYOUT = MemoryLayout.structLayout(
                ValueLayout.JAVA_INT_UNALIGNED.withName("events"),
                EPOLL_DATA_LAYOUT.withName("data")
        );
        private static final long EPOLL_EVENTS_OFFSET = EPOLL_EVENT_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("events"));
        private static final long EPOLL_DATA_OFFSET = EPOLL_EVENT_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("data"));

        private int epfd = -1;

        static {
            if (LINUX_BINDINGS == null) {
                throw new ExceptionInInitializerError("cannot initialize epoll bindings");
            }
            if (POSIX_BINDINGS == null) {
                throw new ExceptionInInitializerError("cannot initialize posix bindings");
            }
        }

        private value record CtlArgs(int op, int type) {

        }

        private static CtlArgs createCtlArgs(int from, int to) {
            if(to == Mux.MUX_NONE_FLAG) {
                return new CtlArgs(LINUX_BINDINGS.epollDel(), 0); // type could be safely ignored for EPOLL_CTL_DEL
            }
            int op = from == Mux.MUX_NONE_FLAG ? LINUX_BINDINGS.epollAdd() : LINUX_BINDINGS.epollMod();
            int type = 0;
            if ((to & Mux.MUX_READABLE_FLAG) != 0) {
                type |= LINUX_BINDINGS.epollIn();
            }
            if ((to & Mux.MUX_WRITEABLE_FLAG) != 0) {
                type |= LINUX_BINDINGS.epollOut();
            }
            return new CtlArgs(op, type);
        }

        @Override
        public MemoryLayout layout() {
            return EPOLL_EVENT_LAYOUT;
        }

        @Override
        public void init() {
            int fd = epfd;
            if(fd != -1) {
                throw new ForeignException("epoll mux state corrupted");
            }
            fd = LINUX_BINDINGS.epollCreate();
            if (fd < 0) {
                int err = -fd;
                throw new ForeignException("failed to create epoll mux, err : " + err);
            }
            epfd = fd;
        }

        @Override
        public void ctl(int socket, int from, int to, int data) {
            int fd = epfd;
            if (fd < 0) {
                throw new ForeignException("epoll mux state corrupted");
            }
            CtlArgs args = createCtlArgs(from, to);
            int r = LINUX_BINDINGS.epollCtl(fd, socket, args.op(), args.type(), data);
            if (r < 0) {
                int err = -r;
                throw new ForeignException("failed to ctl epoll mux, err : " + err);
            }
        }

        @Override
        public int poll(long events, int maxEvents, int timeout, int[] muxEvents) {
            int fd = epfd;
            if (fd < 0) {
                throw new ForeignException("epoll mux state corrupted");
            }
            int r = LINUX_BINDINGS.epollWait(fd, events, maxEvents, timeout);
            if(r < 0) {
                int err = -r;
                throw new ForeignException("failed to poll epoll mux, err : " + err);
            }
            int writeable = LINUX_BINDINGS.epollOut();
            int readable = LINUX_BINDINGS.epollIn() | LINUX_BINDINGS.epollRdhup() | LINUX_BINDINGS.epollHup() | LINUX_BINDINGS.epollErr();
            for(int index = 0; index < r; index++) {
                long base = index * EPOLL_EVENT_LAYOUT.byteSize();
                int event = NativeSegmentAccess.getInt(events, base + EPOLL_EVENTS_OFFSET);
                int data = NativeSegmentAccess.getInt(events, base + EPOLL_DATA_OFFSET);
                int eventType = Mux.MUX_NONE_FLAG;
                if((event & writeable) != 0) {
                    eventType |= Mux.MUX_WRITEABLE_FLAG;
                    event &= ~writeable;
                }
                if((event & readable) != 0) {
                    eventType |= Mux.MUX_READABLE_FLAG;
                    event &= ~readable;
                }
                if(event != 0) {
                    throw new ForeignException("unsupported event type : " + event);
                }
                int mbase = index * 2;
                muxEvents[mbase] = data;
                muxEvents[mbase + 1] = eventType;
            }
            return r;
        }

        @Override
        public void close() {
            int fd = epfd;
            if (fd < 0) {
                return ;
            }
            epfd = Integer.MIN_VALUE;
            int r = POSIX_BINDINGS.posixClose(fd);
            if (r < 0) {
                int err = -r;
                throw new ForeignException("failed to close epoll instance, err : " + err);
            }
        }
    }

    @Fragile
    final class KqueueMux implements Mux {
        private static final MacosBindings MACOS_BINDINGS = Libs.impl(MacosBindings.class);
        private static final PosixBindings POSIX_BINDINGS = Libs.impl(PosixBindings.class);
        private static final MemoryLayout KEVENT_LAYOUT = MemoryLayout.structLayout(
                ValueLayout.JAVA_LONG_UNALIGNED.withName("ident"),
                ValueLayout.JAVA_SHORT_UNALIGNED.withName("filter"),
                ValueLayout.JAVA_SHORT_UNALIGNED.withName("flags"),
                ValueLayout.JAVA_INT_UNALIGNED.withName("fflags"),
                ValueLayout.JAVA_LONG_UNALIGNED.withName("data"),
                ValueLayout.ADDRESS_UNALIGNED.withName("udata")
        ).withByteAlignment(4L);
        private static final long IDENT_OFFSET = KEVENT_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("ident"));
        private static final long FILTER_OFFSET = KEVENT_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("filter"));
        private int kqFd = -1;

        static {
            if (MACOS_BINDINGS == null) {
                throw new ExceptionInInitializerError("cannot initialize macos bindings");
            }
            if (POSIX_BINDINGS == null) {
                throw new ExceptionInInitializerError("cannot initialize posix bindings");
            }
        }

        @Override
        public MemoryLayout layout() {
            return KEVENT_LAYOUT;
        }

        @Override
        public void init() {
            int fd = kqFd;
            if(fd != -1) {
                throw new ForeignException("kqueue mux state corrupted");
            }
            fd = MACOS_BINDINGS.kqueue();
            if (fd < 0) {
                int err = -fd;
                throw new ForeignException("failed to create kqueue mux, err : " + err);
            }
            kqFd = fd;
        }

        private static int mod(int from, int to, int mask) {
            int f = from & mask;
            int t = to & mask;
            return t - f;
        }

        @Override
        public void ctl(int socket, int from, int to, int data) {
            int fd = kqFd;
            if(fd < 0) {
                throw new ForeignException("kqueue mux state corrupted");
            }
            int modRead = mod(from, to, Mux.MUX_READABLE_FLAG);
            int modWrite = mod(from, to, Mux.MUX_WRITEABLE_FLAG);
            int r = MACOS_BINDINGS.keventCtl(fd, socket, modRead, modWrite, data);
            if (r < 0) {
                int err = -r;
                throw new ForeignException("failed to ctl kqueue mux, err : " + err);
            }
        }

        @Override
        public int poll(long events, int maxEvents, int timeout, int[] muxEvents) {
            int fd = kqFd;
            if(fd < 0) {
                throw new ForeignException("kqueue mux state corrupted");
            }
            int r = MACOS_BINDINGS.keventWait(fd, events, maxEvents, timeout);
            if (r < 0) {
                int err = -r;
                throw new ForeignException("failed to poll kqueue mux, err : " + err);
            }
            int evfiltWrite = MACOS_BINDINGS.evfiltWrite();
            int evfiltRead = MACOS_BINDINGS.evfiltRead();
            for(int index = 0; index < r; index++) {
                long base = index * KEVENT_LAYOUT.byteSize();
                short filter = NativeSegmentAccess.getShort(events, base + FILTER_OFFSET);
                int data = (int) NativeSegmentAccess.getLong(events, base + IDENT_OFFSET);
                int eventType;
                if(filter == evfiltWrite) {
                    eventType = Mux.MUX_WRITEABLE_FLAG;
                } else if(filter == evfiltRead) {
                    eventType = Mux.MUX_READABLE_FLAG;
                } else {
                    throw new ForeignException("unsupported kqueue filter : " + filter);
                }
                int mbase = index * 2;
                muxEvents[mbase] = data;
                muxEvents[mbase + 1] = eventType;
            }
            return r;
        }

        @Override
        public void close() {
            int fd = kqFd;
            if(fd < 0) {
                return ;
            }
            kqFd = Integer.MIN_VALUE;
            int r = POSIX_BINDINGS.posixClose(fd);
            if (r < 0) {
                int err = -r;
                throw new ForeignException("failed to close kqueue mux, err : " + err);
            }
        }
    }

    @Fragile
    final class WepollMux implements Mux {
        private static final WinBindings WIN_BINDINGS = Libs.impl(WinBindings.class);
        private static final MemoryLayout WEPOLL_DATA_LAYOUT = MemoryLayout.unionLayout(
                ValueLayout.ADDRESS.withName("ptr"),
                ValueLayout.JAVA_INT.withName("fd"),
                ValueLayout.JAVA_INT.withName("u32"),
                ValueLayout.JAVA_LONG.withName("u64"),
                ValueLayout.ADDRESS.withName("sock"),
                ValueLayout.ADDRESS.withName("hnd")
        );
        private static final MemoryLayout WEPOLL_EVENT_LAYOUT = MemoryLayout.structLayout(
                ValueLayout.JAVA_INT.withName("events"),
                MemoryLayout.paddingLayout(4),
                WEPOLL_DATA_LAYOUT.withName("data")
        );
        private static final long WEPOLL_EVENTS_OFFSET = WEPOLL_EVENT_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("events"));
        private static final long WEPOLL_DATA_OFFSET = WEPOLL_EVENT_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("data"));

        static {
            if (WIN_BINDINGS == null) {
                throw new ExceptionInInitializerError("cannot initialize wepoll mux");
            }
        }

        private long handle = -1L;

        private value record CtlArgs(int op, int type) {

        }

        private static CtlArgs createCtlArgs(int from, int to) {
            if(to == Mux.MUX_NONE_FLAG) {
                return new CtlArgs(WIN_BINDINGS.wepollDel(), 0); // type could be safely ignored for EPOLL_CTL_DEL
            }
            int op = from == Mux.MUX_NONE_FLAG ? WIN_BINDINGS.wepollAdd() : WIN_BINDINGS.wepollMod();
            int type = 0;
            if ((to & Mux.MUX_READABLE_FLAG) != 0) {
                type |= WIN_BINDINGS.wepollIn();
            }
            if ((to & Mux.MUX_WRITEABLE_FLAG) != 0) {
                type |= WIN_BINDINGS.wepollOut();
            }
            return new CtlArgs(op, type);
        }

        @Override
        public MemoryLayout layout() {
            return WEPOLL_EVENT_LAYOUT;
        }

        @Override
        public void init() {
            long hd = handle;
            if(hd != -1L) {
                throw new ForeignException("wepoll mux state corrupted");
            }
            hd = WIN_BINDINGS.wepollCreate();
            if (NativeSegmentAccess.isErrPtr(hd)) {
                int err = NativeSegmentAccess.errCode(hd);
                throw new ForeignException("failed to create wepoll instance, err : " + err);
            }
            handle = hd;
        }

        @Override
        public void ctl(int socket, int from, int to, int data) {
            long hd = handle;
            if(hd < 0L) {
                throw new ForeignException("wepoll mux state corrupted");
            }
            CtlArgs args = createCtlArgs(from, to);
            int r = WIN_BINDINGS.wepollCtl(hd, socket, args.op(), args.type(), data);
            if (r < 0) {
                int err = -r;
                throw new ForeignException("failed to ctl wepoll mux, err : " + err);
            }
        }

        @Override
        public int poll(long events, int maxEvents, int timeout, int[] muxEvents) {
            long hd = handle;
            if(hd < 0L) {
                throw new ForeignException("wepoll mux state corrupted");
            }
            int r = WIN_BINDINGS.wepollWait(hd, events, maxEvents, timeout);
            if (r < 0) {
                int err = -r;
                throw new ForeignException("failed to poll wepoll mux, err : " + err);
            }
            int writeable = WIN_BINDINGS.wepollOut();
            int readable = WIN_BINDINGS.wepollIn() | WIN_BINDINGS.wepollRdhup() | WIN_BINDINGS.wepollHup() | WIN_BINDINGS.wepollErr();
            for(int index = 0; index < r; index++) {
                long base = index * WEPOLL_EVENT_LAYOUT.byteSize();
                int event = NativeSegmentAccess.getInt(events, base + WEPOLL_EVENTS_OFFSET);
                int data = NativeSegmentAccess.getInt(events, base + WEPOLL_DATA_OFFSET);
                int eventType = Mux.MUX_NONE_FLAG;
                if((event & writeable) != 0) {
                    eventType |= Mux.MUX_WRITEABLE_FLAG;
                    event &= ~writeable;
                }
                if((event & readable) != 0) {
                    eventType |= Mux.MUX_READABLE_FLAG;
                    event &= ~readable;
                }
                if(event != 0) {
                    throw new ForeignException("unsupported event type : " + event);
                }
                int mbase = index * 2;
                muxEvents[mbase] = data;
                muxEvents[mbase + 1] = eventType;
            }
            return r;
        }

        @Override
        public void close() {
            long hd = handle;
            if(hd < 0L) {
                return ;
            }
            handle = Long.MIN_VALUE;
            int r = WIN_BINDINGS.wepollClose(hd);
            if (r < 0) {
                int err = -r;
                throw new ForeignException("failed to close wepoll mux, err : " + err);
            }
        }
    }
}
