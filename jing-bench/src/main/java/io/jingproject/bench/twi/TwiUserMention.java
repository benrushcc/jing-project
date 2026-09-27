package io.jingproject.bench.twi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.helidon.json.binding.Json;
import io.jingproject.marshall.MarshallAttr;
import io.jingproject.marshall.Marshallable;

import java.util.List;

@Marshallable
@Json.Entity
public record TwiUserMention(
        @MarshallAttr(mappedName = "screen_name") @JsonProperty("screen_name") @Json.Property("screen_name") String screenName,
        @MarshallAttr(mappedName = "name") @JsonProperty("name") @Json.Property("name") String name,
        @MarshallAttr(mappedName = "id") @JsonProperty("id") @Json.Property("id") long id,
        @MarshallAttr(mappedName = "id_str") @JsonProperty("id_str") @Json.Property("id_str") String idStr,
        @MarshallAttr(mappedName = "indices") @JsonProperty("indices") @Json.Property("indices") List<Integer> indices
) {

}