package io.jingproject.marshalljson;

import io.jingproject.common.ReadBuffer;
import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.Marshalls;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

public final class JsonDeserializer {
    public static final int INITIAL_SIZE = 4;
    public static final int MAX_SIZE = 4096;
    private final JsonDeserializerOption option;

    public JsonDeserializer(JsonDeserializerOption option) {
        if (option == null) {
            throw new JsonDeserializerException("option must not be null");
        }
        this.option = option;
    }

    public byte[] deserializeByteArray(ReadBuffer readBuffer) {
        if (readBuffer == null) {
            throw new JsonDeserializerException("readBuffer must not be null");
        }
        JsonDeserializerContext context = JsonDeserializerContext.newContext(option, readBuffer);
        if (!context.validate()) {
            throw new JsonDeserializerException("not valid utf-8 content");
        }
        return context.deserializeByteArray(context.nextValuableByte());
    }

    public boolean[] deserializeBooleanArray(ReadBuffer readBuffer) {
        if (readBuffer == null) {
            throw new JsonDeserializerException("readBuffer must not be null");
        }
        JsonDeserializerContext context = JsonDeserializerContext.newContext(option, readBuffer);
        if (!context.validate()) {
            throw new JsonDeserializerException("not valid utf-8 content");
        }
        return context.deserializeBooleanArray(context.nextValuableByte());
    }

    public short[] deserializeShortArray(ReadBuffer readBuffer) {
        if (readBuffer == null) {
            throw new JsonDeserializerException("readBuffer must not be null");
        }
        JsonDeserializerContext context = JsonDeserializerContext.newContext(option, readBuffer);
        if (!context.validate()) {
            throw new JsonDeserializerException("not valid utf-8 content");
        }
        return context.deserializeShortArray(context.nextValuableByte());
    }

    public char[] deserializeCharArray(ReadBuffer readBuffer) {
        if (readBuffer == null) {
            throw new JsonDeserializerException("readBuffer must not be null");
        }
        JsonDeserializerContext context = JsonDeserializerContext.newContext(option, readBuffer);
        if (!context.validate()) {
            throw new JsonDeserializerException("not valid utf-8 content");
        }
        return context.deserializeCharArray(context.nextValuableByte());
    }

    public int[] deserializeIntArray(ReadBuffer readBuffer) {
        if (readBuffer == null) {
            throw new JsonDeserializerException("readBuffer must not be null");
        }
        JsonDeserializerContext context = JsonDeserializerContext.newContext(option, readBuffer);
        if (!context.validate()) {
            throw new JsonDeserializerException("not valid utf-8 content");
        }
        return context.deserializeIntArray(context.nextValuableByte());
    }

    public long[] deserializeLongArray(ReadBuffer readBuffer) {
        if (readBuffer == null) {
            throw new JsonDeserializerException("readBuffer must not be null");
        }
        JsonDeserializerContext context = JsonDeserializerContext.newContext(option, readBuffer);
        if (!context.validate()) {
            throw new JsonDeserializerException("not valid utf-8 content");
        }
        return context.deserializeLongArray(context.nextValuableByte());
    }

    public float[] deserializeFloatArray(ReadBuffer readBuffer) {
        if (readBuffer == null) {
            throw new JsonDeserializerException("readBuffer must not be null");
        }
        JsonDeserializerContext context = JsonDeserializerContext.newContext(option, readBuffer);
        if (!context.validate()) {
            throw new JsonDeserializerException("not valid utf-8 content");
        }
        return context.deserializeFloatArray(context.nextValuableByte());
    }

    public double[] deserializeDoubleArray(ReadBuffer readBuffer) {
        if (readBuffer == null) {
            throw new JsonDeserializerException("readBuffer must not be null");
        }
        JsonDeserializerContext context = JsonDeserializerContext.newContext(option, readBuffer);
        if (!context.validate()) {
            throw new JsonDeserializerException("not valid utf-8 content");
        }
        return context.deserializeDoubleArray(context.nextValuableByte());
    }

    @SuppressWarnings("unchecked")
    public <T> T deserializeMarshallableObject(Class<T> marshallableType, ReadBuffer readBuffer) {
        if (marshallableType == null) {
            throw new JsonDeserializerException("marshallable type must not be null");
        }
        if (readBuffer == null) {
            throw new JsonDeserializerException("readBuffer must not be null");
        }
        if(marshallableType.isEnum()) {
            throw new JsonDeserializerException("enum cannot be directly deserialized");
        }
        MarshallFacade fc = Marshalls.beanMarshallFacade(marshallableType);
        if (fc == null) {
            throw new JsonDeserializerException("type not marshallable : " + marshallableType.getName());
        }
        JsonDeserializerContext context = JsonDeserializerContext.newContext(option, readBuffer);
        if (!context.validate()) {
            throw new JsonDeserializerException("not valid utf-8 content");
        }
        JsonDeserializerContext.checkObjStart(context.nextValuableByte());
        JsonDeserializerNode root = new JsonDeserializerNode();
        root.initObj(fc);
        return (T) process(root, context);
    }

