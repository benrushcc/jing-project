package io.jingproject.bindings;

import io.jingproject.common.Os;
import io.jingproject.ffm.Downcall;
import io.jingproject.ffm.FFM;

import java.lang.foreign.MemorySegment;

@FFM(libraryName = "jing", supportedOS = Os.WINDOWS)
public interface WepollBindings {
    @Downcall(methodName = "jing_wepoll_in", constant = true, critical = true)
    int wepollIn();

    @Downcall(methodName = "jing_wepoll_out", constant = true, critical = true)
    int wepollOut();

    @Downcall(methodName = "jing_wepoll_err", constant = true, critical = true)
    int wepollErr();

    @Downcall(methodName = "jing_wepoll_hup", constant = true, critical = true)
    int wepollHup();

    @Downcall(methodName = "jing_wepoll_ctl_add", constant = true, critical = true)
    int wepollAdd();

    @Downcall(methodName = "jing_wepoll_ctl_mod", constant = true, critical = true)
    int wepollMod();

    @Downcall(methodName = "jing_wepoll_ctl_del", constant = true, critical = true)
    int wepollDel();

    @Downcall(methodName = "jing_wepoll_create", critical = true)
    MemorySegment wepollCreate();

    @Downcall(methodName = "jing_wepoll_ctl")
    int wepollCtl(MemorySegment epfd, long socket, int op, int eventTypes, int data);

    @Downcall(methodName = "jing_wepoll_wait")
    void wepollWait(MemorySegment epfd, MemorySegment events, int maxEvents, int timeout, MemorySegment r);

    @Downcall(methodName = "jing_wepoll_close", critical = true)
    int wepollClose(MemorySegment epfd);
}
