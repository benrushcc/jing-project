package io.jingproject.bench.twi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.helidon.json.binding.Json;
import io.jingproject.marshall.MarshallAttr;
import io.jingproject.marshall.Marshallable;

@Marshallable
@Json.Entity
public record TwiSizes(
        @MarshallAttr(mappedName = "medium") @JsonProperty("medium") @Json.Property("medium") TwiSize medium,
        @MarshallAttr(mappedName = "small") @JsonProperty("small") @Json.Property("small") TwiSize small,
        @MarshallAttr(mappedName = "thumb") @JsonProperty("thumb") @Json.Property("thumb") TwiSize thumb,
        @MarshallAttr(mappedName = "large") @JsonProperty("large") @Json.Property("large") TwiSize large
) {
}
