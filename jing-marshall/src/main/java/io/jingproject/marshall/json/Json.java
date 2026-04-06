package io.jingproject.marshall.json;

public final class Json {
    private Json() {
        throw new UnsupportedOperationException("Utility class");
    }

    // TODO 可以做成接口可扩展替换的实现，先暂且定成这样
    private static final JsonSerializer DEFAULT_SERIALIZER = new JsonSerializer();
    private static final JsonDeserializer DEFAULT_DESERIALIZER = new JsonDeserializer();

    public static JsonSerializer serializer() {
        return DEFAULT_SERIALIZER;
    }

    public static JsonDeserializer deserializer() {
        return DEFAULT_DESERIALIZER;
    }
}