    @SuppressWarnings("unchecked")
    public <T> T[] deserializeArray(Class<T> componentType, ReadBuffer readBuffer) {
        if (componentType == null) {
            throw new JsonDeserializerException("component type must not be null");
        }
        if (readBuffer == null) {
            throw new JsonDeserializerException("readBuffer must not be null");
        }
        if(componentType.isPrimitive()) {
            throw new JsonDeserializerException("primitive array shouldn't be used as parameters : " + componentType.getName());
        }
        if(componentType.isArray()) {
            throw new JsonDeserializerException("multi dimensional array not supported : " + componentType.getName());
        }
        if(componentType.getTypeParameters().length > 0) {
            throw new JsonDeserializerException("generic component type not supported : " + componentType.getName());
        }
        JsonDeserializerContext context = JsonDeserializerContext.newContext(option, readBuffer);
        if (!context.validate()) {
            throw new JsonDeserializerException("not valid utf-8 content");
        }
        byte firstByte = context.nextValuableByte();
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
        if (elementType == null) {
            throw new JsonDeserializerException("element type must not be null");
        }
        if (readBuffer == null) {
            throw new JsonDeserializerException("readBuffer must not be null");
        }
        if (supplier == null) {
            throw new JsonDeserializerException("supplier must not be null");
        }
        if(elementType.isPrimitive()) {
            throw new JsonDeserializerException("primitive type shouldn't be used as parameters : " + elementType.getName());
        }
        if(elementType.isArray() && elementType.getComponentType().isArray()) {
            throw new JsonDeserializerException("multi dimensional array not supported : " + elementType.getName());
        }
        if(elementType.getTypeParameters().length > 0) {
            throw new JsonDeserializerException("generic element type not supported : " + elementType.getName());
        }
        Collection<T> col = supplier.get();
        if(col == null) {
            throw new JsonDeserializerException("supplied collection must not be null");
        }
        JsonDeserializerContext context = JsonDeserializerContext.newContext(option, readBuffer);
        if (!context.validate()) {
            throw new JsonDeserializerException("not valid utf-8 content");
        }
        JsonDeserializerContext.checkArrayStart(context.nextValuableByte());
        JsonDeserializeFunc func = context.valueDeserializeFunc(elementType);
        JsonDeserializerNode root = new JsonDeserializerNode();
        root.initCol(col, func);
        return (Collection<T>) process(root, context);
    }

    @SuppressWarnings("unchecked")
    public <K, V> Map<K, V> deserializeMap(Class<K> keyType, Class<V> valueType, ReadBuffer readBuffer, Supplier<Map<K, V>> supplier) {
        if (keyType == null) {
            throw new JsonDeserializerException("key type must not be null");
        }
        if (valueType == null) {
            throw new JsonDeserializerException("value type must not be null");
        }
        if (readBuffer == null) {
            throw new JsonDeserializerException("readBuffer must not be null");
        }
        if (supplier == null) {
            throw new JsonDeserializerException("supplier must not be null");
        }
        if(keyType != CharSequence.class && keyType != String.class) {
            throw new JsonDeserializerException("key type not supported : " + keyType.getName());
        }
        if(valueType.isPrimitive()) {
            throw new JsonDeserializerException("primitive type shouldn't be used as parameters : " + valueType.getName());
        }
        if(valueType.getTypeParameters().length > 0) {
            throw new JsonDeserializerException("generic value type are not supported : " + valueType.getName());
        }
        Map<K, V> map = supplier.get();
        if(map == null) {
            throw new JsonDeserializerException("supplied map must not be null");
        }
        JsonDeserializerContext context = JsonDeserializerContext.newContext(option, readBuffer);
        if (!context.validate()) {
            throw new JsonDeserializerException("not valid utf-8 content");
        }
        JsonDeserializerContext.checkObjStart(context.nextValuableByte());
        JsonDeserializeFunc func = context.valueDeserializeFunc(valueType);
        JsonDeserializerNode root = new JsonDeserializerNode();
        root.initMap(map, func);
        return (Map<K, V>) process(root, context);
    }

    private Object process(JsonDeserializerNode root, JsonDeserializerContext context) {
        JsonDeserializerNode probed = nextNode(root, null, false, context);
        if(probed == null) {
            context.commit();
            return context.obj();
        }
        final int maxNestedSize = option.maxNestedSize();
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
                    context.commit();
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
        return switch(current.process(hasValue, context)) {
            case Finish -> null;
            case NewMarshallable -> {
                Class<?> marshallableType = context.type();
                MarshallFacade fc = Marshalls.beanMarshallFacade(marshallableType);
                if(fc == null) {
                    throw new JsonDeserializerException("type not marshallable : " + marshallableType.getName());
                }
                JsonDeserializerNode r = given == null ? new JsonDeserializerNode() : given;
                r.initObj(fc);
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
