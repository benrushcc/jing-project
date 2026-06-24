package io.jingproject.marshalljson;

import io.jingproject.common.WriteBuffer;
import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.Marshalls;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public final class JsonSerializerState {
    private static final int INITIAL_SIZE = 4;
    private static final int SHRINK_SIZE = 16;
    private final JsonSerializerOption option;
    private WriteBuffer writeBuffer;
    private JsonSerializerNode[] nodes;
    private JsonSerializerObjNode[] objNodes;
    private int objNodeIndex = 0;
    private JsonSerializerArrNode[] arrNodes;
    private int arrNodeIndex = 0;
    private JsonSerializerColNode[] colNodes;
    private int colNodeIndex = 0;
    private JsonSerializerMapNode[] mapNodes;
    private int mapNodeIndex = 0;

    public JsonSerializerState(JsonSerializerOption option) {
        this.option = option;
    }

    public void setWriteBuffer(WriteBuffer writeBuffer) {
        this.writeBuffer = writeBuffer;
    }

    private void checkInitialized() {
        if(nodes == null) {
            nodes = new JsonSerializerNode[option.initialSize()];
        }
    }

    public void preFill() {
        checkInitialized();
        if(objNodeIndex == 0) {
            objNodes = new JsonSerializerObjNode[INITIAL_SIZE];
            objNodeIndex = INITIAL_SIZE;
            for(int i = 0; i < INITIAL_SIZE; i++) {
                objNodes[i] = new JsonSerializerObjNode();
            }
        }
        if(arrNodeIndex == 0) {
            arrNodes = new JsonSerializerArrNode[INITIAL_SIZE];
            arrNodeIndex = INITIAL_SIZE;
            for(int i = 0; i < INITIAL_SIZE; i++) {
                arrNodes[i] = new JsonSerializerArrNode();
            }
        }
        if(colNodeIndex == 0) {
            colNodes = new JsonSerializerColNode[INITIAL_SIZE];
            colNodeIndex = INITIAL_SIZE;
            for(int i = 0; i < INITIAL_SIZE; i++) {
                colNodes[i] = new JsonSerializerColNode();
            }
        }
        if(mapNodeIndex == 0) {
            mapNodes = new JsonSerializerMapNode[INITIAL_SIZE];
            mapNodeIndex = INITIAL_SIZE;
            for(int i = 0; i < INITIAL_SIZE; i++) {
                mapNodes[i] = new JsonSerializerMapNode();
            }
        }
    }

    public void reset() {
        writeBuffer = null;
        Arrays.fill(nodes, null);
        if(objNodeIndex > SHRINK_SIZE) {
            objNodes = Arrays.copyOf(objNodes, SHRINK_SIZE);
            objNodeIndex = SHRINK_SIZE;
        }
        if(arrNodeIndex > SHRINK_SIZE) {
            arrNodes = Arrays.copyOf(arrNodes, SHRINK_SIZE);
            arrNodeIndex = SHRINK_SIZE;
        }
        if(colNodeIndex > SHRINK_SIZE) {
            colNodes = Arrays.copyOf(colNodes, SHRINK_SIZE);
            colNodeIndex = SHRINK_SIZE;
        }
        if(mapNodeIndex > SHRINK_SIZE) {
            mapNodes = Arrays.copyOf(mapNodes, SHRINK_SIZE);
            mapNodeIndex = SHRINK_SIZE;
        }
    }

    private void growNodesIfNeeded(int nextIndex) {
        if(nextIndex == nodes.length) {
            int newLength = Math.addExact(nodes.length, nodes.length);
            if(newLength > option.maxSize()) {
                throw new IllegalStateException("exceeded maximum size : " + option.maxSize() + ", might be circular dependency");
            }
            nodes = Arrays.copyOf(nodes, newLength);
        }
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
        checkInitialized();
        JsonSerializerObjNode objNode = newObjNode();
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
        checkInitialized();
        JsonSerializerArrNode arrNode = newArrNode();
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
        checkInitialized();
        JsonSerializerColNode colNode = newColNode();
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
        checkInitialized();
        JsonSerializerMapNode mapNode = newMapNode();
        mapNode.setSize(map.size());
        mapNode.setIter(map.entrySet().iterator());
        mapNode.setValueType(valueType);
        mapNode.setIndent(1);
        nodes[0] = mapNode;
    }

    private JsonSerializerObjNode newObjNode() {
        if(objNodeIndex == 0) {
            return new JsonSerializerObjNode();
        }
        JsonSerializerObjNode r = objNodes[--objNodeIndex];
        objNodes[objNodeIndex] = null;
        return r;
    }

    private JsonSerializerArrNode newArrNode() {
        if(arrNodeIndex == 0) {
            return new JsonSerializerArrNode();
        }
        JsonSerializerArrNode r = arrNodes[--arrNodeIndex];
        arrNodes[arrNodeIndex] = null;
        return r;
    }

    private JsonSerializerColNode newColNode() {
        if(colNodeIndex == 0) {
            return new JsonSerializerColNode();
        }
        JsonSerializerColNode r = colNodes[--colNodeIndex];
        colNodes[colNodeIndex] = null;
        return r;
    }

    private JsonSerializerMapNode newMapNode() {
        if(mapNodeIndex == 0) {
            return new JsonSerializerMapNode();
        }
        JsonSerializerMapNode r = mapNodes[--mapNodeIndex];
        mapNodes[mapNodeIndex] = null;
        return r;
    }

    private void recycleObjNode(JsonSerializerObjNode objNode) {
        if(objNodes == null) {
            objNodes = new JsonSerializerObjNode[INITIAL_SIZE];
        }
        if(objNodeIndex == objNodes.length) {
            objNodes = Arrays.copyOf(objNodes, Math.addExact(objNodes.length, objNodes.length));
        }
        objNodes[objNodeIndex++] = objNode;
    }

    private void recycleArrNode(JsonSerializerArrNode arrNode) {
        if(arrNodes == null) {
            arrNodes = new JsonSerializerArrNode[INITIAL_SIZE];
        }
        if(arrNodeIndex == arrNodes.length) {
            arrNodes = Arrays.copyOf(arrNodes, Math.addExact(arrNodes.length, arrNodes.length));
        }
        arrNodes[arrNodeIndex++] = arrNode;
    }

    private void recycleColNode(JsonSerializerColNode colNode) {
        if(colNodes == null) {
            colNodes = new JsonSerializerColNode[INITIAL_SIZE];
        }
        if(colNodeIndex == colNodes.length) {
            colNodes = Arrays.copyOf(colNodes, Math.addExact(colNodes.length, colNodes.length));
        }
        colNodes[colNodeIndex++] = colNode;
    }

    private void recycleMapNode(JsonSerializerMapNode mapNode) {
        if(mapNodes == null) {
            mapNodes = new JsonSerializerMapNode[INITIAL_SIZE];
        }
        if(mapNodeIndex == mapNodes.length) {
            mapNodes = Arrays.copyOf(mapNodes, Math.addExact(mapNodes.length, mapNodes.length));
        }
        mapNodes[mapNodeIndex++] = mapNode;
    }

    private void recycle(JsonSerializerNode n) {
        n.reset();
        switch (n) {
            case JsonSerializerObjNode objNode -> recycleObjNode(objNode);
            case JsonSerializerArrNode arrNode -> recycleArrNode(arrNode);
            case JsonSerializerColNode colNode -> recycleColNode(colNode);
            case JsonSerializerMapNode mapNode -> recycleMapNode(mapNode);
            case null, default -> throw new AssertionError();
        }
    }

    public void process() {
        int index = 0;
        JsonSerializerNode n = Objects.requireNonNull(nodes[index], "not initialized");
        for( ; ; ) {
            JsonSerializeResult r = n.process(option, writeBuffer);
            switch (r) {
                case JsonSerializeResult.JsonSerializeContinue _ -> {}
                case JsonSerializeResult.JsonSerializeFinished _ -> {
                    recycle(n);
                    nodes[index--] = null;
                    if(index < 0) {
                        return ;
                    }
                    n = nodes[index];
                }
                case JsonSerializeResult.JsonSerializeNewMarshallable(Object instance) -> {
                    MarshallFacade fc = Marshalls.getMarshallFacade(instance.getClass());
                    if(fc == null) {
                        throw new JsonSerializerException("not marshallable : " + instance.getClass());
                    }
                    growNodesIfNeeded(++index);
                    JsonSerializerObjNode objNode = newObjNode();
                    objNode.setFc(fc);
                    objNode.setInstance(instance);
                    objNode.setIndent(n.indent() + 1); // no overflow
                    nodes[index] = n = objNode;
                }
                case JsonSerializeResult.JsonSerializeNewArray(Object[] instance) -> {
                    growNodesIfNeeded(++index);
                    JsonSerializerArrNode arrNode = newArrNode();
                    arrNode.setArr(instance);
                    arrNode.setIndent(n.indent() + 1); // no overflow
                    nodes[index] = n = arrNode;
                }
                case JsonSerializeResult.JsonSerializeNewCollection(Collection<?> instance, Class<?> elementType) -> {
                    growNodesIfNeeded(++index);
                    JsonSerializerColNode colNode = newColNode();
                    colNode.setSize(instance.size());
                    colNode.setIter(instance.iterator());
                    colNode.setElementType(elementType);
                    colNode.setIndent(n.indent() + 1); // no overflow
                    nodes[index] = n = colNode;
                }
                case JsonSerializeResult.JsonSerializeNewMap(Map<?, ?> instance, Class<?> keyType, Class<?> valueType) -> {
                    if(keyType != CharSequence.class && keyType != String.class) {
                        throw new JsonSerializerException("unsupported key type : " + keyType.getName());
                    }
                    growNodesIfNeeded(++index);
                    JsonSerializerMapNode mapNode = newMapNode();
                    mapNode.setSize(instance.size());
                    mapNode.setIter(instance.entrySet().iterator());
                    mapNode.setValueType(valueType);
                    mapNode.setIndent(n.indent() + 1); // no overflow
                    nodes[index] = n = mapNode;
                }
                case null, default -> throw new AssertionError();
            }
        }
    }
}
