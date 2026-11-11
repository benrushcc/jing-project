package io.jingproject.bindings.net;

import io.jingproject.bindings.LinuxBindings;
import io.jingproject.bindings.PosixBindings;
import io.jingproject.common.Os;
import io.jingproject.ffm.ForeignException;
import io.jingproject.ffm.Libs;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public sealed interface Waker permits Waker.UdsWaker, Waker.EfdWaker, Waker.SpairWaker {

    Supplier<Waker> WAKER_SUPPLIER = makeWakerSupplier();

    static Supplier<Waker> makeWakerSupplier() {
        String wakerType = System.getProperty("jing.net.waker", "");
        Os current = Os.current();
        if(wakerType.equals("efd")) {
            if(current != Os.LINUX) {
                throw new ForeignException("eventfd not supported");
            }
            return EfdWaker::new;
        }
        if(wakerType.equals("spair")) {
            if(current == Os.WINDOWS) {
                throw new ForeignException("spair not supported");
            }
            return SpairWaker::new;
        }
        return UdsWaker::new;
    }

    static Waker newUdsWaker() {
        return new UdsWaker();
    }

    static Waker newEfdWaker() {
        if(Os.current() != Os.LINUX) {
            throw new UnsupportedOperationException("efdWaker is only supported on linux operating system");
        }
        return new EfdWaker();
    }

    static Waker newSpairWaker() {
        if(Os.current() != Os.LINUX && Os.current() != Os.MACOS) {
            throw new UnsupportedOperationException("spairWaker is only supported on linux or macos operating system");
        }
        return new SpairWaker();
    }

    static Waker newWaker() {
        return WAKER_SUPPLIER.get();
    }

    void init();

    int socket();

    IntSupplier consumer();

    IntConsumer trigger();

    void close();

    final class UdsWaker implements Waker {
        private static final NetProvider NET_PROVIDER = NetProvider.getInstance();
        private static final AtomicInteger COUNTER = new AtomicInteger(0);
        private static final String PREFIX = "jing_waker";
        private static final String SUFFIX = ".sock";
        private static final String TMP_DIR = System.getProperty("java.io.tmpdir");

        private String sockPath = null;
        private int serverSocket = -1;
        private int clientSocket = -1;
        private int acceptSocket = -1;

        @Override
        public void init() {
            try (Arena arena = Arena.ofConfined()) {
                int counter = COUNTER.getAndIncrement();
                Path p = Path.of(TMP_DIR, PREFIX + ProcessHandle.current().pid() + "_" + counter + SUFFIX).toAbsolutePath();
                Files.deleteIfExists(p);
                sockPath = p.toString();
                int afUnixCode = NET_PROVIDER.afUnixCode();
                int sockStreamCode = NET_PROVIDER.sockStreamCode();
                serverSocket = NET_PROVIDER.socket(afUnixCode, sockStreamCode, 0);
                if(serverSocket < 0) {
                    int err = -serverSocket;
                    throw new ForeignException("failed to create uds server socket, err : " + err);
                }
                clientSocket = NET_PROVIDER.socket(afUnixCode, sockStreamCode, 0);
                if(clientSocket < 0) {
                    int err = -clientSocket;
                    throw new ForeignException("failed to create uds client socket, err : " + err);
                }
                MemorySegment seg = arena.allocateFrom(sockPath);
                int serverSocketBindResult = NET_PROVIDER.udsBind(serverSocket, seg.address(), seg.byteSize());
                if(serverSocketBindResult < 0) {
                    int err = -serverSocketBindResult;
                    throw new ForeignException("failed to bind uds server socket, err : " + err);
                }
                int serverSocketListenResult = NET_PROVIDER.listen(serverSocket, 1);
                if(serverSocketListenResult < 0) {
                    int err = -serverSocketListenResult;
                    throw new ForeignException("failed to listen uds server socket, err : " + err);
                }
                int clientSocketConnectResult = NET_PROVIDER.udsConnect(clientSocket, seg.address(), seg.byteSize());
                if(clientSocketConnectResult < 0) {
                    int err = -clientSocketConnectResult;
                    throw new ForeignException("failed to connect uds client socket, err : " + err);
                }
                acceptSocket = NET_PROVIDER.udsAccept(serverSocket);
                if(acceptSocket < 0) {
                    int err = -acceptSocket;
                    throw new ForeignException("failed to accept uds server socket, err : " + err);
                }
                int cr = NET_PROVIDER.setNonBlocking(clientSocket);
                if(cr < 0) {
                    int err = -cr;
                    throw new ForeignException("failed to set uds client socket as non-blocking, err : " + err);
                }
                int ar = NET_PROVIDER.setNonBlocking(acceptSocket);
                if(ar < 0) {
                    int err = -ar;
                    throw new ForeignException("failed to set uds accepted socket as non-blocking, err : " + err);
                }
            } catch (IOException e) {
                throw new ForeignException("cound not init uds waker", e);
            }
        }

        @Override
        public int socket() {
            int sock = acceptSocket;
            if(sock < 0) {
                throw new ForeignException("uds waker state corrupted");
            }
            return sock;
        }

        @Override
        public IntSupplier consumer() {
            int sock = acceptSocket;
            if(sock < 0) {
                throw new ForeignException("uds waker state corrupted");
            }
            return () -> {
                int r = NET_PROVIDER.readInt(sock);
                if(r < 0) {
                    int err = -r;
                    throw new ForeignException("failed to read uds accepted socket, err : " + err);
                }
                return r;
            };
        }

        @Override
        public IntConsumer trigger() {
            int sock = clientSocket;
            if(sock < 0) {
                throw new ForeignException("uds waker state corrupted");
            }
            return events -> {
                int r = NET_PROVIDER.writeInt(sock, events);
                if(r < 0) {
                    int err = -r;
                    throw new ForeignException("failed to write uds client socket, err : " + err);
                }
            };
        }

        @Override
        public void close() {
            int err1 = 0;
            int err2 = 0;
            int err3 = 0;
            IOException cause = null;
            int fd1 = acceptSocket;
            if(fd1 >= 0) {
                acceptSocket = Integer.MIN_VALUE;
                int r = NET_PROVIDER.closeSocket(fd1);
                if(r < 0) {
                    err1 = -r;
                }
            }
            int fd2 = clientSocket;
            if(fd2 >= 0) {
                clientSocket = Integer.MIN_VALUE;
                int r = NET_PROVIDER.closeSocket(fd2);
                if(r < 0) {
                    err2 = -r;
                }
            }
            int fd3 = serverSocket;
            if(fd3 >= 0) {
                serverSocket = Integer.MIN_VALUE;
                int r = NET_PROVIDER.closeSocket(fd3);
                if(r < 0) {
                    err3 = -r;
                }
            }
            String sp = sockPath;
            if(sp != null) {
                sockPath = null;
                try {
                    Files.deleteIfExists(Path.of(sp));
                } catch (IOException e) {
                    cause = e;
                }
            }
            if(err1 != 0 || err2 != 0 || err3 != 0 || cause != null) {
                throw new ForeignException(
                        "failed to close uds waker, err1: " + err1 +
                                ", err2: " + err2 + ", err3: " + err3 +
                                ", file: " + (cause != null ? cause.toString() : "ok"),
                        cause);
            }
        }
    }

    final class EfdWaker implements Waker {
        private static final LinuxBindings LINUX_BINDINGS = Libs.impl(LinuxBindings.class);
        private static final PosixBindings POSIX_BINDINGS = Libs.impl(PosixBindings.class);

        private int efd = -1;

        static {
            if (LINUX_BINDINGS == null) {
                throw new ExceptionInInitializerError("cannot initialize epoll bindings");
            }
            if (POSIX_BINDINGS == null) {
                throw new ExceptionInInitializerError("cannot initialize posix bindings");
            }
        }

        @Override
        public void init() {
            int fd = efd;
            if(fd != -1) {
                throw new ForeignException("efd waker state corrupted");
            }
            int efdCloexec = LINUX_BINDINGS.efdCloexec();
            int efdNonblock = LINUX_BINDINGS.efdNonblock();
            fd = LINUX_BINDINGS.eventfd(efdCloexec | efdNonblock);
            if(fd < 0) {
                int err = -fd;
                throw new ForeignException("failed to create eventfd, err : " + err);
            }
            efd = fd;
        }

        @Override
        public int socket() {
            int fd = efd;
            if(fd < 0) {
                throw new ForeignException("efd waker state corrupted");
            }
            return fd;
        }

        @Override
        public IntSupplier consumer() {
            int fd = efd;
            if(fd < 0) {
                throw new ForeignException("efd waker state corrupted");
            }
            return () -> {
                int r = LINUX_BINDINGS.readEfd(fd);
                if(r < 0) {
                    int err = -r;
                    throw new ForeignException("failed to read eventfd, err : " + err);
                }
                return r;
            };
        }

        @Override
        public IntConsumer trigger() {
            int fd = efd;
            if(fd < 0) {
                throw new ForeignException("efd waker state corrupted");
            }
            return events -> {
                int r = LINUX_BINDINGS.writeEfd(fd, events);
                if(r < 0) {
                    int err = -r;
                    throw new ForeignException("failed to write eventfd, err : " + err);
                }
            };
        }

        @Override
        public void close() {
            int fd = efd;
            if(fd < 0) {
                return ;
            }
            efd = Integer.MIN_VALUE;
            int r = POSIX_BINDINGS.posixClose(fd);
            if (r < 0) {
                int err = -r;
                throw new ForeignException("failed to close eventfd, err : " + err);
            }
        }
    }

    final class SpairWaker implements Waker {
        private static final NetProvider NET_PROVIDER = NetProvider.getInstance();
        private static final PosixBindings POSIX_BINDINGS = Libs.impl(PosixBindings.class);

        static {
            if (POSIX_BINDINGS == null) {
                throw new ExceptionInInitializerError("cannot initialize posix bindings");
            }
        }

        private int fd0 = -1;
        private int fd1 = -1;

        @Override
        public void init() {
            int f0 = fd0;
            int f1 = fd1;
            if(f0 != -1 || f1 != -1) {
                throw new ForeignException("spair waker state corrupted");
            }
            int afUnixCode = NET_PROVIDER.afUnixCode();
            int sockStreamCode = NET_PROVIDER.sockStreamCode();
            long socketpair = POSIX_BINDINGS.socketpair(afUnixCode, sockStreamCode);
            f0 = (int) (socketpair >>> 32);
            f1 = (int) socketpair;
            if(f0 < 0) {
                int err = -f0;
                throw new ForeignException("failed to create spair, err : " + err);
            }
            fd0 = f0;
            fd1 = f1;
            int r0 = NET_PROVIDER.setNonBlocking(f0);
            if(r0 < 0) {
                int err = -r0;
                throw new ForeignException("failed to set fd0 as non-blocking, err : " + err);
            }
            int r1 = NET_PROVIDER.setNonBlocking(f1);
            if(r1 < 0) {
                int err = -r1;
                throw new ForeignException("failed to set fd1 as non-blocking, err : " + err);
            }
        }

        @Override
        public int socket() {
            int fd = fd0;
            if(fd < 0) {
                throw new ForeignException("spair waker state corrupted");
            }
            return fd;
        }

        @Override
        public IntSupplier consumer() {
            int fd = fd0;
            if(fd < 0) {
                throw new ForeignException("spair waker state corrupted");
            }
            return () -> {
                int r = NET_PROVIDER.readInt(fd);
                if(r < 0) {
                    int err = -r;
                    throw new ForeignException("failed to read spair, err : " + err);
                }
                return r;
            };
        }

        @Override
        public IntConsumer trigger() {
            int fd = fd1;
            if(fd < 0) {
                throw new ForeignException("spair waker state corrupted");
            }
            return events -> {
                int r = NET_PROVIDER.writeInt(fd, events);
                if(r < 0) {
                    int err = -r;
                    throw new ForeignException("failed to write spair, err : " + err);
                }
            };
        }

        @Override
        public void close() {
            int f0 = fd0;
            int f1 = fd1;
            int err0 = 0;
            int err1 = 0;
            if(f0 >= 0) {
                fd0 = Integer.MIN_VALUE;
                int r = NET_PROVIDER.closeSocket(f0);
                if(r < 0) {
                    err0 = -r;
                }
            }
            if(f1 >= 0) {
                fd1 = Integer.MIN_VALUE;
                int r = NET_PROVIDER.closeSocket(f1);
                if(r < 0) {
                    err1 = -r;
                }
            }
            if(err0 != 0 || err1 != 0) {
                throw new ForeignException("failed to close spair, err0: " + err0 + ", err1: " + err1);
            }
        }
    }

}
