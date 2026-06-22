package io.jingproject.marshalljsontest.twi;

import io.jingproject.marshall.Marshallable;
import io.jingproject.marshall.NamingConvention;

@Marshallable(from = NamingConvention.CAMEL_CASE, to = NamingConvention.SNAKE_CASE)
public record TwiSearchMetaData(
        float completedIn,
        long maxId,
        String maxIdStr,
        String nextResults,
        String query,
        String refreshUrl,
        int count,
        long sinceId,
        String sinceIdStr
) {
}
