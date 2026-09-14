package io.jingproject.marshallcbor;

import io.jingproject.marshall.MarshallTransformerFacade;
import io.jingproject.marshall.Marshalls;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CborSerializerOption {
    private static final CborSerializerOption DEFAULT_OPTION = CborSerializerOption.builder().build();

    private final Map<Class<?>, CborSerializeFunc> customFuncMap;
    private final Map<Class<?>, CborSerializeFunc> customArrFuncMap;
    private final boolean serializeNullInObjOrMap;
    private final boolean deterministicEncoding;
    private final int maxNestedSize;

    private CborSerializerOption(Map<Class<?>, CborSerializeFunc> customFuncMap, Map<Class<?>, CborSerializeFunc> customArrFuncMap,
                                 boolean serializeNullInObjOrMap, boolean deterministicEncoding, int maxNestedSize) {
        this.customFuncMap = customFuncMap;
        this.customArrFuncMap = customArrFuncMap;
        this.serializeNullInObjOrMap = serializeNullInObjOrMap;
        this.deterministicEncoding = deterministicEncoding;
        this.maxNestedSize = maxNestedSize;
    }

    public static CborSerializerOption defaultOption() {
        return DEFAULT_OPTION;
    }

    public static Builder builder() {
        return new Builder();
    }

    public CborSerializeFunc customFunc(Class<?> clazz) {
        return customFuncMap.get(clazz);
    }

    public CborSerializeFunc customArrFunc(Class<?> clazz) {
        return customArrFuncMap.get(clazz);
    }

    public boolean serializeNullInObjOrMap() {
        return serializeNullInObjOrMap;
    }

    public boolean deterministicEncoding() {
        return deterministicEncoding;
    }

    public int maxNestedSize() {
        return maxNestedSize;
    }

    // deterministic encoding requires definite-length containers, which in turn
    // forces null entries to be written; an explicit setSerializeNullInObjOrMap
    // (false) is ignored in deterministic mode
    public boolean effectiveSerializeNullInObjOrMap() {
        return deterministicEncoding || serializeNullInObjOrMap;
    }

    public static class Builder {
        private final List<MarshallTransformerFacade> tfcs = new ArrayList<>();
        private boolean serializeNullInObjOrMap = false;
        private boolean deterministicEncoding = false;
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
                    if (m.customType().equals(customType)) {
                        throw new IllegalArgumentException("custom type already exists : " + customType.getName());
                    }
                }
                // primitive types are not supported in generics, array types are not
                // supported in transformers, so we don't need to double-check them
                if (CborSerializerContext.builtinSerializeObjFunc(customType) != null) {
                    throw new IllegalArgumentException("cannot override builtin type : " + customType.getName());
                }
                // custom type has value semantics, which is feasible for enums, but
                // absolutely not for marshallable beans
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

        public Builder setSerializeNullInObjOrMap(boolean serializeNullInObjOrMap) {
            this.serializeNullInObjOrMap = serializeNullInObjOrMap;
            return this;
        }

        public Builder setDeterministicEncoding(boolean deterministicEncoding) {
            this.deterministicEncoding = deterministicEncoding;
            return this;
        }

        public Builder setMaxNestedSize(int maxNestedSize) {
            if (maxNestedSize < CborSerializer.INITIAL_SIZE || maxNestedSize > CborSerializer.MAX_SIZE) {
                throw new IllegalArgumentException("maxNestedSize out of range : " + maxNestedSize);
            }
            this.maxNestedSize = maxNestedSize;
            return this;
        }

        private static CborSerializeFunc customObjSerializeFunc(MarshallTransformerFacade tfc) {
            Class<?> builtinType = tfc.builtinType();
            if (builtinType == CborPrimitiveType.class) {
                return (o, c) -> {
                    c.serializeCborPrimitiveType((CborPrimitiveType) tfc.toBuiltin(o));
                    return CborSerializeResult.Continue;
                };
            } else if (builtinType == CborBoolType.class) {
                return (o, c) -> {
                    c.serializeCborBoolType((CborBoolType) tfc.toBuiltin(o));
                    return CborSerializeResult.Continue;
                };
            } else if (builtinType == CborNumberType.class) {
                return (o, c) -> {
                    c.serializeCborNumberType((CborNumberType) tfc.toBuiltin(o));
                    return CborSerializeResult.Continue;
                };
            } else if (builtinType == CborStrType.class) {
                return (o, c) -> {
                    c.serializeCborStrType((CborStrType) tfc.toBuiltin(o));
                    return CborSerializeResult.Continue;
                };
            } else if (builtinType == CborBytesType.class) {
                return (o, c) -> {
                    c.serializeCborBytesType((CborBytesType) tfc.toBuiltin(o));
                    return CborSerializeResult.Continue;
                };
            } else {
                throw new AssertionError("unknown builtin type : " + builtinType.getName());
            }
        }

        private static CborSerializeFunc customArrSerializeFunc(MarshallTransformerFacade tfc) {
            Class<?> builtinType = tfc.builtinType();
            if (builtinType == CborPrimitiveType.class) {
                return (o, c) -> {
                    c.serializeCborPrimitiveTypeArray((CborPrimitiveType[]) tfc.toBuiltinArray((Object[]) o));
                    return CborSerializeResult.Continue;
                };
            } else if (builtinType == CborBoolType.class) {
                return (o, c) -> {
                    c.serializeCborBoolTypeArray((CborBoolType[]) tfc.toBuiltinArray((Object[]) o));
                    return CborSerializeResult.Continue;
                };
            } else if (builtinType == CborNumberType.class) {
                return (o, c) -> {
                    c.serializeCborNumberTypeArray((CborNumberType[]) tfc.toBuiltinArray((Object[]) o));
                    return CborSerializeResult.Continue;
                };
            } else if (builtinType == CborStrType.class) {
                return (o, c) -> {
                    c.serializeCborStrTypeArray((CborStrType[]) tfc.toBuiltinArray((Object[]) o));
                    return CborSerializeResult.Continue;
                };
            } else if (builtinType == CborBytesType.class) {
                return (o, c) -> {
                    c.serializeCborBytesTypeArray((CborBytesType[]) tfc.toBuiltinArray((Object[]) o));
                    return CborSerializeResult.Continue;
                };
            } else {
                throw new AssertionError("unknown builtin type : " + builtinType.getName());
            }
        }

        public CborSerializerOption build() {
            Map<Class<?>, CborSerializeFunc> customFuncMap = new HashMap<>();
            Map<Class<?>, CborSerializeFunc> customArrFuncMap = new HashMap<>();
            for (MarshallTransformerFacade tfc : tfcs) {
                Class<?> customType = tfc.customType();
                customFuncMap.put(customType, customObjSerializeFunc(tfc));
                customArrFuncMap.put(customType, customArrSerializeFunc(tfc));
            }
            return new CborSerializerOption(Map.copyOf(customFuncMap), Map.copyOf(customArrFuncMap),
                    serializeNullInObjOrMap, deterministicEncoding, maxNestedSize);
        }
    }
}