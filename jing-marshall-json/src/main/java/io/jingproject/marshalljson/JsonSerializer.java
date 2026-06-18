package io.jingproject.marshalljson;

import io.jingproject.common.WriteBuffer;
import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.Marshalls;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public final class JsonSerializer {
    private final JsonSerializerOption option;

    public JsonSerializer(JsonSerializerOption option) {
        this.option = option;
    }

    public void serializePrimitiveArray(byte[] arr, WriteBuffer writeBuffer) {
        JsonSerializeUtil.serializeByteArray(arr, 1, option.indentationLevel(), writeBuffer);
    }

    public void serializePrimitiveArray(boolean[] arr, WriteBuffer writeBuffer) {
        JsonSerializeUtil.serializeBooleanArray(arr, 1, option.indentationLevel(), writeBuffer);
    }

    public void serializePrimitiveArray(short[] arr, WriteBuffer writeBuffer) {
        JsonSerializeUtil.serializeShortArray(arr, 1, option.indentationLevel(), writeBuffer);
    }

    public void serializePrimitiveArray(char[] arr, WriteBuffer writeBuffer) {
        JsonSerializeUtil.serializeCharArray(arr, 1, option.indentationLevel(), writeBuffer);
    }

    public void serializePrimitiveArray(int[] arr, WriteBuffer writeBuffer) {
        JsonSerializeUtil.serializeIntArray(arr, 1, option.indentationLevel(), writeBuffer);
    }

    public void serializePrimitiveArray(long[] arr, WriteBuffer writeBuffer) {
        JsonSerializeUtil.serializeLongArray(arr, 1, option.indentationLevel(), writeBuffer);
    }

    public void serializePrimitiveArray(float[] arr, WriteBuffer writeBuffer) {
        JsonSerializeUtil.serializeFloatArray(arr, 1, option.indentationLevel(), writeBuffer);
    }

    public void serializePrimitiveArray(double[] arr, WriteBuffer writeBuffer) {
        JsonSerializeUtil.serializeDoubleArray(arr, 1, option.indentationLevel(), writeBuffer);
    }

    public <T> void serializeMarshallableObject(T object, Class<T> type, WriteBuffer writeBuffer) {
        JsonSerializerState state = new JsonSerializerState(option, writeBuffer);
        state.initMarshallableObject(object, type);
        state.process();
    }

    public <K, V> void serializeMap(Map<K, V> map, Class<K> kClass, Class<V> vClass, WriteBuffer writeBuffer) {
        JsonSerializerState state = new JsonSerializerState(option, writeBuffer);
        state.initMap(map, kClass, vClass);
        state.process();
    }

    public <T> void serializeArray(T[] array, Class<T> tClass, WriteBuffer writeBuffer) {
        JsonSerializerState state = new JsonSerializerState(option, writeBuffer);
        state.initArray(array, tClass);
        state.process();
    }

    public <T> void serializeCollection(Collection<T> collection, Class<T> tClass, WriteBuffer writeBuffer) {
        JsonSerializerState state = new JsonSerializerState(option, writeBuffer);
        state.initCollection(collection, tClass);
        state.process();
    }

    private static final class JsonSerializerState {
        private final JsonSerializerOption option;
        private final WriteBuffer writeBuffer;
        private JsonSerializerNode[] nodes;
        private boolean consumed = false;

        public JsonSerializerState(JsonSerializerOption option, WriteBuffer writeBuffer) {
            this.option = Objects.requireNonNull(option);
            this.writeBuffer = Objects.requireNonNull(writeBuffer);
        }

        private void checkInitialized() {
            if(consumed) {
                throw new IllegalStateException("already consumed");
            }
            nodes = new JsonSerializerNode[option.initialSize()];
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

        public void initMarshallableObject(Object instance, Class<?> type) {
            if(instance == null || type == null) {
                throw new JsonSerializerException("empty instance or type");
            }
            if(!type.isInstance(instance)) {
                throw new JsonSerializerException("instance type mismatch");
            }
            checkInitialized();
            MarshallFacade fc = Marshalls.getMarshallFacade(type);
            if(fc == null) {
                throw new JsonSerializerException("type not marshallable : " + type.getName());
            }
            nodes[0] = new JsonSerializerObjNode(option, writeBuffer, 1, instance, fc);
        }

        public <K, V> void initMap(Map<K, V> map, Class<K> keyClass, Class<V> valueClass) {
            if(map == null || keyClass == null || valueClass == null) {
                throw new JsonSerializerException("empty map or keyClass or valueClass");
            }
            checkInitialized();
            if(keyClass != String.class && keyClass != CharSequence.class) {
                throw new JsonSerializerException("unsupported key type : " + keyClass.getName());
            }
            if(valueClass.getTypeParameters().length > 0) {
                throw new JsonSerializerException("unsupported generic map value type : " + valueClass.getName());
            }
            JsonValueSerializer valueSerializer;
            if(valueClass.isArray()) {
                checkComponentType(valueClass.getComponentType());
                valueSerializer = JsonSerializeUtil.arraySerializer(option, valueClass);
            } else {
                valueSerializer = JsonSerializeUtil.rawSerializer(option, valueClass);
            }
            nodes[0] = new JsonSerializerMapNode(option, writeBuffer, 1, map, valueSerializer);
        }

        public <T> void initCollection(Collection<T> collection, Class<T> elementClass) {
            assert collection != null && elementClass != null;
            checkInitialized();
            if(elementClass.getTypeParameters().length > 0) {
                throw new JsonSerializerException("unsupported generic collection value type : " + elementClass.getName());
            }
            JsonValueSerializer valueSerializer;
            if (elementClass.isArray()) {
                checkComponentType(elementClass.getComponentType());
                valueSerializer = JsonSerializeUtil.arraySerializer(option, elementClass);
            } else {
                valueSerializer = JsonSerializeUtil.rawSerializer(option, elementClass);
            }
            nodes[0] = new JsonSerializerArrayNode(option, writeBuffer, 1, collection, valueSerializer);
        }

        public <T> void initArray(T[] arr, Class<T> arrClass) {
            assert arr != null && arrClass != null && !arrClass.isPrimitive();
            checkInitialized();
            checkComponentType(arrClass);
            JsonValueSerializer valueSerializer = JsonSerializeUtil.arraySerializer(option, arrClass);
            nodes[0] = new JsonSerializerArrayNode(option, writeBuffer, 1, arr, valueSerializer);
        }

        private void checkComponentType(Class<?> componentType) {
            if(componentType.isArray()) {
                throw new JsonSerializerException("multi dimensional array not supported, component type : " + componentType.getName());
            }
            if(componentType.getTypeParameters().length > 0) {
                throw new JsonSerializerException("generic array not supported, component type : " + componentType.getName());
            }
        }

        public void process() {
            if(consumed) {
                throw new IllegalStateException("already consumed");
            }
            consumed = true;
            int index = 0;
            JsonSerializerNode current = nodes[index];
            for( ; ; ) {
                JsonSerializerNodeResult r = current.step();
                switch (r) {
                    case JsonSerializerNodeResult.JsonSerializerNodeContinue() -> {}
                    case JsonSerializerNodeResult.JsonSerializerNodeTransfer(JsonSerializerNode n) -> {
                        growNodesIfNeeded(++index);
                        nodes[index] = current = n;
                    }
                    case JsonSerializerNodeResult.JsonSerializerNodeFinished() -> {
                        nodes[index--] = null;
                        if(index < 0) {
                            return ;
                        }
                        current = nodes[index];
                    }
                }
            }
        }
    }
}
