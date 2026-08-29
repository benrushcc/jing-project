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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

// indent never overflows int, as its value is limited by the practical JSON nesting depth.
public final class JsonSerializerContext {
    // whether to escape '/', which was suggested for safely embedding JSON in HTML, it's not required by the JSON spec, so we leave it optional and default to false
    private static final boolean ESCAPE_SLASH =
            Boolean.parseBoolean(System.getProperty("jing.marshalljson.escapeslash", "false"));
    // whether to nable surrogate pair handling
    // when the ASCII fast path does not match, the surrogate pair filtering path is taken
    // this yields performance gains when 3‑byte UTF‑8 data dominates and 4‑byte surrogate pairs are absent
    private static final boolean FILTER_SURR =
            Boolean.parseBoolean(System.getProperty("jing.marshalljson.filtersurr", "true"));
    private static final VectorSpecies<Short> SHORT_SPECIES;
    private static final VectorSpecies<Byte> BYTE_SPECIES;
    private static final int VEC_MASK;
    private static final byte[] ESCAPE_TABLE = makeEscapeTable();
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
        VEC_MASK = SHORT_SPECIES.length() - 1;
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

    public JsonSerializerOption option() {
        return option;
    }

    public WriteBuffer writeBuffer() {
        return writeBuffer;
    }

    public Object obj() {
        return obj;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }

