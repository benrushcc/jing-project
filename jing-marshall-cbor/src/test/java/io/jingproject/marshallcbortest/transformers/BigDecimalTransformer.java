package io.jingproject.marshallcbortest.transformers;

import io.jingproject.marshall.MarshallTransformer;
import io.jingproject.marshall.Transformable;
import io.jingproject.marshallcbor.CborStrType;

import java.math.BigDecimal;

@Transformable
public final class BigDecimalTransformer implements MarshallTransformer<BigDecimal, CborStrType> {
    @Override
    public CborStrType toBuiltin(BigDecimal ct) {
        return new CborStrType(ct.toPlainString());
    }

    @Override
    public BigDecimal toCustom(CborStrType bt) {
        return new BigDecimal(bt.data());
    }
}