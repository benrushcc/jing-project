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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

// indent never overflows int, as its value is limited by the practical JSON nesting depth.
public final class JsonSerializerContext {
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

    private final JsonSerializerOption option;
    private final WriteBuffer writeBuffer;
    private char[] charBuffer;
    private Object obj;
    private Object[] arr;
    private Collection<?> col;
    private Map<?, ?> map;
    private Class<?> firstType;
    private Class<?> secondType;

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

    public char[] charBuffer(int len) {
        assert len > 0;
        if(charBuffer == null || charBuffer.length < len) {
            charBuffer = new char[Integer.highestOneBit(len - 1) << 1]; // no overflow
        }
        return charBuffer;
    }

    public Object obj() {
        Object r = obj;
        obj = null;
        return r;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }

    public Object[] arr() {
        Object[] r = arr;
        arr = null;
        return r;
    }

    public void setArr(Object[] arr) {
        this.arr = arr;
    }

    public Collection<?> col() {
        Collection<?> r = col;
        col = null;
        return r;
    }

    public void setCol(Collection<?> col) {
        this.col = col;
    }

    public Map<?, ?> map() {
        Map<?, ?> r = map;
        map = null;
        return r;
    }

    public void setMap(Map<?, ?> map) {
        this.map = map;
    }

    public Class<?> firstType() {
        Class<?> r = firstType;
        firstType = null;
        return r;
    }

    public void setFirstType(Class<?> firstType) {
        this.firstType = firstType;
    }

    public Class<?> secondType() {
        Class<?> r = secondType;
        secondType = null;
        return r;
    }

    public void setSecondType(Class<?> secondType) {
        this.secondType = secondType;
    }

    public void serializeObjStart() {
        writeBuffer.writeByte((byte) '{');
    }

    public void serializeObjEnd() {
        writeBuffer.writeByte((byte) '}');
    }

    public void serializeArrayStart() {
        writeBuffer.writeByte((byte) '[');
    }

    public void serializeArrayEnd() {
        writeBuffer.writeByte((byte) ']');
    }

    public void serializeQuote() {
        writeBuffer.writeByte((byte) '"');
    }

    public void serializeComma() {
        writeBuffer.writeByte((byte) ',');
    }

    public void serializeKvSep() {
        writeBuffer.writeBytes((byte) ':', (byte) ' ');
    }

    public void serializeNull() {
        writeBuffer.writeBytes((byte) 'n', (byte) 'u', (byte) 'l', (byte) 'l');
    }

    public void serializeByte(byte value) {
        JsonNumberUtil.writeInt(value, writeBuffer);
    }

    public void serializeBoolean(boolean value) {
        if(value) {
            writeBuffer.writeBytes((byte) 't', (byte) 'r', (byte) 'u', (byte) 'e');
        } else {
            writeBuffer.writeBytes((byte) 'f', (byte) 'a', (byte) 'l', (byte) 's', (byte) 'e');
        }
    }

    public void serializeShort(short value) {
        JsonNumberUtil.writeInt(value, writeBuffer);
    }

    private void serializeAsciiByte(byte b) {
        byte v = WRITER_ESCAPE_TABLE[b];
        if(v == 0) {
            writeBuffer.writeByte(b);
        } else if(v > 0) {
            writeBuffer.writeBytes((byte) '\\', v);
        } else {
            writeBuffer.writeBytes((byte) '\\', (byte) 'u', (byte) '0', (byte) '0', HEX_BYTES[(b >>> 4) & 0xF], HEX_BYTES[b & 0xf]);
        }
    }

