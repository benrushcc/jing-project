package io.jingproject.bindings;

import io.jingproject.common.Os;
import io.jingproject.ffm.Downcall;
import io.jingproject.ffm.FFM;

import java.lang.foreign.MemorySegment;

@FFM(libraryName = "jing_bindings", supportedOS = Os.MACOS)
public interface MacosBindings {
    @Downcall(methodName = "jing_macos_evfilt_read",critical = true)
    int evfiltRead();

    @Downcall(methodName = "jing_macos_evfilt_write",critical = true)
    int evfiltWrite();

    @Downcall(methodName = "jing_macos_kqueue", critical = true)
    int kqueue();

    @Downcall(methodName = "jing_macos_kevent_ctl", critical = true)
    int keventCtl(int kqfd, int socket, int modRead, int modWrite, int data);

    @Downcall(methodName = "jing_macos_kevent_wait")
    int keventWait(int kqfd, long events, int nevents, int timeout);
}
