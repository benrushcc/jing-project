package io.jingproject.marshalljson;

import io.jingproject.common.*;
import io.jingproject.marshall.MarshallInfo;
import io.jingproject.marshall.Marshalls;
import jdk.incubator.vector.*;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// indent never overflows int, as its value is limited by the practical JSON nesting depth.
public final class JsonSerializerContext {
    // whether to escape '/', which was suggested for safely embedding JSON in HTML; it's not required by the JSON spec, so we leave it optional and default to false.
    private static final boolean ESCAPE_SLASH =
            Boolean.parseBoolean(System.getProperty("jing.marshalljson.escapeslash", "false"));
    private static final VectorSpecies<Short> SHORT_SPECIES;
    private static final VectorSpecies<Byte> BYTE_SPECIES;
    private static final byte[] WRITER_ESCAPE_TABLE = makeWriterEscapeTable();
    private static final byte[] HEX_BYTES = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
    private static final Map<Class<?>, JsonSerializeFunc> BUILTIN_SERIALIZE_OBJ_FUNC_MAP;
    private static final Map<Class<?>, JsonSerializeFunc> BUILTIN_SERIALIZE_ARRAY_FUNC_MAP;

    static {
        try {
            Class<Os> _ = MethodHandles.lookup().ensureInitialized(Os.class);
        } catch (IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
        int vecSize = Integer.parseInt(System.getProperty("jing.marshalljson.serialize.vecsize", "-1"));
        if (vecSize < 0) {
            vecSize = ShortVector.SPECIES_PREFERRED.vectorBitSize();
        }
        switch (vecSize) {
            case 128 -> {
                SHORT_SPECIES = ShortVector.SPECIES_128;
                BYTE_SPECIES = ByteVector.SPECIES_64;
            }
            case 256 -> {
                SHORT_SPECIES = ShortVector.SPECIES_256;
                BYTE_SPECIES = ByteVector.SPECIES_128;
            }
            case 512 -> {
                SHORT_SPECIES = ShortVector.SPECIES_512;
                BYTE_SPECIES = ByteVector.SPECIES_256;
            }
            default -> throw new UnsupportedOperationException("unknown vector size : " + vecSize);
        }
    }

    static {
        Map<Class<?>, JsonSerializeFunc> r = new HashMap<>();
        r.put(Byte.class, (o, _, c) -> {
            c.serializeByte((Byte) o);
            return JsonSerializeResult.Continue;
        });
        r.put(Boolean.class, (o, _, c) -> {
            c.serializeBoolean((Boolean) o);
            return JsonSerializeResult.Continue;
        });
        r.put(Short.class, (o, _, c) -> {
            c.serializeShort((Short) o);
            return JsonSerializeResult.Continue;
        });
        r.put(Character.class, (o, _, c) -> {
            c.serializeChar((Character) o);
            return JsonSerializeResult.Continue;
        });
        r.put(Integer.class, (o, _, c) -> {
            c.serializeInt((Integer) o);
            return JsonSerializeResult.Continue;
        });
        r.put(Long.class, (o, _, c) -> {
            c.serializeLong((Long) o);
            return JsonSerializeResult.Continue;
        });
        r.put(Float.class, (o, _, c) -> {
            c.serializeFloat((Float) o);
            return JsonSerializeResult.Continue;
        });
        r.put(Double.class, (o, _, c) -> {
            c.serializeDouble((Double) o);
            return JsonSerializeResult.Continue;
        });
        r.put(CharSequence.class, (o, _, c) -> {
            c.serializeEscapedCharSequence((CharSequence) o);
            return JsonSerializeResult.Continue;
        });
        r.put(String.class, (o, _, c) -> {
            c.serializeEscapedString((String) o);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonPrimitiveType.class, (o, _, c) -> {
            c.serializeJsonPrimitiveType((JsonPrimitiveType) o);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonBoolType.class, (o, _, c) -> {
            c.serializeJsonBoolType((JsonBoolType) o);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonNumberType.class, (o, _, c) -> {
            c.serializeJsonNumberType((JsonNumberType) o);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonStrType.class, (o, _, c) -> {
            c.serializeJsonStrType((JsonStrType) o);
            return JsonSerializeResult.Continue;
        });
        BUILTIN_SERIALIZE_OBJ_FUNC_MAP = Map.copyOf(r);
    }

    static {
        Map<Class<?>, JsonSerializeFunc> r = new HashMap<>();
        r.put(byte[].class, (o, i, c) -> {
            c.serializeByteArray((byte[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(boolean[].class, (o, i, c) -> {
            c.serializeBooleanArray((boolean[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(short[].class, (o, i, c) -> {
            c.serializeShortArray((short[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(char[].class, (o, i, c) -> {
            c.serializeCharArray((char[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(int[].class, (o, i, c) -> {
            c.serializeIntArray((int[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(long[].class, (o, i, c) -> {
            c.serializeLongArray((long[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(float[].class, (o, i, c) -> {
            c.serializeFloatArray((float[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(double[].class, (o, i, c) -> {
            c.serializeDoubleArray((double[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(Byte[].class, (o, i, c) -> {
            c.serializeByteWrapperArray((Byte[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(Boolean[].class, (o, i, c) -> {
            c.serializeBooleanWrapperArray((Boolean[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(Short[].class, (o, i, c) -> {
            c.serializeShortWrapperArray((Short[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(Character[].class, (o, i, c) -> {
            c.serializeCharWrapperArray((Character[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(Integer[].class, (o, i, c) -> {
            c.serializeIntWrapperArray((Integer[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(Long[].class, (o, i, c) -> {
            c.serializeLongWrapperArray((Long[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(Float[].class, (o, i, c) -> {
            c.serializeFloatWrapperArray((Float[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(Double[].class, (o, i, c) -> {
            c.serializeDoubleWrapperArray((Double[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(CharSequence[].class, (o, i, c) -> {
            c.serializeEscapedCharSequenceArray((CharSequence[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(String[].class, (o, i, c) -> {
            c.serializeEscapedStringArray((String[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonPrimitiveType[].class, (o, i, c) -> {
            c.serializeJsonPrimitiveTypeArray((JsonPrimitiveType[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonBoolType[].class, (o, i, c) -> {
            c.serializeJsonBoolTypeArray((JsonBoolType[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonNumberType[].class, (o, i, c) -> {
            c.serializeJsonNumberTypeArray((JsonNumberType[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonStrType[].class, (o, i, c) -> {
            c.serializeJsonStrTypeArray((JsonStrType[]) o, i);
            return JsonSerializeResult.Continue;
        });
        BUILTIN_SERIALIZE_ARRAY_FUNC_MAP = Map.copyOf(r);
    }

    private final JsonSerializerOption option;
    private final WriteBuffer writeBuffer;
    private char[] charBuffer;
    private Object obj;
    private Class<?> type;

    public JsonSerializerContext(JsonSerializerOption option, WriteBuffer writeBuffer) {
        this.option = option;
        this.writeBuffer = writeBuffer;
    }

    private static byte[] makeWriterEscapeTable() {
        byte[] table = new byte[Byte.MAX_VALUE - Byte.MIN_VALUE + 1];
        for (int i = 0x00; i < 0x20; i++) {
            table[i] = Byte.MIN_VALUE;
        }
        table[0x22] = (byte) '"';   // \"
        table[0x5C] = (byte) '\\'; // \\
        if (ESCAPE_SLASH) {
            table[0x2F] = (byte) '/';  // \/
        }
        table[0x08] = (byte) 'b';  // \b
        table[0x0C] = (byte) 'f';  // \f
        table[0x0A] = (byte) 'n';  // \n
        table[0x0D] = (byte) 'r';  // \r
        table[0x09] = (byte) 't';  // \t
        return table;
    }

    private static void serializeEscapedUtf8BytesToHeap(byte[] utf8Bytes, HeapWriteBuffer heapWriteBuffer) {
        final byte[] bytes = heapWriteBuffer.rawByteArray();
        int position = heapWriteBuffer.intPosition();
        bytes[position++] = (byte) '"';
        int start = 0;
        for (int index = 0; index < utf8Bytes.length; index++) {
            byte b = utf8Bytes[index];
            byte v = WRITER_ESCAPE_TABLE[b];
            if (v == 0) {
                continue;
            }
            if (index > start) {
                int available = index - start;
                System.arraycopy(utf8Bytes, start, bytes, position, available);
                position += available;
            }
            bytes[position++] = (byte) '\\';
            if (v > 0) {
                bytes[position++] = v;
            } else {
                bytes[position++] = (byte) 'u';
                bytes[position++] = (byte) '0';
                bytes[position++] = (byte) '0';
                bytes[position++] = HEX_BYTES[(b >>> 4) & 0xF];
                bytes[position++] = HEX_BYTES[b & 0xF];
            }
            start = index + 1;
        }
        if (start < utf8Bytes.length) {
            int available = utf8Bytes.length - start;
            System.arraycopy(utf8Bytes, start, bytes, position, available);
            position += available;
        }
        bytes[position++] = (byte) '"';
        heapWriteBuffer.setPosition(position);
    }

    private static void serializeEscapedUtf8BytesToSegment(byte[] utf8Bytes, SegmentWriteBuffer segmentWriteBuffer) {
        final MemorySegment segment = segmentWriteBuffer.rawSegment();
        long position = segmentWriteBuffer.longPosition();
        SegmentAccess.setByte(segment, position++, (byte) '"');
        int start = 0;
        for (int index = 0; index < utf8Bytes.length; index++) {
            byte b = utf8Bytes[index];
            byte v = WRITER_ESCAPE_TABLE[b];
            if (v == 0) {
                continue;
            }
            if (index > start) {
                int available = index - start;
                MemorySegment.copy(utf8Bytes, start, segment, ValueLayout.JAVA_BYTE, position, available);
                position += available;
            }
            SegmentAccess.setByte(segment, position++, (byte) '\\');
            if (v > 0) {
                SegmentAccess.setByte(segment, position++, v);
            } else {
                SegmentAccess.setByte(segment, position++, (byte) 'u');
                SegmentAccess.setByte(segment, position++, (byte) '0');
                SegmentAccess.setByte(segment, position++, (byte) '0');
                SegmentAccess.setByte(segment, position++, HEX_BYTES[(b >>> 4) & 0xF]);
                SegmentAccess.setByte(segment, position++, HEX_BYTES[b & 0xF]);
            }
            start = index + 1;
        }
        if (start < utf8Bytes.length) {
            int available = utf8Bytes.length - start;
            MemorySegment.copy(utf8Bytes, start, segment, ValueLayout.JAVA_BYTE, position, available);
            position += available;
        }
        SegmentAccess.setByte(segment, position++, (byte) '"');
        segmentWriteBuffer.setPosition(position);
    }

    private static int asciiCount(ShortVector shortVector) {
        VectorMask<Short> mask = shortVector.compare(VectorOperators.LT, (short) 0x20)
                .or(shortVector.compare(VectorOperators.GT, (short) 0x7E))
                .or(shortVector.compare(VectorOperators.EQ, (short) 0x22))
                .or(shortVector.compare(VectorOperators.EQ, (short) 0x5C));
        if (ESCAPE_SLASH) {
            mask = mask.or(shortVector.compare(VectorOperators.EQ, (short) 0x2F));
        }
        return mask.firstTrue();
    }

    private void serializeEscapedStringToHeap(String str, HeapWriteBuffer heapWriteBuffer) {
        final byte[] bytes = heapWriteBuffer.rawByteArray();
        int position = heapWriteBuffer.intPosition();
        bytes[position++] = (byte) '"';
        final int len = str.length();
        final int vecLen = SHORT_SPECIES.length();
        if(len < vecLen) {
            position = serializeStrToBytes(str, bytes, position);
        } else {
            char[] buf = charBuffer;
            if(buf == null || buf.length < len) {
                buf = charBuffer = new char[Integer.highestOneBit(len - 1) << 1];
            }
            str.getChars(0, len, buf, 0);
            int index = 0;
            for ( ; index <= len - vecLen; index += vecLen) {
                ShortVector shortVector = ShortVector.fromCharArray(SHORT_SPECIES, buf, index);
                ByteVector byteVector = (ByteVector) shortVector.convertShape(VectorOperators.S2B, BYTE_SPECIES, 0);
                byteVector.intoArray(bytes, position);
                int matched = asciiCount(shortVector);
                position += matched;
                if (matched != vecLen) {
                    index += matched;
                    break;
                }
            }
            position = serializeCharsToBytes(buf, index, len, bytes, position);
        }
        bytes[position++] = (byte) '"';
        heapWriteBuffer.setPosition(position);
    }

    private void serializeEscapedStringToSegment(String str, SegmentWriteBuffer segmentWriteBuffer) {
        final MemorySegment segment = segmentWriteBuffer.rawSegment();
        long position = segmentWriteBuffer.longPosition();
        SegmentAccess.setByte(segment, position++, (byte) '"');
        final int len = str.length();
        final int vecLen = SHORT_SPECIES.length();
        if(len < vecLen) {
            position = serializeStrToSegment(str, segment, position);
        } else {
            char[] buf = charBuffer;
            if(buf.length < len) {
                buf = charBuffer = new char[Integer.highestOneBit(len - 1) << 1];
            }
            str.getChars(0, len, buf, 0);
            int index = 0;
            for ( ; index <= len - vecLen; index += vecLen) {
                ShortVector shortVector = ShortVector.fromCharArray(SHORT_SPECIES, buf, index);
                ByteVector byteVector = (ByteVector) shortVector.convertShape(VectorOperators.S2B, BYTE_SPECIES, 0);
                byteVector.intoMemorySegment(segment, position, ByteOrder.nativeOrder()); // byteOrder will be ignored
                int matched = asciiCount(shortVector);
                position += matched;
                if (matched != vecLen) {
                    index += matched;
                    break;
                }
            }
            position = serializeCharsToSegment(buf, index, len, segment, position);
        }
        SegmentAccess.setByte(segment, position++, (byte) '"');
        segmentWriteBuffer.setPosition(position);
    }

    private static int serializeStrToBytes(String str, byte[] bytes, int position) {
        final int len = str.length();
        int index = 0;
        while (index < len) {
            char c = str.charAt(index++);
            if (c < 0x80) {
                position = serializeCharToBytes(c, bytes, position);
            } else if (c < 0x800) {
                position = serializeCharToBytes2(c, bytes, position);
            } else if (Character.isHighSurrogate(c)) {
                position = serializeCharToBytes4(c, str.charAt(index++), bytes, position);
            } else {
                position = serializeCharToBytes3(c, bytes, position);
            }
        }
        return position;
    }

    private static long serializeStrToSegment(String str, MemorySegment segment, long position) {
        final int len = str.length();
        int index = 0;
        while (index < len) {
            char c = str.charAt(index++);
            if (c < 0x80) {
                position = serializeCharToSegment(c, segment, position);
            } else if (c < 0x800) {
                position = serializeCharToSegment2(c, segment, position);
            } else if (Character.isHighSurrogate(c)) {
                position = serializeCharToSegment4(c, str.charAt(index++), segment, position);
            } else {
                position = serializeCharToSegment3(c, segment, position);
            }
        }
        return position;
    }

    private static int serializeCharsToBytes(char[] buf, int index, int len, byte[] bytes, int position) {
        Objects.checkFromToIndex(index, len, buf.length);
        char sur = 0;
        for( ; index <= len - 2; index += 2) {
            char c1 = buf[index];
            char c2 = buf[index + 1];
            if(c1 < 0x80) {
                position = serializeCharToBytes(c1, bytes, position);
            } else if(c1 < 0x800) {
                position = serializeCharToBytes2(c1, bytes, position);
            } else if(Character.isHighSurrogate(c1)) {
                position = serializeCharToBytes4(c1, c2, bytes, position);
                continue ;
            } else if(Character.isLowSurrogate(c1)) {
                position = serializeCharToBytes4(sur, c1, bytes, position);
            } else {
                position = serializeCharToBytes3(c1, bytes, position);
            }
            if(c2 < 0x80) {
                position = serializeCharToBytes(c2, bytes, position);
            } else if(c2 < 0x800) {
                position = serializeCharToBytes2(c2, bytes, position);
            } else if(Character.isHighSurrogate(c2)) {
                sur = c2;
            } else {
                position = serializeCharToBytes3(c2, bytes, position);
            }
        }
        if(index < len) {
            char last = buf[index];
            if(last < 0x80) {
                position = serializeCharToBytes(last, bytes, position);
            } else if(last < 0x800) {
                position = serializeCharToBytes2(last, bytes, position);
            } else if(Character.isLowSurrogate(last)) {
                position = serializeCharToBytes4(sur, last, bytes, position);
            } else {
                position = serializeCharToBytes3(last, bytes, position);
            }
        }
        return position;
    }

    private static long serializeCharsToSegment(char[] buf, int index, int len, MemorySegment segment, long position) {
        Objects.checkFromToIndex(index, len, buf.length);
        char sur = 0;
        for( ; index <= len - 2; index += 2) {
            char c1 = buf[index];
            char c2 = buf[index + 1];
            if(c1 < 0x80) {
                position = serializeCharToSegment(c1, segment, position);
            } else if(c1 < 0x800) {
                position = serializeCharToSegment2(c1, segment, position);
            } else if(Character.isHighSurrogate(c1)) {
                position = serializeCharToSegment4(c1, c2, segment, position);
                continue ;
            } else if(Character.isLowSurrogate(c1)) {
                position = serializeCharToSegment4(sur, c1, segment, position);
            } else {
                position = serializeCharToSegment3(c1, segment, position);
            }
            if(c2 < 0x80) {
                position = serializeCharToSegment(c2, segment, position);
            } else if(c2 < 0x800) {
                position = serializeCharToSegment2(c2, segment, position);
            } else if(Character.isHighSurrogate(c2)) {
                sur = c2;
            } else {
                position = serializeCharToSegment3(c2, segment, position);
            }
        }
        if(index < len) {
            char last = buf[index];
            if(last < 0x80) {
                position = serializeCharToSegment(last, segment, position);
            } else if(last < 0x800) {
                position = serializeCharToSegment2(last, segment, position);
            } else if(Character.isLowSurrogate(last)) {
                position = serializeCharToSegment4(sur, last, segment, position);
            } else {
                position = serializeCharToSegment3(last, segment, position);
            }
        }
        return position;
    }

    private static int serializeCharToBytes(char c, byte[] bytes, int offset) {
        int v = WRITER_ESCAPE_TABLE[c];
        if (v == 0) {
            bytes[offset++] = (byte) c;
        } else if (v > 0) {
            bytes[offset++] = (byte) '\\';
            bytes[offset++] = (byte) v;
        } else {
            bytes[offset++] = (byte) '\\';
            bytes[offset++] = (byte) 'u';
            bytes[offset++] = (byte) '0';
            bytes[offset++] = (byte) '0';
            bytes[offset++] = HEX_BYTES[c >>> 4];
            bytes[offset++] = HEX_BYTES[c & 0xF];
        }
        return offset;
    }

    private static long serializeCharToSegment(char c, MemorySegment segment, long offset) {
        int v = WRITER_ESCAPE_TABLE[c];
        if (v == 0) {
            SegmentAccess.setByte(segment, offset++, (byte) c);
        } else if (v > 0) {
            SegmentAccess.setByte(segment, offset++, (byte) '\\');
            SegmentAccess.setByte(segment, offset++, (byte) v);
        } else {
            SegmentAccess.setByte(segment, offset++, (byte) '\\');
            SegmentAccess.setByte(segment, offset++, (byte) 'u');
            SegmentAccess.setByte(segment, offset++, (byte) '0');
            SegmentAccess.setByte(segment, offset++, (byte) '0');
            SegmentAccess.setByte(segment, offset++, HEX_BYTES[c >>> 4]);
            SegmentAccess.setByte(segment, offset++, HEX_BYTES[c & 0xF]);
        }
        return offset;
    }

    private static int serializeCharToBytes2(char c, byte[] bytes, int offset) {

        bytes[offset] = (byte) (0xC0 | (c >> 6));
        bytes[offset + 1] = (byte) (0x80 | (c & 0x3F));
        return offset + 2;
    }

    private static long serializeCharToSegment2(char c, MemorySegment bytes, long offset) {
        SegmentAccess.setByte(bytes, offset, (byte) (0xC0 | (c >> 6)));
        SegmentAccess.setByte(bytes, offset + 1L, (byte) (0x80 | (c & 0x3F)));
        return offset + 2L;
    }

    private static int serializeCharToBytes3(char c, byte[] bytes, int offset) {
        bytes[offset] = (byte) (0xE0 | (c >> 12));
        bytes[offset + 1] = (byte) (0x80 | ((c >> 6) & 0x3F));
        bytes[offset + 2] = (byte) (0x80 | (c & 0x3F));
        return offset + 3;
    }

    private static long serializeCharToSegment3(char c, MemorySegment segment, long offset) {
        SegmentAccess.setByte(segment, offset, (byte) (0xE0 | (c >> 12)));
        SegmentAccess.setByte(segment, offset + 1L, (byte) (0x80 | ((c >> 6) & 0x3F)));
        SegmentAccess.setByte(segment, offset + 2L, (byte) (0x80 | (c & 0x3F)));
        return offset + 3L;
    }

    private static int serializeCharToBytes4(char highSurrogate, char lowSurrogate, byte[] bytes, int offset) {
        int cp = Character.toCodePoint(highSurrogate, lowSurrogate);
        bytes[offset] = (byte) (0xF0 | (cp >> 18));
        bytes[offset + 1] = (byte) (0x80 | ((cp >> 12) & 0x3F));
        bytes[offset + 2] = (byte) (0x80 | ((cp >> 6) & 0x3F));
        bytes[offset + 3] = (byte) (0x80 | (cp & 0x3F));
        return offset + 4;
    }

    private static long serializeCharToSegment4(char highSurrogate, char lowSurrogate, MemorySegment segment, long offset) {
        int cp = Character.toCodePoint(highSurrogate, lowSurrogate);
        SegmentAccess.setByte(segment, offset, (byte) (0xF0 | (cp >> 18)));
        SegmentAccess.setByte(segment, offset + 1L, (byte) (0x80 | ((cp >> 12) & 0x3F)));
        SegmentAccess.setByte(segment, offset + 2L, (byte) (0x80 | ((cp >> 6) & 0x3F)));
        SegmentAccess.setByte(segment, offset + 3L, (byte) (0x80 | (cp & 0x3F)));
        return offset + 4L;
    }

    public static JsonSerializeFunc builtinSerializeObjFunc(Class<?> rawType) {

        return BUILTIN_SERIALIZE_OBJ_FUNC_MAP.get(rawType);
    }

    public static JsonSerializeFunc builtinSerializeArrayFunc(Class<?> rawType) {

        return BUILTIN_SERIALIZE_ARRAY_FUNC_MAP.get(rawType);
    }

    public static JsonSerializeFunc valueSerializeFunc(JsonSerializerOption option, Class<?> rawType) {

        // builtin type has the highest priority
        if (rawType.isArray()) {
            JsonSerializeFunc builtinSerializeArrFunc = builtinSerializeArrayFunc(rawType);
            if (builtinSerializeArrFunc != null) {
                return builtinSerializeArrFunc;
            }
            return (o, _, c) -> {
                c.set(o);
                return JsonSerializeResult.NewArray;
            };
        }
        JsonSerializeFunc builtinSerializeFunc = builtinSerializeObjFunc(rawType);
        if (builtinSerializeFunc != null) {
            return builtinSerializeFunc;
        }
        // check if current type could be override by option
        JsonSerializeFunc customFunc = option.customFunc(rawType);
        if (customFunc != null) {
            return customFunc;
        }
        // enum must be specially treated
        if (rawType.isEnum()) {
            return (o, _, c) -> {
                c.serializeEnum((Enum<?>) o);
                return JsonSerializeResult.Continue;
            };
        }
        // assuming marshallable
        return (o, _, c) -> {
            c.set(o);
            return JsonSerializeResult.NewMarshallable;
        };
    }

    public JsonSerializerOption option() {
        return option;
    }

    public WriteBuffer writeBuffer() {
        return writeBuffer;
    }

    public Object obj() {
        return obj;
    }

    public Class<?> type() {
        return type;
    }

    public void set(Object obj) {
        this.obj = obj;
    }

    public void set(Object obj, Class<?> type) {
        this.obj = obj;
        this.type = type;
    }

    public void serializeNull() {
        writeBuffer.writeBytes((byte) 'n', (byte) 'u', (byte) 'l', (byte) 'l');
    }

    public void serializeByte(byte value) {
        JsonNumberUtil.writeInt(value, writeBuffer);
    }

    public void serializeBoolean(boolean value) {
        final WriteBuffer w = this.writeBuffer;
        if (value) {
            w.writeBytes((byte) 't', (byte) 'r', (byte) 'u', (byte) 'e');
        } else {
            w.writeBytes((byte) 'f', (byte) 'a', (byte) 'l', (byte) 's', (byte) 'e');
        }
    }

    public void serializeShort(short value) {
        JsonNumberUtil.writeInt(value, writeBuffer);
    }

    private void serializeAsciiByte(byte b) {
        final WriteBuffer w = this.writeBuffer;
        final byte v = WRITER_ESCAPE_TABLE[b];
        if (v == 0) {
            w.writeByte(b);
        } else if (v > 0) {
            w.writeBytes((byte) '\\', v);
        } else {
            w.writeBytes((byte) '\\', (byte) 'u', (byte) '0', (byte) '0', HEX_BYTES[(b >>> 4) & 0xF], HEX_BYTES[b & 0xf]);
        }
    }

    public void serializeChar(char value) {
        final WriteBuffer w = this.writeBuffer;
        w.writeByte((byte) '"');
        if (Character.isSurrogate(value)) {
            throw new IllegalArgumentException("surrogates not supported");
        }
        if (value < 0x80) {
            serializeAsciiByte((byte) value);
        } else if (value < 0x800) {
            w.writeBytes((byte) (0xC0 | (value >> 6)),
                    (byte) (0x80 | (value & 0x3F)));
        } else {
            w.writeBytes((byte) (0xE0 | (value >> 12)),
                    (byte) (0x80 | ((value >> 6) & 0x3F)),
                    (byte) (0x80 | (value & 0x3F)));
        }
        w.writeByte((byte) '"');
    }

    public void serializeInt(int value) {
        JsonNumberUtil.writeInt(value, writeBuffer);
    }

    public void serializeLong(long value) {
        JsonNumberUtil.writeLong(value, writeBuffer);
    }

    public void serializeFloat(float value) {
        JsonNumberUtil.writeFloat(value, writeBuffer);
    }

    public void serializeDouble(double value) {
        JsonNumberUtil.writeDouble(value, writeBuffer);
    }

    public void serializeIndent(int indent) {
        final WriteBuffer w = this.writeBuffer;
        switch (option.indentationLevel()) {
            case NONE -> {
            }
            case TWO -> {
                w.writeByte((byte) '\n');
                w.writeRepeated((byte) ' ', indent * 2); // no overflow, indent is limited
            }
            case FOUR -> {
                w.writeByte((byte) '\n');
                w.writeRepeated((byte) ' ', indent * 4); // no overflow, indent is limited
            }
            default -> throw new AssertionError();
        }
    }

    private <T> void serializeObjArray(T[] arr, int indent, ElementSerializer<T> elementSerializer) {
        final WriteBuffer w = this.writeBuffer;
        if (arr.length == 0) {
            w.writeBytes((byte) '[', (byte) ']');
            return;
        }
        w.writeByte((byte) '[');
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1); // no overflow, indent is limited
            T o = arr[i];
            if (o == null) {
                serializeNull();
            } else {
                elementSerializer.serialize(this, o);
            }
            if (i != arr.length - 1) {
                w.writeByte((byte) ',');
            }
        }
        serializeIndent(indent);
        w.writeByte((byte) ']');
    }

    public void serializeByteArray(byte[] arr, int indent) {
        final WriteBuffer w = this.writeBuffer;
        if (arr.length == 0) {
            w.writeBytes((byte) '[', (byte) ']');
            return;
        }
        w.writeByte((byte) '[');
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1); // no overflow, indent is limited
            serializeByte(arr[i]);
            if (i != arr.length - 1) {
                w.writeByte((byte) ',');
            }
        }
        serializeIndent(indent);
        w.writeByte((byte) ']');
    }

    public void serializeByteWrapperArray(Byte[] arr, int indent) {
        serializeObjArray(arr, indent, JsonSerializerContext::serializeByte);
    }

    public void serializeBooleanArray(boolean[] arr, int indent) {
        final WriteBuffer w = this.writeBuffer;
        if (arr.length == 0) {
            w.writeBytes((byte) '[', (byte) ']');
            return;
        }
        w.writeByte((byte) '[');
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1); // no overflow, indent is limited
            serializeBoolean(arr[i]);
            if (i != arr.length - 1) {
                w.writeByte((byte) ',');
            }
        }
        serializeIndent(indent);
        w.writeByte((byte) ']');
    }

    public void serializeBooleanWrapperArray(Boolean[] arr, int indent) {
        serializeObjArray(arr, indent, JsonSerializerContext::serializeBoolean);
    }

    public void serializeShortArray(short[] arr, int indent) {
        final WriteBuffer w = this.writeBuffer;
        if (arr.length == 0) {
            w.writeBytes((byte) '[', (byte) ']');
            return;
        }
        w.writeByte((byte) '[');
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1);
            serializeShort(arr[i]);
            if (i != arr.length - 1) {
                w.writeByte((byte) ',');
            }
        }
        serializeIndent(indent);
        w.writeByte((byte) ']');
    }

    public void serializeShortWrapperArray(Short[] arr, int indent) {
        serializeObjArray(arr, indent, JsonSerializerContext::serializeShort);
    }

    public void serializeCharArray(char[] arr, int indent) {
        final WriteBuffer w = this.writeBuffer;
        if (arr.length == 0) {
            w.writeBytes((byte) '[', (byte) ']');
            return;
        }
        w.writeByte((byte) '[');
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1);
            serializeChar(arr[i]);
            if (i != arr.length - 1) {
                w.writeByte((byte) ',');
            }
        }
        serializeIndent(indent);
        w.writeByte((byte) ']');
    }

    public void serializeCharWrapperArray(Character[] arr, int indent) {
        serializeObjArray(arr, indent, JsonSerializerContext::serializeChar);
    }

    public void serializeIntArray(int[] arr, int indent) {
        final WriteBuffer w = this.writeBuffer;
        if (arr.length == 0) {
            w.writeBytes((byte) '[', (byte) ']');
            return;
        }
        w.writeByte((byte) '[');
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1);
            serializeInt(arr[i]);
            if (i != arr.length - 1) {
                w.writeByte((byte) ',');
            }
        }
        serializeIndent(indent);
        w.writeByte((byte) ']');
    }

    public void serializeIntWrapperArray(Integer[] arr, int indent) {
        serializeObjArray(arr, indent, JsonSerializerContext::serializeInt);
    }

    public void serializeLongArray(long[] arr, int indent) {
        final WriteBuffer w = this.writeBuffer;
        if (arr.length == 0) {
            w.writeBytes((byte) '[', (byte) ']');
            return;
        }
        w.writeByte((byte) '[');
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1);
            serializeLong(arr[i]);
            if (i != arr.length - 1) {
                w.writeByte((byte) ',');
            }
        }
        serializeIndent(indent);
        w.writeByte((byte) ']');
    }

    public void serializeLongWrapperArray(Long[] arr, int indent) {
        serializeObjArray(arr, indent, JsonSerializerContext::serializeLong);
    }

    public void serializeFloatArray(float[] arr, int indent) {
        final WriteBuffer w = this.writeBuffer;
        if (arr.length == 0) {
            w.writeBytes((byte) '[', (byte) ']');
            return;
        }
        w.writeByte((byte) '[');
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1);
            serializeFloat(arr[i]);
            if (i != arr.length - 1) {
                w.writeByte((byte) ',');
            }
        }
        serializeIndent(indent);
        w.writeByte((byte) ']');
    }

    public void serializeFloatWrapperArray(Float[] arr, int indent) {
        serializeObjArray(arr, indent, JsonSerializerContext::serializeFloat);
    }

    public void serializeDoubleArray(double[] arr, int indent) {
        final WriteBuffer w = this.writeBuffer;
        if (arr.length == 0) {
            w.writeBytes((byte) '[', (byte) ']');
            return;
        }
        w.writeByte((byte) '[');
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1);
            serializeDouble(arr[i]);
            if (i != arr.length - 1) {
                w.writeByte((byte) ',');
            }
        }
        serializeIndent(indent);
        w.writeByte((byte) ']');
    }

    public void serializeDoubleWrapperArray(Double[] arr, int indent) {
        serializeObjArray(arr, indent, JsonSerializerContext::serializeDouble);
    }

    public void serializeNonEscapedUtf8Bytes(byte[] utf8Bytes) {
        final WriteBuffer w = this.writeBuffer;
        final int len = utf8Bytes.length;
        if (len == 0) {
            w.writeBytes((byte) '"', (byte) '"');
            return;
        }
        w.ensureCapacity(Math.addExact(len, 2));
        switch (w) {
            case HeapWriteBuffer heapWriteBuffer -> {
                final byte[] bytes = heapWriteBuffer.rawByteArray();
                int position = heapWriteBuffer.intPosition();
                bytes[position++] = (byte) '"';
                System.arraycopy(utf8Bytes, 0, bytes, position, len);
                position += len;
                bytes[position++] = (byte) '"';
                heapWriteBuffer.setPosition(position);
            }
            case SegmentWriteBuffer segmentWriteBuffer -> {
                final MemorySegment segment = segmentWriteBuffer.rawSegment();
                long position = segmentWriteBuffer.longPosition();
                SegmentAccess.setByte(segment, position++, (byte) '"');
                MemorySegment.copy(utf8Bytes, 0, segment, ValueLayout.JAVA_BYTE, position, len);
                position += len;
                SegmentAccess.setByte(segment, position++, (byte) '"');
                segmentWriteBuffer.setPosition(position);
            }
        }
    }

    public void serializeEscapedUtf8Bytes(byte[] utf8Bytes) {
        final WriteBuffer w = this.writeBuffer;
        final int len = utf8Bytes.length;
        if (len == 0) {
            w.writeBytes((byte) '"', (byte) '"');
            return;
        }
        // expansion factor 6 covers worst-case escape (backslash + u + 4 hex digits), plus two quote
        w.ensureCapacity(Math.addExact(Math.multiplyExact(len, 6), 2));
        switch (w) {
            case HeapWriteBuffer heapWriteBuffer -> serializeEscapedUtf8BytesToHeap(utf8Bytes, heapWriteBuffer);
            case SegmentWriteBuffer segmentWriteBuffer ->
                    serializeEscapedUtf8BytesToSegment(utf8Bytes, segmentWriteBuffer);
            case null, default -> throw new AssertionError();
        }
    }

    public void serializeEscapedCharSequence(CharSequence charSequence) {
        serializeEscapedString(charSequence.toString());
    }

    public void serializeEscapedString(String str) {
        final WriteBuffer w = writeBuffer;
        final int len = str.length();
        if (len == 0) {
            w.writeBytes((byte) '"', (byte) '"');
            return;
        }
        // expansion factor 6 covers worst-case escape (backslash + u + 4 hex digits), plus two quote
        w.ensureCapacity(Math.addExact(Math.multiplyExact(len, 6), 2));
        switch (w) {
            case HeapWriteBuffer heapWriteBuffer -> serializeEscapedStringToHeap(str, heapWriteBuffer);
            case SegmentWriteBuffer segmentWriteBuffer -> serializeEscapedStringToSegment(str, segmentWriteBuffer);
        }
    }

    public void serializeEscapedCharSequenceArray(CharSequence[] arr, int indent) {
        serializeObjArray(arr, indent, JsonSerializerContext::serializeEscapedCharSequence);
    }

    public void serializeEscapedStringArray(String[] arr, int indent) {
        serializeObjArray(arr, indent, JsonSerializerContext::serializeEscapedString);
    }

    public void serializeJsonPrimitiveType(JsonPrimitiveType jsonPrimitiveType) {
        switch (jsonPrimitiveType) {
            case JsonBoolType jsonBoolType -> serializeJsonBoolType(jsonBoolType);
            case JsonNumberType jsonNumberType -> serializeJsonNumberType(jsonNumberType);
            case JsonStrType jsonStrType -> serializeJsonStrType(jsonStrType);
            case null, default -> throw new AssertionError();
        }
    }

    public void serializeJsonPrimitiveTypeArray(JsonPrimitiveType[] arr, int indent) {
        serializeObjArray(arr, indent, JsonSerializerContext::serializeJsonPrimitiveType);
    }

    public void serializeJsonBoolType(JsonBoolType jsonBoolType) {
        serializeBoolean(jsonBoolType.data());
    }

    public void serializeJsonBoolTypeArray(JsonBoolType[] arr, int indent) {
        serializeObjArray(arr, indent, JsonSerializerContext::serializeJsonBoolType);
    }

    public void serializeJsonNumberType(JsonNumberType jsonNumberType) {
        writeBuffer.writeBytes(jsonNumberType.data());
    }

    public void serializeJsonNumberTypeArray(JsonNumberType[] arr, int indent) {
        serializeObjArray(arr, indent, JsonSerializerContext::serializeJsonNumberType);
    }

    public void serializeJsonStrType(JsonStrType jsonStrType) {
        serializeEscapedString(jsonStrType.data());
    }

    public void serializeJsonStrTypeArray(JsonStrType[] arr, int indent) {
        serializeObjArray(arr, indent, JsonSerializerContext::serializeJsonStrType);
    }

    public void serializeEnum(Enum<?> enumValue) {
        MarshallInfo inf = Marshalls.getEnumItemMarshallInfo(enumValue);
        if (inf == null) {
            serializeEscapedString(enumValue.name());
        } else if (inf.mappedNameSimple()) {
            final WriteBuffer w = this.writeBuffer;
            w.writeByte((byte) '"');
            w.writeBytes(inf.mappedNameUtf8Bytes());
            w.writeByte((byte) '"');
        } else {
            serializeEscapedUtf8Bytes(inf.mappedNameUtf8Bytes());
        }
    }

    @FunctionalInterface
    interface ElementSerializer<T> {
        void serialize(JsonSerializerContext c, T t);
    }

}
