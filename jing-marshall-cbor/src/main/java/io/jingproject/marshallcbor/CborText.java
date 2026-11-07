package io.jingproject.marshallcbor;

public value record CborText(String value) implements CborValue {

    public CborText {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
    }
}
