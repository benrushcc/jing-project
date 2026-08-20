package io.jingproject.marshalljson;

import io.jingproject.common.WriteBuffer;
import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.Marshalls;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class JsonSerializerState {
    public static final int INITIAL_SIZE = 4;
    public static final int MAX_SIZE     = 4096;
    private final JsonSerializerContext context;
    private final JsonSerializerNode rootNode;

    public JsonSerializerState(JsonSerializerOption option, WriteBuffer writeBuffer, Object instance) {
        Class<?> marshallableType = instance.getClass();
        if (marshallableType.isEnum()) {
            throw new JsonSerializerException("enum cannot be directly serialized");
        }
        MarshallFacade fc = Marshalls.beanMarshallFacade(marshallableType);
        if (fc == null) {
            throw new JsonSerializerException("type not marshallable : " + marshallableType.getName());
        }
        JsonSerializerNode objNode = new JsonSerializerNode();
        objNode.initObj(fc, instance, 1);
        writeBuffer.writeByte((byte) '{');
        this.context = new JsonSerializerContext(option, writeBuffer);
        this.rootNode = objNode;
    }

    public JsonSerializerState(JsonSerializerOption option, WriteBuffer writeBuffer, Object[] arr) {
        Class<?> componentType = arr.getClass().getComponentType();
        if (componentType.isArray()) {
            throw new JsonSerializerException("multi dimensional array not supported : " + componentType.getName());
        }
        if (componentType.getTypeParameters().length > 0) {
            throw new JsonSerializerException("generic array not supported : " + componentType.getName());
        }
        JsonSerializerNode arrNode = new JsonSerializerNode();
        arrNode.initArr(arr, 1, JsonSerializerContext.valueSerializeFunc(option, arr.getClass().getComponentType()));
        writeBuffer.writeByte((byte) '[');
        this.context = new JsonSerializerContext(option, writeBuffer);
        this.rootNode = arrNode;
    }

    public <T> JsonSerializerState(JsonSerializerOption option, WriteBuffer writeBuffer, Collection<T> collection, Class<T> elementType) {
        if (elementType.getTypeParameters().length > 0) {
            throw new JsonSerializerException("generic collection not supported : " + elementType.getName());
        }
        JsonSerializerNode n = new JsonSerializerNode();
        JsonSerializeFunc fn = JsonSerializerContext.valueSerializeFunc(option, elementType);
        if (collection instanceof List<T> list) {
            n.initList(list, 1, fn);
        } else {
            n.initCol(collection.size(), collection.iterator(), 1, fn);
        }
        writeBuffer.writeByte((byte) '[');
        this.context = new JsonSerializerContext(option, writeBuffer);
        this.rootNode = n;
    }

    public <K, V> JsonSerializerState(JsonSerializerOption option, WriteBuffer writeBuffer, Map<K, V> map, Class<K> keyType, Class<V> valueType) {
        if (keyType != CharSequence.class && keyType != String.class) {
            throw new JsonSerializerException("key type not supported: " + keyType.getName());
        }
        if (valueType.getTypeParameters().length > 0) {
            throw new JsonSerializerException("generic map value type not supported: " + valueType.getName());
        }
        JsonSerializerNode mapNode = new JsonSerializerNode();
        mapNode.initMap(map.size(), map.entrySet().iterator(), 1, JsonSerializerContext.valueSerializeFunc(option, valueType));
        writeBuffer.writeByte((byte) '{');
        this.context = new JsonSerializerContext(option, writeBuffer);
        this.rootNode = mapNode;
    }

    public void process() {
        final JsonSerializerContext c = context;
        final JsonSerializerOption option = c.option();
        final WriteBuffer w = c.writeBuffer();
        final int maxNestedSize = option.maxNestedSize();
        JsonSerializerNode[] nodes = new JsonSerializerNode[INITIAL_SIZE];
        JsonSerializerNode n = nodes[0] = rootNode;
        int p = 0;
        for ( ; ; ) {
            JsonSerializeResult r = n.process(c);
            switch (r) {
                case Finished -> {
                    if (--p < 0) {
                        return;
                    }
                    n = nodes[p];
                }
                case NewMarshallable -> {
                    int ind = n.indent();
                    Object marshallable = c.obj();
                    Class<?> marshallableType = marshallable.getClass();
                    MarshallFacade fc = Marshalls.beanMarshallFacade(marshallableType);
                    if (fc == null) {
                        throw new JsonSerializerException("type not marshallable : " + marshallableType.getName());
                    }
                    if (++p == nodes.length) {
                        int newLength = nodes.length << 1; // no overflow
                        if (newLength > maxNestedSize) {
                            throw new JsonSerializerException("exceeded maximum nested size : " + maxNestedSize);
                        }
                        nodes = Arrays.copyOf(nodes, newLength);
                    }
                    n = nodes[p];
                    if (n == null) {
                        n = nodes[p] = new JsonSerializerNode();
                    }
                    n.initObj(fc, marshallable, ind + 1); // no overflow
                    w.writeByte((byte) '{');
                }
                case NewArray -> {
                    int ind = n.indent();
                    Object[] arr = (Object[]) c.obj();
                    if (++p == nodes.length) {
                        int newLength = nodes.length << 1; // no overflow
                        if (newLength > maxNestedSize) {
                            throw new JsonSerializerException("exceeded maximum nested size : " + maxNestedSize);
                        }
                        nodes = Arrays.copyOf(nodes, newLength);
                    }
                    n = nodes[p];
                    if (n == null) {
                        n = nodes[p] = new JsonSerializerNode();
                    }
                    n.initArr(arr, ind + 1, JsonSerializerContext.valueSerializeFunc(option, arr.getClass().getComponentType())); // no overflow
                    w.writeByte((byte) '[');
                }
                case NewCollection -> {
                    int ind = n.indent();
                    Collection<?> col = (Collection<?>) c.obj();
                    Class<?> elementType = c.type();
                    if (++p == nodes.length) {
                        int newLength = nodes.length << 1; // no overflow
                        if (newLength > maxNestedSize) {
                            throw new JsonSerializerException("exceeded maximum nested size : " + maxNestedSize);
                        }
                        nodes = Arrays.copyOf(nodes, newLength);
                    }
                    n = nodes[p];
                    if (n == null) {
                        n = nodes[p] = new JsonSerializerNode();
                    }
                    JsonSerializeFunc fn = JsonSerializerContext.valueSerializeFunc(option, elementType);
                    if (col instanceof List<?> list) {
                        n.initList(list, ind + 1, fn); // no overflow
                    } else {
                        n.initCol(col.size(), col.iterator(), ind + 1, fn); // no overflow
                    }
                    w.writeByte((byte) '[');
                }
                case NewMap -> {
                    int ind = n.indent();
                    Map<?, ?> map = (Map<?, ?>) c.obj();
                    Class<?> valueType = c.type();
                    if (++p == nodes.length) {
                        int newLength = nodes.length << 1; // no overflow
                        if (newLength > maxNestedSize) {
                            throw new JsonSerializerException("exceeded maximum nested size : " + maxNestedSize);
                        }
                        nodes = Arrays.copyOf(nodes, newLength);
                    }
                    n = nodes[p];
                    if (n == null) {
                        n = nodes[p] = new JsonSerializerNode();
                    }
                    n.initMap(map.size(), map.entrySet().iterator(), ind + 1, JsonSerializerContext.valueSerializeFunc(option, valueType)); // no overflow
                    w.writeByte((byte) '{');
                }
                case null, default -> throw new AssertionError();
            }
        }
    }
}