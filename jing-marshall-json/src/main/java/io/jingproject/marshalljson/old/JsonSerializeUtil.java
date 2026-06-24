package io.jingproject.marshalljson.old;

import io.jingproject.common.Utils;
import io.jingproject.common.WriteBuffer;
import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.MarshallInfo;
import io.jingproject.marshall.Marshalls;
import io.jingproject.marshalljson.*;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

public final class JsonSerializeUtil {
    private static final JsonSerializerNodeResult CONTINUE = new JsonSerializerNodeResult.JsonSerializerNodeContinue();
    private static final JsonSerializerNodeResult FINISHED = new JsonSerializerNodeResult.JsonSerializerNodeFinished();

    public static JsonSerializerNodeResult serializerContinue() {
        return CONTINUE;
    }

    public static JsonSerializerNodeResult serializerFinished() {
        return FINISHED;
    }

    private static final byte BYTE_quote = (byte) '"';
    private static final byte BYTE_rsolidus = (byte) '\\';
    private static final byte BYTE_solidus = (byte) '/';
    private static final byte BYTE_b = (byte) 'b';
    private static final byte BYTE_f = (byte) 'f';
    private static final byte BYTE_n = (byte) 'n';
    private static final byte BYTE_r = (byte) 'r';
    private static final byte BYTE_t = (byte) 't';
    private static final byte BYTE_u = (byte) 'u';
    private static final byte BYTE_a = (byte) 'a';
    private static final byte BYTE_e = (byte) 'e';
    private static final byte BYTE_l =  (byte) 'l';
    private static final byte BYTE_s = (byte) 's';
    private static final byte BYTE_zero = (byte) '0';
    private static final byte BYTE_bracket = (byte) '[';
    private static final byte BYTE_rbracket = (byte) ']';
    private static final byte BYTE_brace = (byte) '{';
    private static final byte BYTE_rbrace = (byte) '}';
    private static final byte BYTE_comma = (byte) ',';
    private static final byte BYTE_colon = (byte) ':';
    private static final byte BYTE_space = (byte) ' ';
    private static final byte BYTE_lf = (byte) '\n';

    private static final byte[] WRITER_ESCAPE_TABLE = new byte[Byte.MAX_VALUE - Byte.MIN_VALUE + 1];
    private static final short ESCAPED_quote = Utils.compact(BYTE_rsolidus, BYTE_quote);
    private static final short ESCAPED_rsolidus = Utils.compact(BYTE_rsolidus, BYTE_rsolidus);
    private static final short ESCAPED_solidus = Utils.compact(BYTE_rsolidus, BYTE_solidus);
    private static final short ESCAPED_b = Utils.compact(BYTE_rsolidus, BYTE_b);
    private static final short ESCAPED_f = Utils.compact(BYTE_rsolidus, BYTE_f);
    private static final short ESCAPED_n = Utils.compact(BYTE_rsolidus, BYTE_n);
    private static final short ESCAPED_r = Utils.compact(BYTE_rsolidus, BYTE_r);
    private static final short ESCAPED_t = Utils.compact(BYTE_rsolidus, BYTE_t);
    private static final byte[] HEX_BYTES = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
    private static final int COMPACT_PREFIX_u = Utils.compact(Utils.compact(BYTE_rsolidus, BYTE_u), Utils.compact(BYTE_zero, BYTE_zero)); // value of \\u00
    private static final short COMPACT_EMPTY_STR = Utils.compact(BYTE_quote, BYTE_quote);
    private static final short COMPACT_EMPTY_ARR = Utils.compact(BYTE_bracket, BYTE_rbracket);
    private static final short COMPACT_KV_SEP = Utils.compact(BYTE_colon, BYTE_space);
    private static final int COMPACT_NULL = Utils.compact(Utils.compact(BYTE_n, BYTE_u), Utils.compact(BYTE_l, BYTE_l));
    private static final int COMPACT_TRUE = Utils.compact(Utils.compact(BYTE_t, BYTE_r), Utils.compact(BYTE_u, BYTE_e)); // value of true
    private static final int COMPACT_ALSE = Utils.compact(Utils.compact(BYTE_a, BYTE_l), Utils.compact(BYTE_s, BYTE_e));

    static {
        for(int i = 0x00; i < 0x1f; i++) {
            WRITER_ESCAPE_TABLE[i] = BYTE_u;
        }
        WRITER_ESCAPE_TABLE[0x22] = BYTE_quote;   // \"
        WRITER_ESCAPE_TABLE[0x5C] = BYTE_rsolidus; // \\
        WRITER_ESCAPE_TABLE[0x2F] = BYTE_solidus;  // \/
        WRITER_ESCAPE_TABLE[0x08] = BYTE_b;  // \b
        WRITER_ESCAPE_TABLE[0x0C] = BYTE_f;  // \f
        WRITER_ESCAPE_TABLE[0x0A] = BYTE_n;  // \n
        WRITER_ESCAPE_TABLE[0x0D] = BYTE_r;  // \r
        WRITER_ESCAPE_TABLE[0x09] = BYTE_t;  // \t
    }

