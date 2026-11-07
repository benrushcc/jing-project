package io.jingproject.bindings.net;

import io.jingproject.bindings.PosixBindings;
import io.jingproject.bindings.WinBindings;
import io.jingproject.common.Os;
import io.jingproject.ffm.Libs;

public interface NetProvider {

    LazyConstant<NetProvider> NET_PROVIDER_HOLDER = LazyConstant.of(() -> switch (Os.current()) {
        case WINDOWS -> new WinNetProvider();
        case LINUX, MACOS -> new PosixNetProvider();
    });

    static NetProvider getInstance() {
        return NET_PROVIDER_HOLDER.get();
    }

    int connectBlockCode();

    int sendBlockCode();

    int afInetCode();

    int afInet6Code();

    int afUnixCode();

    int solSocketCode();

    int sockStreamCode();

    int sockDgramCode();

    int ipprotoTcpCode();

    int ipprotoUdpCode();

    int ipprotoIpv6Code();

    int soReuseAddrCode();

    int tcpNoDelayCode();

    int ipv6OnlyCode();

    int socket(int domain, int type, int protocol);

    int closeSocket(int socket);

    int setSockopt(int socket, int level, int opt, long val, int len);

    int getErrOpt(int socket);

    int setNonBlocking(int socket);

    int bind(int socket, int type, long addr, int port);

    int listen(int socket, int backlog);

    int accept(int socket, long buf);

    int connect(int socket, int type, long addr, int port);

    int recv(int socket, long buf, int len);

    int send(int socket, long buf, int len);

    int readInt(int socket);

    int writeInt(int socket, int value);

    int udsBind(int socket, long addr, long len);

    int udsConnect(int socket, long addr, long len);

    int udsAccept(int socket);

    final class PosixNetProvider implements NetProvider {
        private static final PosixBindings POSIX_BINDINGS = Libs.impl(PosixBindings.class);

        static {
            if (POSIX_BINDINGS == null) {
                throw new ExceptionInInitializerError("cannot initialize net provider for posix");
            }
        }

        @Override
        public int connectBlockCode() {
            return POSIX_BINDINGS.connectBlockCode();
        }

        @Override
        public int sendBlockCode() {
            return POSIX_BINDINGS.sendBlockCode();
        }

        @Override
        public int afInetCode() {
            return POSIX_BINDINGS.afInetCode();
        }

        @Override
        public int afInet6Code() {
            return POSIX_BINDINGS.afInet6Code();
        }

        @Override
        public int afUnixCode() {
            return POSIX_BINDINGS.afUnixCode();
        }

        @Override
        public int solSocketCode() {
            return POSIX_BINDINGS.solSocket();
        }

        @Override
        public int sockStreamCode() {
            return POSIX_BINDINGS.tcpTypeCode();
        }

        @Override
        public int sockDgramCode() {
            return POSIX_BINDINGS.udpTypeCode();
        }

        @Override
        public int ipprotoTcpCode() {
            return POSIX_BINDINGS.tcpProtocolCode();
        }

        @Override
        public int ipprotoUdpCode() {
            return POSIX_BINDINGS.udpProtocolCode();
        }

        @Override
        public int ipprotoIpv6Code() {
            return POSIX_BINDINGS.ipprotoIpv6Code();
        }

        @Override
        public int soReuseAddrCode() {
            return POSIX_BINDINGS.soReuseaddr();
        }

        @Override
        public int tcpNoDelayCode() {
            return POSIX_BINDINGS.tcpNodelay();
        }

        @Override
        public int ipv6OnlyCode() {
            return POSIX_BINDINGS.ipv6V6only();
        }

        @Override
        public int socket(int domain, int type, int protocol) {
            return POSIX_BINDINGS.socket(domain, type, protocol);
        }

        @Override
        public int closeSocket(int socket) {
            return POSIX_BINDINGS.posixClose(socket);
        }

        @Override
        public int setSockopt(int socket, int level, int opt, long val, int len) {
            return POSIX_BINDINGS.setSockOpt(socket, level, opt, val, len);
        }

        @Override
        public int getErrOpt(int socket) {
            return POSIX_BINDINGS.getErrOpt(socket);
        }

        @Override
        public int setNonBlocking(int socket) {
            return POSIX_BINDINGS.setNonblocking(socket);
        }

        @Override
        public int bind(int socket, int type, long addr, int port) {
            return POSIX_BINDINGS.bind(socket, type, addr, port);
        }

        @Override
        public int listen(int socket, int backlog) {
            return POSIX_BINDINGS.listen(socket, backlog);
        }

        @Override
        public int accept(int socket, long buf) {
            return POSIX_BINDINGS.accept(socket, buf);
        }

        @Override
        public int connect(int socket, int type, long addr, int port) {
            return POSIX_BINDINGS.connect(socket, type, addr, port);
        }

        @Override
        public int recv(int socket, long buf, int len) {
            return POSIX_BINDINGS.recv(socket, buf, len);
        }

