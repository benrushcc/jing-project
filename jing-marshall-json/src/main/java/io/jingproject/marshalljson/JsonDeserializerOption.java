package io.jingproject.marshalljson;

public final class JsonDeserializerOption {
    // same as JsonSerializerOption
    public static final int DEFAULT_INITIAL_SIZE = 4;
    public static final int DEFAULT_MAX_SIZE     = 64;
    public static final int HARD_MIN_SIZE        = 2;
    public static final int HARD_MAX_SIZE        = 1024;
    private final int initialSize;
    private final int maxSize;
    private static final JsonDeserializerOption DEFAULT_OPTION = JsonDeserializerOption.builder().build();

    private JsonDeserializerOption(int initialSize, int maxSize) {
        this.initialSize = initialSize;
        this.maxSize = maxSize;
    }

    public static JsonDeserializerOption defaultOption() {
        return DEFAULT_OPTION;
    }

    public int initialSize() {
        return initialSize;
    }

    public int maxSize() {
        return maxSize;
    }

    public static JsonSerializerOptionBuilder builder() {
        return new JsonSerializerOptionBuilder();
    }

    public static class JsonSerializerOptionBuilder {
        private int initialSize = DEFAULT_INITIAL_SIZE;
        private int maxSize = DEFAULT_MAX_SIZE;

        public JsonSerializerOptionBuilder setInitialSize(int initialSize) {
            if(initialSize < HARD_MIN_SIZE ||initialSize > HARD_MAX_SIZE) {
                throw new IllegalArgumentException("initialSize : [" + initialSize + "] must be between " + HARD_MIN_SIZE + " and " + HARD_MAX_SIZE);
            }
            this.initialSize = initialSize;
            return this;
        }

        public JsonSerializerOptionBuilder setMaxSize(int maxSize) {
            if(maxSize < HARD_MIN_SIZE ||maxSize > HARD_MAX_SIZE) {
                throw new IllegalArgumentException("maxSize : [" + maxSize + "] must be between " + HARD_MIN_SIZE + " and " + HARD_MAX_SIZE);
            }
            this.maxSize = maxSize;
            return this;
        }

        public JsonDeserializerOption build() {
            return new JsonDeserializerOption(initialSize, maxSize);
        }
    }
}
