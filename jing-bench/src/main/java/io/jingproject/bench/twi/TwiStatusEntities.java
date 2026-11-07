package io.jingproject.bench.twi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.helidon.json.binding.Json;
import io.jingproject.marshall.MarshallAttr;
import io.jingproject.marshall.Marshallable;

import java.util.List;

@Marshallable
@Json.Entity
public record TwiStatusEntities(
        @MarshallAttr(mappedName = "hashtags") @JsonProperty("hashtags") @Json.Property("hashtags") List<TwiHashTag> hashtags,
        @MarshallAttr(mappedName = "symbols") @JsonProperty("symbols") @Json.Property("symbols") List<String> symbols,
        @MarshallAttr(mappedName = "urls") @JsonProperty("urls") @Json.Property("urls") List<TwiUrl> urls,
        @MarshallAttr(mappedName = "user_mentions") @JsonProperty("user_mentions") @Json.Property("user_mentions") List<TwiUserMention> userMentions,
        @MarshallAttr(mappedName = "media") @JsonProperty("media") @Json.Property("media") List<TwiMedia> media
) {
}
