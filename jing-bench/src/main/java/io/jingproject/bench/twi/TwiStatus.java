package io.jingproject.bench.twi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.helidon.json.binding.Json;
import io.jingproject.marshall.MarshallAttr;
import io.jingproject.marshall.Marshallable;
import io.jingproject.marshall.NamingConvention;

@Marshallable
@Json.Entity
public record TwiStatus(
        @MarshallAttr(mappedName = "metadata") @JsonProperty("metadata") @Json.Property("metadata") TwiMetaData metadata,
        @MarshallAttr(mappedName = "created_at") @JsonProperty("created_at") @Json.Property("created_at") String createdAt,
        @MarshallAttr(mappedName = "id") @JsonProperty("id") @Json.Property("id") long id,
        @MarshallAttr(mappedName = "id_str") @JsonProperty("id_str") @Json.Property("id_str") String idStr,
        @MarshallAttr(mappedName = "text") @JsonProperty("text") @Json.Property("text") String text,
        @MarshallAttr(mappedName = "source") @JsonProperty("source") @Json.Property("source") String source,
        @MarshallAttr(mappedName = "truncated") @JsonProperty("truncated") @Json.Property("truncated") boolean truncated,
        @MarshallAttr(mappedName = "in_reply_to_status_id") @JsonProperty("in_reply_to_status_id") @Json.Property("in_reply_to_status_id") Long inReplyToStatusId,
        @MarshallAttr(mappedName = "in_reply_to_status_id_str") @JsonProperty("in_reply_to_status_id_str") @Json.Property("in_reply_to_status_id_str") String inReplyToStatusIdStr,
        @MarshallAttr(mappedName = "in_reply_to_user_id") @JsonProperty("in_reply_to_user_id") @Json.Property("in_reply_to_user_id") Long inReplyToUserId,
        @MarshallAttr(mappedName = "in_reply_to_user_id_str") @JsonProperty("in_reply_to_user_id_str") @Json.Property("in_reply_to_user_id_str") String inReplyToUserIdStr,
        @MarshallAttr(mappedName = "in_reply_to_screen_name") @JsonProperty("in_reply_to_screen_name") @Json.Property("in_reply_to_screen_name") String inReplyToScreenName,
        @MarshallAttr(mappedName = "user") @JsonProperty("user") @Json.Property("user") TwiUser user,
        @MarshallAttr(mappedName = "geo") @JsonProperty("geo") @Json.Property("geo") Object geo,
        @MarshallAttr(mappedName = "coordinates") @JsonProperty("coordinates") @Json.Property("coordinates") Object coordinates,
        @MarshallAttr(mappedName = "place") @JsonProperty("place") @Json.Property("place") Object place,
        @MarshallAttr(mappedName = "contributors") @JsonProperty("contributors") @Json.Property("contributors") Object contributors,
        @MarshallAttr(mappedName = "retweeted_status") @JsonProperty("retweeted_status") @Json.Property("retweeted_status") TwiStatus retweetedStatus,
        @MarshallAttr(mappedName = "retweet_count") @JsonProperty("retweet_count") @Json.Property("retweet_count") long retweetCount,
        @MarshallAttr(mappedName = "favorite_count") @JsonProperty("favorite_count") @Json.Property("favorite_count") long favoriteCount,
        @MarshallAttr(mappedName = "entities") @JsonProperty("entities") @Json.Property("entities") TwiStatusEntities entities,
        @MarshallAttr(mappedName = "favorited") @JsonProperty("favorited") @Json.Property("favorited") boolean favorited,
        @MarshallAttr(mappedName = "retweeted") @JsonProperty("retweeted") @Json.Property("retweeted") boolean retweeted,
        @MarshallAttr(mappedName = "possibly_sensitive") @JsonProperty("possibly_sensitive") @Json.Property("possibly_sensitive") Boolean possiblySensitive,
        @MarshallAttr(mappedName = "lang") @JsonProperty("lang") @Json.Property("lang") String lang
) {
}
