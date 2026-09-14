package io.jingproject.marshallcbor;

// a CBOR boolean value, encoded as simple value 0xf5 (true) or 0xf4 (false).
public value record CborBoolType(boolean data) implements CborPrimitiveType {

}