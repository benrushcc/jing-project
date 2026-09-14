package io.jingproject.marshallcbor;

import io.jingproject.common.ReadBuffer;
import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.Marshalls;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

public final class CborDeserializer {
    public static final int INITIAL_SIZE = 4;
    public static final int MAX_SIZE = 4096;
    private final CborDeserializerOption option;

    public CborDeserializer(CborDeserializerOption option) {
        if (option == null) {
            throw new CborDeserializerException("option must not be null");
        }
        this.option = option;
    }

    public byte[] deserializeByteArray(ReadBuffer readBuffer) {
        if (readBuffer == null) {
            throw new CborDeserializerException("readBuffer must not be null");
        }
        CborDeserializerContext context = CborDeserializerContext.newContext(option, readBuffer);
        return context.deserializeByteArray(context.getByte());
    }

    public boolean[] deserializeBooleanArray(ReadBuffer readBuffer) {
        if (readBuffer == null) {
            throw new CborDeserializerException("readBuffer must not be null");
        }
        CborDeserializerContext context = CborDeserializerContext.newContext(option, readBuffer);
        return context.deserializeBooleanArray(context.getByte());
    }

    public short[] deserializeShortArray(ReadBuffer readBuffer) {
        if (readBuffer == null) {
            throw new CborDeserializerException("readBuffer must not be null");
        }
        CborDeserializerContext context = CborDeserializerContext.newContext(option, readBuffer);
        return context.deserializeShortArray(context.getByte());
    }

    public char[] deserializeCharArray(ReadBuffer readBuffer) {
        if (readBuffer == null) {
            throw new CborDeserializerException("readBuffer must not be null");
        }
        CborDeserializerContext context = CborDeserializerContext.newContext(option, readBuffer);
        return context.deserializeCharArray(context.getByte());
    }

    public int[] deserializeIntArray(ReadBuffer readBuffer) {
        if (readBuffer == null) {
            throw new CborDeserializerException("readBuffer must not be null");
        }
        CborDeserializerContext context = CborDeserializerContext.newContext(option, readBuffer);
        return context.deserializeIntArray(context.getByte());
    }

    public long[] deserializeLongArray(ReadBuffer readBuffer) {
        if (readBuffer == null) {
            throw new CborDeserializerException("readBuffer must not be null");
        }
        CborDeserializerContext context = CborDeserializerContext.newContext(option, readBuffer);
        return context.deserializeLongArray(context.getByte());
    }

    public float[] deserializeFloatArray(ReadBuffer readBuffer) {
        if (readBuffer == null) {
            throw new CborDeserializerException("readBuffer must not be null");
        }
        CborDeserializerContext context = CborDeserializerContext.newContext(option, readBuffer);
        return context.deserializeFloatArray(context.getByte());
    }

    public double[] deserializeDoubleArray(ReadBuffer readBuffer) {
        if (readBuffer == null) {
            throw new CborDeserializerException("readBuffer must not be null");
        }
        CborDeserializerContext context = CborDeserializerContext.newContext(option, readBuffer);
        return context.deserializeDoubleArray(context.getByte());
    }

    @SuppressWarnings("unchecked")
    public <T> T deserializeMarshallableObject(Class<T> marshallableType, ReadBuffer readBuffer) {
        if (marshallableType == null) {
            throw new CborDeserializerException("marshallable type must not be null");
        }
        if (readBuffer == null) {
            throw new CborDeserializerException("readBuffer must not be null");
        }
        if (marshallableType.isEnum()) {
            throw new CborDeserializerException("enum cannot be directly deserialized");
        }
        MarshallFacade fc = Marshalls.beanMarshallFacade(marshallableType);
        if (fc == null) {
            throw new CborDeserializerException("type not marshallable : " + marshallableType.getName());
        }
        CborDeserializerContext context = CborDeserializerContext.newContext(option, readBuffer);
        context.checkObjStart(context.getByte());
        CborDeserializerNode root = new CborDeserializerNode();
        root.initObj(fc, context.declaredCount());
        return (T) process(root, context);
    }

