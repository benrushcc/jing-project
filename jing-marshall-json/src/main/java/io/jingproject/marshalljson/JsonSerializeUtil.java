package io.jingproject.marshalljson;

import io.jingproject.common.*;
import io.jingproject.marshall.MarshallFacade;
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

// indent never overflows int, as its value is limited by the practical JSON nesting depth.
public final class JsonSerializeUtil {
    // whether to escape '/', which was suggested for safely embedding JSON in HTML; it's not required by the JSON spec, so we leave it optional and default to false.
    private static final boolean ESCAPE_SLASH =
            Boolean.parseBoolean(System.getProperty("jing.marshalljson.escapeslash", "false"));
    private static final VectorSpecies<Short> SHORT_SPECIES;
    private static final VectorSpecies<Byte> BYTE_SPECIES;
    private static final int NO_MOVE_MIN;
    private static final int NO_MOVE_MAX;
    private static final byte[] WRITER_ESCAPE_TABLE = makeWriterEscapeTable();
    private static final byte[] HEX_BYTES = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);

    static {
        try {
            Class<Os> _ = MethodHandles.lookup().ensureInitialized(Os.class);
        } catch (IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
        int vecSize = Integer.parseInt(System.getProperty("jing.marshalljson.serialize.vecsize", "-1"));
        if(vecSize < 0) {
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
        NO_MOVE_MIN = SHORT_SPECIES.length();
        NO_MOVE_MAX = Math.multiplyExact(NO_MOVE_MIN, 1024); // for 128bit/512bit, string more than 8k/32k chars will not be moved
    }

    private JsonSerializeUtil() {
        throw new UnsupportedOperationException("utility class");
    }
    
    private static byte[] makeWriterEscapeTable() {
        byte[] table = new byte[Byte.MAX_VALUE - Byte.MIN_VALUE + 1];
        for(int i = 0x00; i < 0x20; i++) {
            table[i] = Byte.MIN_VALUE;
        }
        table[0x22] = (byte) '"';   // \"
        table[0x5C] = (byte) '\\'; // \\
        if(ESCAPE_SLASH) {
            table[0x2F] = (byte) '/';  // \/
        }
        table[0x08] = (byte) 'b';  // \b
        table[0x0C] = (byte) 'f';  // \f
        table[0x0A] = (byte) 'n';  // \n
        table[0x0D] = (byte) 'r';  // \r
        table[0x09] = (byte) 't';  // \t
        return table;
    }

    public static void serializeObjStart(WriteBuffer writeBuffer) {
        assert writeBuffer != null;
        writeBuffer.writeByte((byte) '{');
    }

    public static void serializeObjEnd(WriteBuffer writeBuffer) {
        assert writeBuffer != null;
        writeBuffer.writeByte((byte) '}');
    }

    public static void serializeArrayStart(WriteBuffer writeBuffer) {
        assert writeBuffer != null;
        writeBuffer.writeByte((byte) '[');
    }

    public static void serializeArrayEnd(WriteBuffer writeBuffer) {
        assert writeBuffer != null;
        writeBuffer.writeByte((byte) ']');
    }

    public static void serializeQuote(WriteBuffer writeBuffer) {
        assert writeBuffer != null;
        writeBuffer.writeByte((byte) '"');
    }

    public static void serializeComma(WriteBuffer writeBuffer) {
        assert writeBuffer != null;
        writeBuffer.writeByte((byte) ',');
    }

    public static void serializeKvSep(WriteBuffer writeBuffer) {
        assert writeBuffer != null;
        writeBuffer.writeBytes((byte) ':', (byte) ' ');
    }

    public static void serializeNull(WriteBuffer writeBuffer) {
        assert writeBuffer != null;
        writeBuffer.writeBytes((byte) 'n', (byte) 'u', (byte) 'l', (byte) 'l');
    }

    public static void serializeByte(byte value, WriteBuffer writeBuffer) {
        assert writeBuffer != null;
        JsonNumberUtil.writeInt(value, writeBuffer);
    }

    public static void serializeBoolean(boolean value, WriteBuffer writeBuffer) {
        assert writeBuffer != null;
        if(value) {
            writeBuffer.writeBytes((byte) 't', (byte) 'r', (byte) 'u', (byte) 'e');
        } else {
            writeBuffer.writeBytes((byte) 'f', (byte) 'a', (byte) 'l', (byte) 's', (byte) 'e');
        }
    }

    public static void serializeShort(short value, WriteBuffer writeBuffer) {
        assert writeBuffer != null;
        JsonNumberUtil.writeInt(value, writeBuffer);
    }

    public static void serializeChar(char value, WriteBuffer writeBuffer) {
        assert writeBuffer != null;
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
        assert writeBuffer != null;
        byte v = WRITER_ESCAPE_TABLE[b];
        if(v == 0) {
            writeBuffer.writeByte(b);
        } else if(v > 0) {
            writeBuffer.writeBytes((byte) '\\', v);
        } else {
            writeBuffer.writeBytes((byte) '\\', (byte) 'u', (byte) '0', (byte) '0', HEX_BYTES[(b >>> 4) & 0xF], HEX_BYTES[b & 0xf]);
        }
    }

    public static void serializeInt(int value, WriteBuffer writeBuffer) {
        assert writeBuffer != null;
        JsonNumberUtil.writeInt(value, writeBuffer);
    }

    public static void serializeLong(long value, WriteBuffer writeBuffer) {
        assert writeBuffer != null;
        JsonNumberUtil.writeLong(value, writeBuffer);
    }

    public static void serializeFloat(float value, WriteBuffer writeBuffer) {
        assert writeBuffer != null;
        JsonNumberUtil.writeFloat(value, writeBuffer);
    }

    public static void serializeDouble(double value, WriteBuffer writeBuffer) {
        assert writeBuffer != null;
        JsonNumberUtil.writeDouble(value, writeBuffer);
    }

    public static void serializeIndent(int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert jsonIndentationLevel != null && writeBuffer != null;
        switch (jsonIndentationLevel) {
            case NONE -> {}
            case TWO -> {
                writeBuffer.writeByte((byte) '\n');
                writeBuffer.writeRepeated((byte) ' ', indent * 2); // no overflow, indent is limited
            }
            case FOUR -> {
                writeBuffer.writeByte((byte) '\n');
                writeBuffer.writeRepeated((byte) ' ', indent * 4); // no overflow, indent is limited
            }
            default -> throw new AssertionError();
        }
    }

    @FunctionalInterface
    interface ObjectSerializer {
        void serialize(Object value, WriteBuffer writeBuffer, JsonSerializerContext context);
    }

    private static void serializeObjArray(Object[] arr, int indent, JsonIndentationLevel jsonIndentationLevel,
                                          WriteBuffer writeBuffer, JsonSerializerContext context, ObjectSerializer serializer) {
        assert arr != null && writeBuffer != null;
        if(arr.length == 0) {
            writeBuffer.writeBytes((byte) '[', (byte) ']');
            return ;
        }
        serializeArrayStart(writeBuffer);
        for(int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1, jsonIndentationLevel, writeBuffer); // no overflow, indent is limited
            Object o = arr[i];
            if(o == null) {
                serializeNull(writeBuffer);
            } else {
                serializer.serialize(o, writeBuffer, context);
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
            writeBuffer.writeBytes((byte) '[', (byte) ']');
            return ;
        }
        serializeArrayStart(writeBuffer);
        for(int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1, jsonIndentationLevel, writeBuffer); // no overflow, indent is limited
            serializeByte(arr[i], writeBuffer);
            if(i != arr.length - 1) {
                serializeComma(writeBuffer);
            }
        }
        serializeIndent(indent, jsonIndentationLevel, writeBuffer);
        serializeArrayEnd(writeBuffer);
    }

    public static void serializeByteWrapperArray(Byte[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        serializeObjArray(arr, indent, jsonIndentationLevel, writeBuffer, null, (o, w, _) -> serializeByte((Byte) o, w));
    }

    public static void serializeBooleanArray(boolean[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && writeBuffer != null;
        if (arr.length == 0) {
            writeBuffer.writeBytes((byte) '[', (byte) ']');
            return;
        }
        serializeArrayStart(writeBuffer);
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1, jsonIndentationLevel, writeBuffer); // no overflow, indent is limited
            serializeBoolean(arr[i], writeBuffer);
            if (i != arr.length - 1) {
                serializeComma(writeBuffer);
            }
        }
        serializeIndent(indent, jsonIndentationLevel, writeBuffer);
        serializeArrayEnd(writeBuffer);
    }

    public static void serializeBooleanWrapperArray(Boolean[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && indent >= 1 && jsonIndentationLevel != null && writeBuffer != null;
        serializeObjArray(arr, indent, jsonIndentationLevel, writeBuffer, null, (o, w, _) -> serializeBoolean((Boolean) o, w));
    }

    public static void serializeShortArray(short[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && indent >= 1 && jsonIndentationLevel != null && writeBuffer != null;
        if (arr.length == 0) {
            writeBuffer.writeBytes((byte) '[', (byte) ']');
            return;
        }
        serializeArrayStart(writeBuffer);
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1, jsonIndentationLevel, writeBuffer); // no overflow, indent is limited
            serializeShort(arr[i], writeBuffer);
            if (i != arr.length - 1) {
                serializeComma(writeBuffer);
            }
        }
        serializeIndent(indent, jsonIndentationLevel, writeBuffer);
        serializeArrayEnd(writeBuffer);
    }

    public static void serializeShortWrapperArray(Short[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && indent >= 1 && jsonIndentationLevel != null && writeBuffer != null;
        serializeObjArray(arr, indent, jsonIndentationLevel, writeBuffer, null, (o, w, _) -> serializeShort((Short) o, w));
    }

    public static void serializeCharArray(char[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && indent >= 1 && jsonIndentationLevel != null && writeBuffer != null;
        if (arr.length == 0) {
            writeBuffer.writeBytes((byte) '[', (byte) ']');
            return;
        }
        serializeArrayStart(writeBuffer);
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1, jsonIndentationLevel, writeBuffer); // no overflow, indent is limited
            serializeChar(arr[i], writeBuffer);
            if (i != arr.length - 1) {
                serializeComma(writeBuffer);
            }
        }
        serializeIndent(indent, jsonIndentationLevel, writeBuffer);
        serializeArrayEnd(writeBuffer);
    }

    public static void serializeCharWrapperArray(Character[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && indent >= 1 && jsonIndentationLevel != null && writeBuffer != null;
        serializeObjArray(arr, indent, jsonIndentationLevel, writeBuffer, null, (o, w, _) -> serializeChar((Character) o, w));
    }

    public static void serializeIntArray(int[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && indent >= 1 && jsonIndentationLevel != null && writeBuffer != null;
        if (arr.length == 0) {
            writeBuffer.writeBytes((byte) '[', (byte) ']');
            return;
        }
        serializeArrayStart(writeBuffer);
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1, jsonIndentationLevel, writeBuffer); // no overflow, indent is limited
            serializeInt(arr[i], writeBuffer);
            if (i != arr.length - 1) {
                serializeComma(writeBuffer);
            }
        }
        serializeIndent(indent, jsonIndentationLevel, writeBuffer);
        serializeArrayEnd(writeBuffer);
    }

    public static void serializeIntWrapperArray(Integer[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && indent >= 1 && jsonIndentationLevel != null && writeBuffer != null;
        serializeObjArray(arr, indent, jsonIndentationLevel, writeBuffer, null, (o, w, _) -> serializeInt((Integer) o, w));
    }

    public static void serializeLongArray(long[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && indent >= 1 && jsonIndentationLevel != null && writeBuffer != null;
        if (arr.length == 0) {
            writeBuffer.writeBytes((byte) '[', (byte) ']');
            return;
        }
        serializeArrayStart(writeBuffer);
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1, jsonIndentationLevel, writeBuffer); // no overflow, indent is limited
            serializeLong(arr[i], writeBuffer);
            if (i != arr.length - 1) {
                serializeComma(writeBuffer);
            }
        }
        serializeIndent(indent, jsonIndentationLevel, writeBuffer);
        serializeArrayEnd(writeBuffer);
    }

    public static void serializeLongWrapperArray(Long[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && indent >= 1 && jsonIndentationLevel != null && writeBuffer != null;
        serializeObjArray(arr, indent, jsonIndentationLevel, writeBuffer, null, (o, w, _) -> serializeLong((Long) o, w));
    }

    public static void serializeFloatArray(float[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && indent >= 1 && jsonIndentationLevel != null && writeBuffer != null;
        if (arr.length == 0) {
            writeBuffer.writeBytes((byte) '[', (byte) ']');
            return;
        }
        serializeArrayStart(writeBuffer);
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1, jsonIndentationLevel, writeBuffer); // no overflow, indent is limited
            serializeFloat(arr[i], writeBuffer);
            if (i != arr.length - 1) {
                serializeComma(writeBuffer);
            }
        }
        serializeIndent(indent, jsonIndentationLevel, writeBuffer);
        serializeArrayEnd(writeBuffer);
    }

    public static void serializeFloatWrapperArray(Float[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && indent >= 1 && jsonIndentationLevel != null && writeBuffer != null;
        serializeObjArray(arr, indent, jsonIndentationLevel, writeBuffer, null, (o, w, _) -> serializeFloat((Float) o, w));
    }

    public static void serializeDoubleArray(double[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && indent >= 1 && jsonIndentationLevel != null && writeBuffer != null;
        if (arr.length == 0) {
            writeBuffer.writeBytes((byte) '[', (byte) ']');
            return;
        }
        serializeArrayStart(writeBuffer);
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1, jsonIndentationLevel, writeBuffer); // no overflow, indent is limited
            serializeDouble(arr[i], writeBuffer);
            if (i != arr.length - 1) {
                serializeComma(writeBuffer);
            }
        }
        serializeIndent(indent, jsonIndentationLevel, writeBuffer);
        serializeArrayEnd(writeBuffer);
    }

    public static void serializeDoubleWrapperArray(Double[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && indent >= 1 && jsonIndentationLevel != null && writeBuffer != null;
        serializeObjArray(arr, indent, jsonIndentationLevel, writeBuffer, null, (o, w, _) -> serializeDouble((Double) o, w));
    }

    public static void serializeNonEscapedUtf8Bytes(byte[] utf8Bytes, WriteBuffer writeBuffer) {
        assert utf8Bytes != null && writeBuffer != null;
        final int len = utf8Bytes.length;
        if(len == 0) {
            writeBuffer.writeBytes((byte) '"', (byte) '"');
            return ;
        }
        writeBuffer.ensureCapacity(Math.addExact(len, 2));
        switch (writeBuffer) {
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

    public static void serializeEscapedUtf8Bytes(byte[] utf8Bytes, WriteBuffer writeBuffer) {
        assert utf8Bytes != null && writeBuffer != null;
        final int len = utf8Bytes.length;
        if(len == 0) {
            writeBuffer.writeBytes((byte) '"', (byte) '"');
            return ;
        }
        // expansion factor 6 covers worst-case escape (backslash + u + 4 hex digits), plus two quote
        writeBuffer.ensureCapacity(Math.addExact(Math.multiplyExact(len, 6), 2));
        switch (writeBuffer) {
            case HeapWriteBuffer heapWriteBuffer -> serializeEscapedUtf8BytesToHeap(utf8Bytes, len, heapWriteBuffer);
            case SegmentWriteBuffer segmentWriteBuffer -> serializeEscapedUtf8BytesToSegment(utf8Bytes, len, segmentWriteBuffer);
            default -> throw new AssertionError();
        }
    }

    private static void serializeEscapedUtf8BytesToHeap(byte[] utf8Bytes, int len, HeapWriteBuffer heapWriteBuffer) {
        assert utf8Bytes != null && len > 0 && heapWriteBuffer != null;
        final byte[] bytes = heapWriteBuffer.rawByteArray();
        int position = heapWriteBuffer.intPosition();
        bytes[position++] = (byte) '"';
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
            bytes[position++] = (byte) '\\';
            if(v > 0) {
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
        if(start < len) {
            int available = len - start;
            System.arraycopy(utf8Bytes, start, bytes, position, available);
            position += available;
        }
        bytes[position++] = (byte) '"';
        heapWriteBuffer.setPosition(position);
    }

    private static void serializeEscapedUtf8BytesToSegment(byte[] utf8Bytes, int len, SegmentWriteBuffer segmentWriteBuffer) {
        assert utf8Bytes != null && len > 0 && segmentWriteBuffer != null;
        final MemorySegment segment = segmentWriteBuffer.rawSegment();
        long position = segmentWriteBuffer.longPosition();
        SegmentAccess.setByte(segment, position++, (byte) '"');
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
            SegmentAccess.setByte(segment, position++, (byte) '\\');
            if(v > 0) {
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
        if(start < len) {
            int available = len - start;
            MemorySegment.copy(utf8Bytes, start, segment, ValueLayout.JAVA_BYTE, position, available);
            position += available;
        }
        SegmentAccess.setByte(segment, position++, (byte) '"');
        segmentWriteBuffer.setPosition(position);
    }

    public static void serializeEscapedCharSequence(CharSequence charSequence, WriteBuffer writeBuffer, JsonSerializerContext context) {
        assert charSequence != null && writeBuffer != null && context != null;
        serializeEscapedString(charSequence.toString(), writeBuffer, context);
    }

    public static void serializeEscapedString(String str, WriteBuffer writeBuffer, JsonSerializerContext context) {
        assert str != null && writeBuffer != null && context != null;
        final int len = str.length();
        if(len == 0) {
            writeBuffer.writeBytes((byte) '"', (byte) '"');
            return ;
        }
        // expansion factor 6 covers worst-case escape (backslash + u + 4 hex digits), plus two quote
        writeBuffer.ensureCapacity(Math.addExact(Math.multiplyExact(len, 6), 2));
        switch (writeBuffer) {
            case HeapWriteBuffer heapWriteBuffer -> {
                final byte[] bytes = heapWriteBuffer.rawByteArray();
                int position = heapWriteBuffer.intPosition();
                bytes[position++] = (byte) '"';
                position = serializeEscapedStringToBytes(str, len, bytes, position, context);
                bytes[position++] = (byte) '"';
                heapWriteBuffer.setPosition(position);
            }
            case SegmentWriteBuffer segmentWriteBuffer -> {
                final MemorySegment segment = segmentWriteBuffer.rawSegment();
                long position = segmentWriteBuffer.longPosition();
                SegmentAccess.setByte(segment, position++, (byte) '"');
                position = serializeEscapedStringToSegment(str, len, segment, position, context);
                SegmentAccess.setByte(segment, position++, (byte) '"');
                segmentWriteBuffer.setPosition(position);
            }
        }
    }

    private static int asciiCount(ShortVector shortVector) {
        assert shortVector != null;
        VectorMask<Short> mask = shortVector.compare(VectorOperators.LT, (short) 0x20)
                .or(shortVector.compare(VectorOperators.GT, (short) 0x7E))
                .or(shortVector.compare(VectorOperators.EQ, (short) 0x22))
                .or(shortVector.compare(VectorOperators.EQ, (short) 0x5C));
        if (ESCAPE_SLASH) {
            mask = mask.or(shortVector.compare(VectorOperators.EQ, (short) 0x2F));
        }
        return mask.firstTrue();
    }

    private static int serializeEscapedStringToBytes(String str, int len, byte[] bytes, int offset, JsonSerializerContext context) {
        assert str != null && len > 0 && bytes != null && offset >= 0 && context != null;
        if(len < NO_MOVE_MIN || len > NO_MOVE_MAX) {
            return serializeStrToBytes(str, len, bytes, offset);
        }
        final char[] buffer = context.charBuffer(len);
        str.getChars(0, len, buffer, 0);
        final int upper = SHORT_SPECIES.loopBound(len);
        int index = 0;
        for ( ; index < upper; index += SHORT_SPECIES.length()) {
            ShortVector shortVector = ShortVector.fromCharArray(SHORT_SPECIES, buffer, index);
            ByteVector byteVector = (ByteVector) shortVector.convertShape(VectorOperators.S2B, BYTE_SPECIES, 0);
            byteVector.intoArray(bytes, offset);
            int matched = asciiCount(shortVector);
            offset += matched;
            if(matched != SHORT_SPECIES.length()) {
                index += matched;
                break ;
            }
        }
        if(index < len) {
            offset = serializeRemainingCharsToBytes(buffer, index, len, bytes, offset);
        }
        return offset;
    }

    private static long serializeEscapedStringToSegment(String str, int len, MemorySegment segment, long offset, JsonSerializerContext context) {
        assert str != null && len > 0 && segment != null && offset >= 0L && context != null;
        if(len < NO_MOVE_MIN || len > NO_MOVE_MAX) {
            return serializeStrToSegment(str, len, segment, offset);
        }
        final char[] buffer = context.charBuffer(len);
        str.getChars(0, len, buffer, 0);
        final int upper = SHORT_SPECIES.loopBound(len);
        int index = 0;
        for ( ; index < upper; index += SHORT_SPECIES.length()) {
            ShortVector shortVector = ShortVector.fromCharArray(SHORT_SPECIES, buffer, index);
            ByteVector byteVector = (ByteVector) shortVector.convertShape(VectorOperators.S2B, BYTE_SPECIES, 0);
            byteVector.intoMemorySegment(segment, offset, ByteOrder.nativeOrder()); // byteOrder will be ignored
            int matched = asciiCount(shortVector);
            offset += matched;
            if(matched != SHORT_SPECIES.length()) {
                index += matched;
                break ;
            }
        }
        if(index < len) {
            offset = serializeRemainingCharsToSegment(buffer, index, len, segment, offset);
        }
        return offset;
    }

    private static int serializeStrToBytes(String str, int len, byte[] bytes, int offset) {
        assert str != null && len > 0 && bytes != null && offset >= 0;
        int index = 0;
        while (index < len) {
            char c = str.charAt(index++);
            if(c < 0x80) {
                offset = serializeCharToBytes(c, bytes, offset);
            } else if(c < 0x800) {
                offset = serializeCharToBytes2(c, bytes, offset);
            } else if(Character.isHighSurrogate(c)) {
                if(index == len) {
                    throw new IllegalArgumentException("invalid high surrogate without low surrogate characters : " + c);
                }
                char c2 = str.charAt(index++);
                if(!Character.isLowSurrogate(c2)) {
                    throw new IllegalArgumentException("invalid low surrogate : " + c2);
                }
                offset = serializeCharToBytes4(c, c2, bytes, offset);
            } else if(Character.isLowSurrogate(c)) {
                throw new IllegalArgumentException("invalid low surrogate without high surrogate: " + c);
            } else {
                offset = serializeCharToBytes3(c, bytes, offset);
            }
        }
        return offset;
    }

    private static long serializeStrToSegment(String str, int len, MemorySegment segment, long offset) {
        assert str != null && len > 0 && segment != null && offset >= 0L;
        int index = 0;
        while (index < len) {
            char c = str.charAt(index++);
            if(c < 0x80) {
                offset = serializeCharToSegment(c, segment, offset);
            } else if(c < 0x800) {
                offset = serializeCharToSegment2(c, segment, offset);
            } else if(Character.isHighSurrogate(c)) {
                if(index == len) {
                    throw new IllegalArgumentException("invalid high surrogate without low surrogate characters : " + c);
                }
                char c2 = str.charAt(index++);
                if(!Character.isLowSurrogate(c2)) {
                    throw new IllegalArgumentException("invalid low surrogate : " + c2);
                }
                offset = serializeCharToSegment4(c, c2, segment, offset);
            } else if(Character.isLowSurrogate(c)) {
                throw new IllegalArgumentException("invalid low surrogate without high surrogate: " + c);
            } else {
                offset = serializeCharToSegment3(c, segment, offset);
            }
        }
        return offset;
    }

    private static int serializeRemainingCharsToBytes(char[] buffer, int index, int len, byte[] bytes, int offset) {
        assert buffer != null && index >= 0 && len > 0 && bytes != null && offset >= 0;
        while (index < len) {
            char c = buffer[index++];
            if(c < 0x80) {
                offset = serializeCharToBytes(c, bytes, offset);
            } else if(c < 0x800) {
                offset = serializeCharToBytes2(c, bytes, offset);
            } else if(Character.isHighSurrogate(c)) {
                if(index == len) {
                    throw new IllegalArgumentException("invalid high surrogate without low surrogate characters : " + c);
                }
                char c2 = buffer[index++];
                if(!Character.isLowSurrogate(c2)) {
                    throw new IllegalArgumentException("invalid low surrogate : " + c2);
                }
                offset = serializeCharToBytes4(c, c2, bytes, offset);
            } else if(Character.isLowSurrogate(c)) {
                throw new IllegalArgumentException("invalid low surrogate without high surrogate: " + c);
            } else {
                offset = serializeCharToBytes3(c, bytes, offset);
            }
        }
        return offset;
    }

    private static long serializeRemainingCharsToSegment(char[] buffer, int index, int len, MemorySegment segment, long offset) {
        assert buffer != null && index >= 0 && len > 0 && segment != null && offset >= 0;
        while (index < len) {
            char c = buffer[index++];
            if(c < 0x80) {
                offset = serializeCharToSegment(c, segment, offset);
            } else if(c < 0x800) {
                offset = serializeCharToSegment2(c, segment, offset);
            } else if(Character.isHighSurrogate(c)) {
                if(index == len) {
                    throw new IllegalArgumentException("invalid high surrogate without low surrogate characters : " + c);
                }
                char c2 = buffer[index++];
                if(!Character.isLowSurrogate(c2)) {
                    throw new IllegalArgumentException("invalid low surrogate : " + c2);
                }
                offset = serializeCharToSegment4(c, c2, segment, offset);
            } else if(Character.isLowSurrogate(c)) {
                throw new IllegalArgumentException("invalid low surrogate without high surrogate: " + c);
            } else {
                offset = serializeCharToSegment3(c, segment, offset);
            }
        }
        return offset;
    }

    private static int serializeCharToBytes(char c, byte[] bytes, int offset) {
        assert bytes != null && offset >= 0;
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
        assert segment != null && offset >= 0;
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
        assert bytes != null && offset >= 0;
        bytes[offset]     = (byte) (0xC0 | (c >> 6));
        bytes[offset + 1] = (byte) (0x80 | (c & 0x3F));
        return offset + 2;
    }

    private static long serializeCharToSegment2(char c, MemorySegment bytes, long offset) {
        assert bytes != null && offset >= 0L;
        SegmentAccess.setByte(bytes, offset, (byte) (0xC0 | (c >> 6)));
        SegmentAccess.setByte(bytes, offset + 1L, (byte) (0x80 | (c & 0x3F)));
        return offset + 2L;
    }

    private static int serializeCharToBytes3(char c, byte[] bytes, int offset) {
        assert bytes != null && offset >= 0;
        bytes[offset]     = (byte) (0xE0 | (c >> 12));
        bytes[offset + 1] = (byte) (0x80 | ((c >> 6) & 0x3F));
        bytes[offset + 2] = (byte) (0x80 | (c & 0x3F));
        return offset + 3;
    }

    private static long serializeCharToSegment3(char c, MemorySegment segment, long offset) {
        assert segment != null && offset >= 0L;
        SegmentAccess.setByte(segment, offset, (byte) (0xE0 | (c >> 12)));
        SegmentAccess.setByte(segment, offset + 1L, (byte) (0x80 | ((c >> 6) & 0x3F)));
        SegmentAccess.setByte(segment, offset + 2L, (byte) (0x80 | (c & 0x3F)));
        return offset + 3L;
    }

    private static int serializeCharToBytes4(char highSurrogate, char lowSurrogate, byte[] bytes, int offset) {
        assert Character.isHighSurrogate(highSurrogate) && Character.isLowSurrogate(lowSurrogate) && bytes != null && offset >= 0;
        int cp = Character.toCodePoint(highSurrogate, lowSurrogate);
        bytes[offset]     = (byte) (0xF0 | (cp >> 18));
        bytes[offset + 1] = (byte) (0x80 | ((cp >> 12) & 0x3F));
        bytes[offset + 2] = (byte) (0x80 | ((cp >> 6) & 0x3F));
        bytes[offset + 3] = (byte) (0x80 | (cp & 0x3F));
        return offset + 4;
    }

    private static long serializeCharToSegment4(char highSurrogate, char lowSurrogate, MemorySegment segment, long offset) {
        assert Character.isHighSurrogate(highSurrogate) && Character.isLowSurrogate(lowSurrogate) && segment != null && offset >= 0L;
        int cp = Character.toCodePoint(highSurrogate, lowSurrogate);
        SegmentAccess.setByte(segment, offset, (byte) (0xF0 | (cp >> 18)));
        SegmentAccess.setByte(segment, offset + 1L, (byte) (0x80 | ((cp >> 12) & 0x3F)));
        SegmentAccess.setByte(segment, offset + 2L, (byte) (0x80 | ((cp >> 6) & 0x3F)));
        SegmentAccess.setByte(segment, offset + 3L, (byte) (0x80 | (cp & 0x3F)));
        return offset + 4L;
    }

    public static void serializeEscapedCharSequenceArray(CharSequence[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer, JsonSerializerContext context) {
        assert arr != null && indent >= 1 && jsonIndentationLevel != null && context != null;
        serializeObjArray(arr, indent, jsonIndentationLevel, writeBuffer, context, (o, w, c) -> serializeEscapedCharSequence((CharSequence) o, w, c));
    }

    public static void serializeEscapedStringArray(String[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer, JsonSerializerContext context) {
        assert arr != null && indent >= 1 && jsonIndentationLevel != null && context != null;
        serializeObjArray(arr, indent, jsonIndentationLevel, writeBuffer, context, (o, w, c) -> serializeEscapedString((String) o, w, c));
    }

    public static void serializeJsonPrimitiveType(JsonPrimitiveType jsonPrimitiveType, WriteBuffer writeBuffer, JsonSerializerContext context) {
        assert jsonPrimitiveType != null && writeBuffer != null && context != null;
        switch (jsonPrimitiveType) {
            case JsonBoolType jsonBoolType -> serializeJsonBoolType(jsonBoolType, writeBuffer);
            case JsonNumberType jsonNumberType -> serializeJsonNumberType(jsonNumberType, writeBuffer);
            case JsonStrType jsonStrType -> serializeJsonStrType(jsonStrType, writeBuffer, context);
            default -> throw new AssertionError();
        }
    }

    public static void serializeJsonPrimitiveTypeArray(JsonPrimitiveType[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer, JsonSerializerContext context) {
        assert arr != null && indent >= 1 && jsonIndentationLevel != null && writeBuffer != null && context != null;
        serializeObjArray(arr, indent, jsonIndentationLevel, writeBuffer, context, (o, w, c) -> serializeJsonPrimitiveType((JsonPrimitiveType) o, w, c));
    }

    public static void serializeJsonBoolType(JsonBoolType jsonBoolType, WriteBuffer writeBuffer) {
        assert jsonBoolType != null && writeBuffer != null;
        serializeBoolean(jsonBoolType.data(), writeBuffer);
    }

    public static void serializeJsonBoolTypeArray(JsonBoolType[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && indent >= 1 && jsonIndentationLevel != null && writeBuffer != null;
        serializeObjArray(arr, indent, jsonIndentationLevel, writeBuffer, null, (o, w, _) -> serializeJsonBoolType((JsonBoolType) o, w));
    }

    public static void serializeJsonNumberType(JsonNumberType jsonNumberType, WriteBuffer writeBuffer) {
        assert jsonNumberType != null && writeBuffer != null;
        writeBuffer.writeBytes(jsonNumberType.data());
    }

    public static void serializeJsonNumberTypeArray(JsonNumberType[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer) {
        assert arr != null && indent >= 1 && jsonIndentationLevel != null && writeBuffer != null;
        serializeObjArray(arr, indent, jsonIndentationLevel, writeBuffer, null, (o, w, _) -> serializeJsonNumberType((JsonNumberType) o, w));
    }

    public static void serializeJsonStrType(JsonStrType jsonStrType, WriteBuffer writeBuffer, JsonSerializerContext context) {
        assert jsonStrType != null && writeBuffer != null;
        serializeEscapedString(jsonStrType.data(), writeBuffer, context);
    }

    public static void serializeJsonStrTypeArray(JsonStrType[] arr, int indent, JsonIndentationLevel jsonIndentationLevel, WriteBuffer writeBuffer, JsonSerializerContext context) {
        assert arr != null && indent >= 1 && jsonIndentationLevel != null && writeBuffer != null && context != null;
        serializeObjArray(arr, indent, jsonIndentationLevel, writeBuffer, context, (o, w, c) -> serializeJsonStrType((JsonStrType) o, w, c));
    }

    public static void serializeEnum(Enum<?> enumValue, Class<?> rawType, WriteBuffer writeBuffer, JsonSerializerContext context) {
        assert enumValue != null && rawType != null && rawType.isEnum() && writeBuffer != null && context != null;
        // experiment
        //MarshallFacade fc = Marshalls.getEnumMarshallFacade(rawType);
        MarshallFacade fc = Marshalls.getMarshallFacade(rawType);
        if(fc == null) {
            serializeEscapedString(enumValue.name(), writeBuffer, context);
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
        assert option != null && rawType != null;
        // builtin type has the highest priority
        if(rawType.isArray()) {
            JsonSerializeFunc builtinSerializeArrFunc = builtinSerializeArrayFunc(rawType);
            if(builtinSerializeArrFunc != null) {
                return builtinSerializeArrFunc;
            }
            return (_, _, c, o, _) -> {
                c.setArr((Object[]) o);
                return JsonSerializeResult.NewArray;
            };
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
        return (_, _, c, o, _) -> {
            c.setObj(o);
            return JsonSerializeResult.NewMarshallable;
        };
    }

    public static JsonSerializeFunc enumSerializeFunc(Class<?> rawType) {
        assert rawType != null && rawType.isEnum();
        // MarshallFacade fc = Marshalls.getEnumMarshallFacade(rawType);
        MarshallFacade fc = Marshalls.getMarshallFacade(rawType);
        if(fc == null) {
            return (_, w, c, o, _) -> {
                serializeEscapedString(((Enum<?>) o).name(), w, c);
                return JsonSerializeResult.Continue;
            };
        } else {
            // have to use capture lambda
            return (_, w, _, o, _) -> {
                MarshallInfo marshallInfo = fc.marshallInfoByIndex(((Enum<?>) o).ordinal());
                if(marshallInfo.mappedNameSimple()) {
                    serializeQuote(w);
                    w.writeBytes(marshallInfo.mappedNameUtf8Bytes());
                    serializeQuote(w);
                } else {
                    serializeEscapedUtf8Bytes(marshallInfo.mappedNameUtf8Bytes(), w);
                }
                return JsonSerializeResult.Continue;
            };
        }
    }

    private static final Map<Class<?>, JsonSerializeFunc> BUILTIN_SERIALIZE_OBJ_FUNC_MAP;

    static {
        Map<Class<?>, JsonSerializeFunc> r = new HashMap<>();
        r.put(Byte.class, (_, w, _, o, _) -> {
            serializeByte((Byte) o, w);
            return JsonSerializeResult.Continue;
        });
        r.put(Boolean.class, (_, w, _, o, _) -> {
            serializeBoolean((Boolean) o, w);
            return JsonSerializeResult.Continue;
        });
        r.put(Short.class, (_, w, _, o, _) -> {
            serializeShort((Short) o, w);
            return JsonSerializeResult.Continue;
        });
        r.put(Character.class, (_, w, _, o, _) -> {
            serializeChar((Character) o, w);
            return JsonSerializeResult.Continue;
        });
        r.put(Integer.class, (_, w, _, o, _) -> {
            serializeInt((Integer) o, w);
            return JsonSerializeResult.Continue;
        });
        r.put(Long.class, (_, w, _, o, _) -> {
            serializeLong((Long) o, w);
            return JsonSerializeResult.Continue;
        });
        r.put(Float.class, (_, w, _, o, _) -> {
            serializeFloat((Float) o, w);
            return JsonSerializeResult.Continue;
        });
        r.put(Double.class, (_, w, _, o, _) -> {
            serializeDouble((Double) o, w);
            return JsonSerializeResult.Continue;
        });
        r.put(CharSequence.class, (_, w, c, o, _) -> {
            serializeEscapedCharSequence((CharSequence) o, w, c);
            return JsonSerializeResult.Continue;
        });
        r.put(String.class, (_, w, c, o, _) -> {
            serializeEscapedString((String) o, w, c);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonPrimitiveType.class, (_, w, c, o, _) -> {
            serializeJsonPrimitiveType((JsonPrimitiveType) o, w, c);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonBoolType.class, (_, w, _, o, _) -> {
            serializeJsonBoolType((JsonBoolType) o, w);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonNumberType.class, (_, w, _, o, _) -> {
            serializeJsonNumberType((JsonNumberType) o, w);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonStrType.class, (_, w, c, o, _) -> {
            serializeJsonStrType((JsonStrType) o, w, c);
            return JsonSerializeResult.Continue;
        });
        BUILTIN_SERIALIZE_OBJ_FUNC_MAP = Map.copyOf(r);
    }

    public static JsonSerializeFunc builtinSerializeObjFunc(Class<?> rawType) {
        assert rawType != null;
        return BUILTIN_SERIALIZE_OBJ_FUNC_MAP.get(rawType);
    }

    private static final Map<Class<?>, JsonSerializeFunc> BUILTIN_SERIALIZE_ARRAY_FUNC_MAP;

    static {
        Map<Class<?>, JsonSerializeFunc> r = new HashMap<>();
        r.put(byte[].class, (op, w, _, o, ind) -> {
            serializeByteArray((byte[]) o, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        });
        r.put(boolean[].class, (op, w, _, o, ind) -> {
            serializeBooleanArray((boolean[]) o, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        });
        r.put(short[].class, (op, w, _, o, ind) -> {
            serializeShortArray((short[]) o, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        });
        r.put(char[].class, (op, w, _, o, ind) -> {
            serializeCharArray((char[]) o, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        });
        r.put(int[].class, (op, w, _, o, ind) -> {
            serializeIntArray((int[]) o, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        });
        r.put(long[].class, (op, w, _, o, ind) -> {
            serializeLongArray((long[]) o, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        });
        r.put(float[].class, (op, w, _, o, ind) -> {
            serializeFloatArray((float[]) o, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        });
        r.put(double[].class, (op, w, _, o, ind) -> {
            serializeDoubleArray((double[]) o, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        });
        r.put(Byte[].class, (op, w, _, o, ind) -> {
            serializeByteWrapperArray((Byte[]) o, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        });
        r.put(Boolean[].class, (op, w, _, o, ind) -> {
            serializeBooleanWrapperArray((Boolean[]) o, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        });
        r.put(Short[].class, (op, w, _, o, ind) -> {
            serializeShortWrapperArray((Short[]) o, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        });
        r.put(Character[].class, (op, w, _, o, ind) -> {
            serializeCharWrapperArray((Character[]) o, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        });
        r.put(Integer[].class, (op, w, _, o, ind) -> {
            serializeIntWrapperArray((Integer[]) o, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        });
        r.put(Long[].class, (op, w, _, o, ind) -> {
            serializeLongWrapperArray((Long[]) o, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        });
        r.put(Float[].class, (op, w, _, o, ind) -> {
            serializeFloatWrapperArray((Float[]) o, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        });
        r.put(Double[].class, (op, w, _, o, ind) -> {
            serializeDoubleWrapperArray((Double[]) o, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        });
        r.put(CharSequence[].class, (op, w, c, o, ind) -> {
            serializeEscapedCharSequenceArray((CharSequence[]) o, ind, op.indentationLevel(), w, c);
            return JsonSerializeResult.Continue;
        });
        r.put(String[].class, (op, w, c, o, ind) -> {
            serializeEscapedStringArray((String[]) o, ind, op.indentationLevel(), w, c);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonPrimitiveType[].class, (op, w, c, o, ind) -> {
            serializeJsonPrimitiveTypeArray((JsonPrimitiveType[]) o, ind, op.indentationLevel(), w, c);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonBoolType[].class, (op, w, _, o, ind) -> {
            serializeJsonBoolTypeArray((JsonBoolType[]) o, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonNumberType[].class, (op, w, _, o, ind) -> {
            serializeJsonNumberTypeArray((JsonNumberType[]) o, ind, op.indentationLevel(), w);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonStrType[].class, (op, w, c, o, ind) -> {
            serializeJsonStrTypeArray((JsonStrType[]) o, ind, op.indentationLevel(), w, c);
            return JsonSerializeResult.Continue;
        });
        BUILTIN_SERIALIZE_ARRAY_FUNC_MAP = Map.copyOf(r);
    }

    public static JsonSerializeFunc builtinSerializeArrayFunc(Class<?> rawType) {
        assert rawType != null;
        return BUILTIN_SERIALIZE_ARRAY_FUNC_MAP.get(rawType);
    }

}
