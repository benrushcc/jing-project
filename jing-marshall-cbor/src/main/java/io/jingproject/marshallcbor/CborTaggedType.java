package io.jingproject.marshallcbor;

// a CBOR tag (major type 6) and its payload. this is the only built-in type that
// a transformer may use: custom java types are mapped to a specific tag number
// and a CborValue payload, and back.
public value record CborTaggedType(long tag, CborValue value) implements CborValue {

    public CborTaggedType {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
    }
}
