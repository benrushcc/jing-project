package io.jingproject.bench.twi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.helidon.json.binding.Json;
import io.jingproject.marshall.MarshallAttr;
import io.jingproject.marshall.Marshallable;

@Marshallable
@Json.Entity
public record TwiMetaData(
        @MarshallAttr(mappedName = "result_type") @JsonProperty("result_type") @Json.Property("result_type") String resultType,
        @MarshallAttr(mappedName = "iso_language_code") @JsonProperty("iso_language_code") @Json.Property("iso_language_code") String isoLanguageCode
) {
}
