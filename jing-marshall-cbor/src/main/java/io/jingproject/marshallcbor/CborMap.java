package io.jingproject.marshallcbor;

import java.util.List;

public value record CborMap(List<CborMapEntry> entries) implements CborValue {

    public CborMap {
        if (entries == null) {
            throw new IllegalArgumentException("entries must not be null");
        }
        entries = List.copyOf(entries);
    }
}
