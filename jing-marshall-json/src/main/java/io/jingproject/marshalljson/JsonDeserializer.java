package io.jingproject.marshalljson;

import io.jingproject.common.ReadBuffer;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class JsonDeserializer {
    private static final int PRIMITIVE_ARR_BUFFER_SIZE = 16;
    private final JsonDeserializerOption option;

    public JsonDeserializer(JsonDeserializerOption option) {
        this.option = Objects.requireNonNull(option, "option must not be null");
    }

    public byte[] deserializeByteArray(ReadBuffer readBuffer) {
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        JsonDeserializeUtil.validateUtf8ReadBuffer(readBuffer);
        byte firstByte = JsonDeserializeUtil.nextFirstValuableByte(option, readBuffer);
        if(!JsonDeserializeUtil.validateJsonArrayStart(firstByte)) {
            throw new JsonDeserializerException("array start not found, got : " + firstByte);
        }
        JsonDeserializerContext context = new JsonDeserializerContext(0, PRIMITIVE_ARR_BUFFER_SIZE);
        return JsonDeserializeUtil.deserializeByteArray(option, readBuffer, context, firstByte);
    }

    public boolean[] deserializeBooleanArray(ReadBuffer readBuffer) {
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        JsonDeserializeUtil.validateUtf8ReadBuffer(readBuffer);
        byte firstByte = JsonDeserializeUtil.nextFirstValuableByte(option, readBuffer);
        if(!JsonDeserializeUtil.validateJsonArrayStart(firstByte)) {
            throw new JsonDeserializerException("array start not found, got : " + firstByte);
        }
        JsonDeserializerContext context = new JsonDeserializerContext(0, PRIMITIVE_ARR_BUFFER_SIZE);
        return JsonDeserializeUtil.deserializeBooleanArray(option, readBuffer, context, firstByte);
    }

    public short[] deserializeShortArray(ReadBuffer readBuffer) {
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        JsonDeserializeUtil.validateUtf8ReadBuffer(readBuffer);
        byte firstByte = JsonDeserializeUtil.nextFirstValuableByte(option, readBuffer);
        if(!JsonDeserializeUtil.validateJsonArrayStart(firstByte)) {
            throw new JsonDeserializerException("array start not found, got : " + firstByte);
        }
        JsonDeserializerContext context = new JsonDeserializerContext(0, PRIMITIVE_ARR_BUFFER_SIZE);
        return JsonDeserializeUtil.deserializeShortArray(option, readBuffer, context, firstByte);
    }

    public char[] deserializeCharArray(ReadBuffer readBuffer) {
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        JsonDeserializeUtil.validateUtf8ReadBuffer(readBuffer);
        byte firstByte = JsonDeserializeUtil.nextFirstValuableByte(option, readBuffer);
        if(!JsonDeserializeUtil.validateJsonArrayStart(firstByte)) {
            throw new JsonDeserializerException("array start not found, got : " + firstByte);
        }
        JsonDeserializerContext context = new JsonDeserializerContext(PRIMITIVE_ARR_BUFFER_SIZE, 0);
        return JsonDeserializeUtil.deserializeCharArray(option, readBuffer, context, firstByte);
    }

    public int[] deserializeIntArray(ReadBuffer readBuffer) {
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        JsonDeserializeUtil.validateUtf8ReadBuffer(readBuffer);
        byte firstByte = JsonDeserializeUtil.nextFirstValuableByte(option, readBuffer);
        if(!JsonDeserializeUtil.validateJsonArrayStart(firstByte)) {
            throw new JsonDeserializerException("array start not found, got : " + firstByte);
        }
        JsonDeserializerContext context = new JsonDeserializerContext(0, PRIMITIVE_ARR_BUFFER_SIZE);
        return JsonDeserializeUtil.deserializeIntArray(option, readBuffer, context, firstByte);
    }

    public long[] deserializeLongArray(ReadBuffer readBuffer) {
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        JsonDeserializeUtil.validateUtf8ReadBuffer(readBuffer);
        byte firstByte = JsonDeserializeUtil.nextFirstValuableByte(option, readBuffer);
        if(!JsonDeserializeUtil.validateJsonArrayStart(firstByte)) {
            throw new JsonDeserializerException("array start not found, got : " + firstByte);
        }
        JsonDeserializerContext context = new JsonDeserializerContext(0, PRIMITIVE_ARR_BUFFER_SIZE);
        return JsonDeserializeUtil.deserializeLongArray(option, readBuffer, context, firstByte);
    }

    public float[] deserializeFloatArray(ReadBuffer readBuffer) {
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        JsonDeserializeUtil.validateUtf8ReadBuffer(readBuffer);
        byte firstByte = JsonDeserializeUtil.nextFirstValuableByte(option, readBuffer);
        if(!JsonDeserializeUtil.validateJsonArrayStart(firstByte)) {
            throw new JsonDeserializerException("array start not found, got : " + firstByte);
        }
        JsonDeserializerContext context = new JsonDeserializerContext(0, PRIMITIVE_ARR_BUFFER_SIZE);
        return JsonDeserializeUtil.deserializeFloatArray(option, readBuffer, context, firstByte);
    }

    public double[] deserializeDoubleArray(ReadBuffer readBuffer) {
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        JsonDeserializeUtil.validateUtf8ReadBuffer(readBuffer);
        byte firstByte = JsonDeserializeUtil.nextFirstValuableByte(option, readBuffer);
        if(!JsonDeserializeUtil.validateJsonArrayStart(firstByte)) {
            throw new JsonDeserializerException("array start not found, got : " + firstByte);
        }
        JsonDeserializerContext context = new JsonDeserializerContext(0, PRIMITIVE_ARR_BUFFER_SIZE);
        return JsonDeserializeUtil.deserializeDoubleArray(option, readBuffer, context, firstByte);
    }

    @SuppressWarnings("unchecked")
    public <T> T deserializeMarshallableObject(Class<T> type, ReadBuffer readBuffer) {
        Objects.requireNonNull(type, "marshallable type must not be null");
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        JsonDeserializeUtil.validateUtf8ReadBuffer(readBuffer);
        JsonDeserializerState state = new JsonDeserializerState(option, readBuffer);
        state.initMarshallableType(type);
        return (T) state.process();
    }

    public <T> List<T> deserializeList(Class<T> type, ReadBuffer readBuffer) {
        Objects.requireNonNull(type, "list type must not be null");
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        JsonDeserializeUtil.validateUtf8ReadBuffer(readBuffer);
        return null;
    }

    public <K, V> Map<K, V> deserializeMap(Class<K> keyType, Class<V> valueType, ReadBuffer readBuffer) {
        Objects.requireNonNull(keyType, "key type must not be null");
        Objects.requireNonNull(valueType, "value type must not be null");
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        JsonDeserializeUtil.validateUtf8ReadBuffer(readBuffer);
        return null;
    }
}
