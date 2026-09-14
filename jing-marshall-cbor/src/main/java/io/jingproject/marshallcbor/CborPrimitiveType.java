package io.jingproject.marshallcbor;

// sealed interface for the native primitive value types of the CBOR serializer.
// the permitted implementations cover boolean, integer, text-string, and
// byte-string values.
public sealed interface CborPrimitiveType
        permits CborBoolType, CborNumberType, CborStrType, CborBytesType {

}