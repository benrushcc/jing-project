package io.jingproject.ffm.demo;

import io.jingproject.ffm.Downcall;
import io.jingproject.ffm.FFM;

@FFM(libraryName = "demo")
public interface DemoFacade {
    @Downcall(methodName = "empty", critical = true)
    void empty();

    @Downcall(methodName = "constant", critical = true, constant = true)
    int constant();

    @Downcall(methodName = "address", critical = true)
    void address(long addr, long size);
}
