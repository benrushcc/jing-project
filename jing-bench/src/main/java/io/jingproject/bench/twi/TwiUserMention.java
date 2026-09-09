package io.jingproject.bench.twi;

import io.jingproject.marshall.Marshallable;
import io.jingproject.marshall.NamingConvention;

import java.util.List;

@Marshallable(from = NamingConvention.CAMEL_CASE, to = NamingConvention.SNAKE_CASE)
public record TwiUserMention(
        String screenName,
        String name,
        long id,
        String idStr,
        List<Integer> indices
) {

}
