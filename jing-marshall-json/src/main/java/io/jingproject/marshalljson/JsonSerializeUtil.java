package io.jingproject.marshalljson;

import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.common.SegmentAccess;
import io.jingproject.common.SegmentWriteBuffer;
import io.jingproject.common.WriteBuffer;
import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.MarshallInfo;
import io.jingproject.marshall.Marshalls;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;

public final class JsonSerializeUtil {
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

    private static final byte[] WRITER_ESCAPE_TABLE = makeWriterEscapeTable();
    private static final byte[] HEX_BYTES = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);

    private JsonSerializeUtil() {
        throw new UnsupportedOperationException("utility class");
    }
    
    private static byte[] makeWriterEscapeTable() {
        byte[] table = new byte[Byte.MAX_VALUE - Byte.MIN_VALUE + 1];
        for(int i = 0x00; i < 0x1f; i++) {
            table[i] = Byte.MIN_VALUE;
        }
        table[0x22] = BYTE_quote;   // \"
        table[0x5C] = BYTE_rsolidus; // \\
        table[0x2F] = BYTE_solidus;  // \/
        table[0x08] = BYTE_b;  // \b
        table[0x0C] = BYTE_f;  // \f
        table[0x0A] = BYTE_n;  // \n
        table[0x0D] = BYTE_r;  // \r
        table[0x09] = BYTE_t;  // \t
        return table;
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
        writeBuffer.writeBytes(BYTE_colon, BYTE_space);
    }

    public static void serializeNull(WriteBuffer writeBuffer) {
        writeBuffer.writeBytes(BYTE_n, BYTE_u, BYTE_l, BYTE_l);
    }

    public static void serializeByte(byte value, WriteBuffer writeBuffer) {
        JsonNumberUtil.writeInt(value, writeBuffer);
    }

    public static void serializeBoolean(boolean value, WriteBuffer writeBuffer) {
        if(value) {
            writeBuffer.writeBytes(BYTE_t, BYTE_r, BYTE_u, BYTE_e);
        } else {
            writeBuffer.writeBytes(BYTE_f, BYTE_a, BYTE_l, BYTE_s, BYTE_e);
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
        } else if(value < 0x800) {
            writeBuffer.writeBytes((byte) (0xC0 | (value >> 6)),
                    (byte) (0x80 | (value & 0x3F)));
        } else {
            writeBuffer.writeBytes((byte) (0xE0 | (value >> 12)),
                    (byte) (0x80 | ((value >> 6) & 0x3F)),
                    (byte) (0x80 | (value & 0x3F)));
        }
        serializeQuote(writeBuffer);
    }

    private static void serializeAsciiByte(byte b, WriteBuffer writeBuffer) {
        byte v = WRITER_ESCAPE_TABLE[b];
        if(v == 0) {
            writeBuffer.writeByte(b);
        } else if(v > 0) {
            writeBuffer.writeBytes(BYTE_rsolidus, v);
        } else {
            writeBuffer.writeBytes(BYTE_rsolidus, BYTE_u, BYTE_zero, BYTE_zero, HEX_BYTES[(b >>> 4) & 0xF], HEX_BYTES[b & 0xf]);
        }
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
        switch (jsonIndentationLevel) {
            case NONE -> {}
            case TWO -> {
                writeBuffer.writeByte(BYTE_lf);
                writeBuffer.writeRepeated(BYTE_space, indent * 2); // no overflow, indent is limited
            }
            case FOUR -> {
                writeBuffer.writeByte(BYTE_lf);
                writeBuffer.writeRepeated(BYTE_space, indent * 4); // no overflow, indent is limited
            }
            default -> throw new AssertionError();
        }
    }

    private static void serializeObjArray(Object[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer, BiConsumer<Object, WriteBuffer> consumer) {
        assert arr != null && writeBuffer != null;
        if(arr.length == 0) {
            writeBuffer.writeBytes(BYTE_bracket, BYTE_rbracket);
            return ;
        }
        serializeArrayStart(writeBuffer);
        for(int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1, jsonIndentationLevel, writeBuffer);
            Object o = arr[i];
            if(o == null) {
                serializeNull(writeBuffer);
            } else {
                consumer.accept(o, writeBuffer);
            }
            if(i != arr.length - 1) {
                serializeComma(writeBuffer);
            }
        }
        serializeIndent(indent, jsonIndentationLevel, writeBuffer);
        serializeArrayEnd(writeBuffer);
    }

    public static void serializeByteArray(byte[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && writeBuffer != null;
        if(arr.length == 0) {
            writeBuffer.writeBytes(BYTE_bracket, BYTE_rbracket);
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
        serializeObjArray(arr, indent, jsonIndentationLevel, writeBuffer, (o, w) -> serializeByte((Byte) o, w));
    }

    public static void serializeBooleanArray(boolean[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && writeBuffer != null;
        if (arr.length == 0) {
            writeBuffer.writeBytes(BYTE_bracket, BYTE_rbracket);
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
        serializeObjArray(arr, indent, jsonIndentationLevel, writeBuffer, (o, w) -> serializeBoolean((Boolean) o, w));
    }

    public static void serializeShortArray(short[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && writeBuffer != null;
        if (arr.length == 0) {
            writeBuffer.writeBytes(BYTE_bracket, BYTE_rbracket);
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
        serializeObjArray(arr, indent, jsonIndentationLevel, writeBuffer, (o, w) -> serializeShort((Short) o, w));
    }

    public static void serializeCharArray(char[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && writeBuffer != null;
        if (arr.length == 0) {
            writeBuffer.writeBytes(BYTE_bracket, BYTE_rbracket);
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
        serializeObjArray(arr, indent, jsonIndentationLevel, writeBuffer, (o, w) -> serializeChar((Character) o, w));
    }

    public static void serializeIntArray(int[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && writeBuffer != null;
        if (arr.length == 0) {
            writeBuffer.writeBytes(BYTE_bracket, BYTE_rbracket);
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
        serializeObjArray(arr, indent, jsonIndentationLevel, writeBuffer, (o, w) -> serializeInt((Integer) o, w));
    }

    public static void serializeLongArray(long[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && writeBuffer != null;
        if (arr.length == 0) {
            writeBuffer.writeBytes(BYTE_bracket, BYTE_rbracket);
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
        serializeObjArray(arr, indent, jsonIndentationLevel, writeBuffer, (o, w) -> serializeLong((Long) o, w));
    }

    public static void serializeFloatArray(float[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && writeBuffer != null;
        if (arr.length == 0) {
            writeBuffer.writeBytes(BYTE_bracket, BYTE_rbracket);
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
        serializeObjArray(arr, indent, jsonIndentationLevel, writeBuffer, (o, w) -> serializeFloat((Float) o, w));
    }

    public static void serializeDoubleArray(double[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && writeBuffer != null;
        if (arr.length == 0) {
            writeBuffer.writeBytes(BYTE_bracket, BYTE_rbracket);
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
        serializeObjArray(arr, indent, jsonIndentationLevel, writeBuffer, (o, w) -> serializeDouble((Double) o, w));
    }

    public static void serializeNonEscapedUtf8Bytes(byte[] utf8Bytes, WriteBuffer writeBuffer) {
        writeBuffer.ensureCapacity(utf8Bytes.length + 2);
        switch (writeBuffer) {
            case HeapWriteBuffer heapWriteBuffer -> {
                final byte[] bytes = heapWriteBuffer.rawByteArray();
                int position = heapWriteBuffer.intPosition();
                bytes[position++] = BYTE_quote;
                System.arraycopy(utf8Bytes, 0, bytes, position, utf8Bytes.length);
                position += utf8Bytes.length;
                bytes[position++] = BYTE_quote;
                heapWriteBuffer.setPosition(position);
            }
            case SegmentWriteBuffer segmentWriteBuffer -> {
                final MemorySegment segment = segmentWriteBuffer.rawSegment();
                long position = segmentWriteBuffer.longPosition();
                SegmentAccess.setByte(segment, position++, BYTE_quote);
                MemorySegment.copy(utf8Bytes, 0, segment, ValueLayout.JAVA_BYTE, position, utf8Bytes.length);
                position += utf8Bytes.length;
                SegmentAccess.setByte(segment, position++, BYTE_quote);
                segmentWriteBuffer.setPosition(position);
            }
        }
    }

    public static void serializeEscapedUtf8Bytes(byte[] utf8Bytes, WriteBuffer writeBuffer) {
        assert utf8Bytes != null && writeBuffer != null;
        final int len = utf8Bytes.length;
        // expansion factor 6 covers worst-case escape (backslash + u + 4 hex digits), plus two quote
        writeBuffer.ensureCapacity(Math.addExact(Math.multiplyExact(len, 6), 2));
        switch (writeBuffer) {
            case HeapWriteBuffer heapWriteBuffer -> serializeEscapedUtf8BytesToHeap(utf8Bytes, len, heapWriteBuffer);
            case SegmentWriteBuffer segmentWriteBuffer -> serializeEscapedUtf8BytesToSegment(utf8Bytes, len, segmentWriteBuffer);
            default -> throw new AssertionError();
        }
    }

    private static void serializeEscapedUtf8BytesToHeap(byte[] utf8Bytes, int len, HeapWriteBuffer heapWriteBuffer) {
        final byte[] bytes = heapWriteBuffer.rawByteArray();
        int position = heapWriteBuffer.intPosition();
        bytes[position++] = BYTE_quote;
        int start = 0;
        for(int index = 0; index < len; index++) {
            byte b = utf8Bytes[index];
            byte v = WRITER_ESCAPE_TABLE[b];
            if(v == 0) {
                continue ;
            }
            if(index > start) {
                int available = index - start;
                System.arraycopy(utf8Bytes, start, bytes, position, available);
                position += available;
            }
            bytes[position++] = BYTE_rsolidus;
            if(v > 0) {
                bytes[position++] = v;
            } else {
                bytes[position++] = BYTE_u;
                bytes[position++] = BYTE_zero;
                bytes[position++] = BYTE_zero;
                bytes[position++] = HEX_BYTES[(b >>> 4) & 0xF];
                bytes[position++] = HEX_BYTES[b & 0xF];
            }
            start = index + 1;
        }
        if(start < len) {
            int available = len - start;
            System.arraycopy(utf8Bytes, start, bytes, position, available);
            position += available;
        }
        bytes[position++] = BYTE_quote;
        heapWriteBuffer.setPosition(position);
    }

    private static void serializeEscapedUtf8BytesToSegment(byte[] utf8Bytes, int len, SegmentWriteBuffer segmentWriteBuffer) {
        final MemorySegment segment = segmentWriteBuffer.rawSegment();
        long position = segmentWriteBuffer.longPosition();
        SegmentAccess.setByte(segment, position++, BYTE_quote);
        int start = 0;
        for(int index = 0; index < len; index++) {
            byte b = utf8Bytes[index];
            byte v = WRITER_ESCAPE_TABLE[b];
            if(v == 0) {
                continue ;
            }
            if(index > start) {
                int available = index - start;
                MemorySegment.copy(utf8Bytes, start, segment, ValueLayout.JAVA_BYTE, position, available);
                position += available;
            }
            SegmentAccess.setByte(segment, position++, BYTE_rsolidus);
            if(v > 0) {
                SegmentAccess.setByte(segment, position++, v);
            } else {
                SegmentAccess.setByte(segment, position++, BYTE_u);
                SegmentAccess.setByte(segment, position++, BYTE_zero);
                SegmentAccess.setByte(segment, position++, BYTE_zero);
                SegmentAccess.setByte(segment, position++, HEX_BYTES[(b >>> 4) & 0xF]);
                SegmentAccess.setByte(segment, position++, HEX_BYTES[b & 0xF]);
            }
            start = index + 1;
        }
        if(start < len) {
            int available = len - start;
            MemorySegment.copy(utf8Bytes, start, segment, ValueLayout.JAVA_BYTE, position, available);
            position += available;
        }
        SegmentAccess.setByte(segment, position++, BYTE_quote);
        segmentWriteBuffer.setPosition(position);
    }

    public static void serializeEscapedCharSequence(CharSequence charSequence, WriteBuffer writeBuffer) {
        assert charSequence != null && writeBuffer != null;
        serializeEscapedString(charSequence.toString(), writeBuffer);
    }

    public static void serializeEscapedString(String str, WriteBuffer writeBuffer) {
        assert str != null && writeBuffer != null;
        final int len = str.length();
        // expansion factor 6 covers worst-case escape (backslash + u + 4 hex digits), plus two quote
        writeBuffer.ensureCapacity(Math.addExact(Math.multiplyExact(len, 6), 2));
        switch (writeBuffer) {
            case HeapWriteBuffer heapWriteBuffer -> serializeEscapedStringToHeap(str, len, heapWriteBuffer);
            case SegmentWriteBuffer segmentWriteBuffer -> serializeEscapedStringToSegment(str, len, segmentWriteBuffer);
            default -> throw new AssertionError();
        }
    }

    private static void serializeEscapedStringToHeap(String str, int len, HeapWriteBuffer heapWriteBuffer) {
        final byte[] bytes = heapWriteBuffer.rawByteArray();
        int position = heapWriteBuffer.intPosition();
        bytes[position++] = BYTE_quote;
        int index = 0;
        while (index < len) {
            char c = str.charAt(index++);
            if(c < 0x80) {
                position = serializeEscapedCharToHeap(c, bytes, position);
            } else if(c < 0x800) {
                bytes[position++] = (byte) (0xC0 | (c >> 6));
                bytes[position++] = (byte) (0x80 | (c & 0x3F));
            } else if(Character.isHighSurrogate(c)) {
                if(index == len) {
                    throw new IllegalArgumentException("invalid high surrogate without low surrogate characters : " + c);
                }
                char c2 = str.charAt(index++);
                if(!Character.isLowSurrogate(c2)) {
                    throw new IllegalArgumentException("invalid low surrogate : " + c2);
                }
                int cp = Character.toCodePoint(c, c2);
                bytes[position++] = (byte) (0xF0 | (cp >> 18));
                bytes[position++] = (byte) (0x80 | ((cp >> 12) & 0x3F));
                bytes[position++] = (byte) (0x80 | ((cp >> 6) & 0x3F));
                bytes[position++] = (byte) (0x80 | (cp & 0x3F));
            } else if(Character.isLowSurrogate(c)) {
                throw new IllegalArgumentException("invalid low surrogate without high surrogate: " + c);
            } else {
                bytes[position++] = (byte) (0xE0 | (c >> 12));
                bytes[position++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                bytes[position++] = (byte) (0x80 | (c & 0x3F));
            }
        }
        bytes[position++] = BYTE_quote;
        heapWriteBuffer.setPosition(position);
    }

    private static int serializeEscapedCharToHeap(char c, byte[] bytes, int position) {
        int v = WRITER_ESCAPE_TABLE[c];
        if(v == 0) {
            bytes[position++] = (byte) c;
        } else if(v > 0) {
            bytes[position++] = BYTE_rsolidus;
            bytes[position++] = (byte) v;
        } else {
            bytes[position++] = BYTE_rsolidus;
            bytes[position++] = BYTE_u;
            bytes[position++] = BYTE_zero;
            bytes[position++] = BYTE_zero;
            bytes[position++] = HEX_BYTES[c >>> 4];
            bytes[position++] = HEX_BYTES[c & 0xF];
        }
        return position;
    }

    private static void serializeEscapedStringToSegment(String str, int len, SegmentWriteBuffer segmentWriteBuffer) {
        final MemorySegment segment = segmentWriteBuffer.rawSegment();
        long position = segmentWriteBuffer.longPosition();
        SegmentAccess.setByte(segment, position++, BYTE_quote);
        int index = 0;
        while (index < len) {
            char c = str.charAt(index++);
            if(c < 0x80) {
                position = serializeEscapedCharToSegment(c, segment, position);
            } else if(c < 0x800) {
                SegmentAccess.setByte(segment, position++, (byte) (0xC0 | (c >> 6)));
                SegmentAccess.setByte(segment, position++, (byte) (0x80 | (c & 0x3F)));
            } else if(Character.isHighSurrogate(c)) {
                if(index == len) {
                    throw new IllegalArgumentException("invalid high surrogate without low surrogate characters : " + c);
                }
                char c2 = str.charAt(index++);
                if(!Character.isLowSurrogate(c2)) {
                    throw new IllegalArgumentException("invalid low surrogate : " + c2);
                }
                int cp = Character.toCodePoint(c, c2);
                SegmentAccess.setByte(segment, position++, (byte) (0xF0 | (cp >> 18)));
                SegmentAccess.setByte(segment, position++, (byte) (0x80 | ((cp >> 12) & 0x3F)));
                SegmentAccess.setByte(segment, position++, (byte) (0x80 | ((cp >> 6) & 0x3F)));
                SegmentAccess.setByte(segment, position++, (byte) (0x80 | (cp & 0x3F)));
            } else if(Character.isLowSurrogate(c)) {
                throw new IllegalArgumentException("invalid low surrogate without high surrogate: " + c);
            } else {
                SegmentAccess.setByte(segment, position++, (byte) (0xE0 | (c >> 12)));
                SegmentAccess.setByte(segment, position++, (byte) (0x80 | ((c >> 6) & 0x3F)));
                SegmentAccess.setByte(segment, position++, (byte) (0x80 | (c & 0x3F)));
            }
        }
        SegmentAccess.setByte(segment, position++, BYTE_quote);
        segmentWriteBuffer.setPosition(position);
    }

    private static long serializeEscapedCharToSegment(char c, MemorySegment segment, long position) {
        int v = WRITER_ESCAPE_TABLE[c];
        if(v == 0) {
            SegmentAccess.setByte(segment, position++, (byte) c);
        } else if(v > 0) {
            SegmentAccess.setByte(segment, position++, BYTE_rsolidus);
            SegmentAccess.setByte(segment, position++, (byte) v);
        } else {
            SegmentAccess.setByte(segment, position++, BYTE_rsolidus);
            SegmentAccess.setByte(segment, position++, BYTE_u);
            SegmentAccess.setByte(segment, position++, BYTE_zero);
            SegmentAccess.setByte(segment, position++, BYTE_zero);
            SegmentAccess.setByte(segment, position++, HEX_BYTES[c >>> 4]);
            SegmentAccess.setByte(segment, position++, HEX_BYTES[c & 0xF]);
        }
        return position;
    }

    public static void serializeEscapedCharSequenceArray(CharSequence[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        serializeObjArray(arr, indent, jsonIndentationLevel, writeBuffer, (o, w) -> serializeEscapedCharSequence((CharSequence) o, w));
    }

    public static void serializeEscapedStringArray(String[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        serializeObjArray(arr, indent, jsonIndentationLevel, writeBuffer, (o, w) -> serializeEscapedString((String) o, w));
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

    public static void serializeJsonPrimitiveTypeArray(JsonPrimitiveType[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        serializeObjArray(arr, indent, jsonIndentationLevel, writeBuffer, (o, w) -> serializeJsonPrimitiveType((JsonPrimitiveType) o, w));
    }

    public static void serializeJsonBoolType(JsonBoolType jsonBoolType, WriteBuffer writeBuffer) {
        assert jsonBoolType != null && writeBuffer != null;
        serializeBoolean(jsonBoolType.data(), writeBuffer);
    }

    public static void serializeJsonBoolTypeArray(JsonBoolType[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        serializeObjArray(arr, indent, jsonIndentationLevel, writeBuffer, (o, w) -> serializeJsonBoolType((JsonBoolType) o, w));
    }

    public static void serializeJsonNumberType(JsonNumberType jsonNumberType, WriteBuffer writeBuffer) {
        assert jsonNumberType != null && writeBuffer != null;
        writeBuffer.writeBytes(jsonNumberType.data());
    }

    public static void serializeJsonNumberTypeArray(JsonNumberType[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        serializeObjArray(arr, indent, jsonIndentationLevel, writeBuffer, (o, w) -> serializeJsonNumberType((JsonNumberType) o, w));
    }

    public static void serializeJsonStrType(JsonStrType jsonStrType, WriteBuffer writeBuffer) {
        assert jsonStrType != null && writeBuffer != null;
        serializeEscapedString(jsonStrType.data(), writeBuffer);
    }

    public static void serializeJsonStrTypeArray(JsonStrType[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        serializeObjArray(arr, indent, jsonIndentationLevel, writeBuffer, (o, w) -> serializeJsonStrType((JsonStrType) o, w));
    }

    public static void serializeEnum(Enum<?> enumValue, Class<?> rawType, WriteBuffer writeBuffer) {
        assert enumValue != null && rawType != null && writeBuffer != null;
        MarshallFacade fc = Marshalls.getMarshallFacade(rawType);
        if(fc == null) {
            serializeEscapedString(enumValue.name(), writeBuffer);
        } else {
            MarshallInfo marshallInfo = fc.marshallInfoByIndex(enumValue.ordinal());
            if(marshallInfo.mappedNameSimple()) {
                serializeQuote(writeBuffer);
                writeBuffer.writeBytes(marshallInfo.mappedNameUtf8Bytes());
                serializeQuote(writeBuffer);
            } else {
                serializeEscapedUtf8Bytes(marshallInfo.mappedNameUtf8Bytes(), writeBuffer);
            }
        }
    }

    public static JsonSerializeFunc valueSerializeFunc(JsonSerializerOption option, Class<?> rawType) {
        // builtin type has the highest priority
        if(rawType.isArray()) {
            JsonSerializeFunc builtinSerializeArrFunc = builtinSerializeArrayFunc(rawType);
            if(builtinSerializeArrFunc != null) {
                return builtinSerializeArrFunc;
            }
            return (_, _, o, _) -> new JsonSerializeResult.JsonSerializeNewArray((Object[]) o);
        }
        JsonSerializeFunc builtinSerializeFunc = builtinSerializeObjFunc(rawType);
        if(builtinSerializeFunc != null) {
            return builtinSerializeFunc;
        }
        // check if current type could be override by option
        JsonSerializeFunc customFunc = option.customFunc(rawType);
        if(customFunc != null) {
            return customFunc;
        }
        // enum must be specially treated
        if(rawType.isEnum()) {
            return enumSerializeFunc(rawType);
        }
        // assuming marshallable
        return (_, _, o, _) -> new JsonSerializeResult.JsonSerializeNewMarshallable(o);
    }

    public static JsonSerializeFunc enumSerializeFunc(Class<?> rawType) {
        MarshallFacade fc = Marshalls.getMarshallFacade(rawType);
        if(fc == null) {
            return (_, w, o, _) -> {
                serializeEscapedString(((Enum<?>) o).name(), w);
                return JsonSerializeResult.CONTINUE;
            };
        } else {
            // have to use capture lambda
            return (_, w, o, _) -> {
                MarshallInfo marshallInfo = fc.marshallInfoByIndex(((Enum<?>) o).ordinal());
                if(marshallInfo.mappedNameSimple()) {
                    serializeQuote(w);
                    w.writeBytes(marshallInfo.mappedNameUtf8Bytes());
                    serializeQuote(w);
                } else {
                    serializeEscapedUtf8Bytes(marshallInfo.mappedNameUtf8Bytes(), w);
                }
                return JsonSerializeResult.CONTINUE;
            };
        }
    }

    public static JsonSerializeFunc builtinSerializeObjFunc(Class<?> rawType) {
        if(rawType == Byte.class) {
            return (_, w, o, _) -> {
                serializeByte((Byte) o, w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if(rawType == Boolean.class) {
            return  (_, w, o, _) -> {
                serializeBoolean((Boolean) o, w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if(rawType == Short.class) {
            return (_, w, o, _) -> {
                serializeShort((Short) o, w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if(rawType == Character.class) {
            return (_, w, o, _) -> {
                serializeChar((Character) o, w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if(rawType == Integer.class) {
            return (_, w, o, _) -> {
                serializeInt((Integer) o, w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if(rawType == Long.class) {
            return (_, w, o, _) -> {
                serializeLong((Long) o, w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if(rawType == Float.class) {
            return (_, w, o, _) -> {
                serializeFloat((Float) o, w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if(rawType == Double.class) {
            return (_, w, o, _) -> {
                serializeDouble((Double) o, w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if(rawType == CharSequence.class) {
            return (_, w, o, _) -> {
                serializeEscapedCharSequence((CharSequence) o, w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if(rawType == String.class) {
            return (_, w, o, _) -> {
                serializeEscapedString((String) o, w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if(rawType == JsonPrimitiveType.class) {
            return (_, w, o, _) -> {
                serializeJsonPrimitiveType((JsonPrimitiveType) o, w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if(rawType == JsonBoolType.class) {
            return (_, w, o, _) -> {
                serializeJsonBoolType((JsonBoolType) o, w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if(rawType == JsonNumberType.class) {
            return (_, w, o, _) -> {
                serializeJsonNumberType((JsonNumberType) o, w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if(rawType == JsonStrType.class) {
            return (_, w, o, _) -> {
                serializeJsonStrType((JsonStrType) o, w);
                return JsonSerializeResult.CONTINUE;
            };
        } else {
            return null;
        }
    }

    public static JsonSerializeFunc builtinSerializeArrayFunc(Class<?> rawType) {
        if(rawType == byte[].class) {
            return (op, w, o, ind) -> {
                serializeByteArray((byte[]) o, ind, op.indentationLevel(), w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if (rawType == boolean[].class) {
            return (op, w, o, ind) -> {
                serializeBooleanArray((boolean[]) o, ind, op.indentationLevel(), w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if (rawType == short[].class) {
            return (op, w, o, ind) -> {
                serializeShortArray((short[]) o, ind, op.indentationLevel(), w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if (rawType == char[].class) {
            return (op, w, o, ind) -> {
                serializeCharArray((char[]) o, ind, op.indentationLevel(), w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if (rawType == int[].class) {
            return (op, w, o, ind) -> {
                serializeIntArray((int[]) o, ind, op.indentationLevel(), w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if (rawType == long[].class) {
            return (op, w, o, ind) -> {
                serializeLongArray((long[]) o, ind, op.indentationLevel(), w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if (rawType == float[].class) {
            return (op, w, o, ind) -> {
                serializeFloatArray((float[]) o, ind, op.indentationLevel(), w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if (rawType == double[].class) {
            return (op, w, o, ind) -> {
                serializeDoubleArray((double[]) o, ind, op.indentationLevel(), w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if (rawType == Byte[].class) {
            return (op, w, o, ind) -> {
                serializeByteWrapperArray((Byte[]) o, ind, op.indentationLevel(), w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if (rawType == Boolean[].class) {
            return (op, w, o, ind) -> {
                serializeBooleanWrapperArray((Boolean[]) o, ind, op.indentationLevel(), w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if (rawType == Short[].class) {
            return (op, w, o, ind) -> {
                serializeShortWrapperArray((Short[]) o, ind, op.indentationLevel(), w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if (rawType == Character[].class) {
            return (op, w, o, ind) -> {
                serializeCharWrapperArray((Character[]) o, ind, op.indentationLevel(), w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if (rawType == Integer[].class) {
            return (op, w, o, ind) -> {
                serializeIntWrapperArray((Integer[]) o, ind, op.indentationLevel(), w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if (rawType == Long[].class) {
            return (op, w, o, ind) -> {
                serializeLongWrapperArray((Long[]) o, ind, op.indentationLevel(), w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if (rawType == Float[].class) {
            return (op, w, o, ind) -> {
                serializeFloatWrapperArray((Float[]) o, ind, op.indentationLevel(), w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if (rawType == Double[].class) {
            return (op, w, o, ind) -> {
                serializeDoubleWrapperArray((Double[]) o, ind, op.indentationLevel(), w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if (rawType == CharSequence[].class) {
            return (op, w, o, ind) -> {
                serializeEscapedCharSequenceArray((CharSequence[]) o, ind, op.indentationLevel(), w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if (rawType == String[].class) {
            return (op, w, o, ind) -> {
                serializeEscapedStringArray((String[]) o, ind, op.indentationLevel(), w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if (rawType == JsonPrimitiveType[].class) {
            return (op, w, o, ind) -> {
                serializeJsonPrimitiveTypeArray((JsonPrimitiveType[]) o, ind, op.indentationLevel(), w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if (rawType == JsonBoolType[].class) {
            return (op, w, o, ind) -> {
                serializeJsonBoolTypeArray((JsonBoolType[]) o, ind, op.indentationLevel(), w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if (rawType == JsonNumberType[].class) {
            return (op, w, o, ind) -> {
                serializeJsonNumberTypeArray((JsonNumberType[]) o, ind, op.indentationLevel(), w);
                return JsonSerializeResult.CONTINUE;
            };
        } else if (rawType == JsonStrType[].class) {
            return (op, w, o, ind) -> {
                serializeJsonStrTypeArray((JsonStrType[]) o, ind, op.indentationLevel(), w);
                return JsonSerializeResult.CONTINUE;
            };
        } else {
            return null;
        }
    }


}