    private JsonSerializeUtil() {
        throw new UnsupportedOperationException("utility class");
    }

    public static final JsonValueSerializer BYTE_SERIALIZER = (o, _, w, _) -> {
        serializeByte((Byte) o, w);
        return null;
    };
    public static final JsonValueSerializer BOOLEAN_SERIALIZER = (o, _, w, _) -> {
        serializeBoolean((Boolean) o, w);
        return null;
    };
    public static final JsonValueSerializer SHORT_SERIALIZER = (o, _, w, _) -> {
        serializeShort((Short) o, w);
        return null;
    };
    public static final JsonValueSerializer CHARACTER_SERIALIZER = (o, _, w, _) -> {
        serializeChar((Character) o, w);
        return null;
    };
    public static final JsonValueSerializer INTEGER_SERIALIZER = (o, _, w, _) -> {
        serializeInt((Integer) o, w);
        return null;
    };
    public static final JsonValueSerializer LONG_SERIALIZER = (o, _, w, _) -> {
        serializeLong((Long) o, w);
        return null;
    };
    public static final JsonValueSerializer FLOAT_SERIALIZER = (o, _, w, _) -> {
        serializeFloat((Float) o, w);
        return null;
    };
    public static final JsonValueSerializer DOUBLE_SERIALIZER = (o, _, w, _) -> {
        serializeDouble((Double) o, w);
        return null;
    };
    public static final JsonValueSerializer BYTE_ARRAY_SERIALIZER = (o, op, w, indent) -> {
        serializeByteArray((byte[]) o, indent, op.indentationLevel(), w);
        return null;
    };
    public static final JsonValueSerializer BOOLEAN_ARRAY_SERIALIZER = (o, op, w, indent) -> {
        serializeBooleanArray((boolean[]) o, indent, op.indentationLevel(), w);
        return null;
    };
    public static final JsonValueSerializer SHORT_ARRAY_SERIALIZER = (o, op, w, indent) -> {
        serializeShortArray((short[]) o, indent, op.indentationLevel(), w);
        return null;
    };
    public static final JsonValueSerializer CHAR_ARRAY_SERIALIZER = (o, op, w, indent) -> {
        serializeCharArray((char[]) o, indent, op.indentationLevel(), w);
        return null;
    };
    public static final JsonValueSerializer INT_ARRAY_SERIALIZER = (o, op, w, indent) -> {
        serializeIntArray((int[]) o, indent, op.indentationLevel(), w);
        return null;
    };
    public static final JsonValueSerializer LONG_ARRAY_SERIALIZER = (o, op, w, indent) -> {
        serializeLongArray((long[]) o, indent, op.indentationLevel(), w);
        return null;
    };
    public static final JsonValueSerializer FLOAT_ARRAY_SERIALIZER = (o, op, w, indent) -> {
        serializeFloatArray((float[]) o, indent, op.indentationLevel(), w);
        return null;
    };
    public static final JsonValueSerializer DOUBLE_ARRAY_SERIALIZER = (o, op, w, indent) -> {
        serializeDoubleArray((double[]) o, indent, op.indentationLevel(), w);
        return null;
    };
    public static final JsonValueSerializer BYTE_WRAPPER_ARRAY_SERIALIZER = (o, op, w, indent) -> {
        serializeByteWrapperArray((Byte[]) o, indent, op.indentationLevel(), w);
        return null;
    };
    public static final JsonValueSerializer BOOLEAN_WRAPPER_ARRAY_SERIALIZER = (o, op, w, indent) -> {
        serializeBooleanWrapperArray((Boolean[]) o, indent, op.indentationLevel(), w);
        return null;
    };
    public static final JsonValueSerializer SHORT_WRAPPER_ARRAY_SERIALIZER = (o, op, w, indent) -> {
        serializeShortWrapperArray((Short[]) o, indent, op.indentationLevel(), w);
        return null;
    };
    public static final JsonValueSerializer CHARACTER_WRAPPER_ARRAY_SERIALIZER = (o, op, w, indent) -> {
        serializeCharWrapperArray((Character[]) o, indent, op.indentationLevel(), w);
        return null;
    };
    public static final JsonValueSerializer INTEGER_WRAPPER_ARRAY_SERIALIZER = (o, op, w, indent) -> {
        serializeIntWrapperArray((Integer[]) o, indent, op.indentationLevel(), w);
        return null;
    };
    public static final JsonValueSerializer LONG_WRAPPER_ARRAY_SERIALIZER = (o, op, w, indent) -> {
        serializeLongWrapperArray((Long[]) o, indent, op.indentationLevel(), w);
        return null;
    };
    public static final JsonValueSerializer FLOAT_WRAPPER_ARRAY_SERIALIZER = (o, op, w, indent) -> {
        serializeFloatWrapperArray((Float[]) o, indent, op.indentationLevel(), w);
        return null;
    };
    public static final JsonValueSerializer DOUBLE_WRAPPER_ARRAY_SERIALIZER = (o, op, w, indent) -> {
        serializeDoubleWrapperArray((Double[]) o, indent, op.indentationLevel(), w);
        return null;
    };
    public static final JsonValueSerializer CHAR_SEQUENCE_SERIALIZER = (o, _, w, _) -> {
        serializeEscapedCharSequence((CharSequence) o, w);
        return null;
    };
    public static final JsonValueSerializer JSON_PRIMITIVE_TYPE_SERIALIZER = (o, _, w, _) -> {
        serializeJsonPrimitiveType((JsonPrimitiveType) o, w);
        return null;
    };
    public static final JsonValueSerializer JSON_BOOL_TYPE_SERIALIZER = (o, _, w, _) -> {
        serializeJsonBoolType((JsonBoolType) o, w);
        return null;
    };
    public static final JsonValueSerializer JSON_NUMBER_TYPE_SERIALIZER = (o, _, w, _) -> {
        serializeJsonNumberType((JsonNumberType) o, w);
        return null;
    };
    public static final JsonValueSerializer JSON_STR_TYPE_SERIALIZER = (o, _, w, _) -> {
        serializeJsonStrType((JsonStrType) o, w);
        return null;
    };

