package io.jingproject.marshalljson;

import io.jingproject.common.WriteBuffer;
import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.Marshalls;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 *  state holder for a serialization run, managing a stack of {@link JsonSerializerNode} instances.
 */
public final class JsonSerializerState {
    public static final int INITIAL_SIZE = 4;
    public static final int MAX_SIZE = 4096;
    private final JsonSerializerOption option;
    private final WriteBuffer writeBuffer;
    private JsonSerializerNode[] nodes;

    public JsonSerializerState(JsonSerializerOption option, WriteBuffer writeBuffer) {
        assert option != null && writeBuffer != null;
        this.option = option;
        this.writeBuffer = writeBuffer;
    }

    /**
     * initializes the state for serializing a single marshallable object.
     * @param instance instance the object to serialize (must be marshallable)
     */
    public void initMarshallableObject(Object instance) {
        assert instance != null;
        Class<?> marshallableType = instance.getClass();
        if(marshallableType.isEnum()) {
            throw new JsonSerializerException("enum cannot be directly serialized");
        }
        MarshallFacade fc = Marshalls.getMarshallFacade(marshallableType);
        if(fc == null) {
            throw new JsonSerializerException("type not marshallable : " + marshallableType.getName());
        }
        if(nodes == null) {
            nodes = new JsonSerializerNode[INITIAL_SIZE];
        }
        JsonSerializerObjNode objNode = new JsonSerializerObjNode();
        objNode.init(fc, instance, 1);
        nodes[0] = objNode;
        JsonSerializeUtil.serializeObjStart(writeBuffer);
    }

    /**
     * initializes the state for serializing an array of objects.
     * @param arr the array to serialize (multi‑dimensional or generic arrays are not supported)
     */
    public void initArray(Object[] arr) {
        assert arr != null;
        Class<?> componentType = arr.getClass().getComponentType();
        if(componentType.isArray()) {
            throw new JsonSerializerException("multi dimensional array not supported : " + componentType.getName());
        }
        if(componentType.getTypeParameters().length > 0) {
            throw new JsonSerializerException("generic array not supported : " + componentType.getName());
        }
        if(nodes == null) {
            nodes = new JsonSerializerNode[INITIAL_SIZE];
        }
        JsonSerializerArrNode arrNode = new JsonSerializerArrNode();
        arrNode.init(option, arr, 1);
        nodes[0] = arrNode;
        JsonSerializeUtil.serializeArrayStart(writeBuffer);
    }

    /**
     * initializes the state for serializing a collection.
     * @param collection the collection to serialize
     * @param elementType the runtime element type (must be concrete, not generic)
     */
    public <T> void initCol(Collection<T> collection, Class<T> elementType) {
        assert collection != null && elementType != null;
        if(elementType.getTypeParameters().length > 0) {
            throw new JsonSerializerException("generic collection not supported : " + elementType.getName());
        }
        if(nodes == null) {
            nodes = new JsonSerializerNode[INITIAL_SIZE];
        }
        if(collection instanceof List<T> list) {
            JsonSerializerListNode listNode = new JsonSerializerListNode();
            listNode.init(option, list, elementType, 1);
            nodes[0] = listNode;
        } else {
            JsonSerializerColNode colNode = new JsonSerializerColNode();
            colNode.init(option, collection.size(), collection.iterator(), elementType, 1);
            nodes[0] = colNode;
        }
        JsonSerializeUtil.serializeArrayStart(writeBuffer);
    }

    /**
     * initializes the state for serializing a map.
     *
     * @param map the map to serialize
     * @param keyType the key type (must be String.class or CharSequence.class)
     * @param valueType the value type (must be concrete, not generic)
     */
    public <K, V> void initMap(Map<K, V> map, Class<K> keyType, Class<V> valueType) {
        assert map != null && keyType != null && valueType != null;
        if(keyType != CharSequence.class && keyType != String.class) {
            throw new JsonSerializerException("key type not supported: " + keyType.getName());
        }
        if(valueType.getTypeParameters().length > 0) {
            throw new JsonSerializerException("generic map value type not supported: " + valueType.getName());
        }
        if(nodes == null) {
            nodes = new JsonSerializerNode[INITIAL_SIZE];
        }
        JsonSerializerMapNode mapNode = new JsonSerializerMapNode();
        mapNode.init(option, map.size(), map.entrySet().iterator(), valueType, 1);
        nodes[0] = mapNode;
        JsonSerializeUtil.serializeObjStart(writeBuffer);
    }

