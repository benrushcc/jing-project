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

    public char[] charBuffer(int len, int min, int max) {
        assert len > 0 && min > 1 && max > min && max <= (Integer.MAX_VALUE >> 1);
        if(len < min || len > max) {
            return null;
        }
        if(charBuffer == null || charBuffer.length < len) {
            charBuffer = new char[Integer.highestOneBit(len - 1) << 1]; // no overflow
        }
        return charBuffer;
    }

    public Object obj() {
        return obj;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }

    public Object[] arr() {
        return arr;
    }

    public void setArr(Object[] arr) {
        this.arr = arr;
    }

    public Collection<?> col() {
        return col;
    }

    public void setCol(Collection<?> col) {
        this.col = col;
    }

    public Map<?, ?> map() {
        return map;
    }

    public void setMap(Map<?, ?> map) {
        this.map = map;
    }

    public Class<?> firstType() {
        return firstType;
    }

    public void setFirstType(Class<?> firstType) {
        this.firstType = firstType;
    }

    public Class<?> secondType() {
        return secondType;
    }

    public void setSecondType(Class<?> secondType) {
        this.secondType = secondType;
    }
}
