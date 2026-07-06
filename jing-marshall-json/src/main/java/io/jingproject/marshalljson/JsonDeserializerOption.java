package io.jingproject.marshalljson;

import io.jingproject.marshall.MarshallTransformerFacade;
import io.jingproject.marshall.Marshalls;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class JsonDeserializerOption {
    private static final int HARD_MIN_EMPTY_SIZE  = 8;
    private static final int HARD_MIN_NUMBER_SIZE = 24;
    private static final int HARD_MIN_STRING_SIZE = 64;
    private static final int HARD_MIN_ARRAY_SIZE = 100;
    private static final int HARD_MIN_MAP_SIZE   = 20;
    private static final JsonDeserializerOption DEFAULT_OPTION = JsonDeserializerOption.builder().build();

    private final Map<Class<?>, MarshallTransformerFacade> tfcMap;
    private final boolean consumeAllBytes;
    private final boolean ignoreUnknownFields;
    private final boolean ensureAllFieldsPresent;
    private final int maxEmptyBytes;
    private final int maxNumberBytes;
    private final int maxStringBytes;
    private final int maxArrayElements;
    private final int maxMapElements;
    private final int maxNestedSize;

    public JsonDeserializerOption(Map<Class<?>, MarshallTransformerFacade> tfcMap, boolean consumeAllBytes, boolean ignoreUnknownFields,
                                  boolean ensureAllFieldsPresent, int maxEmptyBytes, int maxNumberBytes,
                                  int maxStringBytes, int maxArrayElements, int maxMapElements, int maxNestedSize) {
        this.tfcMap = tfcMap;
        this.consumeAllBytes = consumeAllBytes;
        this.ignoreUnknownFields = ignoreUnknownFields;
        this.ensureAllFieldsPresent = ensureAllFieldsPresent;
        this.maxEmptyBytes = maxEmptyBytes;
        this.maxNumberBytes = maxNumberBytes;
        this.maxStringBytes = maxStringBytes;
        this.maxArrayElements = maxArrayElements;
        this.maxMapElements = maxMapElements;
        this.maxNestedSize = maxNestedSize;
    }

    public static JsonDeserializerOption defaultOption() {
        return DEFAULT_OPTION;
    }

    public static JsonDeserializerOptionBuilder builder() {
        return new JsonDeserializerOptionBuilder();
    }

    public MarshallTransformerFacade customTfc(Class<?> clazz) {
        return tfcMap.get(clazz);
    }

    public boolean consumeAllBytes() {
        return consumeAllBytes;
    }

    public boolean ignoreUnknownFields() {
        return ignoreUnknownFields;
    }

    // 开启时，会记录object的每个field是否被赋值，只有每个字段都被赋值才能允许构造，否则不允许构造
    // 关闭时，不会记录每个field的赋值情况，这意味着某些错误的json结构也会被允许，比如出现重复的key，后出现的key会覆盖先出现的值
    public boolean ensureAllFieldsPresent() {
        return ensureAllFieldsPresent;
    }

    public int maxEmptyBytes() {
        return maxEmptyBytes;
    }

    public int maxNumberBytes() {
        return maxNumberBytes;
    }

    public int maxStringBytes() {
        return maxStringBytes;
    }

    public int maxArrayElements() {
        return maxArrayElements;
    }

    public int maxMapElements() {
        return maxMapElements;
    }

    public int maxSize() {
        return maxNestedSize;
    }

    public static class JsonDeserializerOptionBuilder {
        private final Set<MarshallTransformerFacade> transformerFacades = new HashSet<>();
        private boolean consumeAllBytes = true;
        private boolean ignoreUnknownFields = true;
        private boolean ensureAllFieldsPresent = false;
        private int maxEmptyBytes = 256;
        private int maxNumberBytes = 24;
        private int maxStringBytes = 65535;
        private int maxArrayElements = 1000;
        private int maxMapElements = 200;
        private int maxNestedSize = 64;

        public JsonDeserializerOptionBuilder withTransfromer(Class<?>... transformers) {
            if(transformers == null || transformers.length == 0) {
                throw new IllegalArgumentException("transformers must not be null or empty");
            }
            for (Class<?> transformer : transformers) {
                MarshallTransformerFacade tfc = Marshalls.getMarshallTransformerFacade(transformer);
                if(tfc == null) {
                    throw new IllegalArgumentException("transformer not found : " + transformer.getName());
                }
                Class<?> customType = tfc.customType();
                if(transformerFacades.stream().anyMatch(fc -> fc.customType() == customType)) {
                    throw new IllegalArgumentException("custom type already exists : " + customType.getName());
                }
                // primitive types are not supported in generics, array types are not supported in transformers, so we don't need to double-check them
                if(JsonSerializeUtil.builtinSerializeObjFunc(customType) != null) {
                    throw new IllegalArgumentException("cannot override builtin type : " + customType.getName());
                }
                Class<?> builtinType = tfc.builtinType();
                if (JsonPrimitiveType.class.isAssignableFrom(builtinType)) {
                    throw new IllegalArgumentException("builtinType not implementing JsonPrimitiveType interface : " + builtinType.getName());
                }
                transformerFacades.add(tfc); // no conflict
            }
            return this;
        }

        public JsonDeserializerOptionBuilder setConsumeAllBytes(boolean consumeAllBytes) {
            this.consumeAllBytes = consumeAllBytes;
            return this;
        }

        public JsonDeserializerOptionBuilder setIgnoreUnknownFields(boolean ignoreUnknownFields) {
            this.ignoreUnknownFields = ignoreUnknownFields;
            return this;
        }

        public JsonDeserializerOptionBuilder setEnsureAllFieldsPresent(boolean ensureAllFieldsPresent) {
            this.ensureAllFieldsPresent = ensureAllFieldsPresent;
            return this;
        }

        public JsonDeserializerOptionBuilder setMaxEmptyBytes(int maxEmptyBytes) {
            if(maxEmptyBytes < HARD_MIN_EMPTY_SIZE) {
                throw new IllegalArgumentException("maxEmptyBytes must be >= " + HARD_MIN_EMPTY_SIZE);
            }
            this.maxEmptyBytes = maxEmptyBytes;
            return this;
        }

        public JsonDeserializerOptionBuilder setMaxNumberBytes(int maxNumberBytes) {
            if(maxNumberBytes < HARD_MIN_NUMBER_SIZE) {
                throw new IllegalArgumentException("maxNumberBytes must be >= " + HARD_MIN_NUMBER_SIZE);
            }
            this.maxNumberBytes = maxNumberBytes;
            return this;
        }

        public JsonDeserializerOptionBuilder setMaxStringBytes(int maxStringBytes) {
            if(maxStringBytes < HARD_MIN_STRING_SIZE) {
                throw new IllegalArgumentException("maxStringBytes must be >= " + HARD_MIN_STRING_SIZE);
            }
            this.maxStringBytes = maxStringBytes;
            return this;
        }

        public void setMaxArrayElements(int maxArrayElements) {
            if(maxArrayElements < HARD_MIN_ARRAY_SIZE) {
                throw new IllegalArgumentException("maxArrayElements must be >= " + HARD_MIN_ARRAY_SIZE);
            }
            this.maxArrayElements = maxArrayElements;
        }

        public void setMaxMapElements(int maxMapElements) {
            if(maxMapElements < HARD_MIN_MAP_SIZE) {
                throw new IllegalArgumentException("maxMapElements must be >= " + HARD_MIN_MAP_SIZE);
            }
            this.maxMapElements = maxMapElements;
        }

        public JsonDeserializerOptionBuilder setMaxNestedSize(int maxNestedSize) {
            if(Integer.bitCount(maxNestedSize) != 1 || maxNestedSize < 4) {
                throw new IllegalArgumentException("maxNestedSize must be a power of 2, and bigger than 4");
            }
            this.maxNestedSize = maxNestedSize;
            return this;
        }

        public JsonDeserializerOption build() {
            Map<Class<?>, MarshallTransformerFacade> transformerFacadeMap = new HashMap<>();
            for (MarshallTransformerFacade fc : transformerFacades) {
                transformerFacadeMap.put(fc.customType(), fc);
            }
            return new JsonDeserializerOption(Map.copyOf(transformerFacadeMap), consumeAllBytes, ignoreUnknownFields, ensureAllFieldsPresent,
                    maxEmptyBytes, maxNumberBytes, maxStringBytes, maxArrayElements, maxMapElements, maxNestedSize);
        }
    }
}
