package io.jingproject.marshalljsontest.transformers;

import io.jingproject.marshall.MarshallTransformer;
import io.jingproject.marshall.Transformable;
import io.jingproject.marshalljson.JsonNumberType;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@Transformable
public final class BigDecimalTransformer implements MarshallTransformer<BigDecimal, JsonNumberType> {
    @Override
    public JsonNumberType toBuiltin(BigDecimal ct) {
        return new JsonNumberType(ct.toPlainString().getBytes(StandardCharsets.US_ASCII));
    }

    @Override
    public BigDecimal toCustom(JsonNumberType bt) {
        return new BigDecimal(new String(bt.data(), StandardCharsets.US_ASCII));
    }
}
