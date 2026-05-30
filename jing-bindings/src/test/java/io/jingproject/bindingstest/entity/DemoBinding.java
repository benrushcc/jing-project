package io.jingproject.bindingstest.entity;

import io.jingproject.ffm.Downcall;
import io.jingproject.ffm.FFM;

import java.lang.foreign.MemorySegment;

@FFM(libraryName = "jing_demo")
public interface DemoBinding {
    @Downcall(methodName = "demo_single_int", critical = true, constant = true)
    int singleInt();

    @Downcall(methodName = "demo_compute_add", critical = true)
    int computeAdd(int a, int b);

    @Downcall(methodName = "demo_compute_pointer")
    int computePointer(MemorySegment a, MemorySegment b);

    @Downcall(methodName = "demo_str_to_int64")
    long strToLong(MemorySegment str);

    @Downcall(methodName = "demo_str_to_double")
    double strToDouble(MemorySegment str);

    @Downcall(methodName = "demo_int64_to_str")
    int longToStr(long var, MemorySegment str, int len);

    @Downcall(methodName = "demo_double_to_str")
    int doubleToStr(double var, MemorySegment str, int len);

    @Downcall(methodName = "demo_nonexist")
    void nonexist();
}
