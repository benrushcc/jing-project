package io.jingproject.bench.twi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.helidon.json.binding.Json;
import io.jingproject.marshall.MarshallAttr;
import io.jingproject.marshall.Marshallable;

@Marshallable
@Json.Entity
public record TwiUserEntities(
        @MarshallAttr(mappedName = "url") @JsonProperty("url") @Json.Property("url") TwiUserUrl url,
        @MarshallAttr(mappedName = "description") @JsonProperty("description") @Json.Property("description") TwiUserEntitiesDescription description
) {
}