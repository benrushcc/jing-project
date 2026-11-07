package io.jingproject.bench.twi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.helidon.json.binding.Json;
import io.jingproject.marshall.MarshallAttr;
import io.jingproject.marshall.Marshallable;

@Marshallable
@Json.Entity
public record TwiSize(
        @MarshallAttr(mappedName = "w") @JsonProperty("w") @Json.Property("w") int w,
        @MarshallAttr(mappedName = "h") @JsonProperty("h") @Json.Property("h") int h,
        @MarshallAttr(mappedName = "resize") @JsonProperty("resize") @Json.Property("resize") String resize
) {
}
