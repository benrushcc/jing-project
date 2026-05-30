package io.jingproject.marshall;

public interface MarshallTransformer<CustomType, BuiltinType> {
    BuiltinType toBuiltin(CustomType ct);

    CustomType toCustom(BuiltinType bt);
}
