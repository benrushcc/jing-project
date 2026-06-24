package io.jingproject.marshalljson;

import io.jingproject.common.WriteBuffer;

import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public final class JsonSerializer {
    private final JsonSerializerOption option;
    private final Queue<JsonSerializerState> pool;

    public JsonSerializer(JsonSerializerOption option) {
        this.option = option;
        int poolSize = option.poolSize();
        if(poolSize > 0) {
            this.pool = new ArrayBlockingQueue<>(poolSize);
            for(int i = 0; i < poolSize; i++) {
                JsonSerializerState preAllocatedState = new JsonSerializerState(option);
                preAllocatedState.preFill();
                this.pool.add(preAllocatedState);
            }
        } else {
            this.pool = null;
        }
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

    public void serializeMarshallableObject(Object instance, WriteBuffer writeBuffer) {
        JsonSerializerState state;
        boolean recycle = false;
        if(pool != null) {
            state = pool.poll();
            if(state == null) {
                state = new JsonSerializerState(option);
            } else {
                recycle = true;
            }
        } else {
            state = new JsonSerializerState(option);
        }
        state.setWriteBuffer(writeBuffer);
        state.initMarshallableObject(instance);
        state.process();
        if(recycle) {
            state.reset();
            pool.offer(state);
        }
    }

    public void serializeArray(Object[] arr, WriteBuffer writeBuffer) {
        JsonSerializerState state;
        boolean recycle = false;
        if(pool != null) {
            state = pool.poll();
            if(state == null) {
                state = new JsonSerializerState(option);
            } else {
                recycle = true;
            }
        } else {
            state = new JsonSerializerState(option);
        }
        state.setWriteBuffer(writeBuffer);
        state.initArray(arr);
        state.process();
        if(recycle) {
            state.reset();
            pool.offer(state);
        }
    }

    public <T> void serializeCollection(Collection<T> collection, Class<T> elementType, WriteBuffer writeBuffer) {
        JsonSerializerState state;
        boolean recycle = false;
        if(pool != null) {
            state = pool.poll();
            if(state == null) {
                state = new JsonSerializerState(option);
            } else {
                recycle = true;
            }
        } else {
            state = new JsonSerializerState(option);
        }
        state.setWriteBuffer(writeBuffer);
        state.initCol(collection, elementType);
        state.process();
        if(recycle) {
            state.reset();
            pool.offer(state);
        }
    }

    public <K, V> void serializeMap(Map<K, V> map, Class<K> keyType, Class<V> valueType, WriteBuffer writeBuffer) {
        JsonSerializerState state;
        boolean recycle = false;
        if(pool != null) {
            state = pool.poll();
            if(state == null) {
                state = new JsonSerializerState(option);
            } else {
                recycle = true;
            }
        } else {
            state = new JsonSerializerState(option);
        }
        state.setWriteBuffer(writeBuffer);
        state.initMap(map, keyType, valueType);
        state.process();
        if(recycle) {
            state.reset();
            pool.offer(state);
        }
    }
}
