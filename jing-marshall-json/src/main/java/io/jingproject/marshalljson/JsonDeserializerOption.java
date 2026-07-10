package io.jingproject.marshalljson;

import io.jingproject.marshall.MarshallTransformerFacade;
import io.jingproject.marshall.Marshalls;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class JsonDeserializerOption {
    private static final int MIN_EMPTY_SIZE = 8;
    private static final int MAX_EMPTY_SIZE = Integer.parseInt(System.getProperty("jing.marshalljson.maxemptysize", "4096"));
    private static final int MIN_NUMBER_SIZE = 24;
    private static final int MAX_NUMBER_SIZE = Integer.parseInt(System.getProperty("jing.marshalljson.maxnumbersize", "256"));
    private static final int MIN_STRING_SIZE = 64;
    private static final int MAX_STRING_SIZE = Integer.parseInt(System.getProperty("jing.marshalljson.maxstringsize", "65535"));
    private static final int MIN_ARRAY_SIZE = 100;
    private static final int MAX_ARRAY_SIZE = Integer.parseInt(System.getProperty("jing.marshalljson.maxarraysize", "4000"));
    private static final int MIN_MAP_SIZE = 20;
    private static final int MAX_MAP_SIZE = Integer.parseInt(System.getProperty("jing.marshalljson.maxmapsize", "800"));
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

    static {
        if(MAX_EMPTY_SIZE <=  MIN_EMPTY_SIZE) {
            throw new IllegalArgumentException("max empty size too small : " + MAX_EMPTY_SIZE);
        }
        if(MAX_NUMBER_SIZE <=  MIN_NUMBER_SIZE) {
            throw new IllegalArgumentException("max number size too small : " + MAX_NUMBER_SIZE);
        }
        if(MAX_STRING_SIZE <= MIN_STRING_SIZE) {
            throw new IllegalArgumentException("max string size too small : " + MAX_STRING_SIZE);
        }
        if(MAX_ARRAY_SIZE <= MIN_ARRAY_SIZE) {
            throw new IllegalArgumentException("max array size too small : " + MAX_ARRAY_SIZE);
        }
        if(MAX_MAP_SIZE <= MIN_MAP_SIZE) {
            throw new IllegalArgumentException("max map size too small : " + MAX_MAP_SIZE);
        }
    }

    private JsonDeserializerOption(Map<Class<?>, MarshallTransformerFacade> tfcMap, boolean consumeAllBytes, boolean ignoreUnknownFields,
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

    public static Builder builder() {
        return new Builder();
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

    public int maxNestedSize() {
        return maxNestedSize;
    }

    public static class Builder {
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

        public Builder registerTransformerClasses(Class<?>... transformers) {
            if(transformers == null || transformers.length == 0) {
                throw new IllegalArgumentException("transformers must not be null or empty");
            }
            for (Class<?> transformer : transformers) {
                if(transformer == null) {
                    throw new IllegalArgumentException("transformer must not be null");
                }
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
                if (!JsonPrimitiveType.class.isAssignableFrom(builtinType)) {
                    throw new IllegalArgumentException("builtinType not implementing JsonPrimitiveType interface : " + builtinType.getName());
                }
                transformerFacades.add(tfc); // no conflict
            }
            return this;
        }

        public Builder setConsumeAllBytes(boolean consumeAllBytes) {
            this.consumeAllBytes = consumeAllBytes;
            return this;
        }

        public Builder setIgnoreUnknownFields(boolean ignoreUnknownFields) {
            this.ignoreUnknownFields = ignoreUnknownFields;
            return this;
        }

        public Builder setEnsureAllFieldsPresent(boolean ensureAllFieldsPresent) {
            this.ensureAllFieldsPresent = ensureAllFieldsPresent;
            return this;
        }

        public Builder setMaxEmptyBytes(int maxEmptyBytes) {
            if(maxEmptyBytes < MIN_EMPTY_SIZE || maxEmptyBytes > MAX_EMPTY_SIZE) {
                throw new IllegalArgumentException("maxEmptyBytes out of range : " + maxEmptyBytes);
            }
            this.maxEmptyBytes = maxEmptyBytes;
            return this;
        }

        public Builder setMaxNumberBytes(int maxNumberBytes) {
            if(maxNumberBytes < MIN_NUMBER_SIZE || maxNumberBytes > MAX_NUMBER_SIZE) {
                throw new IllegalArgumentException("maxNumberBytes out of range : " + maxNumberBytes);
            }
            this.maxNumberBytes = maxNumberBytes;
            return this;
        }

        public Builder setMaxStringBytes(int maxStringBytes) {
            if(maxStringBytes < MIN_STRING_SIZE || maxStringBytes > MAX_STRING_SIZE) {
                throw new IllegalArgumentException("maxStringBytes out of range : " + maxStringBytes);
            }
            this.maxStringBytes = maxStringBytes;
            return this;
        }

        public void setMaxArrayElements(int maxArrayElements) {
            if(maxArrayElements < MIN_ARRAY_SIZE || maxArrayElements > MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException("maxArrayElements out of range : " + maxArrayElements);
            }
            this.maxArrayElements = maxArrayElements;
        }

        public void setMaxMapElements(int maxMapElements) {
            if(maxMapElements < MIN_MAP_SIZE || maxMapElements > MAX_MAP_SIZE) {
                throw new IllegalArgumentException("maxMapElements out of range : " + maxMapElements);
            }
            this.maxMapElements = maxMapElements;
        }

        public Builder setMaxNestedSize(int maxNestedSize) {
            if(maxNestedSize < JsonDeserializerState.INITIAL_SIZE || maxNestedSize > JsonDeserializerState.MAX_SIZE) {
                throw new IllegalArgumentException("maxNestedSize out of range : " + maxNestedSize);
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