    public static JsonValueSerializer makeObjSerializer(MarshallFacade fc) {
        return (o, op, w, indent) ->
                new JsonSerializerObjNode(op, w, indent, o, fc);
    }

    public static JsonValueSerializer makeArraySerializer(JsonValueSerializer arrayValueSerializer) {
        return (o, op, w, indent) ->
                new JsonSerializerArrayNode(op, w, indent + 1, (Object[]) o, arrayValueSerializer);
    }

    public static JsonValueSerializer makeCollectionSerializer(JsonValueSerializer collectionValueSerializer) {
        return (o, op, w, indent) ->
                new JsonSerializerArrayNode(op, w, indent + 1, (Collection<?>) o, collectionValueSerializer);
    }

    public static JsonValueSerializer makeMapSerializer(JsonValueSerializer mapValueSerializer) {
        return (o, op, w, indent) ->
                new JsonSerializerMapNode(op, w, indent + 1, (Map<?, ?>) o, mapValueSerializer);
    }

    public static void serializeObjStart(WriteBuffer writeBuffer) {
        writeBuffer.writeByte(BYTE_brace);
    }

    public static void serializeObjEnd(WriteBuffer writeBuffer) {
        writeBuffer.writeByte(BYTE_rbrace);
    }
    
    public static void serializeArrayStart(WriteBuffer writeBuffer) {
        writeBuffer.writeByte(BYTE_bracket);
    }
    
    public static void serializeArrayEnd(WriteBuffer writeBuffer) {
        writeBuffer.writeByte(BYTE_rbracket);
    }
    
    public static void serializeQuote(WriteBuffer writeBuffer) {
        writeBuffer.writeByte(BYTE_quote);
    }
    
    public static void serializeComma(WriteBuffer writeBuffer) {
        writeBuffer.writeByte(BYTE_comma);
    }

    public static void serializeKvSep(WriteBuffer writeBuffer) {
        writeBuffer.writeShort(COMPACT_KV_SEP);
    }

    public static void serializeEscapedByte(byte v, WriteBuffer writeBuffer) {
        assert writeBuffer != null;
        switch (v) {
            case BYTE_quote    -> writeBuffer.writeShort(ESCAPED_quote);
            case BYTE_rsolidus -> writeBuffer.writeShort(ESCAPED_rsolidus);
            case BYTE_solidus  -> writeBuffer.writeShort(ESCAPED_solidus);
            case BYTE_b        -> writeBuffer.writeShort(ESCAPED_b);
            case BYTE_f        -> writeBuffer.writeShort(ESCAPED_f);
            case BYTE_n        -> writeBuffer.writeShort(ESCAPED_n);
            case BYTE_r        -> writeBuffer.writeShort(ESCAPED_r);
            case BYTE_t        -> writeBuffer.writeShort(ESCAPED_t);
            case BYTE_u        -> {
                // v is in the range [0x00, 0x1F], so the conversion is guaranteed to be safe.
                writeBuffer.writeInt(COMPACT_PREFIX_u);
                writeBuffer.writeBytes(HEX_BYTES[v >>> 4], HEX_BYTES[v & 0xf]);
            }
            default -> throw new AssertionError();
        }
    }