        @Override
        public int send(int socket, long buf, int len) {
            return POSIX_BINDINGS.send(socket, buf, len);
        }

        @Override
        public int readInt(int socket) {
            return POSIX_BINDINGS.readInt(socket);
        }

        @Override
        public int writeInt(int socket, int value) {
            return POSIX_BINDINGS.writeInt(socket, value);
        }

        @Override
        public int udsBind(int socket, long addr, long len) {
            return POSIX_BINDINGS.udsBind(socket, addr, len);
        }

        @Override
        public int udsConnect(int socket, long addr, long len) {
            return POSIX_BINDINGS.udsConnect(socket, addr, len);
        }

        @Override
        public int udsAccept(int socket) {
            return POSIX_BINDINGS.udsAccept(socket);
        }
    }

    final class WinNetProvider implements NetProvider {
        private static final WinBindings WIN_BINDINGS = Libs.impl(WinBindings.class);

        static {
            if (WIN_BINDINGS == null) {
                throw new ExceptionInInitializerError("cannot initialize net provider for windows");
            }
            int r = WIN_BINDINGS.wsaStartup();
            if(r < 0) {
                throw new ExceptionInInitializerError("cannot initialize ws2_32 library for windows, err : " + -r);
            }
        }

        @Override
        public int connectBlockCode() {
            return WIN_BINDINGS.connectBlockCode();
        }

        @Override
        public int sendBlockCode() {
            return WIN_BINDINGS.sendBlockCode();
        }

        @Override
        public int afInetCode() {
            return WIN_BINDINGS.afInetCode();
        }

        @Override
        public int afInet6Code() {
            return WIN_BINDINGS.afInet6Code();
        }

        @Override
        public int afUnixCode() {
            return WIN_BINDINGS.afUnixCode();
        }

        @Override
        public int solSocketCode() {
            return WIN_BINDINGS.solSocket();
        }

        @Override
        public int sockStreamCode() {
            return WIN_BINDINGS.sockStreamCode();
        }

        @Override
        public int sockDgramCode() {
            return WIN_BINDINGS.sockDgramCode();
        }

        @Override
        public int ipprotoTcpCode() {
            return WIN_BINDINGS.ipprotoTcpCode();
        }

        @Override
        public int ipprotoUdpCode() {
            return WIN_BINDINGS.ipprotoUdpCode();
        }

        @Override
        public int ipprotoIpv6Code() {
            return WIN_BINDINGS.ipprotoIpv6Code();
        }

        @Override
        public int soReuseAddrCode() {
            return WIN_BINDINGS.soReuseaddr();
        }

        @Override
        public int tcpNoDelayCode() {
            return WIN_BINDINGS.tcpNodelay();
        }

        @Override
        public int ipv6OnlyCode() {
            return WIN_BINDINGS.ipv6V6only();
        }

        @Override
        public int socket(int domain, int type, int protocol) {
            return WIN_BINDINGS.socket(domain, type, protocol);
        }

        @Override
        public int closeSocket(int socket) {
            return WIN_BINDINGS.closeSocket(socket);
        }

        @Override
        public int setSockopt(int socket, int level, int opt, long val, int len) {
            return WIN_BINDINGS.setSockOpt(socket, level, opt, val, len);
        }

        @Override
        public int getErrOpt(int socket) {
            return WIN_BINDINGS.getErrOpt(socket);
        }

        @Override
        public int setNonBlocking(int socket) {
            return WIN_BINDINGS.setNonblocking(socket);
        }

        @Override
        public int bind(int socket, int type, long addr, int port) {
            return WIN_BINDINGS.bind(socket, type, addr, port);
        }

        @Override
        public int listen(int socket, int backlog) {
            return WIN_BINDINGS.listen(socket, backlog);
        }

        @Override
        public int accept(int socket, long buf) {
            return WIN_BINDINGS.accept(socket, buf);
        }

        @Override
        public int connect(int socket, int type, long addr, int port) {
            return WIN_BINDINGS.connect(socket, type, addr, port);
        }

        @Override
        public int recv(int socket, long buf, int len) {
            return WIN_BINDINGS.recv(socket, buf, len);
        }

        @Override
        public int send(int socket, long buf, int len) {
            return WIN_BINDINGS.send(socket, buf, len);
        }

        @Override
        public int readInt(int socket) {
            return WIN_BINDINGS.readInt(socket);
        }

        @Override
        public int writeInt(int socket, int value) {
            return WIN_BINDINGS.writeInt(socket, value);
        }

        @Override
        public int udsBind(int socket, long addr, long len) {
            return WIN_BINDINGS.udsBind(socket, addr, len);
        }

        @Override
        public int udsConnect(int socket, long addr, long len) {
            return WIN_BINDINGS.udsConnect(socket, addr, len);
        }

        @Override
        public int udsAccept(int socket) {
            return WIN_BINDINGS.udsAccept(socket);
        }
    }
}
