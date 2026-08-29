package io.jingproject.marshalljson;

import io.jingproject.marshall.MarshallTransformerFacade;
import io.jingproject.marshall.Marshalls;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class JsonSerializerOption {
    private static final JsonSerializerOption DEFAULT_OPTION = JsonSerializerOption.builder().build();

    private final Map<Class<?>, JsonSerializeFunc> customFuncMap;
    private final Map<Class<?>, JsonSerializeFunc> customArrFuncMap;
    private final boolean serializeNullInObjOrMap;
    private final JsonIndentationLevel jsonIndentationLevel;
    private final int maxNestedSize;

    private JsonSerializerOption(Map<Class<?>, JsonSerializeFunc> customFuncMap, Map<Class<?>, JsonSerializeFunc> customArrFuncMap, boolean serializeNullInObjOrMap,
                                 JsonIndentationLevel jsonIndentationLevel, int maxNestedSize) {
        this.customFuncMap = customFuncMap;
        this.customArrFuncMap = customArrFuncMap;
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
        return customFuncMap.get(clazz);
    }

    public JsonSerializeFunc customArrFunc(Class<?> clazz) {
        return customArrFuncMap.get(clazz);
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
        private final List<MarshallTransformerFacade> tfcs = new ArrayList<>();
        private boolean serializeNullInObjOrMap = false;
        private JsonIndentationLevel jsonIndentationLevel = JsonIndentationLevel.NONE;
        private int maxNestedSize = 64;

        public Builder setTransformerClasses(Class<?>... transformerClasses) {
            if (transformerClasses == null || transformerClasses.length == 0) {
                throw new IllegalArgumentException("transformers must not be null or empty");
            }
            for (Class<?> c : transformerClasses) {
                if (c == null) {
                    throw new IllegalArgumentException("transformer must not be null");
                }
                MarshallTransformerFacade tfc = Marshalls.marshallTransformerFacade(c);
                if (tfc == null) {
                    throw new IllegalArgumentException("transformer not found : " + c.getName());
                }
                Class<?> customType = tfc.customType();
                for (MarshallTransformerFacade m : tfcs) {
                    if(m.customType().equals(customType)) {
                        throw new IllegalArgumentException("custom type already exists : " + customType.getName());
                    }
                }
                // primitive types are not supported in generics, array types are not supported in transformers, so we don't need to double-check them
                if (JsonSerializerContext.builtinSerializeObjFunc(customType) != null) {
                    throw new IllegalArgumentException("cannot override builtin type : " + customType.getName());
                }
                // custom type has value semantics, which is feasible for enums, but absolutely not for marshallable beans
                if(Marshalls.beanMarshallFacade(customType) != null) {
                    throw new IllegalArgumentException("custom type can not be marshallable : " + customType.getName());
                }
                // builtin type must be an implementation class of JsonPrimitiveType
                Class<?> builtinType = tfc.builtinType();
                if (!JsonPrimitiveType.class.isAssignableFrom(builtinType)) {
                    throw new IllegalArgumentException("builtinType not implementing JsonPrimitiveType interface : " + builtinType.getName());
                }
                tfcs.add(tfc);
            }
            return this;
        }

        public Builder setSerializeNullInObjOrMap(boolean serializeNullInObjOrMap) {
            this.serializeNullInObjOrMap = serializeNullInObjOrMap;
            return this;
        }

        public Builder setIndentationLevel(JsonIndentationLevel jsonIndentationLevel) {
            if (jsonIndentationLevel == null) {
                throw new IllegalArgumentException("jsonIndentationLevel must not be null");
            }
            this.jsonIndentationLevel = jsonIndentationLevel;
            return this;
        }

        public Builder setMaxNestedSize(int maxNestedSize) {
            if (maxNestedSize < JsonSerializer.INITIAL_SIZE || maxNestedSize > JsonSerializer.MAX_SIZE) {
                throw new IllegalArgumentException("maxNestedSize out of range : " + maxNestedSize);
            }
            this.maxNestedSize = maxNestedSize;
            return this;
        }

        private static JsonSerializeFunc customObjSerializeFunc(MarshallTransformerFacade tfc) {
            Class<?> builtinType = tfc.builtinType();
            if(builtinType == JsonPrimitiveType.class) {
                return (o, _, c) -> {
                    c.serializeJsonPrimitiveType((JsonPrimitiveType) tfc.toBuiltin(o));
                    return JsonSerializeResult.Continue;
                };
            } else if(builtinType == JsonBoolType.class) {
                return (o, _, c) -> {
                    c.serializeJsonBoolType((JsonBoolType) tfc.toBuiltin(o));
                    return JsonSerializeResult.Continue;
                };
            } else if(builtinType == JsonNumberType.class) {
                return (o, _, c) -> {
                    c.serializeJsonNumberType((JsonNumberType) tfc.toBuiltin(o));
                    return JsonSerializeResult.Continue;
                };
            } else if(builtinType == JsonStrType.class) {
                return (o, _, c) -> {
                    c.serializeJsonStrType((JsonStrType) tfc.toBuiltin(o));
                    return JsonSerializeResult.Continue;
                };
            } else {
                throw new AssertionError("unknown builtin type : " + builtinType.getName());
            }
        }

        private static JsonSerializeFunc customArrSerializeFunc(MarshallTransformerFacade tfc) {
            Class<?> builtinType = tfc.builtinType();
            if(builtinType == JsonPrimitiveType.class) {
                return (o, i, c) -> {
                    c.serializeJsonPrimitiveTypeArray((JsonPrimitiveType[]) tfc.toBuiltinArray((Object[]) o), i);
                    return JsonSerializeResult.Continue;
                };
            } else if(builtinType == JsonBoolType.class) {
                return (o, i, c) -> {
                    c.serializeJsonBoolTypeArray((JsonBoolType[]) tfc.toBuiltinArray((Object[]) o), i);
                    return JsonSerializeResult.Continue;
                };
            } else if(builtinType == JsonNumberType.class) {
                return (o, i, c) -> {
                    c.serializeJsonNumberTypeArray((JsonNumberType[]) tfc.toBuiltinArray((Object[]) o), i);
                    return JsonSerializeResult.Continue;
                };
            } else if(builtinType == JsonStrType.class) {
                return  (o, i, c) -> {
                    c.serializeJsonStrTypeArray((JsonStrType[]) tfc.toBuiltinArray((Object[]) o), i);
                    return JsonSerializeResult.Continue;
                };
            } else {
                throw new AssertionError("unknown builtin type : " + builtinType.getName());
            }
        }

        public JsonSerializerOption build() {
            Map<Class<?>, JsonSerializeFunc> customFuncMap = new HashMap<>();
            Map<Class<?>, JsonSerializeFunc> customArrFuncMap = new HashMap<>();
            for (MarshallTransformerFacade tfc : tfcs) {
                Class<?> customType = tfc.customType();
                customFuncMap.put(customType, customObjSerializeFunc(tfc));
                customArrFuncMap.put(customType, customArrSerializeFunc(tfc));
            }
            return new JsonSerializerOption(Map.copyOf(customFuncMap), Map.copyOf(customArrFuncMap),
                    serializeNullInObjOrMap, jsonIndentationLevel, maxNestedSize);
        }
    }

}
