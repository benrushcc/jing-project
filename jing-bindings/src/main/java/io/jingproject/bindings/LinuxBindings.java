package io.jingproject.bindings;

import io.jingproject.common.Os;
import io.jingproject.ffm.Downcall;
import io.jingproject.ffm.FFM;

@FFM(libraryName = "jing_bindings", supportedOS = {Os.LINUX})
public interface LinuxBindings {
    @Downcall(methodName = "jing_linux_epoll_in", constant = true, critical = true)
    int epollIn();

    @Downcall(methodName = "jing_linux_epoll_out", constant = true, critical = true)
    int epollOut();

    @Downcall(methodName = "jing_linux_epoll_rdhup", constant = true, critical = true)
    int epollRdhup();

    @Downcall(methodName = "jing_linux_epoll_err", constant = true, critical = true)
    int epollErr();

    @Downcall(methodName = "jing_linux_epoll_hup", constant = true, critical = true)
    int epollHup();

    @Downcall(methodName = "jing_linux_epoll_ctl_add", constant = true, critical = true)
    int epollAdd();

    @Downcall(methodName = "jing_linux_epoll_ctl_mod", constant = true, critical = true)
    int epollMod();

    @Downcall(methodName = "jing_linux_epoll_ctl_del", constant = true, critical = true)
    int epollDel();

    @Downcall(methodName = "jing_linux_epoll_cloexec", constant = true, critical = true)
    int epollCloexec();

    @Downcall(methodName = "jing_linux_epoll_create", critical = true)
    int epollCreate();

    @Downcall(methodName = "jing_linux_epoll_ctl", critical = true)
    int epollCtl(int epfd, int socket, int op, int eventTypes, int data);

    @Downcall(methodName = "jing_linux_epoll_wait")
    int epollWait(int epfd, long events, int maxevnets, int timeout);

    @Downcall(methodName = "jing_linux_efd_cloexec", constant = true, critical = true)
    int efdCloexec();

    @Downcall(methodName = "jing_linux_efd_nonblock", constant = true, critical = true)
    int efdNonblock();

    @Downcall(methodName = "jing_linux_eventfd", critical = true)
    int eventfd(int flags);

    @Downcall(methodName = "jing_linux_read_efd", critical = true)
    int readEfd(int efd);

    @Downcall(methodName = "jing_linux_write_efd", critical = true)
    int writeEfd(int efd, int value);
}
