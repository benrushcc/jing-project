package io.jingproject.marshalljson;

import java.util.Collection;
import java.util.Map;

public final class JsonSerializerContext {
    private char[] charBuffer;
    private Object obj;
    private Object[] arr;
    private Collection<?> col;
    private Map<?, ?> map;
    private Class<?> firstType;
    private Class<?> secondType;

    public char[] charBuffer(int len) {
        assert len > 0;
        if(charBuffer == null || charBuffer.length < len) {
            charBuffer = new char[Integer.highestOneBit(len - 1) << 1]; // no overflow
        }
        return charBuffer;
    }

    public Object obj() {
        Object r = obj;
        obj = null;
        return r;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }

    public Object[] arr() {
        Object[] r = arr;
        arr = null;
        return r;
    }

    public void setArr(Object[] arr) {
        this.arr = arr;
    }

    public Collection<?> col() {
        Collection<?> r = col;
        col = null;
        return r;
    }

    public void setCol(Collection<?> col) {
        this.col = col;
    }

    public Map<?, ?> map() {
        Map<?, ?> r = map;
        map = null;
        return r;
    }

    public void setMap(Map<?, ?> map) {
        this.map = map;
    }

    public Class<?> firstType() {
        Class<?> r = firstType;
        firstType = null;
        return r;
    }

    public void setFirstType(Class<?> firstType) {
        this.firstType = firstType;
    }

    public Class<?> secondType() {
        Class<?> r = secondType;
        secondType = null;
        return r;
    }

    public void setSecondType(Class<?> secondType) {
        this.secondType = secondType;
    }
}
