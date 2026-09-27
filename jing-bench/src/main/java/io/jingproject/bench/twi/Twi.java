package io.jingproject.bench.twi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.helidon.json.binding.Json;
import io.jingproject.marshall.MarshallAttr;
import io.jingproject.marshall.Marshallable;
import io.jingproject.marshall.NamingConvention;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Marshallable
@Json.Entity
public record Twi(
        @MarshallAttr(mappedName = "statuses") @JsonProperty("statuses") @Json.Property("statuses") List<TwiStatus> statuses,
        @MarshallAttr(mappedName = "search_metadata") @JsonProperty("search_metadata") @Json.Property("search_metadata") TwiSearchMetaData searchMetadata
) {
    private static final String TWI_FILE_NAME = "twitter.json";

    public static String asString() {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(TWI_FILE_NAME)) {
            if (stream == null) {
                throw new AssertionError(TWI_FILE_NAME + " file not found from resources");
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return reader.readAllAsString();
            }
        } catch (IOException e) {
            throw new AssertionError("Failed to load " + TWI_FILE_NAME + " file from resources", e);
        }
    }

    public static byte[] asBytes() {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(TWI_FILE_NAME)) {
            if (stream == null) {
                throw new AssertionError(TWI_FILE_NAME + " file not found from resources");
            }
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new AssertionError("Failed to load " + TWI_FILE_NAME + " file from resources", e);
        }
    }
}