    @SuppressWarnings("unchecked")
    public <T> T[] deserializeArray(Class<T> componentType, ReadBuffer readBuffer) {
        if (componentType == null) {
            throw new CborDeserializerException("component type must not be null");
        }
        if (readBuffer == null) {
            throw new CborDeserializerException("readBuffer must not be null");
        }
        if (componentType.isPrimitive()) {
            throw new CborDeserializerException("primitive array shouldn't be used as parameters : " + componentType.getName());
        }
        if (componentType.isArray()) {
            throw new CborDeserializerException("multi dimensional array not supported : " + componentType.getName());
        }
        if (componentType.getTypeParameters().length > 0) {
            throw new CborDeserializerException("generic component type not supported : " + componentType.getName());
        }
        CborDeserializerContext context = CborDeserializerContext.newContext(option, readBuffer);
        byte firstByte = context.getByte();
        CborDeserializeFunc builtinDeserializeArrayFunc = CborDeserializerContext.builtinDeserializeArrayFunc(componentType.arrayType());
        if (builtinDeserializeArrayFunc != null) {
            CborDeserializeResult _ = builtinDeserializeArrayFunc.deserialize(firstByte, context);
            return (T[]) context.obj();
        }
        context.checkArrayStart(firstByte);
        CborDeserializerNode root = new CborDeserializerNode();
        CborDeserializeFunc func = context.valueDeserializeFunc(componentType);
        root.initArr(componentType, func, context.declaredCount());
        return (T[]) process(root, context);
    }

    @SuppressWarnings("unchecked")
    public <T> Collection<T> deserializeCol(Class<T> elementType, ReadBuffer readBuffer, Supplier<Collection<T>> supplier) {
        if (elementType == null) {
            throw new CborDeserializerException("element type must not be null");
        }
        if (readBuffer == null) {
            throw new CborDeserializerException("readBuffer must not be null");
        }
        if (supplier == null) {
            throw new CborDeserializerException("supplier must not be null");
        }
        if (elementType.isPrimitive()) {
            throw new CborDeserializerException("primitive type shouldn't be used as parameters : " + elementType.getName());
        }
        if (elementType.isArray() && elementType.getComponentType().isArray()) {
            throw new CborDeserializerException("multi dimensional array not supported : " + elementType.getName());
        }
        if (elementType.getTypeParameters().length > 0) {
            throw new CborDeserializerException("generic element type not supported : " + elementType.getName());
        }
        Collection<T> col = supplier.get();
        if (col == null) {
            throw new CborDeserializerException("supplied collection must not be null");
        }
        CborDeserializerContext context = CborDeserializerContext.newContext(option, readBuffer);
        context.checkArrayStart(context.getByte());
        CborDeserializeFunc func = context.valueDeserializeFunc(elementType);
        CborDeserializerNode root = new CborDeserializerNode();
        root.initCol(col, func, context.declaredCount());
        return (Collection<T>) process(root, context);
    }

    @SuppressWarnings("unchecked")
    public <K, V> Map<K, V> deserializeMap(Class<K> keyType, Class<V> valueType, ReadBuffer readBuffer, Supplier<Map<K, V>> supplier) {
        if (keyType == null) {
            throw new CborDeserializerException("key type must not be null");
        }
        if (valueType == null) {
            throw new CborDeserializerException("value type must not be null");
        }
        if (readBuffer == null) {
            throw new CborDeserializerException("readBuffer must not be null");
        }
        if (supplier == null) {
            throw new CborDeserializerException("supplier must not be null");
        }
        if (keyType != CharSequence.class && keyType != String.class) {
            throw new CborDeserializerException("key type not supported : " + keyType.getName());
        }
        if (valueType.isPrimitive()) {
            throw new CborDeserializerException("primitive type shouldn't be used as parameters : " + valueType.getName());
        }
        if (valueType.getTypeParameters().length > 0) {
            throw new CborDeserializerException("generic value type are not supported : " + valueType.getName());
        }
        Map<K, V> map = supplier.get();
        if (map == null) {
            throw new CborDeserializerException("supplied map must not be null");
        }
        CborDeserializerContext context = CborDeserializerContext.newContext(option, readBuffer);
        context.checkObjStart(context.getByte());
        CborDeserializeFunc func = context.valueDeserializeFunc(valueType);
        CborDeserializerNode root = new CborDeserializerNode();
        root.initMap(map, func, context.declaredCount());
        return (Map<K, V>) process(root, context);
    }

