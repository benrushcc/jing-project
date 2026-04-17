package io.jingproject.ffmtest.entity;

import io.jingproject.ffm.Downcall;
import io.jingproject.ffm.FFM;

import java.lang.foreign.MemorySegment;

@FFM(libraryName = "jing_demo")
public interface DemoBinding {
    @Downcall(methodName = "single_int", critical = true, constant = true)
    int singleInt();

    // methodName = computeAdd, critical = true
    @Downcall(methodName = "compute_add", critical = true)
    int computeAdd(int a, int b);

    // methodName = computePointer
    @Downcall(methodName = "compute_pointer")
    int computePointer(MemorySegment a, MemorySegment b);
}
