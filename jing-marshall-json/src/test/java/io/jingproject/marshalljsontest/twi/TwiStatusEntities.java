package io.jingproject.marshalljsontest.twi;

import io.jingproject.marshall.Marshallable;
import io.jingproject.marshall.NamingConvention;

import java.util.List;

@Marshallable(from = NamingConvention.CAMEL_CASE, to = NamingConvention.SNAKE_CASE)
public record TwiStatusEntities(
        List<TwiHashTag> hashtags,
        List<String> symbols,
        List<TwiUrl> urls,
        List<TwiUserMention> userMentions,
        List<TwiMedia> media
) {
}
