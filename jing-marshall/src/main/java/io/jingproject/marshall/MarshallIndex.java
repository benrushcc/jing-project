package io.jingproject.marshall;

public final class MarshallIndex {
    private int objectIndex;
    private int primitiveIndex;

    public MarshallIndex(int objectIndex, int primitiveIndex) {
        this.objectIndex = objectIndex;
        this.primitiveIndex = primitiveIndex;
    }

    public void setObjectIndex(int objectIndex) {
        this.objectIndex = objectIndex;
    }

    public void setPrimitiveIndex(int primitiveIndex) {
        this.primitiveIndex = primitiveIndex;
    }

    public int objectIndex() {
        return objectIndex;
    }

    public int primitiveIndex() {
        return primitiveIndex;
    }
}
