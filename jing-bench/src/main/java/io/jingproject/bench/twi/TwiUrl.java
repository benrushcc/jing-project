package io.jingproject.bench.twi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.helidon.json.binding.Json;
import io.jingproject.marshall.MarshallAttr;
import io.jingproject.marshall.Marshallable;
import io.jingproject.marshall.NamingConvention;

import java.util.List;

@Marshallable
@Json.Entity
public record TwiUrl(
        @MarshallAttr(mappedName = "url") @JsonProperty("url") @Json.Property("url") String url,
        @MarshallAttr(mappedName = "expanded_url") @JsonProperty("expanded_url") @Json.Property("expanded_url") String expandedUrl,
        @MarshallAttr(mappedName = "display_url") @JsonProperty("display_url") @Json.Property("display_url") String displayUrl,
        @MarshallAttr(mappedName = "indices") @JsonProperty("indices") @Json.Property("indices") List<Integer> indices
) {

}
