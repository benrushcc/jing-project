package io.jingproject.marshallcbor;

// value model for a single CBOR data item, used as the payload of a tagged type
// and as a generic field value. containers are always definite-length; indefinite
// length is a wire concern that the reader normalizes while decoding.
public sealed interface CborValue permits CborBool, CborInteger, CborFloat, CborText,
        CborArray, CborMap, CborNull, CborBytesType, CborTaggedType {

}
