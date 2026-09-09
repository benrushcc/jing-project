package io.jingproject.bench.twi;

import io.jingproject.marshall.Marshallable;
import io.jingproject.marshall.NamingConvention;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Marshallable(from = NamingConvention.CAMEL_CASE, to = NamingConvention.SNAKE_CASE)
public record TwiMedia(
        long id,
        String idStr,
        List<Integer> indices,
        String mediaUrl,
        String mediaUrlHttps,
        String url,
        String displayUrl,
        String expandedUrl,
        String type,
        TwiSizes sizes,
        Long sourceStatusId,
        String sourceStatusIdStr
) {

}
