package io.jingproject.marshalljson;

import io.jingproject.common.Utils;
import io.jingproject.marshall.MarshallTransformerFacade;
import io.jingproject.marshall.Marshalls;
import jdk.incubator.vector.ByteVector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class JsonDeserializerOption {
    public static final int DEFAULT_CHAR_BUFFER_SIZE = 128;
    private static final int MIN_EMPTY_SIZE = 8;
    private static final int MAX_EMPTY_SIZE = Math.min(Integer.parseInt(System.getProperty("jing.marshalljson.maxemptysize", "4096")), 65535);
    private static final int MIN_NUMBER_SIZE = 24;
    private static final int MAX_NUMBER_SIZE = Math.min(Integer.parseInt(System.getProperty("jing.marshalljson.maxnumbersize", "256")), 65535);
    private static final int MIN_STRING_SIZE = 64;
    private static final int MAX_STRING_SIZE = Integer.parseInt(System.getProperty("jing.marshalljson.maxstringsize", "65535"));
    private static final int MIN_ARRAY_SIZE = 100;
    private static final int MAX_ARRAY_SIZE = Math.min(Integer.parseInt(System.getProperty("jing.marshalljson.maxarraysize", "4000")), 65535);
    private static final int MIN_MAP_SIZE = 20;
    private static final int MAX_MAP_SIZE = Math.min(Integer.parseInt(System.getProperty("jing.marshalljson.maxmapsize", "800")), 65535);
    private static final JsonDeserializerOption DEFAULT_OPTION = JsonDeserializerOption.builder().build();

    static {
        if (MAX_EMPTY_SIZE <= MIN_EMPTY_SIZE) {
            throw new IllegalArgumentException("max empty size too small : " + MAX_EMPTY_SIZE);
        }
        if (MAX_NUMBER_SIZE <= MIN_NUMBER_SIZE) {
            throw new IllegalArgumentException("max number size too small : " + MAX_NUMBER_SIZE);
        }
        if (MAX_STRING_SIZE <= MIN_STRING_SIZE) {
            throw new IllegalArgumentException("max string size too small : " + MAX_STRING_SIZE);
        }
        if (MAX_ARRAY_SIZE <= MIN_ARRAY_SIZE) {
            throw new IllegalArgumentException("max array size too small : " + MAX_ARRAY_SIZE);
        }
        if (MAX_MAP_SIZE <= MIN_MAP_SIZE) {
            throw new IllegalArgumentException("max map size too small : " + MAX_MAP_SIZE);
        }
    }

    private final Map<Class<?>, JsonDeserializeFunc> customFuncMap;
    private final Map<Class<?>, JsonDeserializeFunc> customArrFuncMap;
    private final boolean consumeAllBytes;
    private final boolean ensureAllFieldsPresent;
    private final int maxEmptyBytes;
    private final int maxNumberBytes;
    private final int maxStringBytes;
    private final int maxArrayElements;
    private final int maxMapElements;
    private final int maxDummyElements;
    private final int maxDummyArrayElements;
    private final int maxNestedSize;
    private final int charBufferSize;

    private JsonDeserializerOption(Map<Class<?>, JsonDeserializeFunc> customFuncMap, Map<Class<?>, JsonDeserializeFunc> customArrFuncMap,
                                   boolean consumeAllBytes, boolean ensureAllFieldsPresent, int maxEmptyBytes, int maxNumberBytes,
                                   int maxStringBytes, int maxArrayElements, int maxMapElements, int maxDummyElements,
                                   int maxDummyArrayElements, int maxNestedSize, int charBufferSize) {
        this.customFuncMap = customFuncMap;
        this.customArrFuncMap = customArrFuncMap;
        this.consumeAllBytes = consumeAllBytes;
        this.ensureAllFieldsPresent = ensureAllFieldsPresent;
        this.maxEmptyBytes = maxEmptyBytes;
        this.maxNumberBytes = maxNumberBytes;
        this.maxStringBytes = maxStringBytes;
        this.maxArrayElements = maxArrayElements;
        this.maxMapElements = maxMapElements;
        this.maxDummyElements = maxDummyElements;
        this.maxDummyArrayElements = maxDummyArrayElements;
        this.maxNestedSize = maxNestedSize;
        this.charBufferSize = charBufferSize;
    }

    public static JsonDeserializerOption defaultOption() {
        return DEFAULT_OPTION;
    }

    public static Builder builder() {
        return new Builder();
    }

    public JsonDeserializeFunc customFunc(Class<?> clazz) {
        return customFuncMap.get(clazz);
    }

    public JsonDeserializeFunc customArrFunc(Class<?> clazz) {
        return customArrFuncMap.get(clazz);
    }

    public boolean consumeAllBytes() {
        return consumeAllBytes;
    }

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

    public int maxDummyElements() {
        return maxDummyElements;
    }

    public int maxDummyArrayElements() {
        return maxDummyArrayElements;
    }

    public int maxNestedSize() {
        return maxNestedSize;
    }

    public int charBufferSize() {
        return charBufferSize;
    }

    public static class Builder {
        private final List<MarshallTransformerFacade> tfcs = new ArrayList<>();
        private boolean consumeAllBytes = true;
        private boolean ensureAllFieldsPresent = false;
        private int maxEmptyBytes = 256;
        private int maxNumberBytes = 24;
        private int maxStringBytes = 65535;
        private int maxArrayElements = 1000;
        private int maxMapElements = 200;
        private int maxDummyElements = 4;
        private int maxDummyArrayElements = 4;
        private int maxNestedSize = 64;
        private int charBufferSize = DEFAULT_CHAR_BUFFER_SIZE;

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
                    if (m.customType().equals(customType)) {
                        throw new IllegalArgumentException("custom type already exists : " + customType.getName());
                    }
                }
                // primitive types are not supported in generics, array types are not supported in transformers, so we don't need to double-check them
                if (JsonDeserializerContext.builtinDeserializeObjFunc(customType) != null) {
                    throw new IllegalArgumentException("cannot override builtin type : " + customType.getName());
                }
                // custom type has value semantics, which is feasible for enums, but absolutely not for marshallable beans
                if (Marshalls.beanMarshallFacade(customType) != null) {
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

        public Builder setConsumeAllBytes(boolean consumeAllBytes) {
            this.consumeAllBytes = consumeAllBytes;
            return this;
        }

        public Builder setEnsureAllFieldsPresent(boolean ensureAllFieldsPresent) {
            this.ensureAllFieldsPresent = ensureAllFieldsPresent;
            return this;
        }

        public Builder setMaxEmptyBytes(int maxEmptyBytes) {
            if (maxEmptyBytes < MIN_EMPTY_SIZE || maxEmptyBytes > MAX_EMPTY_SIZE) {
                throw new IllegalArgumentException("maxEmptyBytes out of range : " + maxEmptyBytes);
            }
            this.maxEmptyBytes = maxEmptyBytes;
            return this;
        }

        public Builder setMaxNumberBytes(int maxNumberBytes) {
            if (maxNumberBytes < MIN_NUMBER_SIZE || maxNumberBytes > MAX_NUMBER_SIZE) {
                throw new IllegalArgumentException("maxNumberBytes out of range : " + maxNumberBytes);
            }
            this.maxNumberBytes = maxNumberBytes;
            return this;
        }

        public Builder setMaxStringBytes(int maxStringBytes) {
            if (maxStringBytes < MIN_STRING_SIZE || maxStringBytes > MAX_STRING_SIZE) {
                throw new IllegalArgumentException("maxStringBytes out of range : " + maxStringBytes);
            }
            this.maxStringBytes = maxStringBytes;
            return this;
        }

        public void setMaxArrayElements(int maxArrayElements) {
            if (maxArrayElements < MIN_ARRAY_SIZE || maxArrayElements > MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException("maxArrayElements out of range : " + maxArrayElements);
            }
            this.maxArrayElements = maxArrayElements;
        }

        public void setMaxMapElements(int maxMapElements) {
            if (maxMapElements < MIN_MAP_SIZE || maxMapElements > MAX_MAP_SIZE) {
                throw new IllegalArgumentException("maxMapElements out of range : " + maxMapElements);
            }
            this.maxMapElements = maxMapElements;
        }

        public void setMaxDummyElements(int maxDummyElements) {
            // reuse map size limits for dummy elements, as they are structurally analogous.
            if (maxMapElements < 0 || maxMapElements > MAX_MAP_SIZE) {
                throw new IllegalArgumentException("maxDummyElements out of range : " + maxDummyElements);
            }
            this.maxDummyElements = maxDummyElements;
        }

        public void setMaxDummyArrayElements(int maxDummyArrayElements) {
            // reuse array size limits for dummy array elements, as they are structurally analogous.
            if (maxDummyArrayElements < 0 || maxDummyArrayElements > MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException("maxDummyArrayElements out of range : " + maxDummyArrayElements);
            }
            this.maxDummyArrayElements = maxDummyArrayElements;
        }

        public Builder setMaxNestedSize(int maxNestedSize) {
            if (maxNestedSize < JsonDeserializer.INITIAL_SIZE || maxNestedSize > JsonDeserializer.MAX_SIZE) {
                throw new IllegalArgumentException("maxNestedSize out of range : " + maxNestedSize);
            }
            this.maxNestedSize = maxNestedSize;
            return this;
        }

        public void setCharBufferSize(int charBufferSize) {
            if (charBufferSize < DEFAULT_CHAR_BUFFER_SIZE) {
                throw new IllegalArgumentException("charBufferSize out of range : " + charBufferSize);
            }
            this.charBufferSize = Utils.roundUp(charBufferSize, ByteVector.SPECIES_MAX.length());
        }

        private static JsonDeserializeFunc customObjDeserializeFunc(MarshallTransformerFacade tfc) {
            Class<?> builtinType = tfc.builtinType();
            if (builtinType == JsonPrimitiveType.class) {
                return (b, c) -> {
                    c.setObj(tfc.toCustom(c.deserializeJsonPrimitiveType(b))); // self guarded
                    return JsonDeserializeResult.Continue;
                };
            } else if (builtinType == JsonBoolType.class) {
                return (b, c) -> {
                    c.setObj(tfc.toCustom(c.deserializeJsonBoolType(b)));
                    return JsonDeserializeResult.Continue;
                };
            } else if (builtinType == JsonNumberType.class) {
                return (b, c) -> {
                    c.setObj(tfc.toCustom(c.deserializeJsonNumberType(b)));
                    return JsonDeserializeResult.Continue;
                };
            } else if (builtinType == JsonStrType.class) {
                return (b, c) -> {
                    c.setObj(tfc.toCustom(c.deserializeJsonStrType(b)));
                    return JsonDeserializeResult.Continue;
                };
            } else {
                throw new AssertionError("unknown builtin type : " + builtinType.getName());
            }
        }

        private static JsonDeserializeFunc customArrDeserializeFunc(MarshallTransformerFacade tfc) {
            Class<?> builtinType = tfc.builtinType();
            if (builtinType == JsonPrimitiveType.class) {
                return (b, c) -> {
                    c.setObj(tfc.toCustomArray(c.deserializeJsonPrimitiveTypeArray(b)));
                    return JsonDeserializeResult.Continue;
                };
            } else if (builtinType == JsonBoolType.class) {
                return (b, c) -> {
                    c.setObj(tfc.toCustomArray(c.deserializeJsonBoolTypeArray(b)));
                    return JsonDeserializeResult.Continue;
                };
            } else if (builtinType == JsonNumberType.class) {
                return (b, c) -> {
                    c.setObj(tfc.toCustomArray(c.deserializeJsonNumberTypeArray(b)));
                    return JsonDeserializeResult.Continue;
                };
            } else if (builtinType == JsonStrType.class) {
                return (b, c) -> {
                    c.setObj(tfc.toCustomArray(c.deserializeJsonStrTypeArray(b)));
                    return JsonDeserializeResult.Continue;
                };
            } else {
                throw new AssertionError("unknown builtin type : " + builtinType.getName());
            }
        }

        public JsonDeserializerOption build() {
            Map<Class<?>, JsonDeserializeFunc> customFuncMap = new HashMap<>();
            Map<Class<?>, JsonDeserializeFunc> customArrFuncMap = new HashMap<>();
            for (MarshallTransformerFacade tfc : tfcs) {
                Class<?> customType = tfc.customType();
                customFuncMap.put(customType, customObjDeserializeFunc(tfc));
                customArrFuncMap.put(customType, customArrDeserializeFunc(tfc));
            }
            return new JsonDeserializerOption(Map.copyOf(customFuncMap), Map.copyOf(customArrFuncMap),
                    consumeAllBytes, ensureAllFieldsPresent, maxEmptyBytes, maxNumberBytes, maxStringBytes, maxArrayElements,
                    maxMapElements, maxDummyElements, maxDummyArrayElements, maxNestedSize, charBufferSize);
        }
    }
}
