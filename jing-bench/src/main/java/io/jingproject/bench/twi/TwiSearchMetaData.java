package io.jingproject.bench.twi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.helidon.json.binding.Json;
import io.jingproject.marshall.MarshallAttr;
import io.jingproject.marshall.Marshallable;
import io.jingproject.marshall.NamingConvention;

@Marshallable
@Json.Entity
public record TwiSearchMetaData(
        @MarshallAttr(mappedName = "completed_in") @JsonProperty("completed_in") @Json.Property("completed_in") float completedIn,
        @MarshallAttr(mappedName = "max_id") @JsonProperty("max_id") @Json.Property("max_id") long maxId,
        @MarshallAttr(mappedName = "max_id_str") @JsonProperty("max_id_str") @Json.Property("max_id_str") String maxIdStr,
        @MarshallAttr(mappedName = "next_results") @JsonProperty("next_results") @Json.Property("next_results") String nextResults,
        @MarshallAttr(mappedName = "query") @JsonProperty("query") @Json.Property("query") String query,
        @MarshallAttr(mappedName = "refresh_url") @JsonProperty("refresh_url") @Json.Property("refresh_url") String refreshUrl,
        @MarshallAttr(mappedName = "count") @JsonProperty("count") @Json.Property("count") int count,
        @MarshallAttr(mappedName = "since_id") @JsonProperty("since_id") @Json.Property("since_id") long sinceId,
        @MarshallAttr(mappedName = "since_id_str") @JsonProperty("since_id_str") @Json.Property("since_id_str") String sinceIdStr
) {
}
