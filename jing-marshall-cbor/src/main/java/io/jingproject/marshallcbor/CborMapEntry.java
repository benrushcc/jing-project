package io.jingproject.marshallcbor;

public value record CborMapEntry(CborValue key, CborValue value) {

    public CborMapEntry {
        if (key == null || value == null) {
            throw new IllegalArgumentException("key and value must not be null");
        }
    }
}
