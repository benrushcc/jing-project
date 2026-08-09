package io.jingproject.marshalljson;

import io.jingproject.common.ReadBuffer;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class JsonDeserializer {
    private final JsonDeserializerOption option;

    public JsonDeserializer(JsonDeserializerOption option) {
        this.option = Objects.requireNonNull(option, "option must not be null");
    }

    public byte[] deserializeByteArray(ReadBuffer readBuffer) {
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        Utf8Validator.validate(readBuffer);
        JsonDeserializerContext context = new JsonDeserializerContext(option, readBuffer);
        byte firstByte = context.nextFirstValuableByte();
        if (firstByte != (byte) '[') {
            throw new JsonDeserializerException("array start not found, got : " + firstByte);
        }
        return context.deserializeByteArray(firstByte);
    }

    public boolean[] deserializeBooleanArray(ReadBuffer readBuffer) {
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        Utf8Validator.validate(readBuffer);
        JsonDeserializerContext context = new JsonDeserializerContext(option, readBuffer);
        byte firstByte = context.nextFirstValuableByte();
        if (firstByte != (byte) '[') {
            throw new JsonDeserializerException("array start not found, got : " + firstByte);
        }
        return context.deserializeBooleanArray(firstByte);
    }

    public short[] deserializeShortArray(ReadBuffer readBuffer) {
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        Utf8Validator.validate(readBuffer);
        JsonDeserializerContext context = new JsonDeserializerContext(option, readBuffer);
        byte firstByte = context.nextFirstValuableByte();
        if (firstByte != (byte) '[') {
            throw new JsonDeserializerException("array start not found, got : " + firstByte);
        }
        return context.deserializeShortArray(firstByte);
    }

    public char[] deserializeCharArray(ReadBuffer readBuffer) {
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        Utf8Validator.validate(readBuffer);
        JsonDeserializerContext context = new JsonDeserializerContext(option, readBuffer);
        byte firstByte = context.nextFirstValuableByte();
        if (firstByte != (byte) '[') {
            throw new JsonDeserializerException("array start not found, got : " + firstByte);
        }
        return context.deserializeCharArray(firstByte);
    }

    public int[] deserializeIntArray(ReadBuffer readBuffer) {
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        Utf8Validator.validate(readBuffer);
        JsonDeserializerContext context = new JsonDeserializerContext(option, readBuffer);
        byte firstByte = context.nextFirstValuableByte();
        if (firstByte != (byte) '[') {
            throw new JsonDeserializerException("array start not found, got : " + firstByte);
        }
        return context.deserializeIntArray(firstByte);
    }

    public long[] deserializeLongArray(ReadBuffer readBuffer) {
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        Utf8Validator.validate(readBuffer);
        JsonDeserializerContext context = new JsonDeserializerContext(option, readBuffer);
        byte firstByte = context.nextFirstValuableByte();
        if (firstByte != (byte) '[') {
            throw new JsonDeserializerException("array start not found, got : " + firstByte);
        }
        return context.deserializeLongArray(firstByte);
    }

    public float[] deserializeFloatArray(ReadBuffer readBuffer) {
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        Utf8Validator.validate(readBuffer);
        JsonDeserializerContext context = new JsonDeserializerContext(option, readBuffer);
        byte firstByte = context.nextFirstValuableByte();
        if (firstByte != (byte) '[') {
            throw new JsonDeserializerException("array start not found, got : " + firstByte);
        }
        return context.deserializeFloatArray(firstByte);
    }

    public double[] deserializeDoubleArray(ReadBuffer readBuffer) {
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        Utf8Validator.validate(readBuffer);
        JsonDeserializerContext context = new JsonDeserializerContext(option, readBuffer);
        byte firstByte = context.nextFirstValuableByte();
        if (firstByte != (byte) '[') {
            throw new JsonDeserializerException("array start not found, got : " + firstByte);
        }
        return context.deserializeDoubleArray(firstByte);
    }

    @SuppressWarnings("unchecked")
    public <T> T deserializeMarshallableObject(Class<T> type, ReadBuffer readBuffer) {
        Objects.requireNonNull(type, "marshallable type must not be null");
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        Utf8Validator.validate(readBuffer);
//        JsonDeserializerState state = new JsonDeserializerState(option, readBuffer);
//        state.initMarshallableType(type);
//        return (T) state.process();
        return null;
    }

    public <T> List<T> deserializeList(Class<T> type, ReadBuffer readBuffer) {
        Objects.requireNonNull(type, "list type must not be null");
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        Utf8Validator.validate(readBuffer);
        return null;
    }

    public <K, V> Map<K, V> deserializeMap(Class<K> keyType, Class<V> valueType, ReadBuffer readBuffer) {
        Objects.requireNonNull(keyType, "key type must not be null");
        Objects.requireNonNull(valueType, "value type must not be null");
        Objects.requireNonNull(readBuffer, "readBuffer must not be null");
        Utf8Validator.validate(readBuffer);
        return null;
    }
}
