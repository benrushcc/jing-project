package io.jingproject.bench.twi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.helidon.json.binding.Json;
import io.jingproject.marshall.MarshallAttr;
import io.jingproject.marshall.Marshallable;

import java.util.List;

@Marshallable
@Json.Entity
public record TwiMedia(
        @MarshallAttr(mappedName = "id") @JsonProperty("id") @Json.Property("id") long id,
        @MarshallAttr(mappedName = "id_str") @JsonProperty("id_str") @Json.Property("id_str") String idStr,
        @MarshallAttr(mappedName = "indices") @JsonProperty("indices") @Json.Property("indices") List<Integer> indices,
        @MarshallAttr(mappedName = "media_url") @JsonProperty("media_url") @Json.Property("media_url") String mediaUrl,
        @MarshallAttr(mappedName = "media_url_https") @JsonProperty("media_url_https") @Json.Property("media_url_https") String mediaUrlHttps,
        @MarshallAttr(mappedName = "url") @JsonProperty("url") @Json.Property("url") String url,
        @MarshallAttr(mappedName = "display_url") @JsonProperty("display_url") @Json.Property("display_url") String displayUrl,
        @MarshallAttr(mappedName = "expanded_url") @JsonProperty("expanded_url") @Json.Property("expanded_url") String expandedUrl,
        @MarshallAttr(mappedName = "type") @JsonProperty("type") @Json.Property("type") String type,
        @MarshallAttr(mappedName = "sizes") @JsonProperty("sizes") @Json.Property("sizes") TwiSizes sizes,
        @MarshallAttr(mappedName = "source_status_id") @JsonProperty("source_status_id") @Json.Property("source_status_id") Long sourceStatusId,
        @MarshallAttr(mappedName = "source_status_id_str") @JsonProperty("source_status_id_str") @Json.Property("source_status_id_str") String sourceStatusIdStr
) {

}
