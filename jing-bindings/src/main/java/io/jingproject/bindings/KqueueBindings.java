package io.jingproject.bindings;

import io.jingproject.common.Os;
import io.jingproject.ffm.Downcall;
import io.jingproject.ffm.FFM;

import java.lang.foreign.MemorySegment;

@FFM(libraryName = "jing", supportedOS = Os.MACOS)
public interface KqueueBindings {
    @Downcall(methodName = "jing_kqueue_in", constant = true, critical = true)
    int kqueueIn();

    @Downcall(methodName = "jing_kqueue_out", constant = true, critical = true)
    int kqueueOut();

    @Downcall(methodName = "jing_kqueue", critical = true)
    int kqueue();

    @Downcall(methodName = "jing_kevent_ctl", critical = true)
    int keventCtl(int kqfd, int socket, int modRead, int modWrite, MemorySegment udata);

    @Downcall(methodName = "jing_kqueue_wait")
    int keventWait(int kqfd, MemorySegment events, int nevents, int timeout);
}
