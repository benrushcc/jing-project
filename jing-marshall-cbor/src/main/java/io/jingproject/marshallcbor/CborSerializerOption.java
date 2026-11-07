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
    private final boolean serializeNullInObjOrMap;
    private final int maxNestedSize;

    private CborSerializerOption(Map<Class<?>, CborSerializeFunc> customFuncMap,
                                 boolean serializeNullInObjOrMap, int maxNestedSize) {
        this.customFuncMap = customFuncMap;
        this.serializeNullInObjOrMap = serializeNullInObjOrMap;
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

    public boolean serializeNullInObjOrMap() {
        return serializeNullInObjOrMap;
    }

    public int maxNestedSize() {
        return maxNestedSize;
    }

    public static class Builder {
        private final List<MarshallTransformerFacade> tfcs = new ArrayList<>();
        private boolean serializeNullInObjOrMap = false;
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
                // builtin mappings always win; a transformer may not override them
                if (CborSerializerContext.builtinSerializeObjFunc(customType) != null
                        || CborSerializerContext.builtinSerializeArrayFunc(customType) != null) {
                    throw new IllegalArgumentException("cannot override builtin type : " + customType.getName());
                }
                // custom types have value semantics, which is feasible for enums,
                // but not for marshallable beans
                if (Marshalls.beanMarshallFacade(customType) != null) {
                    throw new IllegalArgumentException("custom type can not be marshallable : " + customType.getName());
                }
                // a transformer must map to the single tagged-type builtin
                if (tfc.builtinType() != CborTaggedType.class) {
                    throw new IllegalArgumentException(
                            "builtinType must be CborTaggedType : " + tfc.builtinType().getName());
                }
                tfcs.add(tfc);
            }
            return this;
        }

        public Builder setSerializeNullInObjOrMap(boolean serializeNullInObjOrMap) {
            this.serializeNullInObjOrMap = serializeNullInObjOrMap;
            return this;
        }

        public Builder setMaxNestedSize(int maxNestedSize) {
            if (maxNestedSize < CborSerializer.INITIAL_SIZE || maxNestedSize > CborSerializer.MAX_SIZE) {
                throw new IllegalArgumentException("maxNestedSize out of range : " + maxNestedSize);
            }
            this.maxNestedSize = maxNestedSize;
            return this;
        }

        public CborSerializerOption build() {
            Map<Class<?>, CborSerializeFunc> customFuncMap = new HashMap<>();
            for (MarshallTransformerFacade tfc : tfcs) {
                customFuncMap.put(tfc.customType(), (o, c) -> {
                    c.serializeCborTaggedType((CborTaggedType) tfc.toBuiltin(o));
                    return CborSerializeResult.Continue;
                });
            }
            return new CborSerializerOption(Map.copyOf(customFuncMap), serializeNullInObjOrMap, maxNestedSize);
        }
    }
}
