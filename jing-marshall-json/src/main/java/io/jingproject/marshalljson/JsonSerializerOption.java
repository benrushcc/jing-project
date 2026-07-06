package io.jingproject.marshalljson;

import io.jingproject.marshall.MarshallTransformerFacade;
import io.jingproject.marshall.Marshalls;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class JsonSerializerOption {
    private static final JsonSerializerOption DEFAULT_OPTION = JsonSerializerOption.builder().build();

    private final Map<Class<?>, JsonSerializeFunc> funcMap;
    private final boolean serializeNullInObjOrMap;
    private final JsonIndentationLevel jsonIndentationLevel;
    private final int maxNestedSize;

    public JsonSerializerOption(Map<Class<?>, JsonSerializeFunc> funcMap, boolean serializeNullInObjOrMap,
                                JsonIndentationLevel jsonIndentationLevel, int maxNestedSize) {
        this.funcMap = funcMap;
        this.serializeNullInObjOrMap = serializeNullInObjOrMap;
        this.jsonIndentationLevel = jsonIndentationLevel;
        this.maxNestedSize = maxNestedSize;
    }

    public static JsonSerializerOption defaultOption() {
        return DEFAULT_OPTION;
    }

    public static JsonSerializerOptionBuilder builder() {
        return new JsonSerializerOptionBuilder();
    }

    public JsonSerializeFunc customFunc(Class<?> clazz) {
        return funcMap.get(clazz);
    }

    public boolean serializeNullInObjOrMap() {
        return serializeNullInObjOrMap;
    }

    public JsonIndentationLevel indentationLevel() {
        return jsonIndentationLevel;
    }

    public int maxSize() {
        return maxNestedSize;
    }

    public static class JsonSerializerOptionBuilder {
        private final Set<MarshallTransformerFacade> transformerFacades = new HashSet<>();
        private boolean serializeNullInObjOrMap = false;
        private JsonIndentationLevel jsonIndentationLevel = JsonIndentationLevel.NONE;
        private int maxNestedSize = 64;

        public JsonSerializerOptionBuilder withTransformers(Class<?>... transformers) {
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

        public JsonSerializerOptionBuilder setSerializeNullInObjOrMap(boolean serializeNullInObjOrMap) {
            this.serializeNullInObjOrMap = serializeNullInObjOrMap;
            return this;
        }

        public JsonSerializerOptionBuilder setIndentationLevel(JsonIndentationLevel jsonIndentationLevel) {
            this.jsonIndentationLevel = jsonIndentationLevel;
            return this;
        }

        public JsonSerializerOptionBuilder setMaxNestedSize(int maxNestedSize) {
            if(Integer.bitCount(maxNestedSize) != 1 || maxNestedSize < 4) {
                throw new IllegalArgumentException("maxNestedSize must be a power of 2, and bigger than 4");
            }
            this.maxNestedSize = maxNestedSize;
            return this;
        }

        public JsonSerializerOption build() {
            Map<Class<?>, JsonSerializeFunc> funcMap = new HashMap<>();
            for (MarshallTransformerFacade fc : transformerFacades) {
                funcMap.put(fc.customType(), (_, w, o, _) -> {
                    JsonSerializeUtil.serializeJsonPrimitiveType((JsonPrimitiveType) fc.toBuiltin(o), w);
                    return JsonSerializeResult.CONTINUE;
                });
            }
            return new JsonSerializerOption(Map.copyOf(funcMap), serializeNullInObjOrMap,
                    jsonIndentationLevel, maxNestedSize);
        }
    }

    
}
