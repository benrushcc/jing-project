package io.jingproject.marshallcbor;

// a CBOR integer value (major 0 / major 1), carrying integer semantics only.
// CBOR integers and floating-point values are encoded on separate paths, and
// MarshallUtil already distinguishes FLOAT_TYPE/DOUBLE_TYPE natively, so
// floating-point values never pass through this primitive label.
public value record CborNumberType(long data) implements CborPrimitiveType {

}