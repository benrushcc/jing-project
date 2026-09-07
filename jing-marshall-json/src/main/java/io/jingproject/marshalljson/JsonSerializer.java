package io.jingproject.marshalljson;

import io.jingproject.common.WriteBuffer;
import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.Marshalls;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class JsonSerializer {
    public static final int INITIAL_SIZE = 4;
    public static final int MAX_SIZE     = 4096;
    private final JsonSerializerOption option;

    public JsonSerializer(JsonSerializerOption option) {
        this.option = Objects.requireNonNull(option, "option must not be null");
    }

    public void serializeByteArray(byte[] arr, WriteBuffer writeBuffer) {
        Objects.requireNonNull(arr, "arr must not be null");
        Objects.requireNonNull(writeBuffer, "writeBuffer must not be null");
        JsonSerializerContext context = JsonSerializerContext.newCtx(option, writeBuffer);
        context.serializeByteArray(arr, 1);
        context.commit();
    }

    public void serializeBooleanArray(boolean[] arr, WriteBuffer writeBuffer) {
        Objects.requireNonNull(arr, "arr must not be null");
        Objects.requireNonNull(writeBuffer, "writeBuffer must not be null");
        JsonSerializerContext context = JsonSerializerContext.newCtx(option, writeBuffer);
        context.serializeBooleanArray(arr, 1);
        context.commit();
    }

    public void serializeShortArray(short[] arr, WriteBuffer writeBuffer) {
        Objects.requireNonNull(arr, "arr must not be null");
        Objects.requireNonNull(writeBuffer, "writeBuffer must not be null");
        JsonSerializerContext context = JsonSerializerContext.newCtx(option, writeBuffer);
        context.serializeShortArray(arr, 1);
        context.commit();
    }

    public void serializeCharArray(char[] arr, WriteBuffer writeBuffer) {
        Objects.requireNonNull(arr, "arr must not be null");
        Objects.requireNonNull(writeBuffer, "writeBuffer must not be null");
        JsonSerializerContext context = JsonSerializerContext.newCtx(option, writeBuffer);
        context.serializeCharArray(arr, 1);
        context.commit();
    }

    public void serializeIntArray(int[] arr, WriteBuffer writeBuffer) {
        Objects.requireNonNull(arr, "arr must not be null");
        Objects.requireNonNull(writeBuffer, "writeBuffer must not be null");
        JsonSerializerContext context = JsonSerializerContext.newCtx(option, writeBuffer);
        context.serializeIntArray(arr, 1);
        context.commit();
    }

    public void serializeLongArray(long[] arr, WriteBuffer writeBuffer) {
        Objects.requireNonNull(arr, "arr must not be null");
        Objects.requireNonNull(writeBuffer, "writeBuffer must not be null");
        JsonSerializerContext context = JsonSerializerContext.newCtx(option, writeBuffer);
        context.serializeLongArray(arr, 1);
        context.commit();
    }

    public void serializeFloatArray(float[] arr, WriteBuffer writeBuffer) {
        Objects.requireNonNull(arr, "arr must not be null");
        Objects.requireNonNull(writeBuffer, "writeBuffer must not be null");
        JsonSerializerContext context = JsonSerializerContext.newCtx(option, writeBuffer);
        context.serializeFloatArray(arr, 1);
        context.commit();
    }

    public void serializeDoubleArray(double[] arr, WriteBuffer writeBuffer) {
        Objects.requireNonNull(arr, "arr must not be null");
        Objects.requireNonNull(writeBuffer, "writeBuffer must not be null");
        JsonSerializerContext context = JsonSerializerContext.newCtx(option, writeBuffer);
        context.serializeDoubleArray(arr, 1);
        context.commit();
    }

    public void serializeMarshallableObject(Object instance, WriteBuffer writeBuffer) {
        Objects.requireNonNull(instance, "instance must not be null");
        Objects.requireNonNull(writeBuffer, "writeBuffer must not be null");
        Class<?> marshallableType = instance.getClass();
        if (marshallableType.isEnum()) {
            throw new JsonSerializerException("enum cannot be directly serialized");
        }
        MarshallFacade fc = Marshalls.beanMarshallFacade(marshallableType);
        if (fc == null) {
            throw new JsonSerializerException("type not marshallable : " + marshallableType.getName());
        }
        JsonSerializerContext context = JsonSerializerContext.newCtx(option, writeBuffer);
        JsonSerializerNode root = new JsonSerializerNode();
        root.initObj(fc, instance, 1);
        process(root, context);
    }

    public void serializeArray(Object[] arr, WriteBuffer writeBuffer) {
        Objects.requireNonNull(arr, "arr must not be null");
        Objects.requireNonNull(writeBuffer, "writeBuffer must not be null");
        Class<? extends Object[]> arrType = arr.getClass();
        Class<?> componentType = arrType.getComponentType();
        if(componentType.isPrimitive()) {
            throw new JsonSerializerException("primitive type shouldn't be used as parameters : " + componentType.getName());
        }
        if (componentType.isArray()) {
            throw new JsonSerializerException("multi dimensional array not supported : " + componentType.getName());
        }
        if (componentType.getTypeParameters().length > 0) {
            throw new JsonSerializerException("generic component type not supported : " + componentType.getName());
        }
        JsonSerializerContext context = JsonSerializerContext.newCtx(option, writeBuffer);
        JsonSerializeFunc builtinSerializeArrFunc = JsonSerializerContext.builtinSerializeArrayFunc(arrType);
        if(builtinSerializeArrFunc != null) {
            builtinSerializeArrFunc.serialize(arr, 1, context);
            context.commit();
            return ;
        }
        JsonSerializerNode root = new JsonSerializerNode();
        JsonSerializeFunc fn = context.valueSerializeFunc(componentType);
        root.initArr(arr, 1, fn);
        process(root, context);
    }

    public <T> void serializeCollection(Collection<T> collection, Class<T> elementType, WriteBuffer writeBuffer) {
        Objects.requireNonNull(collection, "collection must not be null");
        Objects.requireNonNull(elementType, "elementType must not be null");
        Objects.requireNonNull(writeBuffer, "writeBuffer must not be null");
        if(elementType.isPrimitive()) {
            throw new JsonSerializerException("primitive type shouldn't be used as parameters : " + elementType.getName());
        }
        if(elementType.isArray() && elementType.getComponentType().isArray()) {
            throw new JsonSerializerException("multi dimensional array not supported : " + elementType.getName());
        }
        if (elementType.getTypeParameters().length > 0) {
            throw new JsonSerializerException("generic element type not supported : " + elementType.getName());
        }
        JsonSerializerContext context = JsonSerializerContext.newCtx(option, writeBuffer);
        JsonSerializerNode root = new JsonSerializerNode();
        JsonSerializeFunc fn = context.valueSerializeFunc(elementType);
        if (collection instanceof List<T> list) {
            root.initList(list, 1, fn);
        } else {
            root.initCol(collection.iterator(), 1, fn);
        }
        process(root, context);
    }

    public <K, V> void serializeMap(Map<K, V> map, Class<K> keyType, Class<V> valueType, WriteBuffer writeBuffer) {
        Objects.requireNonNull(map, "map must not be null");
        Objects.requireNonNull(keyType, "keyType must not be null");
        Objects.requireNonNull(valueType, "valueType must not be null");
        Objects.requireNonNull(writeBuffer, "writeBuffer must not be null");
        if (keyType != CharSequence.class && keyType != String.class) {
            throw new JsonSerializerException("key type not supported : " + keyType.getName());
        }
        if(valueType.isPrimitive()) {
            throw new JsonSerializerException("primitive type shouldn't be used as parameters : " + valueType.getName());
        }
        if(valueType.isArray() && valueType.getComponentType().isArray()) {
            throw new JsonSerializerException("multi dimensional array not supported : " + valueType.getName());
        }
        if (valueType.getTypeParameters().length > 0) {
            throw new JsonSerializerException("generic value type not supported : " + valueType.getName());
        }
        JsonSerializerContext context = JsonSerializerContext.newCtx(option, writeBuffer);
        JsonSerializerNode root = new JsonSerializerNode();
        JsonSerializeFunc fn = context.valueSerializeFunc(valueType);
        root.initMap(map.entrySet().iterator(), 1, fn);
        process(root, context);
    }

    private void process(JsonSerializerNode root, JsonSerializerContext context) {
        JsonSerializerNode probed = nextNode(root, null, context);
        if(probed == null) {
            context.commit();
            return ;
        }
        final int maxNestedSize = option.maxNestedSize();
        JsonSerializerNode[] nodes = new JsonSerializerNode[INITIAL_SIZE];
        nodes[0] = root;
        nodes[1] = probed;
        for(int p = 1; ; ) {
            if(p + 1 == nodes.length) {
                JsonSerializerNode[] newNodes = new JsonSerializerNode[Math.multiplyExact(nodes.length, 2)];
                System.arraycopy(nodes, 0, newNodes, 0, nodes.length);
                nodes = newNodes;
            }
            JsonSerializerNode next = nextNode(nodes[p], nodes[p + 1], context);
            if(next == null) {
                if(p-- == 0) {
                    context.commit();
                    return ;
                }
            } else {
                if(++p == maxNestedSize) {
                    throw new JsonSerializerException("exceeded maximum nested size : " + maxNestedSize);
                }
                nodes[p] = next;
            }
        }
    }

    private static JsonSerializerNode nextNode(JsonSerializerNode current, JsonSerializerNode given, JsonSerializerContext context) {
        final int nextIndent = current.indent() + 1; // no overflow
        return switch (current.process(context)) {
            case Finished -> null;
            case NewMarshallable -> {
                Object marshallable = context.obj();
                Class<?> marshallableType = marshallable.getClass();
                MarshallFacade fc = Marshalls.beanMarshallFacade(marshallableType);
                if (fc == null) {
                    throw new JsonSerializerException("type not marshallable : " + marshallableType.getName());
                }
                JsonSerializerNode r = given == null ? new JsonSerializerNode() : given;
                r.initObj(fc, marshallable, nextIndent);
                yield r;
            }
            case NewArray -> {
                Object[] arr = (Object[]) context.obj();
                JsonSerializeFunc fn = context.valueSerializeFunc(arr.getClass().getComponentType());
                JsonSerializerNode r = given == null ? new JsonSerializerNode() : given;
                r.initArr(arr, nextIndent, fn);
                yield r;
            }
            case NewCollection -> {
                Collection<?> col = (Collection<?>) context.obj();
                Class<?> elementType = context.type();
                JsonSerializeFunc fn = context.valueSerializeFunc(elementType);
                JsonSerializerNode r = given == null ? new JsonSerializerNode() : given;
                if (col instanceof List<?> list) {
                    r.initList(list, nextIndent, fn);
                } else {
                    r.initCol(col.iterator(), nextIndent, fn);
                }
                yield r;
            }
            case NewMap -> {
                Map<?, ?> map = (Map<?, ?>) context.obj();
                Class<?> valueType = context.type();
                JsonSerializeFunc fn = context.valueSerializeFunc(valueType);
                JsonSerializerNode r = given == null ? new JsonSerializerNode() : given;
                r.initMap(map.entrySet().iterator(), nextIndent, fn);
                yield r;
            }
            case null, default -> throw new AssertionError();
        };
    }
}
