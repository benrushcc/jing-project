package io.jingproject.marshalljson;

import io.jingproject.common.ReadBuffer;
import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.Marshalls;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class JsonDeserializerState {
    public static final int INITIAL_SIZE = 4;
    public static final int MAX_SIZE = 4096;
    private final JsonDeserializerOption option;
    private final ReadBuffer readBuffer;
    private final JsonDeserializerContext context;
    private JsonDeserializerNode[] nodes;

    public JsonDeserializerState(JsonDeserializerOption option, ReadBuffer readBuffer) {
        this.option = option;
        this.readBuffer = readBuffer;
        this.context = new JsonDeserializerContext(option);
    }

    public void initMarshallableType(Class<?> marshallableType) {
        if(marshallableType == null) {
            throw new JsonDeserializerException("marshallable type cannot be null");
        }
        MarshallFacade fc = Marshalls.getMarshallFacade(marshallableType);
        if(fc == null) {
            throw new JsonDeserializerException("type not marshallable : " + marshallableType.getName());
        }
        if(nodes == null) {
            nodes = new JsonDeserializerNode[INITIAL_SIZE];
        }
        JsonDeserializerObjNode objNode = new JsonDeserializerObjNode();
        objNode.init(fc, option, context);
        nodes[0] = objNode;
        if (JsonDeserializeUtil.nextFirstValuableByte(option, readBuffer) != (byte) '{') {
            throw new JsonDeserializerException("invalid json structure, missing object start");
        }
    }

    private JsonDeserializerNode newNode(final int cur, Supplier<JsonDeserializerNode> sup, Predicate<JsonDeserializerNode> filter) {
        final JsonDeserializerNode[] nds = this.nodes;
        int i = cur;
        for( ; i < nds.length; i++) {
            JsonDeserializerNode n = nds[i];
            if(n == null) {
                // new and swap
                JsonDeserializerNode r = sup.get();
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
                throw new IllegalStateException("exceeded maximum nested size : " + option.maxNestedSize());
            }
            JsonDeserializerNode r = sup.get();
            nds[cur] = r;
            return r;
        }
        // tried every existing node, now grow and allocate new one
        int newLength = Math.addExact(nodes.length, nodes.length);
        if(newLength > option.maxNestedSize()) {
            throw new IllegalStateException("exceeded maximum nested size : " + option.maxNestedSize());
        }
        nodes = Arrays.copyOf(nodes, newLength);
        if(i != cur) {
            nodes[i] = nodes[cur];
        }
        JsonDeserializerNode r = sup.get();
        nodes[cur] = r;
        return r;
    }

    public Object process() {
        JsonDeserializerContext context = new JsonDeserializerContext();
        Object v = null;
        int cur = 0;
        for( ; ; ) {
            JsonDeserializerNode n = nodes[cur];
            JsonDeserializeResult r = n.process(option, readBuffer, context, v);
            v = null;
            switch (r) {
                case JsonDeserializeResult.JsonDeserializeContinue _ -> throw new AssertionError("continue should have been filtered");
                case JsonDeserializeResult.JsonDeserializeFinished(Object result) -> {
                    if(--cur < 0) {
                        return result;
                    }
                    v = result;
                }
                case JsonDeserializeResult.JsonDeserializeNewMarshallable(Class<?> marshallableType) -> {
                    MarshallFacade fc = Marshalls.getMarshallFacade(marshallableType);
                    if(fc == null) {
                        throw new JsonSerializerException("not marshallable : " + marshallableType.getName());
                    }
                    JsonDeserializerObjNode objNode = (JsonDeserializerObjNode) newNode(++cur, JsonDeserializerObjNode::new, o -> o instanceof JsonDeserializerObjNode);
                    objNode.init(fc, option, context);
                    if (JsonDeserializeUtil.nextFirstValuableByte(option, readBuffer) != (byte) '{') {
                        throw new JsonDeserializerException("invalid json structure, missing object start");
                    }
                }
                case null, default -> throw new AssertionError();
            }
        }
    }
}