    public static void serializeNonAsciiCodePointInUtf8(int codePoint, WriteBuffer writeBuffer) {
        assert Character.isValidCodePoint(codePoint) && codePoint >= 0x80;
        if (codePoint < 0x800) {
            writeBuffer.writeBytes((byte) (0xC0 | (codePoint >> 6)), (byte) (0x80 | (codePoint & 0x3F)));
        } else if (codePoint < 0x10000) {
            writeBuffer.writeBytes((byte) (0xE0 | (codePoint >> 12)),
                    (byte) (0x80 | ((codePoint >> 6) & 0x3F)),
                    (byte) (0x80 | (codePoint & 0x3F)));
        } else {
            writeBuffer.writeBytes((byte) (0xF0 | (codePoint >> 18)),
                    (byte) (0x80 | ((codePoint >> 12) & 0x3F)),
                    (byte) (0x80 | ((codePoint >> 6) & 0x3F)),
                    (byte) (0x80 | (codePoint & 0x3F)));
        }
    }

    public static void serializeAsciiByte(byte b, WriteBuffer writeBuffer) {
        byte v = WRITER_ESCAPE_TABLE[Byte.toUnsignedInt(b)];
        if(v == 0) {
            writeBuffer.writeByte(b);
        } else {
            serializeEscapedByte(v, writeBuffer);
        }
    }

    public static void serializeNull(WriteBuffer writeBuffer) {
        writeBuffer.writeInt(COMPACT_NULL);
    }

    public static void serializeByte(byte value, WriteBuffer writeBuffer) {
        JsonNumberUtil.writeInt(value, writeBuffer);
    }

    public static void serializeBoolean(boolean value, WriteBuffer writeBuffer) {
        if(value) {
            writeBuffer.writeInt(COMPACT_TRUE);
        } else {
            writeBuffer.writeByte(BYTE_f);
            writeBuffer.writeInt(COMPACT_ALSE);
        }
    }

    public static void serializeShort(short value, WriteBuffer writeBuffer) {
        JsonNumberUtil.writeInt(value, writeBuffer);
    }

    public static void serializeChar(char value, WriteBuffer writeBuffer) {
        serializeQuote(writeBuffer);
        if(Character.isSurrogate(value)) {
            throw new IllegalArgumentException("surrogates not supported");
        }
        if(value < 0x80) {
            serializeAsciiByte((byte) value, writeBuffer);
        } else {
            serializeNonAsciiCodePointInUtf8(value, writeBuffer);
        }
        serializeQuote(writeBuffer);
    }

    public static void serializeInt(int value, WriteBuffer writeBuffer) {
        JsonNumberUtil.writeInt(value, writeBuffer);
    }

    public static void serializeLong(long value, WriteBuffer writeBuffer) {
        JsonNumberUtil.writeLong(value, writeBuffer);
    }

    public static void serializeFloat(float value, WriteBuffer writeBuffer) {
        JsonNumberUtil.writeFloat(value, writeBuffer);
    }

    public static void serializeDouble(double value, WriteBuffer writeBuffer) {
        JsonNumberUtil.writeDouble(value, writeBuffer);
    }

    public static void serializeIndent(int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert indent >= 1 && indent <= JsonSerializerOption.HARD_MAX_SIZE && jsonIndentationLevel != null && writeBuffer != null;
        switch (jsonIndentationLevel) {
            case NONE -> {}
            case TWO -> {
                writeBuffer.writeByte(BYTE_lf);
                writeBuffer.writeRepeated(BYTE_space, indent << 1);
            }
            case FOUR -> {
                writeBuffer.writeByte(BYTE_lf);
                writeBuffer.writeRepeated(BYTE_space, indent << 2);
            }
            default -> throw new AssertionError();
        }
    }

    public static void serializeByteArray(byte[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && writeBuffer != null;
        if(arr.length == 0) {
            writeBuffer.writeShort(COMPACT_EMPTY_ARR);
            return ;
        }
        serializeArrayStart(writeBuffer);
        for(int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1, jsonIndentationLevel, writeBuffer);
            serializeByte(arr[i], writeBuffer);
            if(i != arr.length - 1) {
                serializeComma(writeBuffer);
            }
        }
        serializeIndent(indent, jsonIndentationLevel, writeBuffer);
        serializeArrayEnd(writeBuffer);
    }

    public static void serializeByteWrapperArray(Byte[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && writeBuffer != null;
        if(arr.length == 0) {
            writeBuffer.writeShort(COMPACT_EMPTY_ARR);
            return ;
        }
        serializeArrayStart(writeBuffer);
        for(int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1, jsonIndentationLevel, writeBuffer);
            Byte b = arr[i];
            if(b == null) {
                serializeNull(writeBuffer);
            } else {
                serializeByte(b, writeBuffer);
            }
            if(i != arr.length - 1) {
                serializeComma(writeBuffer);
            }
        }
        serializeIndent(indent, jsonIndentationLevel, writeBuffer);
        serializeArrayEnd(writeBuffer);
    }