    private Object process(CborDeserializerNode root, CborDeserializerContext context) {
        CborDeserializerNode probed = nextNode(root, null, false, context);
        if (probed == null) {
            context.commit();
            return context.obj();
        }
        final int maxNestedSize = option.maxNestedSize();
        boolean hasValue = false;
        CborDeserializerNode[] nodes = new CborDeserializerNode[INITIAL_SIZE];
        nodes[0] = root;
        nodes[1] = probed;
        for(int p = 1; ; ) {
            if(p + 1 == nodes.length) {
                CborDeserializerNode[] newNodes = new CborDeserializerNode[Math.multiplyExact(nodes.length, 2)];
                System.arraycopy(nodes, 0, newNodes, 0, nodes.length);
                nodes = newNodes;
            }
            CborDeserializerNode next = nextNode(nodes[p], nodes[p + 1], hasValue, context);
            hasValue = (next == null);
            if(hasValue) {
                if(p-- == 0) {
                    context.commit();
                    return context.obj();
                }
            } else {
                if (++p == maxNestedSize) {
                    throw new CborDeserializerException("exceeded maximum nested size : " + maxNestedSize);
                }
                nodes[p] = next;
            }
        }
    }

    private static CborDeserializerNode nextNode(CborDeserializerNode current, CborDeserializerNode given, boolean hasValue, CborDeserializerContext context) {
        return switch(current.process(hasValue, context)) {
            case Finish -> null;
            case NewMarshallable -> {
                Class<?> marshallableType = context.type();
                MarshallFacade fc = Marshalls.beanMarshallFacade(marshallableType);
                if(fc == null) {
                    throw new CborDeserializerException("type not marshallable : " + marshallableType.getName());
                }
                CborDeserializerNode r = given == null ? new CborDeserializerNode() : given;
                r.initObj(fc, context.declaredCount());
                yield r;
            }
            case NewArr -> {
                Class<?> componentType = context.type();
                CborDeserializeFunc func = context.valueDeserializeFunc(componentType);
                CborDeserializerNode r = given == null ? new CborDeserializerNode() : given;
                r.initArr(componentType, func, context.declaredCount());
                yield r;
            }
            case NewCol -> {
                Collection<?> col = (Collection<?>) context.obj();
                Class<?> elementType = context.type();
                CborDeserializeFunc func = context.valueDeserializeFunc(elementType);
                CborDeserializerNode r = given == null ? new CborDeserializerNode() : given;
                r.initCol(col, func, context.declaredCount());
                yield r;
            }
            case NewMap -> {
                Map<?, ?> map = (Map<?, ?>) context.obj();
                Class<?> valueType = context.type();
                CborDeserializeFunc func = context.valueDeserializeFunc(valueType);
                CborDeserializerNode r = given == null ? new CborDeserializerNode() : given;
                r.initMap(map, func, context.declaredCount());
                yield r;
            }
            case NewDummyObj -> {
                CborDeserializerNode r = given == null ? new CborDeserializerNode() : given;
                r.initDummyObj(context.declaredCount());
                yield r;
            }
            case NewDummyCol -> {
                CborDeserializerNode r = given == null ? new CborDeserializerNode() : given;
                r.initDummyCol(context.declaredCount());
                yield r;
            }
            default -> throw new AssertionError();
        };
    }
}