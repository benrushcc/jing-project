package io.jingproject.marshallcbor;

// a CBOR byte-string value (major 2), carrying raw binary content.
public value record CborBytesType(byte[] data) implements CborPrimitiveType {

}