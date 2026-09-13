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

    @Downcall(methodName = "demo_long_add", critical = true)
    long longAdd(long a, long b);

    @Downcall(methodName = "demo_long_long_add", critical = true)
    long longLongAdd(long a, long b);

    @Downcall(methodName = "demo_size_t_add", critical = true)
    long sizeTAdd(long a, long b);

    @Downcall(methodName = "demo_unsigned_int_add", critical = true)
    int unsignedIntAdd(int a, int b);

    @Downcall(methodName = "demo_unsigned_long_add", critical = true)
    long unsignedLongAdd(long a, long b);

    @Downcall(methodName = "demo_str_len")
    long strLen(MemorySegment str);

    @Downcall(methodName = "demo_void_ptr_identity")
    MemorySegment voidPtrIdentity(MemorySegment p);

    @Downcall(methodName = "demo_sizeof_int", constant = true, critical = true)
    int sizeofInt();

    @Downcall(methodName = "demo_sizeof_long", constant = true, critical = true)
    int sizeofLong();

    @Downcall(methodName = "demo_sizeof_size_t", constant = true, critical = true)
    int sizeofSizeT();

    @Downcall(methodName = "demo_sizeof_pointer", constant = true, critical = true)
    int sizeofPointer();

    @Downcall(methodName = "demo_bool_not", critical = true)
    boolean boolNot(boolean v);

    @Downcall(methodName = "demo_bool_and", critical = true)
    boolean boolAnd(boolean a, boolean b);

    @Downcall(methodName = "demo_bool_true", constant = true, critical = true)
    boolean boolTrue();

    @Downcall(methodName = "demo_bool_false", constant = true, critical = true)
    boolean boolFalse();

    @Downcall(methodName = "demo_int_to_bool", critical = true)
    boolean intToBool(int v);

    @Downcall(methodName = "demo_bool_to_int", critical = true)
    int boolToInt(boolean v);

    @Downcall(methodName = "demo_byte_add", critical = true)
    byte byteAdd(byte a, byte b);

    @Downcall(methodName = "demo_short_add", critical = true)
    short shortAdd(short a, short b);

    @Downcall(methodName = "demo_char_upper", critical = true)
    char charUpper(char c);

    @Downcall(methodName = "demo_float_add", critical = true)
    float floatAdd(float a, float b);

    @Downcall(methodName = "demo_void_noop")
    void voidNoop();

    @Downcall(methodName = "demo_pointer_identity")
    MemorySegment pointerIdentity(MemorySegment p);
}
