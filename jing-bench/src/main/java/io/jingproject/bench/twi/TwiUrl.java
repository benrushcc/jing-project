package io.jingproject.bench.twi;

import io.jingproject.marshall.Marshallable;
import io.jingproject.marshall.NamingConvention;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Marshallable(from = NamingConvention.CAMEL_CASE, to = NamingConvention.SNAKE_CASE)
public record TwiUrl(
        String url,
        String expandedUrl,
        String displayUrl,
        List<Integer> indices
) {

}
