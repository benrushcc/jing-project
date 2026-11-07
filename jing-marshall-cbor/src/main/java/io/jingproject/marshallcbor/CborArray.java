package io.jingproject.marshallcbor;

import java.util.List;

public value record CborArray(List<CborValue> elements) implements CborValue {

    public CborArray {
        if (elements == null) {
            throw new IllegalArgumentException("elements must not be null");
        }
        elements = List.copyOf(elements);
    }
}
