package io.jingproject.marshallcbor;

import io.jingproject.common.WriteBuffer;
import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.Marshalls;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class CborSerializer {
    public static final int INITIAL_SIZE = 4;
    public static final int MAX_SIZE     = 4096;
    private final CborSerializerOption option;

    public CborSerializer(CborSerializerOption option) {
        if (option == null) {
            throw new CborSerializerException("option must not be null");
        }
        this.option = option;
    }

    // byte[] is a CBOR byte string (major 2), not an expanded numeric array
    public void serializeByteArray(byte[] arr, WriteBuffer writeBuffer) {
        if (arr == null) {
            throw new CborSerializerException("arr must not be null");
        }
        if (writeBuffer == null) {
            throw new CborSerializerException("writeBuffer must not be null");
        }
        CborSerializerContext context = CborSerializerContext.newCtx(option, writeBuffer);
        context.serializeBytes(arr);
        context.commit();
    }

    public void serializeBooleanArray(boolean[] arr, WriteBuffer writeBuffer) {
        if (arr == null) {
            throw new CborSerializerException("arr must not be null");
        }
        if (writeBuffer == null) {
            throw new CborSerializerException("writeBuffer must not be null");
        }
        CborSerializerContext context = CborSerializerContext.newCtx(option, writeBuffer);
        context.serializeBooleanArray(arr);
        context.commit();
    }

    public void serializeShortArray(short[] arr, WriteBuffer writeBuffer) {
        if (arr == null) {
            throw new CborSerializerException("arr must not be null");
        }
        if (writeBuffer == null) {
            throw new CborSerializerException("writeBuffer must not be null");
        }
        CborSerializerContext context = CborSerializerContext.newCtx(option, writeBuffer);
        context.serializeShortArray(arr);
        context.commit();
    }

    public void serializeCharArray(char[] arr, WriteBuffer writeBuffer) {
        if (arr == null) {
            throw new CborSerializerException("arr must not be null");
        }
        if (writeBuffer == null) {
            throw new CborSerializerException("writeBuffer must not be null");
        }
        CborSerializerContext context = CborSerializerContext.newCtx(option, writeBuffer);
        context.serializeCharArray(arr);
        context.commit();
    }

    public void serializeIntArray(int[] arr, WriteBuffer writeBuffer) {
        if (arr == null) {
            throw new CborSerializerException("arr must not be null");
        }
        if (writeBuffer == null) {
            throw new CborSerializerException("writeBuffer must not be null");
        }
        CborSerializerContext context = CborSerializerContext.newCtx(option, writeBuffer);
        context.serializeIntArray(arr);
        context.commit();
    }

    public void serializeLongArray(long[] arr, WriteBuffer writeBuffer) {
        if (arr == null) {
            throw new CborSerializerException("arr must not be null");
        }
        if (writeBuffer == null) {
            throw new CborSerializerException("writeBuffer must not be null");
        }
        CborSerializerContext context = CborSerializerContext.newCtx(option, writeBuffer);
        context.serializeLongArray(arr);
        context.commit();
    }

    public void serializeFloatArray(float[] arr, WriteBuffer writeBuffer) {
        if (arr == null) {
            throw new CborSerializerException("arr must not be null");
        }
        if (writeBuffer == null) {
            throw new CborSerializerException("writeBuffer must not be null");
        }
        CborSerializerContext context = CborSerializerContext.newCtx(option, writeBuffer);
        context.serializeFloatArray(arr);
        context.commit();
    }

    public void serializeDoubleArray(double[] arr, WriteBuffer writeBuffer) {
        if (arr == null) {
            throw new CborSerializerException("arr must not be null");
        }
        if (writeBuffer == null) {
            throw new CborSerializerException("writeBuffer must not be null");
        }
        CborSerializerContext context = CborSerializerContext.newCtx(option, writeBuffer);
        context.serializeDoubleArray(arr);
        context.commit();
    }

    public void serializeMarshallableObject(Object marshallable, WriteBuffer writeBuffer) {
        if (marshallable == null) {
            throw new CborSerializerException("marshallable object must not be null");
        }
        if (writeBuffer == null) {
            throw new CborSerializerException("writeBuffer must not be null");
        }
        Class<?> marshallableType = marshallable.getClass();
        if (marshallableType.isEnum()) {
            throw new CborSerializerException("enum cannot be directly serialized");
        }
        MarshallFacade fc = Marshalls.beanMarshallFacade(marshallableType);
        if (fc == null) {
            throw new CborSerializerException("type not marshallable : " + marshallableType.getName());
        }
        CborSerializerContext context = CborSerializerContext.newCtx(option, writeBuffer);
        CborSerializerNode root = new CborSerializerNode();
        root.initObj(fc, marshallable);
        process(root, context);
    }

    public void serializeArray(Object[] arr, WriteBuffer writeBuffer) {
        if (arr == null) {
            throw new CborSerializerException("arr must not be null");
        }
        if (writeBuffer == null) {
            throw new CborSerializerException("writeBuffer must not be null");
        }
        Class<? extends Object[]> arrType = arr.getClass();
        Class<?> componentType = arrType.getComponentType();
        if (componentType.isPrimitive()) {
            throw new CborSerializerException("primitive type shouldn't be used as parameters : " + componentType.getName());
        }
        if (componentType.isArray()) {
            throw new CborSerializerException("multi dimensional array not supported : " + componentType.getName());
        }
        if (componentType.getTypeParameters().length > 0) {
            throw new CborSerializerException("generic component type not supported : " + componentType.getName());
        }
        CborSerializerContext context = CborSerializerContext.newCtx(option, writeBuffer);
        CborSerializeFunc builtinSerializeArrFunc = CborSerializerContext.builtinSerializeArrayFunc(arrType);
        if (builtinSerializeArrFunc != null) {
            builtinSerializeArrFunc.serialize(arr, context);
            context.commit();
            return;
        }
        CborSerializerNode root = new CborSerializerNode();
        CborSerializeFunc fn = context.valueSerializeFunc(componentType);
        root.initArr(arr, fn);
        process(root, context);
    }

    public <T> void serializeCollection(Collection<T> collection, Class<T> elementType, WriteBuffer writeBuffer) {
        if (collection == null) {
            throw new CborSerializerException("collection must not be null");
        }
        if (elementType == null) {
            throw new CborSerializerException("elementType must not be null");
        }
        if (writeBuffer == null) {
            throw new CborSerializerException("writeBuffer must not be null");
        }
        if (elementType.isPrimitive()) {
            throw new CborSerializerException("primitive type shouldn't be used as parameters : " + elementType.getName());
        }
        if (elementType.isArray() && elementType.getComponentType().isArray()) {
            throw new CborSerializerException("multi dimensional array not supported : " + elementType.getName());
        }
        if (elementType.getTypeParameters().length > 0) {
            throw new CborSerializerException("generic element type not supported : " + elementType.getName());
        }
        CborSerializerContext context = CborSerializerContext.newCtx(option, writeBuffer);
        CborSerializerNode root = new CborSerializerNode();
        CborSerializeFunc fn = context.valueSerializeFunc(elementType);
        if (collection instanceof List<T> list) {
            root.initList(list, fn);
        } else {
            root.initCol(collection.iterator(), fn);
        }
        process(root, context);
    }

    public <K, V> void serializeMap(Map<K, V> map, Class<K> keyType, Class<V> valueType, WriteBuffer writeBuffer) {
        if (map == null) {
            throw new CborSerializerException("map must not be null");
        }
        if (keyType == null) {
            throw new CborSerializerException("keyType must not be null");
        }
        if (valueType == null) {
            throw new CborSerializerException("valueType must not be null");
        }
        if (writeBuffer == null) {
            throw new CborSerializerException("writeBuffer must not be null");
        }
        if (keyType != CharSequence.class && keyType != String.class) {
            throw new CborSerializerException("key type not supported : " + keyType.getName());
        }
        if (valueType.isPrimitive()) {
            throw new CborSerializerException("primitive type shouldn't be used as parameters : " + valueType.getName());
        }
        if (valueType.isArray() && valueType.getComponentType().isArray()) {
            throw new CborSerializerException("multi dimensional array not supported : " + valueType.getName());
        }
        if (valueType.getTypeParameters().length > 0) {
            throw new CborSerializerException("generic value type not supported : " + valueType.getName());
        }
        CborSerializerContext context = CborSerializerContext.newCtx(option, writeBuffer);
        CborSerializerNode root = new CborSerializerNode();
        CborSerializeFunc fn = context.valueSerializeFunc(valueType);
        root.initMap(map.entrySet().iterator(), fn);
        process(root, context);
    }

    private void process(CborSerializerNode root, CborSerializerContext context) {
        CborSerializerNode probed = nextNode(root, null, context);
        if (probed == null) {
            context.commit();
            return;
        }
        final int maxNestedSize = option.maxNestedSize();
        CborSerializerNode[] nodes = new CborSerializerNode[INITIAL_SIZE];
        nodes[0] = root;
        nodes[1] = probed;
        for (int p = 1; ; ) {
            if (p + 1 == nodes.length) {
                CborSerializerNode[] newNodes = new CborSerializerNode[Math.multiplyExact(nodes.length, 2)];
                System.arraycopy(nodes, 0, newNodes, 0, nodes.length);
                nodes = newNodes;
            }
            CborSerializerNode next = nextNode(nodes[p], nodes[p + 1], context);
            if (next == null) {
                if (p-- == 0) {
                    context.commit();
                    return;
                }
            } else {
                if (++p == maxNestedSize) {
                    throw new CborSerializerException("exceeded maximum nested size : " + maxNestedSize);
                }
                nodes[p] = next;
            }
        }
    }

    private static CborSerializerNode nextNode(CborSerializerNode current, CborSerializerNode given, CborSerializerContext context) {
        return switch (current.process(context)) {
            case Finished -> null;
            case NewMarshallable -> {
                Object marshallable = context.obj();
                Class<?> marshallableType = marshallable.getClass();
                MarshallFacade fc = Marshalls.beanMarshallFacade(marshallableType);
                if (fc == null) {
                    throw new CborSerializerException("type not marshallable : " + marshallableType.getName());
                }
                CborSerializerNode r = given == null ? new CborSerializerNode() : given;
                r.initObj(fc, marshallable);
                yield r;
            }
            case NewArray -> {
                Object[] arr = (Object[]) context.obj();
                CborSerializeFunc fn = context.valueSerializeFunc(arr.getClass().getComponentType());
                CborSerializerNode r = given == null ? new CborSerializerNode() : given;
                r.initArr(arr, fn);
                yield r;
            }
            case NewCollection -> {
                Collection<?> col = (Collection<?>) context.obj();
                Class<?> elementType = context.type();
                CborSerializeFunc fn = context.valueSerializeFunc(elementType);
                CborSerializerNode r = given == null ? new CborSerializerNode() : given;
                if (col instanceof List<?> list) {
                    r.initList(list, fn);
                } else {
                    r.initCol(col.iterator(), fn);
                }
                yield r;
            }
            case NewMap -> {
                Map<?, ?> map = (Map<?, ?>) context.obj();
                Class<?> valueType = context.type();
                CborSerializeFunc fn = context.valueSerializeFunc(valueType);
                CborSerializerNode r = given == null ? new CborSerializerNode() : given;
                r.initMap(map.entrySet().iterator(), fn);
                yield r;
            }
            case null, default -> throw new AssertionError();
        };
    }
}