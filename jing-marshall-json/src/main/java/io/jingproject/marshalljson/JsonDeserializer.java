package io.jingproject.marshalljson;

import io.jingproject.common.ReadBuffer;
import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.Marshalls;

import java.util.*;
import java.util.function.Supplier;

public final class JsonDeserializer {
    public static final int INITIAL_SIZE = 4;
    public static final int MAX_SIZE = 4096;
    private final JsonDeserializerOption option;

    public JsonDeserializer(JsonDeserializerOption option) {
        this.option = Objects.requireNonNull(option, "option must not be null");
    }

    public byte[] deserializeByteArray(ReadBuffer readBuffer) {
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        Utf8Validator.validate(readBuffer);
        JsonDeserializerContext context = new JsonDeserializerContext(option, readBuffer);
        return context.deserializeByteArray(context.nextValuableByte(true));
    }

    public boolean[] deserializeBooleanArray(ReadBuffer readBuffer) {
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        Utf8Validator.validate(readBuffer);
        JsonDeserializerContext context = new JsonDeserializerContext(option, readBuffer);
        return context.deserializeBooleanArray(context.nextValuableByte(true));
    }

    public short[] deserializeShortArray(ReadBuffer readBuffer) {
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        Utf8Validator.validate(readBuffer);
        JsonDeserializerContext context = new JsonDeserializerContext(option, readBuffer);
        return context.deserializeShortArray(context.nextValuableByte(true));
    }

    public char[] deserializeCharArray(ReadBuffer readBuffer) {
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        Utf8Validator.validate(readBuffer);
        JsonDeserializerContext context = new JsonDeserializerContext(option, readBuffer);
        return context.deserializeCharArray(context.nextValuableByte(true));
    }

    public int[] deserializeIntArray(ReadBuffer readBuffer) {
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        Utf8Validator.validate(readBuffer);
        JsonDeserializerContext context = new JsonDeserializerContext(option, readBuffer);
        return context.deserializeIntArray(context.nextValuableByte(true));
    }

    public long[] deserializeLongArray(ReadBuffer readBuffer) {
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        Utf8Validator.validate(readBuffer);
        JsonDeserializerContext context = new JsonDeserializerContext(option, readBuffer);
        return context.deserializeLongArray(context.nextValuableByte(true));
    }

    public float[] deserializeFloatArray(ReadBuffer readBuffer) {
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        Utf8Validator.validate(readBuffer);
        JsonDeserializerContext context = new JsonDeserializerContext(option, readBuffer);
        return context.deserializeFloatArray(context.nextValuableByte(true));
    }

    public double[] deserializeDoubleArray(ReadBuffer readBuffer) {
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        Utf8Validator.validate(readBuffer);
        JsonDeserializerContext context = new JsonDeserializerContext(option, readBuffer);
        return context.deserializeDoubleArray(context.nextValuableByte(true));
    }

    @SuppressWarnings("unchecked")
    public <T> T deserializeMarshallableObject(Class<T> marshallableType, ReadBuffer readBuffer) {
        Objects.requireNonNull(marshallableType, "marshallable type must not be null");
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        Utf8Validator.validate(readBuffer);
        if(marshallableType.isEnum()) {
            throw new JsonDeserializerException("enum cannot be directly deserialized");
        }
        MarshallFacade fc = Marshalls.beanMarshallFacade(marshallableType);
        if (fc == null) {
            throw new JsonDeserializerException("type not marshallable : " + marshallableType.getName());
        }
        JsonDeserializerContext context = new JsonDeserializerContext(option, readBuffer);
        JsonDeserializerContext.checkObjStart(context.nextValuableByte(true));
        JsonDeserializerNode root = new JsonDeserializerNode();
        root.initObj(fc, context);
        return (T) process(root, context);
    }

    @SuppressWarnings("unchecked")
    public <T> T[] deserializeArray(Class<T> componentType, ReadBuffer readBuffer) {
        Objects.requireNonNull(componentType, "component type must not be null");
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        Utf8Validator.validate(readBuffer);
        if(componentType.isPrimitive()) {
            throw new JsonDeserializerException("primitive array shouldn't be used as parameters : " + componentType.getName());
        }
        if(componentType.isArray()) {
            throw new JsonDeserializerException("multi dimensional array not supported : " + componentType.getName());
        }
        if(componentType.getTypeParameters().length > 0) {
            throw new JsonDeserializerException("generic component type not supported : " + componentType.getName());
        }
        JsonDeserializerContext context = new JsonDeserializerContext(option, readBuffer);
        byte firstByte = context.nextValuableByte(true);
        JsonDeserializeFunc builtinDeserializeArrayFunc = JsonDeserializerContext.builtinDeserializeArrayFunc(componentType.arrayType());
        if(builtinDeserializeArrayFunc != null) {
            JsonDeserializeResult _ = builtinDeserializeArrayFunc.deserialize(firstByte, context);
            return (T[]) context.obj();
        }
        JsonDeserializerContext.checkArrayStart(firstByte);
        JsonDeserializerNode root = new JsonDeserializerNode();
        JsonDeserializeFunc func = context.valueDeserializeFunc(componentType);
        root.initArr(componentType, func);
        return (T[]) process(root, context);
    }