    public static void serializeBooleanArray(boolean[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && writeBuffer != null;
        if (arr.length == 0) {
            writeBuffer.writeShort(COMPACT_EMPTY_ARR);
            return;
        }
        serializeArrayStart(writeBuffer);
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1, jsonIndentationLevel, writeBuffer);
            serializeBoolean(arr[i], writeBuffer);
            if (i != arr.length - 1) {
                serializeComma(writeBuffer);
            }
        }
        serializeIndent(indent, jsonIndentationLevel, writeBuffer);
        serializeArrayEnd(writeBuffer);
    }

    public static void serializeBooleanWrapperArray(Boolean[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && writeBuffer != null;
        if (arr.length == 0) {
            writeBuffer.writeShort(COMPACT_EMPTY_ARR);
            return;
        }
        serializeArrayStart(writeBuffer);
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1, jsonIndentationLevel, writeBuffer);
            Boolean b = arr[i];
            if (b == null) {
                serializeNull(writeBuffer);
            } else {
                serializeBoolean(b, writeBuffer);
            }
            if (i != arr.length - 1) {
                serializeComma(writeBuffer);
            }
        }
        serializeIndent(indent, jsonIndentationLevel, writeBuffer);
        serializeArrayEnd(writeBuffer);
    }

    public static void serializeShortArray(short[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && writeBuffer != null;
        if (arr.length == 0) {
            writeBuffer.writeShort(COMPACT_EMPTY_ARR);
            return;
        }
        serializeArrayStart(writeBuffer);
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1, jsonIndentationLevel, writeBuffer);
            serializeShort(arr[i], writeBuffer);
            if (i != arr.length - 1) {
                serializeComma(writeBuffer);
            }
        }
        serializeIndent(indent, jsonIndentationLevel, writeBuffer);
        serializeArrayEnd(writeBuffer);
    }

    public static void serializeShortWrapperArray(Short[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && writeBuffer != null;
        if (arr.length == 0) {
            writeBuffer.writeShort(COMPACT_EMPTY_ARR);
            return;
        }
        serializeArrayStart(writeBuffer);
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1, jsonIndentationLevel, writeBuffer);
            Short s = arr[i];
            if (s == null) {
                serializeNull(writeBuffer);
            } else {
                serializeShort(s, writeBuffer);
            }
            if (i != arr.length - 1) {
                serializeComma(writeBuffer);
            }
        }
        serializeIndent(indent, jsonIndentationLevel, writeBuffer);
        serializeArrayEnd(writeBuffer);
    }

    public static void serializeCharArray(char[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && writeBuffer != null;
        if (arr.length == 0) {
            writeBuffer.writeShort(COMPACT_EMPTY_ARR);
            return;
        }
        serializeArrayStart(writeBuffer);
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1, jsonIndentationLevel, writeBuffer);
            serializeChar(arr[i], writeBuffer);
            if (i != arr.length - 1) {
                serializeComma(writeBuffer);
            }
        }
        serializeIndent(indent, jsonIndentationLevel, writeBuffer);
        serializeArrayEnd(writeBuffer);
    }

    public static void serializeCharWrapperArray(Character[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && writeBuffer != null;
        if (arr.length == 0) {
            writeBuffer.writeShort(COMPACT_EMPTY_ARR);
            return;
        }
        serializeArrayStart(writeBuffer);
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1, jsonIndentationLevel, writeBuffer);
            Character c = arr[i];
            if (c == null) {
                serializeNull(writeBuffer);
            } else {
                serializeChar(c, writeBuffer);
            }
            if (i != arr.length - 1) {
                serializeComma(writeBuffer);
            }
        }
        serializeIndent(indent, jsonIndentationLevel, writeBuffer);
        serializeArrayEnd(writeBuffer);
    }

    public static void serializeIntArray(int[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && writeBuffer != null;
        if (arr.length == 0) {
            writeBuffer.writeShort(COMPACT_EMPTY_ARR);
            return;
        }
        serializeArrayStart(writeBuffer);
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1, jsonIndentationLevel, writeBuffer);
            serializeInt(arr[i], writeBuffer);
            if (i != arr.length - 1) {
                serializeComma(writeBuffer);
            }
        }
        serializeIndent(indent, jsonIndentationLevel, writeBuffer);
        serializeArrayEnd(writeBuffer);
    }

    public static void serializeIntWrapperArray(Integer[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && writeBuffer != null;
        if (arr.length == 0) {
            writeBuffer.writeShort(COMPACT_EMPTY_ARR);
            return;
        }
        serializeArrayStart(writeBuffer);
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1, jsonIndentationLevel, writeBuffer);
            Integer n = arr[i];
            if (n == null) {
                serializeNull(writeBuffer);
            } else {
                serializeInt(n, writeBuffer);
            }
            if (i != arr.length - 1) {
                serializeComma(writeBuffer);
            }
        }
        serializeIndent(indent, jsonIndentationLevel, writeBuffer);
        serializeArrayEnd(writeBuffer);
    }

    public static void serializeLongArray(long[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && writeBuffer != null;
        if (arr.length == 0) {
            writeBuffer.writeShort(COMPACT_EMPTY_ARR);
            return;
        }
        serializeArrayStart(writeBuffer);
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1, jsonIndentationLevel, writeBuffer);
            serializeLong(arr[i], writeBuffer);
            if (i != arr.length - 1) {
                serializeComma(writeBuffer);
            }
        }
        serializeIndent(indent, jsonIndentationLevel, writeBuffer);
        serializeArrayEnd(writeBuffer);
    }

    public static void serializeLongWrapperArray(Long[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && writeBuffer != null;
        if (arr.length == 0) {
            writeBuffer.writeShort(COMPACT_EMPTY_ARR);
            return;
        }
        serializeArrayStart(writeBuffer);
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1, jsonIndentationLevel, writeBuffer);
            Long n = arr[i];
            if (n == null) {
                serializeNull(writeBuffer);
            } else {
                serializeLong(n, writeBuffer);
            }
            if (i != arr.length - 1) {
                serializeComma(writeBuffer);
            }
        }
        serializeIndent(indent, jsonIndentationLevel, writeBuffer);
        serializeArrayEnd(writeBuffer);
    }

    public static void serializeFloatArray(float[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && writeBuffer != null;
        if (arr.length == 0) {
            writeBuffer.writeShort(COMPACT_EMPTY_ARR);
            return;
        }
        serializeArrayStart(writeBuffer);
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1, jsonIndentationLevel, writeBuffer);
            serializeFloat(arr[i], writeBuffer);
            if (i != arr.length - 1) {
                serializeComma(writeBuffer);
            }
        }
        serializeIndent(indent, jsonIndentationLevel, writeBuffer);
        serializeArrayEnd(writeBuffer);
    }

    public static void serializeFloatWrapperArray(Float[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && writeBuffer != null;
        if (arr.length == 0) {
            writeBuffer.writeShort(COMPACT_EMPTY_ARR);
            return;
        }
        serializeArrayStart(writeBuffer);
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1, jsonIndentationLevel, writeBuffer);
            Float n = arr[i];
            if (n == null) {
                serializeNull(writeBuffer);
            } else {
                serializeFloat(n, writeBuffer);
            }
            if (i != arr.length - 1) {
                serializeComma(writeBuffer);
            }
        }
        serializeIndent(indent, jsonIndentationLevel, writeBuffer);
        serializeArrayEnd(writeBuffer);
    }

    public static void serializeDoubleArray(double[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && writeBuffer != null;
        if (arr.length == 0) {
            writeBuffer.writeShort(COMPACT_EMPTY_ARR);
            return;
        }
        serializeArrayStart(writeBuffer);
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1, jsonIndentationLevel, writeBuffer);
            serializeDouble(arr[i], writeBuffer);
            if (i != arr.length - 1) {
                serializeComma(writeBuffer);
            }
        }
        serializeIndent(indent, jsonIndentationLevel, writeBuffer);
        serializeArrayEnd(writeBuffer);
    }

    public static void serializeDoubleWrapperArray(Double[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && writeBuffer != null;
        if (arr.length == 0) {
            writeBuffer.writeShort(COMPACT_EMPTY_ARR);
            return;
        }
        serializeArrayStart(writeBuffer);
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1, jsonIndentationLevel, writeBuffer);
            Double n = arr[i];
            if (n == null) {
                serializeNull(writeBuffer);
            } else {
                serializeDouble(n, writeBuffer);
            }
            if (i != arr.length - 1) {
                serializeComma(writeBuffer);
            }
        }
        serializeIndent(indent, jsonIndentationLevel, writeBuffer);
        serializeArrayEnd(writeBuffer);
    }

    public static void serializeEscapedUtf8Bytes(byte[] utf8Bytes, WriteBuffer writeBuffer) {
        assert utf8Bytes != null && writeBuffer != null;
        if(utf8Bytes.length == 0) {
            writeBuffer.writeShort(COMPACT_EMPTY_STR);
            return ;
        }
        serializeQuote(writeBuffer);
        int start = 0;
        for (int index = 0; index < utf8Bytes.length; index++) {
            byte b = utf8Bytes[index];
            byte v = WRITER_ESCAPE_TABLE[Byte.toUnsignedInt(b)];
            if(v == 0) {
                continue ;
            }
            if(index > start) {
                writeBuffer.writeBytes(utf8Bytes, start, index - start);
            }
            serializeEscapedByte(v, writeBuffer);
            start = index + 1;
        }
        if(start != utf8Bytes.length) {
            writeBuffer.writeBytes(utf8Bytes, start, utf8Bytes.length - start);
        }
        serializeQuote(writeBuffer);
    }

    public static void serializeEscapedCharSequence(CharSequence charSequence, WriteBuffer writeBuffer) {
        assert charSequence != null && writeBuffer != null;
        int len = charSequence.length();
        if(len == 0) {
            writeBuffer.writeShort(COMPACT_EMPTY_STR);
            return ;
        }
        serializeQuote(writeBuffer);
        for(int index = 0; index < len; index++) {
            char c = charSequence.charAt(index);
            int codePoint;
            if(Character.isHighSurrogate(c)) {
                if(++index == len) {
                    throw new IllegalArgumentException("invalid high surrogate without low surrogate characters : " + c);
                }
                char c2 = charSequence.charAt(index);
                if(!Character.isLowSurrogate(c2)) {
                    throw new IllegalArgumentException("invalid low surrogate : " + c2);
                }
                codePoint = Character.toCodePoint(c, c2);
            } else if(Character.isLowSurrogate(c)) {
                throw new IllegalArgumentException("invalid low surrogate without high surrogate: " + c);
            } else {
                codePoint = c;
            }
            if(codePoint < 0x80) {
                serializeAsciiByte((byte) codePoint, writeBuffer);
            } else {
                serializeNonAsciiCodePointInUtf8(codePoint, writeBuffer);
            }
        }
        serializeQuote(writeBuffer);
    }

    public static void serializeJsonPrimitiveType(JsonPrimitiveType jsonPrimitiveType, WriteBuffer writeBuffer) {
        assert jsonPrimitiveType != null && writeBuffer != null;
        switch (jsonPrimitiveType) {
            case JsonBoolType jsonBoolType -> serializeJsonBoolType(jsonBoolType, writeBuffer);
            case JsonNumberType jsonNumberType -> serializeJsonNumberType(jsonNumberType, writeBuffer);
            case JsonStrType jsonStrType -> serializeJsonStrType(jsonStrType, writeBuffer);
            default -> throw new AssertionError();
        }
    }

    public static void serializeJsonBoolType(JsonBoolType jsonBoolType, WriteBuffer writeBuffer) {
        assert jsonBoolType != null && writeBuffer != null;
        serializeBoolean(jsonBoolType.data(), writeBuffer);
    }

    public static void serializeJsonNumberType(JsonNumberType jsonNumberType, WriteBuffer writeBuffer) {
        assert jsonNumberType != null && writeBuffer != null;
        writeBuffer.writeBytes(jsonNumberType.data());
    }

    public static void serializeJsonStrType(JsonStrType jsonStrType, WriteBuffer writeBuffer) {
        assert jsonStrType != null && writeBuffer != null;
        serializeEscapedCharSequence(jsonStrType.data(), writeBuffer);
    }

    private static final Map<Class<?>, JsonValueSerializer> BUILTIN_OBJ_SERIALIZERS = Map.ofEntries(
            // builtin supported wrapper types
            Map.entry(Byte.class, BYTE_SERIALIZER),
            Map.entry(Boolean.class, BOOLEAN_SERIALIZER),
            Map.entry(Short.class, SHORT_SERIALIZER),
            Map.entry(Character.class, CHARACTER_SERIALIZER),
            Map.entry(Integer.class, INTEGER_SERIALIZER),
            Map.entry(Long.class, LONG_SERIALIZER),
            Map.entry(Float.class, FLOAT_SERIALIZER),
            Map.entry(Double.class, DOUBLE_SERIALIZER),
            // builtin supported str types (String is also treated as a CharSequence)
            Map.entry(CharSequence.class, CHAR_SEQUENCE_SERIALIZER),
            Map.entry(String.class, CHAR_SEQUENCE_SERIALIZER),
            // builtin json types
            Map.entry(JsonPrimitiveType.class, JSON_PRIMITIVE_TYPE_SERIALIZER),
            Map.entry(JsonBoolType.class, JSON_BOOL_TYPE_SERIALIZER),
            Map.entry(JsonNumberType.class, JSON_NUMBER_TYPE_SERIALIZER),
            Map.entry(JsonStrType.class, JSON_STR_TYPE_SERIALIZER)
    );

    public static JsonValueSerializer builtinObjSerializer(Class<?> clazz) {
        return BUILTIN_OBJ_SERIALIZERS.get(clazz);
    }

    private static final JsonValueSerializer CHAR_SEQUENCE_ARRAY_SERIALIZER = makeArraySerializer(CHAR_SEQUENCE_SERIALIZER);
    private static final JsonValueSerializer JSON_PRIMITIVE_TYPE_ARRAY_SERIALIZER = makeArraySerializer(JSON_PRIMITIVE_TYPE_SERIALIZER);
    private static final JsonValueSerializer JSON_BOOL_TYPE_ARRAY_SERIALIZER = makeArraySerializer(JSON_BOOL_TYPE_SERIALIZER);
    private static final JsonValueSerializer JSON_NUMBER_TYPE_ARRAY_SERIALIZER = makeArraySerializer(JSON_NUMBER_TYPE_SERIALIZER);
    private static final JsonValueSerializer JSON_STR_TYPE_ARRAY_SERIALIZER = makeArraySerializer(JSON_STR_TYPE_SERIALIZER);

    private static final Map<Class<?>, JsonValueSerializer> BUILTIN_ARR_SERIALIZERS = Map.ofEntries(
            // builtin supported primitive array types
            Map.entry(byte[].class, BYTE_ARRAY_SERIALIZER),
            Map.entry(boolean[].class, BOOLEAN_ARRAY_SERIALIZER),
            Map.entry(short[].class, SHORT_ARRAY_SERIALIZER),
            Map.entry(char[].class, CHAR_ARRAY_SERIALIZER),
            Map.entry(int[].class, INT_ARRAY_SERIALIZER),
            Map.entry(long[].class, LONG_ARRAY_SERIALIZER),
            Map.entry(float[].class, FLOAT_ARRAY_SERIALIZER),
            Map.entry(double[].class, DOUBLE_ARRAY_SERIALIZER),
            // builtin supported wrapper array types
            Map.entry(Byte[].class, BYTE_WRAPPER_ARRAY_SERIALIZER),
            Map.entry(Boolean[].class, BOOLEAN_WRAPPER_ARRAY_SERIALIZER),
            Map.entry(Short[].class, SHORT_WRAPPER_ARRAY_SERIALIZER),
            Map.entry(Character[].class, CHARACTER_WRAPPER_ARRAY_SERIALIZER),
            Map.entry(Integer[].class, INTEGER_WRAPPER_ARRAY_SERIALIZER),
            Map.entry(Long[].class, LONG_WRAPPER_ARRAY_SERIALIZER),
            Map.entry(Float[].class, FLOAT_WRAPPER_ARRAY_SERIALIZER),
            Map.entry(Double[].class, DOUBLE_WRAPPER_ARRAY_SERIALIZER),
            // builtin supported str types (String is also treated as a CharSequence)
            Map.entry(CharSequence[].class, CHAR_SEQUENCE_ARRAY_SERIALIZER),
            Map.entry(String[].class, CHAR_SEQUENCE_ARRAY_SERIALIZER),
            // builtin json types
            Map.entry(JsonPrimitiveType[].class, JSON_PRIMITIVE_TYPE_ARRAY_SERIALIZER),
            Map.entry(JsonBoolType[].class, JSON_BOOL_TYPE_ARRAY_SERIALIZER),
            Map.entry(JsonNumberType[].class, JSON_NUMBER_TYPE_ARRAY_SERIALIZER),
            Map.entry(JsonStrType[].class, JSON_STR_TYPE_ARRAY_SERIALIZER)
    );

    public static JsonValueSerializer builtinArraySerializer(Class<?> clazz) {
        return BUILTIN_ARR_SERIALIZERS.get(clazz);
    }

    public static JsonValueSerializer marshallableSerializer(Class<?> rawType) {
        MarshallFacade fc = Marshalls.getMarshallFacade(rawType);
        if(fc == null) {
            throw new JsonSerializerException("type not marshallable : " + rawType.getName());
        }
        return makeObjSerializer(fc);
    }

    public static JsonValueSerializer enumSerializer(Class<?> rawType) {
        MarshallFacade fc = Marshalls.getMarshallFacade(rawType);
        if(fc == null) {
            return (o, _, w, _) -> {
                Enum<?> e = (Enum<?>) o;
                serializeEscapedCharSequence(e.name(), w);
                return null;
            };
        } else {
            return (o, _, w, _) -> {
                Enum<?> e = (Enum<?>) o;
                MarshallInfo marshallInfo = fc.marshallInfoByIndex(e.ordinal());
                if(marshallInfo.mappedNameSimple()) {
                    w.writeBytes(marshallInfo.mappedNameUtf8Bytes());
                } else {
                    serializeEscapedUtf8Bytes(marshallInfo.mappedNameUtf8Bytes(), w);
                }
                return null;
            };
        }
    }

    public static JsonValueSerializer arraySerializer(JsonSerializerOption option, Class<?> arrType) {
        JsonValueSerializer builtinArraySerializer = builtinArraySerializer(arrType);
        if(builtinArraySerializer != null) {
            return builtinArraySerializer;
        }
        // marshall ensures that rawType is a one-dimentional array, so no need to check componentType here
        Class<?> componentType = arrType.componentType();
        // check if current type could be override by option
        JsonValueSerializer customSerializer = option.getCustomSerializer(componentType);
        if(customSerializer != null) {
            return makeArraySerializer(customSerializer);
        }
        // enum must be specially treated
        if(arrType.isEnum()) {
            return makeArraySerializer(enumSerializer(arrType));
        }
        // assuming marshallable
        return makeArraySerializer(marshallableSerializer(arrType));
    }

    public static JsonValueSerializer rawSerializer(JsonSerializerOption option, Class<?> rawType) {
        JsonValueSerializer builtinObjSerializer = builtinObjSerializer(rawType);
        if(builtinObjSerializer != null) {
            return builtinObjSerializer;
        }
        // check if current type could be override by option
        JsonValueSerializer customSerializer = option.getCustomSerializer(rawType);
        if(customSerializer != null) {
            return customSerializer;
        }
        // enum must be specially treated
        if(rawType.isEnum()) {
            return enumSerializer(rawType);
        }
        // assuming marshallable
        return marshallableSerializer(rawType);
    }

}
