package io.jingproject.marshallcbortest.transformers;

import io.jingproject.marshall.MarshallTransformer;
import io.jingproject.marshall.Transformable;
import io.jingproject.marshallcbor.CborTaggedType;
import io.jingproject.marshallcbor.CborText;

import java.math.BigDecimal;

// application-specific tag 1000 carrying the plain string form of a BigDecimal.
// a real wire profile should use tag 4 (decimal fraction), but this test
// transformer only needs to exercise the custom type <-> CborTaggedType mapping.
@Transformable
public final class BigDecimalTransformer implements MarshallTransformer<BigDecimal, CborTaggedType> {
    public static final long TAG = 1000L;

    @Override
    public CborTaggedType toBuiltin(BigDecimal customValue) {
        return new CborTaggedType(TAG, new CborText(customValue.toPlainString()));
    }

    @Override
    public BigDecimal toCustom(CborTaggedType builtinValue) {
        if (builtinValue.tag() != TAG || !(builtinValue.value() instanceof CborText text)) {
            throw new IllegalArgumentException("unexpected tagged value : " + builtinValue);
        }
        return new BigDecimal(text.value());
    }
}
