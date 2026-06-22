package io.jingproject.marshalljsontest.twi;

import io.jingproject.marshall.Marshallable;
import io.jingproject.marshall.NamingConvention;

@Marshallable(from = NamingConvention.CAMEL_CASE, to = NamingConvention.SNAKE_CASE)
public record TwiMedia(
        long id,
        String idStr,
        int[] indices,
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