    public void serializeChar(char value) {
        writeBuffer.writeByte((byte) '"');
        if(Character.isSurrogate(value)) {
            throw new IllegalArgumentException("surrogates not supported");
        }
        if(value < 0x80) {
            serializeAsciiByte((byte) value);
        } else if(value < 0x800) {
            writeBuffer.writeBytes((byte) (0xC0 | (value >> 6)),
                    (byte) (0x80 | (value & 0x3F)));
        } else {
            writeBuffer.writeBytes((byte) (0xE0 | (value >> 12)),
                    (byte) (0x80 | ((value >> 6) & 0x3F)),
                    (byte) (0x80 | (value & 0x3F)));
        }
        writeBuffer.writeByte((byte) '"');
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
        switch (option.indentationLevel()) {
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

    private void serializeObjArray(Object[] arr, int indent, BiConsumer<Object, JsonSerializerContext> consumer) {
        assert arr != null && indent > 0;
        if(arr.length == 0) {
            writeBuffer.writeBytes((byte) '[', (byte) ']');
            return ;
        }
        writeBuffer.writeByte((byte) '[');
        for(int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1); // no overflow, indent is limited
            Object o = arr[i];
            if(o == null) {
                serializeNull();
            } else {
                consumer.accept(o, this);
            }
            if(i != arr.length - 1) {
                writeBuffer.writeByte((byte) ',');
            }
        }
        serializeIndent(indent);
        writeBuffer.writeByte((byte) ']');
    }

    public void serializeByteArray(byte[] arr, int indent) {
        assert arr != null && indent > 0;
        if(arr.length == 0) {
            writeBuffer.writeBytes((byte) '[', (byte) ']');
            return ;
        }
        writeBuffer.writeByte((byte) '[');
        for(int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1); // no overflow, indent is limited
            serializeByte(arr[i]);
            if(i != arr.length - 1) {
                writeBuffer.writeByte((byte) ',');
            }
        }
        serializeIndent(indent);
        writeBuffer.writeByte((byte) ']');
    }

    public void serializeByteWrapperArray(Byte[] arr, int indent) {
        serializeObjArray(arr, indent, (o, w) -> w.serializeByte((Byte) o));
    }

