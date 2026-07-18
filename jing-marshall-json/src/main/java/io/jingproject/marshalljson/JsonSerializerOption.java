package io.jingproject.marshalljson;

import io.jingproject.marshall.MarshallTransformerFacade;
import io.jingproject.marshall.Marshalls;

import java.util.HashMap;
import java.util.Map;

public final class JsonSerializerOption {
    private static final JsonSerializerOption DEFAULT_OPTION = JsonSerializerOption.builder().build();

    private final Map<Class<?>, JsonSerializeFunc> funcMap;
    private final boolean serializeNullInObjOrMap;
    private final JsonIndentationLevel jsonIndentationLevel;
    private final int maxNestedSize;

    private JsonSerializerOption(Map<Class<?>, JsonSerializeFunc> funcMap, boolean serializeNullInObjOrMap,
                                JsonIndentationLevel jsonIndentationLevel, int maxNestedSize) {
        this.funcMap = funcMap;
        this.serializeNullInObjOrMap = serializeNullInObjOrMap;
        this.jsonIndentationLevel = jsonIndentationLevel;
        this.maxNestedSize = maxNestedSize;
    }

    public static JsonSerializerOption defaultOption() {
        return DEFAULT_OPTION;
    }

    public static Builder builder() {
        return new Builder();
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

    public int maxNestedSize() {
        return maxNestedSize;
    }

    public static class Builder {
        private final Map<Class<?>, MarshallTransformerFacade> transformerFacadeMap = new HashMap<>();
        private boolean serializeNullInObjOrMap = false;
        private JsonIndentationLevel jsonIndentationLevel = JsonIndentationLevel.NONE;
        private int maxNestedSize = 64;

        public Builder setTransformerClasses(Class<?>... transformerClasses) {
            if(transformerClasses == null || transformerClasses.length == 0) {
                throw new IllegalArgumentException("transformers must not be null or empty");
            }
            for (Class<?> transformer : transformerClasses) {
                if(transformer == null) {
                    throw new IllegalArgumentException("transformer must not be null");
                }
                MarshallTransformerFacade tfc = Marshalls.getMarshallTransformerFacade(transformer);
                if(tfc == null) {
                    throw new IllegalArgumentException("transformer not found : " + transformer.getName());
                }
                Class<?> customType = tfc.customType();
                if(transformerFacadeMap.containsKey(customType)) {
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
                transformerFacadeMap.put(customType, tfc); // no conflict
            }
            return this;
        }

        public Builder setSerializeNullInObjOrMap(boolean serializeNullInObjOrMap) {
            this.serializeNullInObjOrMap = serializeNullInObjOrMap;
            return this;
        }

        public Builder setIndentationLevel(JsonIndentationLevel jsonIndentationLevel) {
            if(jsonIndentationLevel == null) {
                throw new IllegalArgumentException("jsonIndentationLevel must not be null");
            }
            this.jsonIndentationLevel = jsonIndentationLevel;
            return this;
        }

        public Builder setMaxNestedSize(int maxNestedSize) {
            if(maxNestedSize < JsonSerializerState.INITIAL_SIZE || maxNestedSize > JsonSerializerState.MAX_SIZE) {
                throw new IllegalArgumentException("maxNestedSize out of range : " + maxNestedSize);
            }
            this.maxNestedSize = maxNestedSize;
            return this;
        }

        public JsonSerializerOption build() {
            Map<Class<?>, JsonSerializeFunc> funcMap = new HashMap<>();
            transformerFacadeMap.forEach((k, v) -> funcMap.put(k, (_, w, c, o, _) -> {
                JsonSerializeUtil.serializeJsonPrimitiveType((JsonPrimitiveType) v.toBuiltin(o), w, c);
                return JsonSerializeResult.Continue;
            }));
            return new JsonSerializerOption(Map.copyOf(funcMap), serializeNullInObjOrMap,
                    jsonIndentationLevel, maxNestedSize);
        }
    }
    
}
