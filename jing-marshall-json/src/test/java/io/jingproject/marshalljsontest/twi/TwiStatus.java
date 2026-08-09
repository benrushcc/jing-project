package io.jingproject.marshalljsontest.twi;

import io.jingproject.marshall.Marshallable;
import io.jingproject.marshall.NamingConvention;

@Marshallable(from = NamingConvention.CAMEL_CASE, to = NamingConvention.SNAKE_CASE)
public record TwiStatus(
        TwiMetaData metadata,
        String createdAt,
        long id,
        String idStr,
        String text,
        String source,
        boolean truncated,
        Long inReplyToStatusId,
        String inReplyToStatusIdStr,
        Long inReplyToUserId,
        String inReplyToUserIdStr,
        String inReplyToScreenName,
        TwiUser user,
        Object geo,
        Object coordinates,
        Object place,
        Object contributors,
        TwiStatus retweetedStatus,
        long retweetCount,
        long favoriteCount,
        TwiStatusEntities entities,
        boolean favorited,
        boolean retweeted,
        Boolean possiblySensitive,
        String lang
) {
}
