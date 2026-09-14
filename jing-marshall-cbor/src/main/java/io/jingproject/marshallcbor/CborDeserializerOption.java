package io.jingproject.marshallcbor;

import io.jingproject.common.Utils;
import io.jingproject.marshall.MarshallTransformerFacade;
import io.jingproject.marshall.Marshalls;
import jdk.incubator.vector.ByteVector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CborDeserializerOption {
    private static final int MIN_STRING_SIZE = 64;
    private static final int MAX_STRING_SIZE = Math.min(Integer.parseInt(System.getProperty("jing.marshallcbor.maxstringsize", "65535")), 1024 * 1024 * 64);
    private static final int MIN_BYTES_SIZE = 64;
    private static final int MAX_BYTES_SIZE = Math.min(Integer.parseInt(System.getProperty("jing.marshallcbor.maxbytesbytes", "65535")), 1024 * 1024 * 64);
    private static final int MIN_ARRAY_SIZE = 100;
    private static final int MAX_ARRAY_SIZE = Math.min(Integer.parseInt(System.getProperty("jing.marshallcbor.maxarraysize", "1000")), 65535);
    private static final int MIN_MAP_SIZE = 20;
    private static final int MAX_MAP_SIZE = Math.min(Integer.parseInt(System.getProperty("jing.marshallcbor.maxmapsize", "200")), 65535);
    private static final CborDeserializerOption DEFAULT_OPTION = CborDeserializerOption.builder().build();

    static {
        if (MAX_STRING_SIZE <= MIN_STRING_SIZE) {
            throw new IllegalArgumentException("max string size too small : " + MAX_STRING_SIZE);
        }
        if (MAX_BYTES_SIZE <= MIN_BYTES_SIZE) {
            throw new IllegalArgumentException("max bytes size too small : " + MAX_BYTES_SIZE);
        }
        if (MAX_ARRAY_SIZE <= MIN_ARRAY_SIZE) {
            throw new IllegalArgumentException("max array size too small : " + MAX_ARRAY_SIZE);
        }
        if (MAX_MAP_SIZE <= MIN_MAP_SIZE) {
            throw new IllegalArgumentException("max map size too small : " + MAX_MAP_SIZE);
        }
    }

    private final Map<Class<?>, CborDeserializeFunc> customFuncMap;
    private final Map<Class<?>, CborDeserializeFunc> customArrFuncMap;
    private final boolean ensureAllFieldsPresent;
    private final int maxStringBytes;
    private final int maxBytesBytes;
    private final int maxArrayElements;
    private final int maxMapElements;
    private final int maxDummyElements;
    private final int maxNestedSize;
    private final int charBufferSize;

    private CborDeserializerOption(Map<Class<?>, CborDeserializeFunc> customFuncMap, Map<Class<?>, CborDeserializeFunc> customArrFuncMap,
                                   boolean ensureAllFieldsPresent, int maxStringBytes, int maxBytesBytes, int maxArrayElements,
                                   int maxMapElements, int maxDummyElements, int maxNestedSize, int charBufferSize) {
        this.customFuncMap = customFuncMap;
        this.customArrFuncMap = customArrFuncMap;
        this.ensureAllFieldsPresent = ensureAllFieldsPresent;
        this.maxStringBytes = maxStringBytes;
        this.maxBytesBytes = maxBytesBytes;
        this.maxArrayElements = maxArrayElements;
        this.maxMapElements = maxMapElements;
        this.maxDummyElements = maxDummyElements;
        this.maxNestedSize = maxNestedSize;
        this.charBufferSize = charBufferSize;
    }

    public static CborDeserializerOption defaultOption() {
        return DEFAULT_OPTION;
    }

    public static Builder builder() {
        return new Builder();
    }

    public CborDeserializeFunc customFunc(Class<?> clazz) {
        return customFuncMap.get(clazz);
    }

    public CborDeserializeFunc customArrFunc(Class<?> clazz) {
        return customArrFuncMap.get(clazz);
    }

    public boolean ensureAllFieldsPresent() {
        return ensureAllFieldsPresent;
    }

    public int maxStringBytes() {
        return maxStringBytes;
    }

    public int maxBytesBytes() {
        return maxBytesBytes;
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

    public int maxNestedSize() {
        return maxNestedSize;
    }

    public int charBufferSize() {
        return charBufferSize;
    }

    public static class Builder {
        private final List<MarshallTransformerFacade> tfcs = new ArrayList<>();
        private boolean ensureAllFieldsPresent = false;
        private int maxStringBytes = 65535;
        private int maxBytesBytes = 65535;
        private int maxArrayElements = 1000;
        private int maxMapElements = 200;
        private int maxDummyElements = 4;
        private int maxNestedSize = 64;
        private int charBufferSize = CborDeserializerContext.CHAR_BUFFER_INITIAL_SIZE;

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
                if (CborDeserializerContext.builtinDeserializeObjFunc(customType) != null) {
                    throw new IllegalArgumentException("cannot override builtin type : " + customType.getName());
                }
                // custom type has value semantics, which is feasible for enums, but absolutely not for marshallable beans
                if (Marshalls.beanMarshallFacade(customType) != null) {
                    throw new IllegalArgumentException("custom type can not be marshallable : " + customType.getName());
                }
                // builtin type must be an implementation class of CborPrimitiveType
                Class<?> builtinType = tfc.builtinType();
                if (!CborPrimitiveType.class.isAssignableFrom(builtinType)) {
                    throw new IllegalArgumentException("builtinType not implementing CborPrimitiveType interface : " + builtinType.getName());
                }
                tfcs.add(tfc);
            }
            return this;
        }

        public Builder setEnsureAllFieldsPresent(boolean ensureAllFieldsPresent) {
            this.ensureAllFieldsPresent = ensureAllFieldsPresent;
            return this;
        }

        public Builder setMaxStringBytes(int maxStringBytes) {
            if (maxStringBytes < MIN_STRING_SIZE || maxStringBytes > MAX_STRING_SIZE) {
                throw new IllegalArgumentException("maxStringBytes out of range : " + maxStringBytes);
            }
            this.maxStringBytes = maxStringBytes;
            return this;
        }

        public Builder setMaxBytesBytes(int maxBytesBytes) {
            if (maxBytesBytes < MIN_BYTES_SIZE || maxBytesBytes > MAX_BYTES_SIZE) {
                throw new IllegalArgumentException("maxBytesBytes out of range : " + maxBytesBytes);
            }
            this.maxBytesBytes = maxBytesBytes;
            return this;
        }

        public Builder setMaxArrayElements(int maxArrayElements) {
            if (maxArrayElements < MIN_ARRAY_SIZE || maxArrayElements > MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException("maxArrayElements out of range : " + maxArrayElements);
            }
            this.maxArrayElements = maxArrayElements;
            return this;
        }

        public Builder setMaxMapElements(int maxMapElements) {
            if (maxMapElements < MIN_MAP_SIZE || maxMapElements > MAX_MAP_SIZE) {
                throw new IllegalArgumentException("maxMapElements out of range : " + maxMapElements);
            }
            this.maxMapElements = maxMapElements;
            return this;
        }

        public Builder setMaxDummyElements(int maxDummyElements) {
            this.maxDummyElements = maxDummyElements;
            return this;
        }

        public Builder setMaxNestedSize(int maxNestedSize) {
            if (maxNestedSize < CborDeserializer.INITIAL_SIZE || maxNestedSize > CborDeserializer.MAX_SIZE) {
                throw new IllegalArgumentException("maxNestedSize out of range : " + maxNestedSize);
            }
            this.maxNestedSize = maxNestedSize;
            return this;
        }

        public Builder setCharBufferSize(int charBufferSize) {
            if (charBufferSize < CborDeserializerContext.CHAR_BUFFER_INITIAL_SIZE) {
                throw new IllegalArgumentException("charBufferSize out of range : " + charBufferSize);
            }
            this.charBufferSize = Utils.roundUp(charBufferSize, ByteVector.SPECIES_MAX.length());
            return this;
        }

        private static CborDeserializeFunc customObjDeserializeFunc(MarshallTransformerFacade tfc) {
            Class<?> builtinType = tfc.builtinType();
            if (builtinType == CborPrimitiveType.class) {
                return (b, c) -> {
                    c.setObj(tfc.toCustom(c.deserializeCborPrimitiveType(b))); // self guarded
                    return CborDeserializeResult.Continue;
                };
            } else if (builtinType == CborBoolType.class) {
                return (b, c) -> {
                    c.setObj(tfc.toCustom(c.deserializeCborBoolType(b)));
                    return CborDeserializeResult.Continue;
                };
            } else if (builtinType == CborNumberType.class) {
                return (b, c) -> {
                    c.setObj(tfc.toCustom(c.deserializeCborNumberType(b)));
                    return CborDeserializeResult.Continue;
                };
            } else if (builtinType == CborStrType.class) {
                return (b, c) -> {
                    c.setObj(tfc.toCustom(c.deserializeCborStrType(b)));
                    return CborDeserializeResult.Continue;
                };
            } else if (builtinType == CborBytesType.class) {
                return (b, c) -> {
                    c.setObj(tfc.toCustom(c.deserializeCborBytesType(b)));
                    return CborDeserializeResult.Continue;
                };
            } else {
                throw new AssertionError("unknown builtin type : " + builtinType.getName());
            }
        }

        private static CborDeserializeFunc customArrDeserializeFunc(MarshallTransformerFacade tfc) {
            Class<?> builtinType = tfc.builtinType();
            if (builtinType == CborPrimitiveType.class) {
                return (b, c) -> {
                    c.setObj(tfc.toCustomArray(c.deserializeCborPrimitiveTypeArray(b)));
                    return CborDeserializeResult.Continue;
                };
            } else if (builtinType == CborBoolType.class) {
                return (b, c) -> {
                    c.setObj(tfc.toCustomArray(c.deserializeCborBoolTypeArray(b)));
                    return CborDeserializeResult.Continue;
                };
            } else if (builtinType == CborNumberType.class) {
                return (b, c) -> {
                    c.setObj(tfc.toCustomArray(c.deserializeCborNumberTypeArray(b)));
                    return CborDeserializeResult.Continue;
                };
            } else if (builtinType == CborStrType.class) {
                return (b, c) -> {
                    c.setObj(tfc.toCustomArray(c.deserializeCborStrTypeArray(b)));
                    return CborDeserializeResult.Continue;
                };
            } else if (builtinType == CborBytesType.class) {
                return (b, c) -> {
                    c.setObj(tfc.toCustomArray(c.deserializeCborBytesTypeArray(b)));
                    return CborDeserializeResult.Continue;
                };
            } else {
                throw new AssertionError("unknown builtin type : " + builtinType.getName());
            }
        }

        public CborDeserializerOption build() {
            Map<Class<?>, CborDeserializeFunc> customFuncMap = new HashMap<>();
            Map<Class<?>, CborDeserializeFunc> customArrFuncMap = new HashMap<>();
            for (MarshallTransformerFacade tfc : tfcs) {
                Class<?> customType = tfc.customType();
                customFuncMap.put(customType, customObjDeserializeFunc(tfc));
                customArrFuncMap.put(customType, customArrDeserializeFunc(tfc));
            }
            return new CborDeserializerOption(Map.copyOf(customFuncMap), Map.copyOf(customArrFuncMap),
                    ensureAllFieldsPresent, maxStringBytes, maxBytesBytes, maxArrayElements,
                    maxMapElements, maxDummyElements, maxNestedSize, charBufferSize);
        }
    }
}