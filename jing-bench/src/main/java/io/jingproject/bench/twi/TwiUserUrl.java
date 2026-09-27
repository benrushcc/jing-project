package io.jingproject.bench.twi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.helidon.json.binding.Json;
import io.jingproject.marshall.MarshallAttr;
import io.jingproject.marshall.Marshallable;

import java.util.List;

@Marshallable
@Json.Entity
public record TwiUserUrl(
        @MarshallAttr(mappedName = "urls") @JsonProperty("urls") @Json.Property("urls") List<TwiUrl> urls
) {
}