    public Class<?> type() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    private static byte[] makeEscapeTable() {
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
            byte v = ESCAPE_TABLE[b];
            if (v == 0) {
                continue;
            }
            if (index > start) {
                int available = index - start;
                System.arraycopy(utf8Bytes, start, bytes, position, available);
                position += available;
            }
            bytes[position] = (byte) '\\';
            if(v > 0) {
                bytes[position + 1] = v;
                position += 2;
            } else {
                bytes[position + 1] = (byte) 'u';
                bytes[position + 2] = (byte) '0';
                bytes[position + 3] = (byte) '0';
                bytes[position + 4] = HEX_BYTES[(b >>> 4) & 0xF];
                bytes[position + 5] = HEX_BYTES[b & 0xF];
                position += 6;
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
            byte v = ESCAPE_TABLE[b];
            if (v == 0) {
                continue;
            }
            if (index > start) {
                int available = index - start;
                MemorySegment.copy(utf8Bytes, start, segment, ValueLayout.JAVA_BYTE, position, available);
                position += available;
            }
            SegmentAccess.setByte(segment, position, (byte) '\\');
            if(v > 0) {
                SegmentAccess.setByte(segment, position + 1L, v);
                position += 2L;
            } else {
                SegmentAccess.setByte(segment, position + 1L, (byte) 'u');
                SegmentAccess.setByte(segment, position + 2L, (byte) '0');
                SegmentAccess.setByte(segment, position + 3L, (byte) '0');
                SegmentAccess.setByte(segment, position + 4L, HEX_BYTES[(b >>> 4) & 0xF]);
                SegmentAccess.setByte(segment, position + 5L, HEX_BYTES[b & 0xF]);
                position += 6L;
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

    private char[] alignedBuf(int alignedLen) {
        if(charBuffer == null || charBuffer.length < alignedLen) {
            charBuffer = new char[alignedLen];
        }
        ShortVector.broadcast(SHORT_SPECIES, (short) '?')
                .intoCharArray(charBuffer, alignedLen - SHORT_SPECIES.length());
        return charBuffer;
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

    private static boolean anySurr(ShortVector shortVector) {
        VectorMask<Short> mask = shortVector.lanewise(VectorOperators.AND, (short) 0xF800)
                .compare(VectorOperators.EQ, (short) 0xD800);
        return mask.anyTrue();
    }

    private int serializeLongStrToHeap(String str, byte[] bytes, int position) {
        final int len = str.length();
        final int alignedLen = Math.addExact(len, VEC_MASK) & (~VEC_MASK);
        final char[] alignedBuf = alignedBuf(alignedLen);
        str.getChars(0, len, alignedBuf, 0);
        int index = 0;
        for( ; index < alignedLen; index += SHORT_SPECIES.length()) {
            ShortVector shortVector = ShortVector.fromCharArray(SHORT_SPECIES, alignedBuf, index);
            ByteVector byteVector = (ByteVector) shortVector.convertShape(VectorOperators.S2B, BYTE_SPECIES, 0);
            byteVector.intoArray(bytes, position + index);
            int matched = asciiCount(shortVector);
            if(matched != SHORT_SPECIES.length()) {
                index += matched;
                break;
            }
        }
        if(index == alignedLen) {
            return position + len;
        }
        if(FILTER_SURR) {
            int alignedIndex = index & (~VEC_MASK);
            for( ; alignedIndex < alignedLen; alignedIndex += SHORT_SPECIES.length()) {
                if (anySurr(ShortVector.fromCharArray(SHORT_SPECIES, alignedBuf, alignedIndex))) {
                    break ;
                }
            }
            if(alignedIndex == alignedLen) {
                return serializeNonSurrCharsToHeap(alignedBuf, index, len, bytes, position + index);
            }
        }
        return serializeCharsToHeap(alignedBuf, index, len, bytes, position + index);
    }

    private long serializeLongStrToSegment(String str, MemorySegment segment, long position) {
        final int len = str.length();
        final int alignedLen = Math.addExact(len, VEC_MASK) & (~VEC_MASK);
        final char[] alignedBuf = alignedBuf(alignedLen);
        str.getChars(0, len, alignedBuf, 0);
        int index = 0;
        for( ; index < alignedLen; index += SHORT_SPECIES.length()) {
            ShortVector shortVector = ShortVector.fromCharArray(SHORT_SPECIES, alignedBuf, index);
            ByteVector byteVector = (ByteVector) shortVector.convertShape(VectorOperators.S2B, BYTE_SPECIES, 0);
            byteVector.intoMemorySegment(segment, position + index, ByteOrder.nativeOrder()); // byteOrder will be ignored
            int matched = asciiCount(shortVector);
            if(matched != SHORT_SPECIES.length()) {
                index += matched;
                break;
            }
        }
        if(index == alignedLen) {
            return position + len;
        }
        if(FILTER_SURR) {
            int alignedIndex = index & (~VEC_MASK);
            for( ; alignedIndex < alignedLen; alignedIndex += SHORT_SPECIES.length()) {
                if (anySurr(ShortVector.fromCharArray(SHORT_SPECIES, alignedBuf, alignedIndex))) {
                    break ;
                }
            }
            if(alignedIndex == alignedLen) {
                return serializeNonSurrCharsToSegment(alignedBuf, index, len, segment, position + index);
            }
        }
        return serializeCharsToSegment(alignedBuf, index, len, segment, position + index);
    }

    private void serializeEscapedStringToHeap(String str, HeapWriteBuffer heapWriteBuffer) {
        final byte[] bytes = heapWriteBuffer.rawByteArray();
        int position = heapWriteBuffer.intPosition();
        bytes[position++] = (byte) '"';
        if(str.length() < SHORT_SPECIES.length()) {
            position = serializeStrToHeap(str, bytes, position);
        } else {
            position = serializeLongStrToHeap(str, bytes, position);
        }
        bytes[position++] = (byte) '"';
        heapWriteBuffer.setPosition(position);
    }

    private void serializeEscapedStringToSegment(String str, SegmentWriteBuffer segmentWriteBuffer) {
        final MemorySegment segment = segmentWriteBuffer.rawSegment();
        long position = segmentWriteBuffer.longPosition();
        SegmentAccess.setByte(segment, position++, (byte) '"');
        if(str.length() < SHORT_SPECIES.length()) {
            position = serializeStrToSegment(str, segment, position);
        } else {
            position = serializeLongStrToSegment(str, segment, position);
        }
        SegmentAccess.setByte(segment, position++, (byte) '"');
        segmentWriteBuffer.setPosition(position);
    }

    private static int serializeStrToHeap(String str, byte[] bytes, int position) {
        final int len = str.length();
        int index = 0;
        while (index < len) {
            char c = str.charAt(index++);
            if(c < 0x80) {
                position = serializeCharToHeap(c, bytes, position);
            } else if(c < 0x800) {
                position = serializeCharToHeap2(c, bytes, position);
            } else {
                // jdk string allows single surrogate, we still need to check them
                final int v = c & 0xFC00;
                if(v == 0xD800) {
                    if(index == len) {
                        throw new JsonSerializerException("illegal high surrogate without low surrogate");
                    }
                    char c1 = str.charAt(index++);
                    if((c1 & 0xFC00) != 0xDC00) {
                        throw new JsonSerializerException("illegal high surrogate without low surrogate");
                    }
                    position = serializeCharToHeap4(c, c1, bytes, position);
                } else if(v == 0xDC00) {
                    throw new JsonSerializerException("illegal low surrogate without high surrogate");
                } else {
                    position = serializeCharToHeap3(c, bytes, position);
                }
            }
        }
        return position;
    }

    private static long serializeStrToSegment(String str, MemorySegment segment, long position) {
        final int len = str.length();
        int index = 0;
        while (index < len) {
            char c = str.charAt(index++);
            if(c < 0x80) {
                position = serializeCharToSegment(c, segment, position);
            } else if(c < 0x800) {
                position = serializeCharToSegment2(c, segment, position);
            } else {
                // jdk string allows single surrogate, we still need to check them
                final int v = c & 0xFC00;
                if(v == 0xD800) {
                    if(index == len) {
                        throw new JsonSerializerException("illegal high surrogate without low surrogate");
                    }
                    char c1 = str.charAt(index++);
                    if((c1 & 0xFC00) != 0xDC00) {
                        throw new JsonSerializerException("illegal high surrogate without low surrogate");
                    }
                    position = serializeCharToSegment4(c, c1, segment, position);
                } else if(v == 0xDC00) {
                    throw new JsonSerializerException("illegal low surrogate without high surrogate");
                } else {
                    position = serializeCharToSegment3(c, segment, position);
                }
            }
        }
        return position;
    }

    private static int serializeNonSurrCharsToHeap(char[] buf, int index, int len, byte[] bytes, int position) {
        for( ; index < len; index++) {
            char c = buf[index];
            if(c < 0x80) {
                position = serializeCharToHeap(c, bytes, position);
            } else if(c < 0x800) {
                position = serializeCharToHeap2(c, bytes, position);
            } else {
                position = serializeCharToHeap3(c, bytes, position);
            }
        }
        return position;
    }

    private static int serializeCharsToHeap(char[] buf, int index, int len, byte[] bytes, int position) {
        for( ; index < len; index++) {
            char c = buf[index];
            if(c < 0x80) {
                position = serializeCharToHeap(c, bytes, position);
            } else if(c < 0x800) {
                position = serializeCharToHeap2(c, bytes, position);
            } else {
                // jdk string allows single surrogate, we still need to check them
                final int v = c & 0xFC00;
                if(v == 0xD800) {
                    final int nextIndex = index + 1;
                    if(nextIndex == len) {
                        throw new JsonSerializerException("illegal high surrogate without low surrogate");
                    }
                    char c1 = buf[nextIndex];
                    if((c1 & 0xFC00) != 0xDC00) {
                        throw new JsonSerializerException("illegal high surrogate without low surrogate");
                    }
                    position = serializeCharToHeap4(c, c1, bytes, position);
                    index = nextIndex;
                } else if(v == 0xDC00) {
                    throw new JsonSerializerException("illegal low surrogate without high surrogate");
                } else {
                    position = serializeCharToHeap3(c, bytes, position);
                }
            }
        }
        return position;
    }

    private static long serializeNonSurrCharsToSegment(char[] buf, int index, int len, MemorySegment segment, long position) {
        for( ; index < len; index++) {
            char c = buf[index];
            if(c < 0x80) {
                position = serializeCharToSegment(c, segment, position);
            } else if(c < 0x800) {
                position = serializeCharToSegment2(c, segment, position);
            } else {
                position = serializeCharToSegment3(c, segment, position);
            }
        }
        return position;
    }

    private static long serializeCharsToSegment(char[] buf, int index, int len, MemorySegment segment, long position) {
        for( ; index < len; index++) {
            char c = buf[index];
            if(c < 0x80) {
                position = serializeCharToSegment(c, segment, position);
            } else if(c < 0x800) {
                position = serializeCharToSegment2(c, segment, position);
            } else {
                // jdk string allows single surrogate, we still need to check them
                final int v = c & 0xFC00;
                if(v == 0xD800) {
                    final int nextIndex = index + 1;
                    if(nextIndex == len) {
                        throw new JsonSerializerException("illegal high surrogate without low surrogate");
                    }
                    char c1 = buf[nextIndex];
                    if((c1 & 0xFC00) != 0xDC00) {
                        throw new JsonSerializerException("illegal high surrogate without low surrogate");
                    }
                    position = serializeCharToSegment4(c, c1, segment, position);
                    index = nextIndex;
                } else if(v == 0xDC00) {
                    throw new JsonSerializerException("illegal low surrogate without high surrogate");
                } else {
                    position = serializeCharToSegment3(c, segment, position);
                }
            }
        }
        return position;
    }

    private static int serializeCharToHeap(char c, byte[] bytes, int offset) {
        int v = ESCAPE_TABLE[c];
        if (v == 0) {
            bytes[offset] = (byte) c;
            return offset + 1;
        }
        bytes[offset] = (byte) '\\';
        if (v > 0) {
            bytes[offset + 1] = (byte) v;
            return offset + 2;
        }
        bytes[offset + 1] = (byte) 'u';
        bytes[offset + 2] = (byte) '0';
        bytes[offset + 3] = (byte) '0';
        bytes[offset + 4] = HEX_BYTES[c >>> 4];
        bytes[offset + 5] = HEX_BYTES[c & 0xF];
        return offset + 6;
    }

    private static long serializeCharToSegment(char c, MemorySegment segment, long offset) {
        int v = ESCAPE_TABLE[c];
        if (v == 0) {
            SegmentAccess.setByte(segment, offset, (byte) c);
            return offset + 1L;
        }
        SegmentAccess.setByte(segment, offset, (byte) '\\');
        if (v > 0) {
            SegmentAccess.setByte(segment, offset + 1L, (byte) v);
            return offset + 2L;
        }
        SegmentAccess.setByte(segment, offset + 1L, (byte) 'u');
        SegmentAccess.setByte(segment, offset + 2L, (byte) '0');
        SegmentAccess.setByte(segment, offset + 3L, (byte) '0');
        SegmentAccess.setByte(segment, offset + 4L, HEX_BYTES[c >>> 4]);
        SegmentAccess.setByte(segment, offset + 5L, HEX_BYTES[c & 0xF]);
        return offset + 6L;
    }

    private static int serializeCharToHeap2(char c, byte[] bytes, int offset) {
        bytes[offset] = (byte) (0xC0 | (c >> 6));
        bytes[offset + 1] = (byte) (0x80 | (c & 0x3F));
        return offset + 2;
    }

    private static long serializeCharToSegment2(char c, MemorySegment bytes, long offset) {
        SegmentAccess.setByte(bytes, offset, (byte) (0xC0 | (c >> 6)));
        SegmentAccess.setByte(bytes, offset + 1L, (byte) (0x80 | (c & 0x3F)));
        return offset + 2L;
    }

    private static int serializeCharToHeap3(char c, byte[] bytes, int offset) {
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

    private static int serializeCharToHeap4(char highSurrogate, char lowSurrogate, byte[] bytes, int offset) {
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

    public void serializeNull() {
        writeBuffer.writeBytes((byte) 'n', (byte) 'u', (byte) 'l', (byte) 'l');
    }

    public void serializeByte(byte value) {
        JsonNumberUtil.writeInt(value, writeBuffer);
    }

    private void serializeBooleanToHeap(boolean value, HeapWriteBuffer heapWriteBuffer) {
        final byte[] bytes = heapWriteBuffer.rawByteArray();
        final int position = heapWriteBuffer.intPosition();
        if(value) {
            bytes[position]     = (byte) 't';
            bytes[position + 1] = (byte) 'r';
            bytes[position + 2] = (byte) 'u';
            bytes[position + 3] = (byte) 'e';
            heapWriteBuffer.setPosition(position + 4);
        } else {
            bytes[position]     = (byte) 'f';
            bytes[position + 1] = (byte) 'a';
            bytes[position + 2] = (byte) 'l';
            bytes[position + 3] = (byte) 's';
            bytes[position + 4] = (byte) 'e';
            heapWriteBuffer.setPosition(position + 5);
        }
    }

    private void serializeBooleanToSegment(boolean value, SegmentWriteBuffer segmentWriteBuffer) {
        final MemorySegment segment = segmentWriteBuffer.rawSegment();
        final int position = segmentWriteBuffer.intPosition();
        if(value) {
            SegmentAccess.setByte(segment, position, (byte) 't');
            SegmentAccess.setByte(segment, position + 1L, (byte) 'r');
            SegmentAccess.setByte(segment, position + 2L, (byte) 'u');
            SegmentAccess.setByte(segment, position + 3L, (byte) 'e');
            segmentWriteBuffer.setPosition(position + 4L);
        } else {
            SegmentAccess.setByte(segment, position, (byte) ('f'));
            SegmentAccess.setByte(segment, position + 1L, (byte) 'a');
            SegmentAccess.setByte(segment, position + 2L, (byte) 'l');
            SegmentAccess.setByte(segment, position + 3L, (byte) 's');
            SegmentAccess.setByte(segment, position + 4L, (byte) 'e');
            segmentWriteBuffer.setPosition(position + 5L);
        }
    }

    public void serializeBoolean(boolean value) {
        WriteBuffer w = writeBuffer;
        w.ensureCapacity(5);
        switch (w) {
            case HeapWriteBuffer heapWriteBuffer -> serializeBooleanToHeap(value, heapWriteBuffer);
            case SegmentWriteBuffer segmentWriteBuffer -> serializeBooleanToSegment(value, segmentWriteBuffer);
        }
    }

    public void serializeShort(short value) {
        JsonNumberUtil.writeInt(value, writeBuffer);
    }

    private void serializeSingleCharToHeap(char value, HeapWriteBuffer heapWriteBuffer) {
        final byte[] bytes = heapWriteBuffer.rawByteArray();
        int position = heapWriteBuffer.intPosition();
        bytes[position] = (byte) '"';
        if(value < 0x80) {
            final byte v = ESCAPE_TABLE[value];
            if(v == 0) {
                bytes[position + 1] = (byte) value;
                position += 2;
            } else {
                bytes[position + 1] = (byte) '\\';
                if(v > 0) {
                    bytes[position + 2] = v;
                    position += 3;
                } else {
                    bytes[position + 2] = (byte) 'u';
                    bytes[position + 3] = (byte) '0';
                    bytes[position + 4] = (byte) '0';
                    bytes[position + 5] = HEX_BYTES[(value >>> 4) & 0xF];
                    bytes[position + 6] = HEX_BYTES[value & 0xf];
                    position += 7;
                }
            }
        } else if(value < 0x800) {
            bytes[position + 1] = (byte) (0xC0 | (value >> 6));
            bytes[position + 2] = (byte) (0x80 | (value & 0x3f));
            position += 3;
        } else {
            bytes[position + 1] = (byte) (0xE0 | (value >> 12));
            bytes[position + 2] = (byte) (0x80 | ((value >> 6) & 0x3F));
            bytes[position + 3] = (byte) (0x80 | (value & 0x3F));
            position += 4;
        }
        bytes[position] = (byte) '"';
        heapWriteBuffer.setPosition(position + 1);
    }

    private void serializeSingleCharToSegment(char value, SegmentWriteBuffer segmentWriteBuffer) {
        final MemorySegment segment = segmentWriteBuffer.rawSegment();
        long position = segmentWriteBuffer.longPosition();
        SegmentAccess.setByte(segment, position, (byte) '"');
        if(value < 0x80) {
            final byte v = ESCAPE_TABLE[value];
            if(v == 0) {
                SegmentAccess.setByte(segment, position + 1L, (byte) value);
                position += 2L;
            } else {
                SegmentAccess.setByte(segment, position + 1L, (byte) '\\');
                if(v > 0) {
                    SegmentAccess.setByte(segment, position + 2L, v);
                    position += 3L;
                } else {
                    SegmentAccess.setByte(segment, position + 2L, (byte) 'u');
                    SegmentAccess.setByte(segment, position + 3L, (byte) '0');
                    SegmentAccess.setByte(segment, position + 4L, (byte) '0');
                    SegmentAccess.setByte(segment, position + 5L, HEX_BYTES[(value >>> 4) & 0xF]);
                    SegmentAccess.setByte(segment, position + 6L, HEX_BYTES[value & 0xf]);
                    position += 7L;
                }
            }
        } else if(value < 0x800) {
            SegmentAccess.setByte(segment, position + 1L, (byte) (0xC0 | (value >> 6)));
            SegmentAccess.setByte(segment, position + 2L, (byte) (0x80 | (value & 0x3f)));
            position += 3L;
        } else {
            SegmentAccess.setByte(segment, position + 1L, (byte) (0xE0 | (value >> 12)));
            SegmentAccess.setByte(segment, position + 2L, (byte) (0x80 | ((value >> 6) & 0x3F)));
            SegmentAccess.setByte(segment, position + 3L, (byte) (0x80 | (value & 0x3F)));
            position += 4L;
        }
        SegmentAccess.setByte(segment, position, (byte) '"');
        segmentWriteBuffer.setPosition(position + 1L);
    }

    public void serializeChar(char value) {
        if (Character.isSurrogate(value)) {
            throw new IllegalArgumentException("surrogates not supported");
        }
        final WriteBuffer w = this.writeBuffer;
        w.ensureCapacity(8);
        switch (w) {
            case HeapWriteBuffer heapWriteBuffer -> serializeSingleCharToHeap(value, heapWriteBuffer);
            case SegmentWriteBuffer segmentWriteBuffer -> serializeSingleCharToSegment(value, segmentWriteBuffer);
        }
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
        final JsonIndentationLevel indentationLevel = option.indentationLevel();
        if(indentationLevel == JsonIndentationLevel.NONE) {
            return ;
        }
        int spaces = (indentationLevel == JsonIndentationLevel.TWO ? 2 : 4) * indent; // no overflow, indent is limited by nested size
        final WriteBuffer w = this.writeBuffer;
        w.ensureCapacity(Math.incrementExact(spaces));
        switch (w) {
            case HeapWriteBuffer heapWriteBuffer -> {
                final byte[] bytes = heapWriteBuffer.rawByteArray();
                final int position = heapWriteBuffer.intPosition();
                bytes[position] = (byte) '\n';
                Arrays.fill(bytes, position + 1, position + 1 + spaces, (byte) ' ');
                heapWriteBuffer.setPosition(position + 1 + spaces);
            }
            case SegmentWriteBuffer segmentWriteBuffer -> {
                final MemorySegment segment = segmentWriteBuffer.rawSegment();
                final long position = segmentWriteBuffer.longPosition();
                SegmentAccess.setByte(segment, position, (byte) '\n');
                segment.asSlice(position + 1L, spaces).fill((byte) ' ');
                segmentWriteBuffer.setPosition(position + 1L + spaces);
            }
        }
    }

    private <T> void serializeObjArray(T[] arr, int indent, ElementSerializer<T> elementSerializer) {
        final WriteBuffer w = this.writeBuffer;
        if (arr.length == 0) {
            w.writeBytes((byte) '[', (byte) ']');
            return ;
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
            case SegmentWriteBuffer segmentWriteBuffer -> serializeEscapedUtf8BytesToSegment(utf8Bytes, segmentWriteBuffer);
        }
    }

    public void serializeEscapedCharSequence(CharSequence charSequence) {
        serializeEscapedString(charSequence.toString());
    }

    public void serializeEscapedString(String str) {
        final WriteBuffer w = writeBuffer;
        if (str.isEmpty()) {
            w.writeBytes((byte) '"', (byte) '"');
            return ;
        }
        // expansion factor 6 covers worst-case escape (backslash + u + 4 hex digits), plus two quote
        w.ensureCapacity(Math.addExact(Math.multiplyExact(str.length(), 6), 2));
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

    private void serializeRawUtf8BytesStr(byte[] utf8Bytes) {
        final WriteBuffer w = this.writeBuffer;
        w.ensureCapacity(Math.addExact(utf8Bytes.length, 2));
        switch (w) {
            case HeapWriteBuffer heapWriteBuffer -> {
                final byte[] bytes = heapWriteBuffer.rawByteArray();
                final int position = heapWriteBuffer.intPosition();
                bytes[position] = (byte) '"';
                System.arraycopy(utf8Bytes, 0, bytes, position + 1, utf8Bytes.length);
                bytes[position + utf8Bytes.length + 1] = (byte) '"';
                heapWriteBuffer.setPosition(position + utf8Bytes.length + 2);
            }
            case SegmentWriteBuffer segmentWriteBuffer -> {
                final MemorySegment segment = segmentWriteBuffer.rawSegment();
                final long position = segmentWriteBuffer.longPosition();
                SegmentAccess.setByte(segment, position, (byte) '"');
                MemorySegment.copy(utf8Bytes, 0, segment, ValueLayout.JAVA_BYTE, position + 1L, utf8Bytes.length);
                SegmentAccess.setByte(segment, position + utf8Bytes.length + 1L, (byte) '"');
                segmentWriteBuffer.setPosition(position + utf8Bytes.length + 2L);
            }
        }
    }

    public void serializeEnum(Enum<?> enumValue) {
        MarshallInfo inf = Marshalls.enumItemMarshallInfo(enumValue);
        if (inf == null) {
            serializeEscapedString(enumValue.name());
        } else if (inf.mappedNameSimple()) {
            serializeRawUtf8BytesStr(inf.mappedNameUtf8Bytes());
        } else {
            serializeEscapedUtf8Bytes(inf.mappedNameUtf8Bytes());
        }
    }

    public void serializeEnumArray(Enum<?>[] arr, int indent) {
        serializeObjArray(arr, indent, JsonSerializerContext::serializeEnum);
    }

    public static JsonSerializeFunc builtinSerializeObjFunc(Class<?> rawType) {
        return BUILTIN_SERIALIZE_OBJ_FUNC_MAP.get(rawType);
    }

    public static JsonSerializeFunc builtinSerializeArrayFunc(Class<?> rawType) {
        return BUILTIN_SERIALIZE_ARRAY_FUNC_MAP.get(rawType);
    }

    // builtin type has the highest priority
    // then check if current type could be override by option
    // enum must be specially treated
    // finally assuming marshallable
    public JsonSerializeFunc valueSerializeFunc(Class<?> rawType) {
        if (rawType.isArray()) {
            JsonSerializeFunc builtinSerializeArrFunc = builtinSerializeArrayFunc(rawType);
            if (builtinSerializeArrFunc != null) {
                return builtinSerializeArrFunc;
            }
            JsonSerializeFunc customArrFunc = option.customArrFunc(rawType);
            if (customArrFunc != null) {
                return customArrFunc;
            }
            Class<?> componentType = rawType.componentType();
            if(componentType.isEnum()) {
                return (o, i, c) -> {
                    c.serializeEnumArray((Enum<?>[]) o, i);
                    return JsonSerializeResult.Continue;
                };
            }
            return (o, _, c) -> {
                c.setObj(o);
                return JsonSerializeResult.NewArray;
            };
        }
        JsonSerializeFunc builtinSerializeFunc = builtinSerializeObjFunc(rawType);
        if (builtinSerializeFunc != null) {
            return builtinSerializeFunc;
        }
        JsonSerializeFunc customFunc = option.customFunc(rawType);
        if (customFunc != null) {
            return customFunc;
        }
        if (rawType.isEnum()) {
            return (o, _, c) -> {
                c.serializeEnum((Enum<?>) o);
                return JsonSerializeResult.Continue;
            };
        }
        return (o, _, c) -> {
            c.setObj(o);
            return JsonSerializeResult.NewMarshallable;
        };
    }

    @FunctionalInterface
    interface ElementSerializer<T> {
        void serialize(JsonSerializerContext c, T t);
    }

}
