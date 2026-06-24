package io.jingproject.marshalljsontest;

import io.jingproject.common.WriteBuffer;
import io.jingproject.marshalljson.JsonSerializer;
import io.jingproject.marshalljson.JsonSerializerOption;
import io.jingproject.marshalljsontest.twi.Twi;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class TwiUtil {
    private TwiUtil() {
        throw new UnsupportedOperationException("utility class");
    }

    public static String load() {
        try (InputStream rawStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("twitter.json")) {
            if (rawStream == null) {
                throw new AssertionError("twitter.json file not found from resources");
            }
            try (InputStreamReader reader = new InputStreamReader(rawStream, StandardCharsets.UTF_8)) {
                return reader.readAllAsString();
            }
        } catch (IOException e) {
            throw new AssertionError("Failed to load words.txt file from resources", e);
        }
    }

    public static Twi deserializeTwiUsingJackson(String json) {
        JsonMapper jsonMapper = JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();
        return jsonMapper.readValue(json, Twi.class);
    }

    public static void serializeTwiUsingJackson(Twi twi, OutputStream outputStream) {
        JsonMapper jsonMapper = JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();
        jsonMapper.writeValue(outputStream, twi);
    }

    public static void serializeTwi(Twi twi, WriteBuffer writeBuffer) {
        JsonSerializer jsonSerializer = new JsonSerializer(JsonSerializerOption.defaultOption());
        jsonSerializer.serializeMarshallableObject(twi, writeBuffer);
    }
}