    /**
     * obtains a node from a pool, reusing an existing compatible node when possible, or creates a new one.
     * <p>
     * the usage pattern of node types is fairly consistent: once a particular type is needed at a certain depth,
     * it tends to be reused again later, and nested structures require high‑frequency node allocation.
     * however, the overall nesting depth is limited, so we keep a pool and perform a linear search
     * for a reusable node (matching the given filter), then swap it to the target position.
     * this approach reduces allocation pressure while maintaining good performance.
     * </p>
     */
    private JsonSerializerNode newNode(final int cur, Supplier<JsonSerializerNode> sup, Predicate<JsonSerializerNode> filter) {
        final JsonSerializerNode[] nds = this.nodes;
        int i = cur;
        for( ; i < nds.length; i++) {
            JsonSerializerNode n = nds[i];
            if(n == null) {
                // new and swap
                JsonSerializerNode r = sup.get();
                nds[i] = nds[cur];
                nds[cur] = r;
                return r;
            }
            if(filter.test(n)) {
                // swap
                if(i != cur) {
                    nds[i] = nds[cur];
                    nds[cur] = n;
                }
                return n;
            }
        }
        // try in-place replacement if we can't grow
        if(nds.length == option.maxNestedSize()) {
            if(cur == nds.length) {
                throw new IllegalStateException("exceeded maximum nested size : " + option.maxNestedSize() + ", might be circular dependency");
            }
            JsonSerializerNode r = sup.get();
            nds[cur] = r;
            return r;
        }
        // tried every existing node, now grow and allocate new one
        int newLength = Math.addExact(nodes.length, nodes.length);
        if(newLength > option.maxNestedSize()) {
            throw new IllegalStateException("exceeded maximum nested size : " + option.maxNestedSize() + ", might be circular dependency");
        }
        nodes = Arrays.copyOf(nodes, newLength);
        if(i != cur) {
            nodes[i] = nodes[cur];
        }
        JsonSerializerNode r = sup.get();
        nodes[cur] = r;
        return r;
    }

    /**
     * drives the serialization process.
     * <p>
     * this method repeatedly processes the current top node and handles the returned
     * result by either finishing the current level, or pushing a new child node onto the stack.
     * the loop terminates when the stack becomes empty.
     * </p>
     */
    public void process() {
        final JsonSerializerContext context = new JsonSerializerContext();
        int cur = 0;
        for( ; ; ) {
            JsonSerializerNode n = nodes[cur];
            JsonSerializeResult r = n.process(option, writeBuffer, context);
            switch (r) {
                case Continue -> throw new AssertionError("continue should have been filtered");
                case Finished -> {
                    if(--cur < 0) {
                        return ;
                    }
                }
                case NewMarshallable -> {
                    Object marshallable = context.obj();
                    Class<?> marshallableType = marshallable.getClass();
                    // assert !marshallableType.isEnum();
                    MarshallFacade fc = Marshalls.getMarshallFacade(marshallableType);
                    if(fc == null) {
                        throw new JsonSerializerException("type not marshallable : " + marshallableType.getName());
                    }
                    JsonSerializerObjNode objNode = (JsonSerializerObjNode) newNode(++cur, JsonSerializerObjNode::new, o -> o instanceof JsonSerializerObjNode);
                    objNode.init(fc, marshallable, n.indent + 1); // no overflow
                    JsonSerializeUtil.serializeObjStart(writeBuffer);
                }
                case NewArray -> {
                    Object[] arr = context.arr();
                    JsonSerializerArrNode arrNode = (JsonSerializerArrNode) newNode(++cur, JsonSerializerArrNode::new, o -> o instanceof JsonSerializerArrNode);
                    arrNode.init(option, arr, n.indent + 1); // no overflow
                    JsonSerializeUtil.serializeArrayStart(writeBuffer);
                }
                case NewCollection -> {
                    Collection<?> col = context.col();
                    Class<?> elementType = context.firstType();
                    if(col instanceof List<?> list) {
                        JsonSerializerListNode listNode = (JsonSerializerListNode) newNode(++cur, JsonSerializerListNode::new, o -> o instanceof JsonSerializerListNode);
                        listNode.init(option, list, elementType, n.indent + 1); // no overflow
                    } else {
                        JsonSerializerColNode colNode = (JsonSerializerColNode) newNode(++cur, JsonSerializerColNode::new, o -> o instanceof JsonSerializerColNode);
                        colNode.init(option, col.size(), col.iterator(), elementType, n.indent + 1); // no overflow
                    }
                    JsonSerializeUtil.serializeArrayStart(writeBuffer);
                }
                case NewMap -> {
                    Map<?, ?> map = context.map();
                    Class<?> keyType = context.firstType();
                    Class<?> valueType = context.secondType();
                    if(keyType != CharSequence.class && keyType != String.class) {
                        throw new JsonSerializerException("unsupported key type : " + keyType.getName());
                    }
                    JsonSerializerMapNode mapNode = (JsonSerializerMapNode) newNode(++cur, JsonSerializerMapNode::new, o -> o instanceof JsonSerializerMapNode);
                    mapNode.init(option, map.size(), map.entrySet().iterator(), valueType, n.indent + 1); // no overflow
                    JsonSerializeUtil.serializeObjStart(writeBuffer);
                }
                case null, default -> throw new AssertionError();
            }
        }
    }
}
