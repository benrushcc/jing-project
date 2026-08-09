package io.jingproject.marshalljson;

public final class JsonDeserializerState {
    public static final int INITIAL_SIZE = 4;
    public static final int MAX_SIZE = 4096;
//    private final JsonDeserializerContext c;
//    private JsonDeserializerNode[] nodes;
//
//    public JsonDeserializerState(JsonDeserializerOption option, ReadBuffer readBuffer) {
//        
//        this.c = new JsonDeserializerContext(option, readBuffer);
//    }
//
//    public void initMarshallableType(Class<?> marshallableType) {
//        
//        if(marshallableType.isEnum()) {
//            throw new JsonDeserializerException("enum cannot be directly deserialized");
//        }
//        MarshallFacade fc = Marshalls.getMarshallFacade(marshallableType);
//        if(fc == null) {
//            throw new JsonDeserializerException("type not marshallable : " + marshallableType.getName());
//        }
//        if(nodes == null) {
//            nodes = new JsonDeserializerNode[INITIAL_SIZE];
//        }
//        JsonDeserializerObjNode objNode = new JsonDeserializerObjNode();
//        objNode.init(fc, option, c);
//        nodes[0] = objNode;
//        if (c.nextFirstValuableByte() != (byte) '{') {
//            throw new JsonDeserializerException("invalid json structure, missing object start");
//        }
//    }
//
//    private JsonDeserializerNode newNode(final int cur, Supplier<JsonDeserializerNode> sup, Predicate<JsonDeserializerNode> filter) {
//        final JsonDeserializerOption option = this.c.option();
//        final JsonDeserializerNode[] nds = this.nodes;
//        int i = cur;
//        for( ; i < nds.length; i++) {
//            JsonDeserializerNode n = nds[i];
//            if(n == null) {
//                // new and swap
//                JsonDeserializerNode r = sup.get();
//                nds[i] = nds[cur];
//                nds[cur] = r;
//                return r;
//            }
//            if(filter.test(n)) {
//                // swap
//                if(i != cur) {
//                    nds[i] = nds[cur];
//                    nds[cur] = n;
//                }
//                return n;
//            }
//        }
//        // try in-place replacement if we can't grow
//        if(nds.length == option.maxNestedSize()) {
//            if(cur == nds.length) {
//                throw new IllegalStateException("exceeded maximum nested size : " + option.maxNestedSize());
//            }
//            JsonDeserializerNode r = sup.get();
//            nds[cur] = r;
//            return r;
//        }
//        // tried every existing node, now grow and allocate new one
//        int newLength = Math.addExact(nodes.length, nodes.length);
//        if(newLength > option.maxNestedSize()) {
//            throw new IllegalStateException("exceeded maximum nested size : " + option.maxNestedSize());
//        }
//        nodes = Arrays.copyOf(nodes, newLength);
//        if(i != cur) {
//            nodes[i] = nodes[cur];
//        }
//        JsonDeserializerNode r = sup.get();
//        nodes[cur] = r;
//        return r;
//    }
//
//    public Object process() {
//        Object v = null;
//        int cur = 0;
//        for( ; ; ) {
//            JsonDeserializerNode n = nodes[cur];
//            JsonDeserializeResult r = n.process(c, v);
//            v = null;
//            switch (r) {
//                case Continue -> throw new AssertionError("continue should have been filtered");
//                case Finish -> {
//                    Object result = c.obj();
//                    if(--cur < 0) {
//                        return result;
//                    }
//                    v = result;
//                }
//                case NewMarshallable -> {
//                    Class<?> marshallableType = c.type();
//                    MarshallFacade fc = Marshalls.getMarshallFacade(marshallableType);
//                    if(fc == null) {
//                        throw new JsonSerializerException("not marshallable : " + marshallableType.getName());
//                    }
//                    JsonDeserializerObjNode objNode = (JsonDeserializerObjNode) newNode(++cur, JsonDeserializerObjNode::new, o -> o instanceof JsonDeserializerObjNode);
//                    objNode.init(fc, option, c);
//                    if (c.nextFirstValuableByte() != (byte) '{') {
//                        throw new JsonDeserializerException("invalid json structure, missing object start");
//                    }
//                }
//                case null, default -> throw new AssertionError();
//            }
//        }
//    }
}