    @SuppressWarnings("unchecked")
    public <T> Collection<T> deserializeCol(Class<T> elementType, ReadBuffer readBuffer, Supplier<Collection<T>> supplier) {
        Objects.requireNonNull(elementType, "element type must not be null");
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        Objects.requireNonNull(supplier, "supplier must not be null");
        Utf8Validator.validate(readBuffer);
        if(elementType.isPrimitive()) {
            throw new JsonDeserializerException("primitive type shouldn't be used as parameters : " + elementType.getName());
        }
        if(elementType.isArray() && elementType.getComponentType().isArray()) {
            throw new JsonDeserializerException("multi dimensional array not supported : " + elementType.getName());
        }
        if(elementType.getTypeParameters().length > 0) {
            throw new JsonDeserializerException("generic element type not supported : " + elementType.getName());
        }
        Collection<T> col = Objects.requireNonNull(supplier.get(), "supplied collection must not be null");
        JsonDeserializerContext context = new JsonDeserializerContext(option, readBuffer);
        JsonDeserializerContext.checkArrayStart(context.nextValuableByte(true));
        JsonDeserializeFunc func = context.valueDeserializeFunc(elementType);
        JsonDeserializerNode root = new JsonDeserializerNode();
        root.initCol(col, func);
        return (Collection<T>) process(root, context);
    }

    @SuppressWarnings("unchecked")
    public <K, V> Map<K, V> deserializeMap(Class<K> keyType, Class<V> valueType, ReadBuffer readBuffer, Supplier<Map<K, V>> supplier) {
        Objects.requireNonNull(keyType, "key type must not be null");
        Objects.requireNonNull(valueType, "value type must not be null");
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        Utf8Validator.validate(readBuffer);
        if(keyType != CharSequence.class && keyType != String.class) {
            throw new JsonDeserializerException("key type not supported : " + keyType.getName());
        }
        if(valueType.isPrimitive()) {
            throw new JsonDeserializerException("primitive type shouldn't be used as parameters : " + valueType.getName());
        }
        if(valueType.getTypeParameters().length > 0) {
            throw new JsonDeserializerException("generic value type are not supported : " + valueType.getName());
        }
        Map<K, V> map = Objects.requireNonNull(supplier.get(), "supplied map must not be null");
        JsonDeserializerContext context = new JsonDeserializerContext(option, readBuffer);
        JsonDeserializerContext.checkObjStart(context.nextValuableByte(true));
        JsonDeserializeFunc func = context.valueDeserializeFunc(valueType);
        JsonDeserializerNode root = new JsonDeserializerNode();
        root.initMap(map, func);
        return (Map<K, V>) process(root, context);
    }

    private static Object process(JsonDeserializerNode root, JsonDeserializerContext context) {
        JsonDeserializerNode probed = nextNode(root, null, false, context);
        if(probed == null) {
            return context.obj();
        }
        final int maxNestedSize = context.option().maxNestedSize();
        boolean hasValue = false;
        JsonDeserializerNode[] nodes = new JsonDeserializerNode[INITIAL_SIZE];
        nodes[0] = root;
        nodes[1] = probed;
        for(int p = 1; ; ) {
            if(p + 1 == nodes.length) {
                JsonDeserializerNode[] newNodes = new JsonDeserializerNode[Math.multiplyExact(nodes.length, 2)];
                System.arraycopy(nodes, 0, newNodes, 0, nodes.length);
                nodes = newNodes;
            }
            JsonDeserializerNode next = nextNode(nodes[p], nodes[p + 1], hasValue, context);
            hasValue = (next == null);
            if(hasValue) {
                if(p-- == 0) {
                    return context.obj();
                }
            } else {
                if (++p == maxNestedSize) {
                    throw new JsonDeserializerException("exceeded maximum nested size : " + maxNestedSize);
                }
                nodes[p] = next;
            }
        }
    }

    private static JsonDeserializerNode nextNode(JsonDeserializerNode current, JsonDeserializerNode given, boolean hasValue, JsonDeserializerContext context) {
        return switch(current.process(context, hasValue)) {
            case Finish -> null;
            case NewMarshallable -> {
                Class<?> marshallableType = context.type();
                MarshallFacade fc = Marshalls.beanMarshallFacade(marshallableType);
                if(fc == null) {
                    throw new JsonSerializerException("type not marshallable : " + marshallableType.getName());
                }
                JsonDeserializerNode r = given == null ? new JsonDeserializerNode() : given;
                r.initObj(fc, context);
                yield r;
            }
            case NewArr -> {
                Class<?> componentType = context.type();
                JsonDeserializeFunc func = context.valueDeserializeFunc(componentType);
                JsonDeserializerNode r = given == null ? new JsonDeserializerNode() : given;
                r.initArr(componentType, func);
                yield r;
            }
            case NewCol -> {
                Collection<?> col = (Collection<?>) context.obj();
                Class<?> elementType = context.type();
                JsonDeserializeFunc func = context.valueDeserializeFunc(elementType);
                JsonDeserializerNode r = given == null ? new JsonDeserializerNode() : given;
                r.initCol(col, func);
                yield r;
            }
            case NewMap -> {
                Map<?, ?> map = (Map<?, ?>) context.obj();
                Class<?> valueType = context.type();
                JsonDeserializeFunc func = context.valueDeserializeFunc(valueType);
                JsonDeserializerNode r = given == null ? new JsonDeserializerNode() : given;
                r.initMap(map, func);
                yield r;
            }
            case NewDummyObj -> {
                JsonDeserializerNode r = given == null ? new JsonDeserializerNode() : given;
                r.initDummyObj();
                yield r;
            }
            case NewDummyCol -> {
                JsonDeserializerNode r = given == null ? new JsonDeserializerNode() : given;
                r.initDummyCol();
                yield r;
            }
            default -> throw new AssertionError();
        };
    }
}
