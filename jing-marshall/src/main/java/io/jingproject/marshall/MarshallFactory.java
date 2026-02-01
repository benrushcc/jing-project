package io.jingproject.marshall;

import io.jingproject.common.Utils;

import java.util.Objects;

public final class MarshallFactory {
    private Object[] objectBuffer;
    private byte[] primitiveBuffer;
    private int objectIndex = 0;
    private int primitiveIndex = 0;

    public MarshallFactory(int objectCapacity, int primitiveCapacity) {
        Objects.checkFromIndexSize(objectCapacity, Utils.minCapacity(), Integer.MAX_VALUE);
        Objects.checkFromIndexSize(primitiveCapacity, Utils.minCapacity(), Integer.MAX_VALUE);
        this.objectBuffer = new Object[objectCapacity];
        this.primitiveBuffer = new byte[primitiveCapacity];
    }

    public MarshallIndex createIndex() {
        // TODO
        return null;
    }
}
