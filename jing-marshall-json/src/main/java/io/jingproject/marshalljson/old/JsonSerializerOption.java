package io.jingproject.marshalljson.old;

import io.jingproject.marshall.MarshallTransformerFacade;
import io.jingproject.marshall.Marshalls;
import io.jingproject.marshalljson.JsonIndentationLevel;
import io.jingproject.marshalljson.JsonPrimitiveType;

import java.util.*;

public final class JsonSerializerOption {
    public static final int DEFAULT_INITIAL_SIZE = 4;
    public static final int DEFAULT_MAX_SIZE     = 64;
    public static final int HARD_MIN_SIZE        = 2;
    public static final int HARD_MAX_SIZE        = 1024;

    private static final JsonSerializerOption DEFAULT_OPTION = JsonSerializerOption.builder().build();

    private final Map<Class<?>, JsonValueSerializer> customSerializers;
    private final boolean serializeNullInObjOrMap;
    private final JsonIndentationLevel jsonIndentationLevel;
    private final int initialSize;
    private final int maxSize;

    private JsonSerializerOption(Map<Class<?>, JsonValueSerializer> customSerializers, boolean serializeNullInObjOrMap,
                                 JsonIndentationLevel jsonIndentationLevel, int initialSize, int maxSize) {
        this.customSerializers = customSerializers;
        this.serializeNullInObjOrMap = serializeNullInObjOrMap;
        this.jsonIndentationLevel = jsonIndentationLevel;
        this.initialSize = initialSize;
        this.maxSize = maxSize;
    }

    public static JsonSerializerOption defaultOption() {
        return DEFAULT_OPTION;
    }

    public static JsonSerializerOptionBuilder builder() {
        return new JsonSerializerOptionBuilder();
    }

    public JsonValueSerializer getCustomSerializer(Class<?> type) {
        return customSerializers.get(type);
    }

    public boolean serializeNullInObjOrMap() {
        return serializeNullInObjOrMap;
    }

    public JsonIndentationLevel indentationLevel() {
        return jsonIndentationLevel;
    }

    public int initialSize() {
        return initialSize;
    }

    public int maxSize() {
        return maxSize;
    }

    public static class JsonSerializerOptionBuilder {
        private final Set<MarshallTransformerFacade> transformerFacades = new HashSet<>();
        private boolean serializeNullInObjOrMap = false;
        private JsonIndentationLevel jsonIndentationLevel = JsonIndentationLevel.NONE;
        private int initialSize = DEFAULT_INITIAL_SIZE;
        private int maxSize = DEFAULT_MAX_SIZE;

        public JsonSerializerOptionBuilder setTransformers(Class<?>... transformers) {
            if(transformers == null || transformers.length == 0) {
                throw new IllegalArgumentException("transformers must not be null or empty");
            }
            for (Class<?> transformer : transformers) {
                MarshallTransformerFacade mtf = Marshalls.getMarshallTransformerFacade(transformer);
                if(mtf == null) {
                    throw new IllegalArgumentException("transformer not found : " + transformer.getName());
                }
                Class<?> customType = mtf.customType();
                if(transformerFacades.stream().anyMatch(fc -> fc.customType() == customType)) {
                    throw new IllegalArgumentException("custom type already exists : " + customType.getName());
                }
                // primitive types are not supported in generics, array types are not supported in transformers, so we don't need to double-check them
                if(JsonSerializeUtil.builtinObjSerializer(customType) != null) {
                    throw new IllegalArgumentException("cannot override builtin type : " + customType.getName());
                }
                Class<?> builtinType = mtf.builtinType();
                if (JsonPrimitiveType.class.isAssignableFrom(builtinType)) {
                    throw new IllegalArgumentException("builtinType not implementing JsonPrimitiveType interface : " + builtinType.getName());
                }
                if(!transformerFacades.add(mtf)) {
                    throw new IllegalArgumentException("transformer already exists : " + transformer.getName());
                }
            }
            return this;
        }

        public JsonSerializerOptionBuilder setSerializeNullInObjOrMap(boolean serializeNullInObjOrMap) {
            this.serializeNullInObjOrMap = serializeNullInObjOrMap;
            return this;
        }

        public JsonSerializerOptionBuilder setIndentationLevel(JsonIndentationLevel jsonIndentationLevel) {
            this.jsonIndentationLevel = jsonIndentationLevel;
            return this;
        }

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

        public JsonSerializerOption build() {
            Map<Class<?>, JsonValueSerializer> customSerializers = new HashMap<>();
            for (MarshallTransformerFacade fc : transformerFacades) {
                customSerializers.put(fc.customType(), (o, _, w, _) -> {
                    JsonPrimitiveType pt = (JsonPrimitiveType) fc.toBuiltin(o);
                    JsonSerializeUtil.serializeJsonPrimitiveType(pt, w);
                    return null;
                });
            }
            if(initialSize > maxSize) {
                throw new IllegalArgumentException("initialSize cannot be less than maxSize");
            }
            if(maxSize % initialSize != 0) {
                throw new IllegalArgumentException("maxSize : [" + maxSize + "] must be multiple of " + initialSize);
            }
            if(Integer.bitCount(maxSize / initialSize) != 1) {
                throw new IllegalArgumentException("maxSize / initialSize must be power of 2");
            }
            return new JsonSerializerOption(Map.copyOf(customSerializers), serializeNullInObjOrMap,
                    jsonIndentationLevel, initialSize, maxSize);
        }
    }
}