    public void serializeBooleanArray(boolean[] arr, int indent) {
        assert arr != null && indent > 0;
        if(arr.length == 0) {
            writeBuffer.writeBytes((byte) '[', (byte) ']');
            return ;
        }
        writeBuffer.writeByte((byte) '[');
        for(int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1); // no overflow, indent is limited
            serializeBoolean(arr[i]);
            if(i != arr.length - 1) {
                writeBuffer.writeByte((byte) ',');
            }
        }
        serializeIndent(indent);
        writeBuffer.writeByte((byte) ']');
    }

    public void serializeBooleanWrapperArray(Boolean[] arr, int indent) {
        serializeObjArray(arr, indent, (o, w) -> w.serializeBoolean((Boolean) o));
    }

    public void serializeShortArray(short[] arr, int indent) {
        assert arr != null && indent > 0;
        if (arr.length == 0) {
            writeBuffer.writeBytes((byte) '[', (byte) ']');
            return;
        }
        writeBuffer.writeByte((byte) '[');
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1);
            serializeShort(arr[i]);
            if (i != arr.length - 1) {
                writeBuffer.writeByte((byte) ',');
            }
        }
        serializeIndent(indent);
        writeBuffer.writeByte((byte) ']');
    }

    public void serializeShortWrapperArray(Short[] arr, int indent) {
        serializeObjArray(arr, indent, (o, w) -> w.serializeShort((Short) o));
    }

    public void serializeCharArray(char[] arr, int indent) {
        assert arr != null && indent > 0;
        if (arr.length == 0) {
            writeBuffer.writeBytes((byte) '[', (byte) ']');
            return;
        }
        writeBuffer.writeByte((byte) '[');
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1);
            serializeChar(arr[i]);
            if (i != arr.length - 1) {
                writeBuffer.writeByte((byte) ',');
            }
        }
        serializeIndent(indent);
        writeBuffer.writeByte((byte) ']');
    }

    public void serializeCharWrapperArray(Character[] arr, int indent) {
        serializeObjArray(arr, indent, (o, w) -> w.serializeChar((Character) o));
    }

    public void serializeIntArray(int[] arr, int indent) {
        assert arr != null && indent > 0;
        if (arr.length == 0) {
            writeBuffer.writeBytes((byte) '[', (byte) ']');
            return;
        }
        writeBuffer.writeByte((byte) '[');
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1);
            serializeInt(arr[i]);
            if (i != arr.length - 1) {
                writeBuffer.writeByte((byte) ',');
            }
        }
        serializeIndent(indent);
        writeBuffer.writeByte((byte) ']');
    }

    public void serializeIntWrapperArray(Integer[] arr, int indent) {
        serializeObjArray(arr, indent, (o, w) -> w.serializeInt((Integer) o));
    }

    public void serializeLongArray(long[] arr, int indent) {
        assert arr != null && indent > 0;
        if (arr.length == 0) {
            writeBuffer.writeBytes((byte) '[', (byte) ']');
            return;
        }
        writeBuffer.writeByte((byte) '[');
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1);
            serializeLong(arr[i]);
            if (i != arr.length - 1) {
                writeBuffer.writeByte((byte) ',');
            }
        }
        serializeIndent(indent);
        writeBuffer.writeByte((byte) ']');
    }

    public void serializeLongWrapperArray(Long[] arr, int indent) {
        serializeObjArray(arr, indent, (o, w) -> w.serializeLong((Long) o));
    }

    public void serializeFloatArray(float[] arr, int indent) {
        assert arr != null && indent > 0;
        if (arr.length == 0) {
            writeBuffer.writeBytes((byte) '[', (byte) ']');
            return;
        }
        writeBuffer.writeByte((byte) '[');
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1);
            serializeFloat(arr[i]);
            if (i != arr.length - 1) {
                writeBuffer.writeByte((byte) ',');
            }
        }
        serializeIndent(indent);
        writeBuffer.writeByte((byte) ']');
    }

    public void serializeFloatWrapperArray(Float[] arr, int indent) {
        serializeObjArray(arr, indent, (o, w) -> w.serializeFloat((Float) o));
    }

    public void serializeDoubleArray(double[] arr, int indent) {
        assert arr != null && indent > 0;
        if (arr.length == 0) {
            writeBuffer.writeBytes((byte) '[', (byte) ']');
            return;
        }
        writeBuffer.writeByte((byte) '[');
        for (int i = 0; i < arr.length; i++) {
            serializeIndent(indent + 1);
            serializeDouble(arr[i]);
            if (i != arr.length - 1) {
                writeBuffer.writeByte((byte) ',');
            }
        }
        serializeIndent(indent);
        writeBuffer.writeByte((byte) ']');
    }

    public void serializeDoubleWrapperArray(Double[] arr, int indent) {
        serializeObjArray(arr, indent, (o, w) -> w.serializeDouble((Double) o));
    }

    public void serializeNonEscapedUtf8Bytes(byte[] utf8Bytes) {
        assert utf8Bytes != null;
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

    public void serializeEscapedUtf8Bytes(byte[] utf8Bytes) {
        assert utf8Bytes != null;
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

    public void serializeEscapedCharSequence(CharSequence charSequence) {
        assert charSequence != null;
        serializeEscapedString(charSequence.toString());
    }

    public void serializeEscapedString(String str) {
        assert str != null && writeBuffer != null;
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
                position = serializeEscapedStringToBytes(str, len, bytes, position);
                bytes[position++] = (byte) '"';
                heapWriteBuffer.setPosition(position);
            }
            case SegmentWriteBuffer segmentWriteBuffer -> {
                final MemorySegment segment = segmentWriteBuffer.rawSegment();
                long position = segmentWriteBuffer.longPosition();
                SegmentAccess.setByte(segment, position++, (byte) '"');
                position = serializeEscapedStringToSegment(str, len, segment, position);
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

    private int serializeEscapedStringToBytes(String str, int len, byte[] bytes, int offset) {
        assert str != null && len > 0 && bytes != null && offset >= 0;
        if(len < NO_MOVE_MIN || len > NO_MOVE_MAX) {
            return serializeStrToBytes(str, len, bytes, offset);
        }
        final char[] buffer = charBuffer(len);
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

    private long serializeEscapedStringToSegment(String str, int len, MemorySegment segment, long offset) {
        assert str != null && len > 0 && segment != null && offset >= 0L;
        if(len < NO_MOVE_MIN || len > NO_MOVE_MAX) {
            return serializeStrToSegment(str, len, segment, offset);
        }
        final char[] buffer = charBuffer(len);
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

    public void serializeEscapedCharSequenceArray(CharSequence[] arr, int indent) {
        assert arr != null && indent > 0;
        serializeObjArray(arr, indent, (o, c) -> c.serializeEscapedCharSequence((CharSequence) o));
    }

    public void serializeEscapedStringArray(String[] arr, int indent) {
        assert arr != null && indent > 0;
        serializeObjArray(arr, indent, (o, c) -> c.serializeEscapedString((String) o));
    }

    public void serializeJsonPrimitiveType(JsonPrimitiveType jsonPrimitiveType) {
        assert jsonPrimitiveType != null;
        switch (jsonPrimitiveType) {
            case JsonBoolType jsonBoolType -> serializeJsonBoolType(jsonBoolType);
            case JsonNumberType jsonNumberType -> serializeJsonNumberType(jsonNumberType);
            case JsonStrType jsonStrType -> serializeJsonStrType(jsonStrType);
            case null, default -> throw new AssertionError();
        }
    }

    public void serializeJsonPrimitiveTypeArray(JsonPrimitiveType[] arr, int indent) {
        assert arr != null && indent > 0;
        serializeObjArray(arr, indent, (o, c) -> c.serializeJsonPrimitiveType((JsonPrimitiveType) o));
    }

    public void serializeJsonBoolType(JsonBoolType jsonBoolType) {
        assert jsonBoolType != null;
        serializeBoolean(jsonBoolType.data());
    }

    public void serializeJsonBoolTypeArray(JsonBoolType[] arr, int indent) {
        assert arr != null && indent > 0;
        serializeObjArray(arr, indent, (o, c) ->c. serializeJsonBoolType((JsonBoolType) o));
    }

    public void serializeJsonNumberType(JsonNumberType jsonNumberType) {
        assert jsonNumberType != null;
        writeBuffer.writeBytes(jsonNumberType.data());
    }

    public void serializeJsonNumberTypeArray(JsonNumberType[] arr, int indent) {
        assert arr != null && indent > 0;
        serializeObjArray(arr, indent, (o, c) -> c.serializeJsonNumberType((JsonNumberType) o));
    }

    public void serializeJsonStrType(JsonStrType jsonStrType) {
        assert jsonStrType != null;
        serializeEscapedString(jsonStrType.data());
    }

    public void serializeJsonStrTypeArray(JsonStrType[] arr, int indent) {
        assert arr != null && indent > 0;
        serializeObjArray(arr, indent, (o, c) -> c.serializeJsonStrType((JsonStrType) o));
    }

    public void serializeEnum(Enum<?> enumValue) {
        assert enumValue != null;
        MarshallInfo inf = Marshalls.getEnumItemMarshallInfo(enumValue);
        if (inf == null) {
            serializeEscapedString(enumValue.name());
        } else if (inf.mappedNameSimple()) {
            writeBuffer.writeByte((byte) '"');
            writeBuffer.writeBytes(inf.mappedNameUtf8Bytes());
            writeBuffer.writeByte((byte) '"');
        } else {
            serializeEscapedUtf8Bytes(inf.mappedNameUtf8Bytes());
        }
    }

    private static final Map<Class<?>, JsonSerializeFunc> BUILTIN_SERIALIZE_OBJ_FUNC_MAP;

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

    public static JsonSerializeFunc builtinSerializeObjFunc(Class<?> rawType) {
        assert rawType != null;
        return BUILTIN_SERIALIZE_OBJ_FUNC_MAP.get(rawType);
    }

    private static final Map<Class<?>, JsonSerializeFunc> BUILTIN_SERIALIZE_ARRAY_FUNC_MAP;

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

    public static JsonSerializeFunc builtinSerializeArrayFunc(Class<?> rawType) {
        assert rawType != null;
        return BUILTIN_SERIALIZE_ARRAY_FUNC_MAP.get(rawType);
    }

    public JsonSerializeFunc valueSerializeFunc(Class<?> rawType) {
        assert rawType != null;
        // builtin type has the highest priority
        if(rawType.isArray()) {
            JsonSerializeFunc builtinSerializeArrFunc = builtinSerializeArrayFunc(rawType);
            if(builtinSerializeArrFunc != null) {
                return builtinSerializeArrFunc;
            }
            return (o, _, c) -> {
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
            return (o, _, c) -> {
                c.serializeEnum((Enum<?>) o);
                return JsonSerializeResult.Continue;
            };
        }
        // assuming marshallable
        return (o, _, c) -> {
            c.setObj(o);
            return JsonSerializeResult.NewMarshallable;
        };
    }


}
