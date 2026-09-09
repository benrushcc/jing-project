package io.jingproject.bench.twi;

import io.jingproject.marshall.Marshallable;
import io.jingproject.marshall.NamingConvention;

@Marshallable(from = NamingConvention.CAMEL_CASE, to = NamingConvention.SNAKE_CASE)
public record TwiUserEntities(
        TwiUserUrl url,
        TwiUserEntitiesDescription description
) {
}
