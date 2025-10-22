package io.jingproject.net;

import io.jingproject.ffm.Downcall;
import io.jingproject.ffm.FFM;
import io.jingproject.ffm.OsType;

@FFM(libraryName = "jing", supportedOsType = OsType.Windows)
public interface WepollFacade {
    @Downcall(methodName = "jing_wepoll_in", constant = true, critical = true)
    int wepollIn();

    @Downcall(methodName = "jing_wepoll_out", constant = true, critical = true)
    int wepollOut();

    @Downcall(methodName = "jing_wepoll_default", constant = true, critical = true)
    int wepollDefault();

    @Downcall(methodName = "jing_wepoll_create")
    void wepollCreate(long r);

    @Downcall(methodName = "jing_wepoll_ctl")
    void wepollCtl(long epfd, long socket, int from, int to, int data, long r);

    @Downcall(methodName = "jing_wepoll_wait")
    void wepollWait(long epfd, long events, int maxevents, int timeout, long r);

    @Downcall(methodName = "jing_wepoll_close")
    void wepollClose(long epfd, long r);
}
