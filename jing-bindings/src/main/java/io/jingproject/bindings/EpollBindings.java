package io.jingproject.bindings;

import io.jingproject.common.Os;
import io.jingproject.ffm.Downcall;
import io.jingproject.ffm.FFM;

import java.lang.foreign.MemorySegment;

@FFM(libraryName = "jing", supportedOS = Os.LINUX)
public interface EpollBindings {
    @Downcall(methodName = "jing_epoll_in", constant = true, critical = true)
    int epollIn();

    @Downcall(methodName = "jing_epoll_out", constant = true, critical = true)
    int epollOut();

    @Downcall(methodName = "jing_epoll_default", constant = true, critical = true)
    int epollDefault();

    @Downcall(methodName = "jing_epoll_ctl_add", constant = true, critical = true)
    int epollAdd();

    @Downcall(methodName = "jing_epoll_ctl_mod", constant = true, critical = true)
    int epollMod();

    @Downcall(methodName = "jing_epoll_ctl_del", constant = true, critical = true)
    int epollDel();

    @Downcall(methodName = "jing_epoll_cloexec",  constant = true, critical = true)
    int epollCloexec();

    @Downcall(methodName = "jing_epoll_create",critical = true)
    int epollCreate();

    @Downcall(methodName = "jing_epoll_ctl", critical = true)
    int epollCtl(int epfd, int socket, int op, int eventTypes, int data);
}
