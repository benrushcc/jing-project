package io.jingproject.marshallcbor;

// a CBOR text-string value (major 3), written as raw UTF-8 bytes.
public value record CborStrType(String data) implements CborPrimitiveType {

}