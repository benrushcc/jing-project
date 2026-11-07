package io.jingproject.bench.twi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.helidon.json.binding.Json;
import io.jingproject.marshall.MarshallAttr;
import io.jingproject.marshall.Marshallable;

import java.util.List;

@Marshallable
@Json.Entity
public record TwiHashTag(
        @MarshallAttr(mappedName = "text") @JsonProperty("text") @Json.Property("text") String text,
        @MarshallAttr(mappedName = "indices") @JsonProperty("indices") @Json.Property("indices") List<Integer> indices
) {

}
