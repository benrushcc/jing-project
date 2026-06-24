package io.jingproject.marshalljson;

import io.jingproject.common.WriteBuffer;
import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.Marshalls;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class JsonSerializerState {
    private final JsonSerializerOption option;
    private final WriteBuffer writeBuffer;
    private JsonSerializerNode[] nodes;

    public JsonSerializerState(JsonSerializerOption option, WriteBuffer writeBuffer) {
        this.option = option;
        this.writeBuffer = writeBuffer;
    }

    public void initMarshallableObject(Object instance) {
        if(instance == null) {
            throw new JsonSerializerException("null instance");
        }
        Class<?> type = instance.getClass();
        MarshallFacade fc = Marshalls.getMarshallFacade(type);
        if(fc == null) {
            throw new JsonSerializerException("type not marshallable : " + type.getName());
        }
        if(nodes == null) {
            nodes = new JsonSerializerNode[option.initialSize()];
        }
        JsonSerializerObjNode objNode = new JsonSerializerObjNode();
        objNode.setFc(fc);
        objNode.setInstance(instance);
        objNode.setIndent(1);
        nodes[0] = objNode;
    }

    public void initArray(Object[] arr) {
        if(arr == null) {
            throw new JsonSerializerException("null array");
        }
        Class<?> componentType = arr.getClass().getComponentType();
        if(componentType.isArray()) {
            throw new JsonSerializerException("multi dimensional array not supported : " + componentType.getName());
        }
        if(componentType.getTypeParameters().length > 0) {
            throw new JsonSerializerException("generic array not supported : " + componentType.getName());
        }
        if(nodes == null) {
            nodes = new JsonSerializerNode[option.initialSize()];
        }
        JsonSerializerArrNode arrNode = new JsonSerializerArrNode();
        arrNode.setArr(arr);
        arrNode.setIndent(1);
        nodes[0] = arrNode;
    }

    public <T> void initCol(Collection<T> collection, Class<T> elementType) {
        if(collection == null) {
            throw new JsonSerializerException("null collection");
        }
        if(elementType == null) {
            throw new JsonSerializerException("null elementClass");
        }
        if(elementType.getTypeParameters().length > 0) {
            throw new JsonSerializerException("generic collection not supported : " + elementType.getName());
        }
        if(nodes == null) {
            nodes = new JsonSerializerNode[option.initialSize()];
        }
        JsonSerializerColNode colNode = new JsonSerializerColNode();
        colNode.setSize(collection.size());
        colNode.setIter(collection.iterator());
        colNode.setElementType(elementType);
        colNode.setIndent(1);
        nodes[0] = colNode;
    }

    public <K, V> void initMap(Map<K, V> map, Class<K> keyType, Class<V> valueType) {
        if(map == null) {
            throw new JsonSerializerException("null map");
        }
        if(keyType == null) {
            throw new JsonSerializerException("null keyType");
        }
        if(valueType == null) {
            throw new JsonSerializerException("null valueType");
        }
        if(keyType != CharSequence.class && keyType != String.class) {
            throw new JsonSerializerException("key type not supported: " + keyType.getName());
        }
        if(valueType.getTypeParameters().length > 0) {
            throw new JsonSerializerException("generic map value type not supported: " + valueType.getName());
        }
        if(nodes == null) {
            nodes = new JsonSerializerNode[option.initialSize()];
        }
        JsonSerializerMapNode mapNode = new JsonSerializerMapNode();
        mapNode.setSize(map.size());
        mapNode.setIter(map.entrySet().iterator());
        mapNode.setValueType(valueType);
        mapNode.setIndent(1);
        nodes[0] = mapNode;
    }

    private JsonSerializerNode newNode(final int cur, Supplier<JsonSerializerNode> sup, Predicate<JsonSerializerNode> filter) {
        int i = cur;
        for( ; i < nodes.length; i++) {
            JsonSerializerNode n = nodes[i];
            if(n == null) {
                // new and swap
                JsonSerializerNode r = sup.get();
                nodes[i] = nodes[cur];
                nodes[cur] = r;
                return r;
            }
            if(filter.test(n)) {
                // swap
                if(i != cur) {
                    nodes[i] = nodes[cur];
                    nodes[cur] = n;
                }
                return n;
            }
        }
        // tried every existing node, now grow and allocate new one
        int newLength = Math.addExact(nodes.length, nodes.length);
        if(newLength > option.maxSize()) {
            throw new IllegalStateException("exceeded maximum size : " + option.maxSize() + ", might be circular dependency");
        }
        nodes = Arrays.copyOf(nodes, newLength);
        nodes[i] = nodes[cur];
        JsonSerializerNode r = sup.get();
        nodes[cur] = r;
        return r;
    }

    public void process() {
        int cur = 0;
        for( ; ; ) {
            JsonSerializerNode n = nodes[cur];
            JsonSerializeResult r = n.process(option, writeBuffer);
            switch (r) {
                case JsonSerializeResult.JsonSerializeContinue _ -> throw new JsonSerializerException("continue should have been filtered");
                case JsonSerializeResult.JsonSerializeFinished _ -> {
                    n.reset();
                    if(--cur < 0) {
                        return ;
                    }
                }
                case JsonSerializeResult.JsonSerializeNewMarshallable(Object instance) -> {
                    MarshallFacade fc = Marshalls.getMarshallFacade(instance.getClass());
                    if(fc == null) {
                        throw new JsonSerializerException("not marshallable : " + instance.getClass());
                    }
                    JsonSerializerObjNode objNode = (JsonSerializerObjNode) newNode(++cur, JsonSerializerObjNode::new, o -> o instanceof JsonSerializerObjNode);
                    objNode.setFc(fc);
                    objNode.setInstance(instance);
                    objNode.setIndent(n.indent() + 1); // no overflow
                }
                case JsonSerializeResult.JsonSerializeNewArray(Object[] instance) -> {
                    JsonSerializerArrNode arrNode = (JsonSerializerArrNode) newNode(++cur, JsonSerializerArrNode::new, o -> o instanceof JsonSerializerArrNode);
                    arrNode.setArr(instance);
                    arrNode.setIndent(n.indent() + 1); // no overflow
                }
                case JsonSerializeResult.JsonSerializeNewCollection(Collection<?> instance, Class<?> elementType) -> {
                    JsonSerializerColNode colNode = (JsonSerializerColNode) newNode(++cur, JsonSerializerColNode::new, o -> o instanceof JsonSerializerColNode);
                    colNode.setSize(instance.size());
                    colNode.setIter(instance.iterator());
                    colNode.setElementType(elementType);
                    colNode.setIndent(n.indent() + 1); // no overflow
                }
                case JsonSerializeResult.JsonSerializeNewMap(Map<?, ?> instance, Class<?> keyType, Class<?> valueType) -> {
                    if(keyType != CharSequence.class && keyType != String.class) {
                        throw new JsonSerializerException("unsupported key type : " + keyType.getName());
                    }
                    JsonSerializerMapNode mapNode = (JsonSerializerMapNode) newNode(++cur, JsonSerializerMapNode::new, o -> o instanceof JsonSerializerMapNode);
                    mapNode.setSize(instance.size());
                    mapNode.setIter(instance.entrySet().iterator());
                    mapNode.setValueType(valueType);
                    mapNode.setIndent(n.indent() + 1); // no overflow
                }
                case null, default -> throw new AssertionError();
            }
        }
    }
}
