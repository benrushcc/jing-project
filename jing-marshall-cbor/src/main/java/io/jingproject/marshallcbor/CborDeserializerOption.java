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
    private static final int MAX_STRING_SIZE = Math.min(
            Integer.parseInt(System.getProperty("jing.marshallcbor.maxstringsize", "65535")), 1024 * 1024 * 64);
    private static final int MIN_BYTES_SIZE = 64;
    private static final int MAX_BYTES_SIZE = Math.min(
            Integer.parseInt(System.getProperty("jing.marshallcbor.maxbytesbytes", "65535")), 1024 * 1024 * 64);
    private static final int MIN_ARRAY_SIZE = 100;
    private static final int MAX_ARRAY_SIZE = Math.min(
            Integer.parseInt(System.getProperty("jing.marshallcbor.maxarraysize", "1000")), 65535);
    private static final int MIN_MAP_SIZE = 20;
    private static final int MAX_MAP_SIZE = Math.min(
            Integer.parseInt(System.getProperty("jing.marshallcbor.maxmapsize", "200")), 65535);
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
    private final boolean ensureAllFieldsPresent;
    private final int maxStringBytes;
    private final int maxBytesBytes;
    private final int maxArrayElements;
    private final int maxMapElements;
    private final int maxDummyElements;
    private final int maxNestedSize;
    private final int charBufferSize;

    private CborDeserializerOption(Map<Class<?>, CborDeserializeFunc> customFuncMap,
                                   boolean ensureAllFieldsPresent, int maxStringBytes, int maxBytesBytes,
                                   int maxArrayElements, int maxMapElements, int maxDummyElements,
                                   int maxNestedSize, int charBufferSize) {
        this.customFuncMap = customFuncMap;
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
                // builtin mappings always win; a transformer may not override them
                if (CborDeserializerContext.builtinDeserializeObjFunc(customType) != null
                        || CborDeserializerContext.builtinDeserializeArrayFunc(customType) != null) {
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
            if (maxDummyElements < 0) {
                throw new IllegalArgumentException("maxDummyElements out of range : " + maxDummyElements);
            }
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

        public CborDeserializerOption build() {
            Map<Class<?>, CborDeserializeFunc> customFuncMap = new HashMap<>();
            for (MarshallTransformerFacade tfc : tfcs) {
                customFuncMap.put(tfc.customType(), (b, c) -> {
                    c.setObj(tfc.toCustom(c.deserializeCborTaggedType(b)));
                    return CborDeserializeResult.Continue;
                });
            }
            return new CborDeserializerOption(Map.copyOf(customFuncMap), ensureAllFieldsPresent,
                    maxStringBytes, maxBytesBytes, maxArrayElements, maxMapElements,
                    maxDummyElements, maxNestedSize, charBufferSize);
        }
    }
}
