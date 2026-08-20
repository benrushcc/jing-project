package io.jingproject.marshalljson;

import io.jingproject.common.ReadBuffer;
import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.MarshallUtil;
import io.jingproject.marshall.Marshalls;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public final class JsonDeserializerState {
    public static final int INITIAL_SIZE = 4;
    public static final int MAX_SIZE = 4096;
    private final JsonDeserializerContext context;
    private final JsonDeserializerNode rootNode;

    public JsonDeserializerState(JsonDeserializerOption option, ReadBuffer readBuffer, Class<?> marshallableType) {
        if(marshallableType.isEnum()) {
            throw new JsonDeserializerException("enum cannot be directly deserialized");
        }
        MarshallFacade fc = Marshalls.beanMarshallFacade(marshallableType);
        if (fc == null) {
            throw new JsonSerializerException("type not marshallable : " + marshallableType.getName());
        }
        JsonDeserializerContext c = new JsonDeserializerContext(option, readBuffer);
        JsonDeserializerNode objNode = new JsonDeserializerNode();
        objNode.initObj(fc, c);
        this.context = c;
        this.rootNode = objNode;
    }

    private static JsonDeserializerNode[] grow(JsonDeserializerNode[] nodes, int p, int maxNestedSize) {
        if (p == nodes.length) {
            int newLength = Math.multiplyExact(nodes.length, 2);
            if (newLength > maxNestedSize) {
                throw new JsonDeserializerException("exceeded maximum nested size : " + maxNestedSize);
            }
            return Arrays.copyOf(nodes, newLength);
        }
        return nodes;
    }

    @SuppressWarnings("unchecked")
    public Object process() {
        final JsonDeserializerContext c = context;
        final JsonDeserializerOption option = c.option();
        final int maxNestedSize = option.maxNestedSize();
        JsonDeserializerNode[] nodes = new JsonDeserializerNode[INITIAL_SIZE];
        JsonDeserializerNode n = nodes[0] = rootNode;
        JsonDeserializeResult last = JsonDeserializeResult.Start;
        int p = 0;
        for( ; ; ) {
            last = n.process(c, last);
            switch (last) {
                case Finish, Trivial -> {
                    if(--p < 0) {
                        return c.obj();
                    }
                    n = nodes[p];
                }
                case NewMarshallable -> {
                    Class<?> marshallableType = (Class<?>) c.obj();
                    MarshallFacade fc = Marshalls.beanMarshallFacade(marshallableType);
                    if(fc == null) {
                        throw new JsonSerializerException("type not marshallable : " + marshallableType.getName());
                    }
                    nodes = grow(nodes, p, maxNestedSize);
                    n = nodes[++p];
                    if(n == null) {
                        n = nodes[p] = new JsonDeserializerNode();
                    }
                    n.initObj(fc, c);
                }
                case NewArr -> {
                    Class<?> arrayType = c.type();
                    JsonDeserializeFunc func = JsonDeserializerContext.valueDeserializeFunc(option, arrayType);
                    nodes = grow(nodes, p, maxNestedSize);
                    n = nodes[++p];
                    if(n == null) {
                        n = nodes[p] = new JsonDeserializerNode();
                    }
                    n.initCol(true, new ArrayList<>(), func, c);
                }
                case NewCol -> {
                    Collection<Object> col = (Collection<Object>) c.obj();
                    Class<?> elementType = c.type();
                    JsonDeserializeFunc func = JsonDeserializerContext.valueDeserializeFunc(option, elementType);
                    nodes = grow(nodes, p, maxNestedSize);
                    n = nodes[++p];
                    if(n == null) {
                        n = nodes[p] = new JsonDeserializerNode();
                    }
                    n.initCol(false, col, func, c);
                }
                case NewMap -> {
                    Map<Object, Object> map = (Map<Object, Object>) c.obj();
                    Class<?> elementType = c.type();
                    JsonDeserializeFunc func = JsonDeserializerContext.valueDeserializeFunc(option, elementType);
                    nodes = grow(nodes, p, maxNestedSize);
                    n = nodes[++p];
                    if(n == null) {
                        n = nodes[p] = new JsonDeserializerNode();
                    }
                    n.initMap(map, func, c);
                }
                case NewDummyObj -> {
                    nodes = grow(nodes, p, maxNestedSize);
                    n = nodes[++p];
                    if(n == null) {
                        n = nodes[p] = new JsonDeserializerNode();
                    }
                    n.initDummyObj(c);
                }
                case NewDummyCol -> {
                    nodes = grow(nodes, p, maxNestedSize);
                    n = nodes[++p];
                    if(n == null) {
                        n = nodes[p] = new JsonDeserializerNode();
                    }
                    n.initDummyCol(c);
                }
                case null, default -> throw new AssertionError();
            }
        }
    }
}
