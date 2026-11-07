package io.jingproject.marshallcbor;

import java.util.Arrays;

// a CBOR byte string (major type 2). it is kept as a distinct wrapper so code
// that needs to distinguish raw bytes from a java byte[] can do so; it is also
// the byte-string variant of the CborValue model.
public value record CborBytesType(byte[] data) implements CborValue {

    public CborBytesType {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CborBytesType that && Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }

    @Override
    public String toString() {
        return "CborBytesType[" + data.length + " bytes]";
    }
}
