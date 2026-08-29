package io.jingproject.marshalljson;

import io.jingproject.common.*;
import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.MarshallInfo;
import io.jingproject.marshall.Marshalls;
import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntFunction;

public final class JsonDeserializerContext {
    private static final int BYTE_BUFFER_INITIAL_SIZE = 16;
    private static final VectorSpecies<Short> SHORT_SPECIES;
    private static final VectorSpecies<Byte> BYTE_SPECIES;
    private static final int COMPACT_TRUE = Utils.compact(Utils.compact((byte) 't', (byte) 'r'), Utils.compact((byte) 'u', (byte) 'e'));
    private static final int COMPACT_ALSE = Utils.compact(Utils.compact((byte) 'a', (byte) 'l'), Utils.compact((byte) 's', (byte) 'e'));
    private static final int COMPACT_NULL = Utils.compact(Utils.compact((byte) 'n', (byte) 'u'), Utils.compact((byte) 'l', (byte) 'l'));
    private static final int MARSHALL_INDEX_OFFSET = 0;
    private static final int DUMMY_INDEX_OFFSET = 4;
    private static final int MATCHED_INDEX_OFFSET = 8;
    private static final int TOTAL_INDEX_OFFSET = 12;
    private static final int OBJ_ARR_INITIAL_SIZE = 8;
    private static final byte[] SYM_TABLE = makeSymTable();
    private static final byte[] HEX_TABLE = makeHexTable();
    private static final byte EMPTY_SYM = (byte) -1;
    private static final byte NUM_SYM = (byte) -2;
    private static final Map<Class<?>, JsonDeserializeFunc> BUILTIN_DESERIALIZE_OBJ_FUNC_MAP;
    private static final Map<Class<?>, JsonDeserializeFunc> BUILTIN_DESERIALIZE_ARRAY_FUNC_MAP;

    static {
        try {
            Class<Os> _ = MethodHandles.lookup().ensureInitialized(Os.class);
        } catch (IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
        int vecSize = Integer.parseInt(System.getProperty("jing.marshalljson.deserialize.vecsize", "-1"));
        if (vecSize < 0) {
            vecSize = ShortVector.SPECIES_PREFERRED.vectorBitSize();
        }
        switch (vecSize) {
            case 64 -> {
                SHORT_SPECIES = ShortVector.SPECIES_64;
                BYTE_SPECIES = ByteVector.SPECIES_64;
            }
            case 128 -> {
                SHORT_SPECIES = ShortVector.SPECIES_128;
                BYTE_SPECIES = ByteVector.SPECIES_128;
            }
            case 256 -> {
                SHORT_SPECIES = ShortVector.SPECIES_256;
                BYTE_SPECIES = ByteVector.SPECIES_256;
            }
            case 512 -> {
                SHORT_SPECIES = ShortVector.SPECIES_512;
                BYTE_SPECIES = ByteVector.SPECIES_512;
            }
            default -> throw new UnsupportedOperationException("unknown vector size : " + vecSize);
        }
    }

    static {
        Map<Class<?>, JsonDeserializeFunc> r = new HashMap<>();
        r.put(Byte.class, (b, c) -> {
            c.setObj(c.deserializeByte(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(Boolean.class, (b, c) -> {
            c.setObj(c.deserializeBoolean(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(Short.class, (b, c) -> {
            c.setObj(c.deserializeShort(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(Character.class, (b, c) -> {
            c.setObj(c.deserializeChar(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(Integer.class, (b, c) -> {
            c.setObj(c.deserializeInt(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(Long.class, (b, c) -> {
            c.setObj(c.deserializeLong(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(Float.class, (b, c) -> {
            c.setObj(c.deserializeFloat(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(Double.class, (b, c) -> {
            c.setObj(c.deserializeDouble(b));
            return JsonDeserializeResult.Continue;
        });
        JsonDeserializeFunc strFunc = (b, c) -> {
            c.setObj(c.deserializeString(b));
            return JsonDeserializeResult.Continue;
        };
        r.put(CharSequence.class, strFunc);
        r.put(String.class, strFunc);
        r.put(JsonPrimitiveType.class, (b, c) -> {
            c.setObj(c.deserializeJsonPrimitiveType(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(JsonBoolType.class, (b, c) -> {
            c.setObj(c.deserializeJsonBoolType(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(JsonNumberType.class, (b, c) -> {
            c.setObj(c.deserializeJsonNumberType(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(JsonStrType.class, (b, c) -> {
            c.setObj(c.deserializeJsonStrType(b));
            return JsonDeserializeResult.Continue;
        });
        BUILTIN_DESERIALIZE_OBJ_FUNC_MAP = Map.copyOf(r);
    }

    static {
        Map<Class<?>, JsonDeserializeFunc> r = new HashMap<>();
        r.put(byte[].class, (b, c) -> {
            c.setObj(c.deserializeByteArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(boolean[].class, (b, c) -> {
            c.setObj(c.deserializeBooleanArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(short[].class, (b, c) -> {
            c.setObj(c.deserializeShortArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(char[].class, (b, c) -> {
            c.setObj(c.deserializeCharArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(int[].class, (b, c) -> {
            c.setObj(c.deserializeIntArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(long[].class, (b, c) -> {
            c.setObj(c.deserializeLongArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(float[].class, (b, c) -> {
            c.setObj(c.deserializeFloatArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(double[].class, (b, c) -> {
            c.setObj(c.deserializeDoubleArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(Byte[].class, (b, c) -> {
            c.setObj(c.deserializeByteWrapperArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(Boolean[].class, (b, c) -> {
            c.setObj(c.deserializeBooleanWrapperArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(Short[].class, (b, c) -> {
            c.setObj(c.deserializeShortWrapperArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(Character[].class, (b, c) -> {
            c.setObj(c.deserializeCharWrapperArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(Integer[].class, (b, c) -> {
            c.setObj(c.deserializeIntWrapperArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(Long[].class, (b, c) -> {
            c.setObj(c.deserializeLongWrapperArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(Float[].class, (b, c) -> {
            c.setObj(c.deserializeFloatWrapperArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(Double[].class, (b, c) -> {
            c.setObj(c.deserializeDoubleWrapperArray(b));
            return JsonDeserializeResult.Continue;
        });
        JsonDeserializeFunc strArrayFunc = (b, c) -> {
            c.setObj(c.deserializeStringArray(b));
            return JsonDeserializeResult.Continue;
        };
        r.put(CharSequence[].class, strArrayFunc);
        r.put(String[].class, strArrayFunc);
        r.put(JsonPrimitiveType[].class, (b, c) -> {
            c.setObj(c.deserializeJsonPrimitiveTypeArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(JsonBoolType[].class, (b, c) -> {
            c.setObj(c.deserializeJsonBoolTypeArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(JsonNumberType[].class, (b, c) -> {
            c.setObj(c.deserializeJsonNumberTypeArray(b));
            return JsonDeserializeResult.Continue;
        });
        r.put(JsonStrType[].class, (b, c) -> {
            c.setObj(c.deserializeJsonStrTypeArray(b));
            return JsonDeserializeResult.Continue;
        });
        BUILTIN_DESERIALIZE_ARRAY_FUNC_MAP = Map.copyOf(r);
    }

    private static byte[] makeSymTable() {
        byte[] table = new byte[Byte.MAX_VALUE - Byte.MIN_VALUE + 1];
        table['\"'] = '\"';
        table['\\'] = '\\';
        table['/'] = '/';
        table['b'] = '\b';
        table['f'] = '\f';
        table['n'] = '\n';
        table['r'] = '\r';
        table['t'] = '\t';
        table[' '] = EMPTY_SYM;
        table['\n'] = EMPTY_SYM;
        table['\r'] = EMPTY_SYM;
        table['\t'] = EMPTY_SYM;
        for(int i = '0'; i <= '9'; i++) {
            table[i] = NUM_SYM;
        }
        table['-'] = NUM_SYM;
        return table;
    }

    private static byte[] makeHexTable() {
        byte[] table = new byte[Byte.MAX_VALUE - Byte.MIN_VALUE + 1];
        Arrays.fill(table, Byte.MIN_VALUE);
        for(int i = '0'; i <= '9'; i++) {
            table[i] = (byte) (i - '0');
        }
        for(int i = 'A'; i <= 'F'; i++) {
            table[i] = (byte) (i - 'A' + 10);
        }
        for(int i = 'a'; i <= 'f'; i++) {
            table[i] = (byte) (i - 'a' + 10);
        }
        return table;
    }

    private static void checkBoolStart(byte b) {
        if(b != (byte) 't' && b != (byte) 'f') {
            throw new JsonDeserializerException("not a bool start : " + b);
        }
    }

    private static void checkNumStart(byte b) {
        if (SYM_TABLE[Byte.toUnsignedInt(b)] != NUM_SYM) {
            throw new JsonDeserializerException("not a number start : " + b);
        }
    }

    private static void checkStrStart(byte b) {
        if(b != (byte) '"') {
            throw new JsonDeserializerException("not a string start : " + b);
        }
    }

    public static void checkArrayStart(byte b) {
        if(b != (byte) '[') {
            throw new JsonDeserializerException("not an array start : " + b);
        }
    }

    public static void checkObjStart(byte b) {
        if(b != (byte) '{') {
            throw new JsonDeserializerException("not a object start : " + b);
        }
    }

    private final JsonDeserializerOption option;
    private final ReadBuffer readBuffer;
    private char[] charBuffer;
    private byte[] byteBuffer;
    private int index;
    private Object obj;
    private Class<?> type;
    private Object[] arr;

    public JsonDeserializerContext(JsonDeserializerOption option, ReadBuffer readBuffer) {
        this.option = option;
        this.readBuffer = readBuffer;
        this.charBuffer = new char[option.charBufferSize()];
        this.byteBuffer = new byte[BYTE_BUFFER_INITIAL_SIZE];
    }

    public JsonDeserializerOption option() {
        return option;
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

    public int alloc(int fieldCount) {
        final int requiredBytes = TOTAL_INDEX_OFFSET + ((fieldCount + 7) >> 3);
        final int currentIndex = index;
        final int newIndex = Math.addExact(requiredBytes, currentIndex);
        if(byteBuffer.length < newIndex) {
            byte[] newByteBuffer = new byte[Math.multiplyExact(byteBuffer.length, 2)];
            System.arraycopy(byteBuffer, 0, newByteBuffer, 0, currentIndex);
            byteBuffer = newByteBuffer;
        } else {
            Arrays.fill(byteBuffer, currentIndex, newIndex, (byte) 0);
        }
        index = newIndex;
        return currentIndex;
    }

    public int marshallIndex(int contextIndex) {
        return ArrayAccess.getInt(byteBuffer, contextIndex + MARSHALL_INDEX_OFFSET);
    }

    public int dummyIndex(int contextIndex) {
        return ArrayAccess.getInt(byteBuffer, contextIndex + DUMMY_INDEX_OFFSET);
    }

    public int matchedIndex(int contextIndex) {
        return ArrayAccess.getInt(byteBuffer, contextIndex + MATCHED_INDEX_OFFSET);
    }

    public void store(int contextIndex, int marshallIndex, int dummyIndex, int matchedIndex) {
        ArrayAccess.setInt(byteBuffer, contextIndex + MARSHALL_INDEX_OFFSET, marshallIndex);
        ArrayAccess.setInt(byteBuffer, contextIndex + DUMMY_INDEX_OFFSET, dummyIndex);
        ArrayAccess.setInt(byteBuffer, contextIndex + MATCHED_INDEX_OFFSET, matchedIndex);
    }

    public boolean assign(int contextIndex, int marshallIndex) {
        final int byteOffset = contextIndex + TOTAL_INDEX_OFFSET + (marshallIndex >> 3);
        final int bitOffset = marshallIndex & 0x7;
        final byte mask = (byte) (1 << bitOffset);
        final byte val = byteBuffer[byteOffset];
        byteBuffer[byteOffset] = (byte) (val | mask);
        return (val & mask) != 0;
    }

    public MarshallInfo filter(int contextIndex, MarshallFacade fc) {
        final int fieldCount = fc.totalElements();
        for(int i = 0; i < fieldCount; i++) {
            final int byteOffset = contextIndex + TOTAL_INDEX_OFFSET + (i >> 3);
            final int bitOffset = i & 0x7;
            final byte mask = (byte) (1 << bitOffset);
            if ((byteBuffer[byteOffset] & mask) == 0) {
                MarshallInfo inf = fc.marshallInfoByIndex(i);
                if(option.ensureAllFieldsPresent() || inf.rawType().isPrimitive()) {
                    return inf;
                }
            }
        }
        throw new AssertionError("predicate doesn't match for any bits");
    }

    public void rewind(int contextIndex) {
        index = contextIndex;
    }

    private static int nextValuableByte(byte[] bytes, int position, int maxEmptyBytes) {
        final int range = Math.min(maxEmptyBytes, bytes.length - position);
        for(int i = 0; i < range; i++) {
            byte b = bytes[position + i];
            if(SYM_TABLE[Byte.toUnsignedInt(b)] != EMPTY_SYM) {
                return position + i;
            }
        }
        throw new JsonDeserializerException("valuable byte not found");
    }

    private static long nextValuableByte(MemorySegment segment, long position, int maxEmptyBytes) {
        final int range = Math.min(maxEmptyBytes, Math.toIntExact(segment.byteSize() - position));
        for(int i = 0; i < range; i++) {
            byte b = SegmentAccess.getByte(segment, position + i);
            if(SYM_TABLE[Byte.toUnsignedInt(b)] != EMPTY_SYM) {
                return position + i;
            }
        }
        throw new JsonDeserializerException("valuable byte not found");
    }

    private byte nextValuableByteFromHeap(HeapReadBuffer heapReadBuffer, boolean consume) {
        final byte[] bytes = heapReadBuffer.rawByteArray();
        final int position = heapReadBuffer.intPosition();
        final int nextPosition = nextValuableByte(bytes, position, option.maxEmptyBytes());
        heapReadBuffer.setPosition(consume ? nextPosition + 1: nextPosition);
        return bytes[nextPosition];
    }

    private byte nextValuableByteFromSegment(SegmentReadBuffer segmentReadBuffer, boolean consume) {
        final MemorySegment segment = segmentReadBuffer.rawSegment();
        final long position = segmentReadBuffer.longPosition();
        final long nextPosition = nextValuableByte(segment, position, option.maxEmptyBytes());
        segmentReadBuffer.setPosition(consume ? nextPosition + 1L : nextPosition);
        return SegmentAccess.getByte(segment, nextPosition);
    }

    public byte nextValuableByte(boolean consume) {
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> nextValuableByteFromHeap(heapReadBuffer, consume);
            case SegmentReadBuffer segmentReadBuffer -> nextValuableByteFromSegment(segmentReadBuffer, consume);
        };
    }

    private static void deserializeNullFromHeap(HeapReadBuffer heapReadBuffer) {
        final byte[] bytes = heapReadBuffer.rawByteArray();
        final int position = heapReadBuffer.intPosition();
        if (position > bytes.length - 3) {
            throw new JsonDeserializerException("eof reached while deserializing null");
        }
        if (ArrayAccess.getInt(bytes, position - 1) != COMPACT_NULL) {
            throw new JsonDeserializerException("illegal null token, position : " + position);
        }
        heapReadBuffer.setPosition(position + 3);
    }

    private static void deserializeNullFromSegment(SegmentReadBuffer segmentReadBuffer) {
        final MemorySegment segment = segmentReadBuffer.rawSegment();
        final long position = segmentReadBuffer.longPosition();
        if (position > segment.byteSize() - 3L) {
            throw new JsonDeserializerException("eof reached while deserializing null");
        }
        if (SegmentAccess.getInt(segment, position - 1L) != COMPACT_NULL) {
            throw new JsonDeserializerException("illegal null token, position : " + position);
        }
        segmentReadBuffer.setPosition(position + 3L);
    }

    public void deserializeFollowingNull() {
        switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> deserializeNullFromHeap(heapReadBuffer);
            case SegmentReadBuffer segmentReadBuffer -> deserializeNullFromSegment(segmentReadBuffer);
        }
    }

    public byte deserializeByte(byte firstByte) {
        checkNumStart(firstByte);
        int v = JsonNumberUtil.readInt(readBuffer, firstByte);
        if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
            return (byte) v;
        }
        throw new JsonDeserializerException("byte value overflow : " + v);
    }

    private static boolean deserializeBooleanFromHeap(HeapReadBuffer heapReadBuffer, byte firstByte) {
        final byte[] bytes = heapReadBuffer.rawByteArray();
        final int position = heapReadBuffer.intPosition();
        boolean r = firstByte == (byte) 't';
        int newPosition;
        int expectedValue;
        if(r) {
            newPosition = Math.addExact(position, 3);
            expectedValue = COMPACT_TRUE;
        } else {
            newPosition = Math.addExact(position, 4);
            expectedValue = COMPACT_ALSE;
        }
        if (newPosition > bytes.length) {
            throw new JsonDeserializerException("eof reached while deserializing boolean value");
        }
        if (ArrayAccess.getInt(bytes, newPosition - 4) != expectedValue) {
            throw new JsonDeserializerException("illegal boolean literal 'true' value");
        }
        heapReadBuffer.setPosition(newPosition);
        return r;
    }

    private static boolean deserializeBooleanFromSegment(SegmentReadBuffer segmentReadBuffer, byte firstByte) {
        final MemorySegment segment = segmentReadBuffer.rawSegment();
        final long position = segmentReadBuffer.longPosition();
        boolean r = firstByte == (byte) 't';
        long newPosition;
        long expectedValue;
        if(r) {
            newPosition = Math.addExact(position, 3L);
            expectedValue = COMPACT_TRUE;
        } else {
            newPosition = Math.addExact(position, 4L);
            expectedValue = COMPACT_ALSE;
        }
        if (newPosition > segment.byteSize()) {
            throw new JsonDeserializerException("eof reached while deserializing boolean value");
        }
        if (SegmentAccess.getInt(segment, newPosition - 4L) != expectedValue) {
            throw new JsonDeserializerException("illegal boolean literal 'true' value");
        }
        segmentReadBuffer.setPosition(newPosition);
        return r;
    }

    public boolean deserializeBoolean(byte firstByte) {
        checkBoolStart(firstByte);
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> deserializeBooleanFromHeap(heapReadBuffer, firstByte);
            case SegmentReadBuffer segmentReadBuffer -> deserializeBooleanFromSegment(segmentReadBuffer, firstByte);
        };
    }

    public short deserializeShort(byte firstByte) {
        checkNumStart(firstByte);
        int v = JsonNumberUtil.readInt(readBuffer, firstByte);
        if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
            return (short) v;
        }
        throw new JsonDeserializerException("short value overflow : " + v);
    }

    public char deserializeCharFromHeap(HeapReadBuffer heapReadBuffer) {
        final byte[] bytes = heapReadBuffer.rawByteArray();
        int position = heapReadBuffer.intPosition();
        if(position == bytes.length) {
            throw new JsonDeserializerException("eof reached while deserializing char from heap");
        }
        int i = Byte.toUnsignedInt(bytes[position]);
        char r;
        if(i == '\\') {
            if(position > bytes.length - 2) {
                throw new JsonDeserializerException("eof reached while deserializing escaped char from heap");
            }
            byte escaped = bytes[position + 1];
            if(escaped == (byte) 'u') {
                if(position > bytes.length - 6) {
                    throw new JsonDeserializerException("illegal escaped unicode sequence");
                }
                r = parseHexFromHeap(bytes, position);
                if(Character.isSurrogate(r)) {
                    throw new JsonDeserializerException("illegal escaped unicode surrogate sequence");
                }
                position += 6;
            } else {
                byte b = SYM_TABLE[Byte.toUnsignedInt(escaped)];
                if(b <= 0) {
                    throw new JsonDeserializerException("illegal escaped char from heap");
                }
                r = (char) b;
                position += 2;
            }
        } else if(i < 0x20) {
            throw new JsonDeserializerException("illegal unescaped ascii control byte : " + i);
        } else if(i < 0x80) {
            r = (char) i;
            position += 1;
        } else if(i < 0xE0) {
            int i1 = bytes[position + 1] & 0x3F;
            r = (char) (((i & 0x1F) << 6) | i1);
            position += 2;
        } else if(i < 0xF0) {
            int i1 = bytes[position + 1] & 0x3F;
            int i2 = bytes[position + 2] & 0x3F;
            r = (char) (((i & 0x0F) << 12) | (i1 << 6) | i2);
            position += 3;
        } else {
            throw new JsonDeserializerException("illegal surrogate start : " + i);
        }
        if(position == bytes.length || bytes[position++] != (byte) '"') {
            throw new JsonDeserializerException("not a single char");
        }
        heapReadBuffer.setPosition(position);
        return r;
    }

    public char deserializeCharFromSegment(SegmentReadBuffer segmentReadBuffer) {
        final MemorySegment segment = segmentReadBuffer.rawSegment();
        long position = segmentReadBuffer.longPosition();
        if(position >= segment.byteSize()) {
            throw new JsonDeserializerException("eof reached while deserializing char from segment");
        }
        int i = Byte.toUnsignedInt(SegmentAccess.getByte(segment, position));
        char r;
        if(i == '\\') {
            if(position > segment.byteSize() - 2L) {
                throw new JsonDeserializerException("eof reached while deserializing escaped char from segment");
            }
            byte escaped = SegmentAccess.getByte(segment, position + 1L);
            if(escaped == (byte) 'u') {
                if(position > segment.byteSize() - 6L) {
                    throw new JsonDeserializerException("illegal escaped unicode sequence");
                }
                r = parseHexFromSegment(segment, position);
                if(Character.isSurrogate(r)) {
                    throw new JsonDeserializerException("illegal escaped unicode surrogate sequence");
                }
                position += 6L;
            } else {
                byte b = SYM_TABLE[Byte.toUnsignedInt(escaped)];
                if(b <= 0) {
                    throw new JsonDeserializerException("illegal escaped char from heap");
                }
                r = (char) b;
                position += 2L;
            }
        } else if(i < 0x20) {
            throw new JsonDeserializerException("illegal unescaped ascii control byte : " + i);
        } else if(i < 0x80) {
            r = (char) i;
            position += 1L;
        } else if(i < 0xE0) {
            int i1 = SegmentAccess.getByte(segment, position + 1L) & 0x3F;
            r = (char) (((i & 0x1F) << 6) | i1);
            position += 2L;
        } else if(i < 0xF0) {
            int i1 = SegmentAccess.getByte(segment, position + 1L) & 0x3F;
            int i2 = SegmentAccess.getByte(segment, position + 2L) & 0x3F;
            r = (char) (((i & 0x0F) << 12) | (i1 << 6) | i2);
            position += 3L;
        } else {
            throw new JsonDeserializerException("illegal surrogate start : " + i);
        }
        if(position >= segment.byteSize() || SegmentAccess.getByte(segment, position++) != (byte) '"') {
            throw new JsonDeserializerException("not a single char");
        }
        segmentReadBuffer.setPosition(position);
        return r;
    }

    public char deserializeChar(byte firstByte) {
        checkStrStart(firstByte);
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> deserializeCharFromHeap(heapReadBuffer);
            case SegmentReadBuffer segmentReadBuffer -> deserializeCharFromSegment(segmentReadBuffer);
        };
    }

    public int deserializeInt(byte firstByte) {
        checkNumStart(firstByte);
        return JsonNumberUtil.readInt(readBuffer, firstByte);
    }

    public long deserializeLong(byte firstByte) {
        checkNumStart(firstByte);
        return JsonNumberUtil.readLong(readBuffer, firstByte);
    }

    public float deserializeFloat(byte firstByte) {
        checkNumStart(firstByte);
        return JsonNumberUtil.readFloat(readBuffer, option.maxNumberBytes(), firstByte);
    }

    public double deserializeDouble(byte firstByte) {
        checkNumStart(firstByte);
        return JsonNumberUtil.readDouble(readBuffer, option.maxNumberBytes(), firstByte);
    }

    public byte[] deserializeByteArrayFromHeap(HeapReadBuffer heapReadBuffer) {
        byte b = nextValuableByteFromHeap(heapReadBuffer, true);
        if (b == (byte) ']') {
            return Utils.emptyByteArray();
        }
        byte[] buf = byteBuffer;
        final int maxArrayElements = option.maxArrayElements();
        for(int i = 0, idx = index; i < maxArrayElements; i++) {
            byte v = deserializeByte(b);
            // no overflow, maxArrayElements is limited
            int newIdx = idx + 1;
            if(newIdx > buf.length) {
                buf = Arrays.copyOf(buf, buf.length << 1);
            }
            buf[idx] = v;
            idx = newIdx;
            b = nextValuableByteFromHeap(heapReadBuffer, true);
            if(b == (byte) ']') {
                byteBuffer = buf;
                return Arrays.copyOfRange(buf, index, idx);
            } else if(b == (byte) ',') {
                b = nextValuableByteFromHeap(heapReadBuffer, true);
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, exceeded limit : " + maxArrayElements);
    }

    public byte[] deserializeByteArrayFromSegment(SegmentReadBuffer segmentReadBuffer) {
        byte b = nextValuableByteFromSegment(segmentReadBuffer, true);
        if (b == (byte) ']') {
            return Utils.emptyByteArray();
        }
        byte[] buf = byteBuffer;
        final int maxArrayElements = option.maxArrayElements();
        for(int i = 0, idx = index; i < maxArrayElements; i++) {
            byte v = deserializeByte(b);
            // no overflow, maxArrayElements is limited
            int newIdx = idx + 1;
            if(newIdx > buf.length) {
                buf = Arrays.copyOf(buf, buf.length << 1);
            }
            buf[idx] = v;
            idx = newIdx;
            b = nextValuableByteFromSegment(segmentReadBuffer, true);
            if(b == (byte) ']') {
                byteBuffer = buf;
                return Arrays.copyOfRange(buf, index, idx);
            } else if(b == (byte) ',') {
                b = nextValuableByteFromSegment(segmentReadBuffer, true);
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, exceeded limit : " + maxArrayElements);
    }

    public byte[] deserializeByteArray(byte firstByte) {
        checkArrayStart(firstByte);
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> deserializeByteArrayFromHeap(heapReadBuffer);
            case SegmentReadBuffer segmentReadBuffer -> deserializeByteArrayFromSegment(segmentReadBuffer);
        };
    }

    private boolean[] deserializeBooleanArrayFromHeap(HeapReadBuffer heapReadBuffer) {
        byte b = nextValuableByteFromHeap(heapReadBuffer, true);
        if (b == (byte) ']') {
            return Utils.emptyBooleanArray();
        }
        byte[] buf = byteBuffer;
        final int maxArrayElements = option.maxArrayElements();
        for(int i = 0, idx = index; i < maxArrayElements; i++) {
            checkBoolStart(b);
            boolean v = deserializeBooleanFromHeap(heapReadBuffer, b);
            // no overflow, maxArrayElements is limited
            int newIdx = idx + 1;
            if(newIdx > buf.length) {
                buf = Arrays.copyOf(buf, buf.length << 1);
            }
            buf[idx] = v ? Byte.MAX_VALUE : Byte.MIN_VALUE;
            idx = newIdx;
            b = nextValuableByteFromHeap(heapReadBuffer, true);
            if(b == (byte) ']') {
                byteBuffer = buf;
                int len = idx - index;
                final boolean[] r = new boolean[len];
                for(int t = 0; t < len; t++) {
                    r[t] = buf[index + t] > 0;
                }
                return r;
            } else if(b == (byte) ',') {
                b = nextValuableByteFromHeap(heapReadBuffer, true);
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, exceeded limit : " + maxArrayElements);
    }

    private boolean[] deserializeBooleanArrayFromSegment(SegmentReadBuffer segmentReadBuffer) {
        byte b = nextValuableByteFromSegment(segmentReadBuffer, true);
        if (b == (byte) ']') {
            return Utils.emptyBooleanArray();
        }
        byte[] buf = byteBuffer;
        final int maxArrayElements = option.maxArrayElements();
        for(int i = 0, idx = index; i < maxArrayElements; i++) {
            checkBoolStart(b);
            boolean v = deserializeBooleanFromSegment(segmentReadBuffer, b);
            // no overflow, maxArrayElements is limited
            int newIdx = idx + 1;
            if(newIdx > buf.length) {
                buf = Arrays.copyOf(buf, buf.length << 1);
            }
            buf[idx] = v ? Byte.MAX_VALUE : Byte.MIN_VALUE;
            idx = newIdx;
            b = nextValuableByteFromSegment(segmentReadBuffer, true);
            if(b == (byte) ']') {
                byteBuffer = buf;
                int len = idx - index;
                final boolean[] r = new boolean[len];
                for(int t = 0; t < len; t++) {
                    r[t] = buf[index + t] > 0;
                }
                return r;
            } else if(b == (byte) ',') {
                b = nextValuableByteFromSegment(segmentReadBuffer, true);
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, exceeded limit : " + maxArrayElements);
    }

    public boolean[] deserializeBooleanArray(byte firstByte) {
        checkArrayStart(firstByte);
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> deserializeBooleanArrayFromHeap(heapReadBuffer);
            case SegmentReadBuffer segmentReadBuffer -> deserializeBooleanArrayFromSegment(segmentReadBuffer);
        };
    }

    private short[] deserializeShortArrayFromHeap(HeapReadBuffer heapReadBuffer) {
        byte b = nextValuableByteFromHeap(heapReadBuffer, true);
        if (b == (byte) ']') {
            return Utils.emptyShortArray();
        }
        byte[] buf = byteBuffer;
        final int maxArrayElements = option.maxArrayElements();
        for(int i = 0, idx = index; i < maxArrayElements; i++) {
            short v = deserializeShort(b);
            // no overflow, maxArrayElements is limited
            int newIdx = idx + 2;
            if(newIdx > buf.length) {
                buf = Arrays.copyOf(buf, buf.length << 1);
            }
            ArrayAccess.setShort(buf, idx, v);
            idx = newIdx;
            b = nextValuableByteFromHeap(heapReadBuffer, true);
            if(b == (byte) ']') {
                byteBuffer = buf;
                int len = (idx - index) >> 1;
                short[] r = new short[len];
                MemorySegment.copy(MemorySegment.ofArray(buf), ValueLayout.JAVA_SHORT_UNALIGNED, index, r, 0, len);
                return r;
            } else if(b == (byte) ',') {
                b = nextValuableByteFromHeap(heapReadBuffer, true);
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, exceeded limit : " + maxArrayElements);
    }

    private short[] deserializeShortArrayFromSegment(SegmentReadBuffer segmentReadBuffer) {
        byte b = nextValuableByteFromSegment(segmentReadBuffer, true);
        if (b == (byte) ']') {
            return Utils.emptyShortArray();
        }
        byte[] buf = byteBuffer;
        final int maxArrayElements = option.maxArrayElements();
        for(int i = 0, idx = index; i < maxArrayElements; i++) {
            short v = deserializeShort(b);
            // no overflow, maxArrayElements is limited
            int newIdx = idx + 2;
            if(newIdx > buf.length) {
                buf = Arrays.copyOf(buf, buf.length << 1);
            }
            ArrayAccess.setShort(buf, idx, v);
            idx = newIdx;
            b = nextValuableByteFromSegment(segmentReadBuffer, true);
            if(b == (byte) ']') {
                byteBuffer = buf;
                int len = (idx - index) >> 1;
                short[] r = new short[len];
                MemorySegment.copy(MemorySegment.ofArray(buf), ValueLayout.JAVA_SHORT_UNALIGNED, index, r, 0, len);
                return r;
            } else if(b == (byte) ',') {
                b = nextValuableByteFromSegment(segmentReadBuffer, true);
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, exceeded limit : " + maxArrayElements);
    }

    public short[] deserializeShortArray(byte firstByte) {
        checkArrayStart(firstByte);
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> deserializeShortArrayFromHeap(heapReadBuffer);
            case SegmentReadBuffer segmentReadBuffer -> deserializeShortArrayFromSegment(segmentReadBuffer);
        };
    }

    private char[] deserializeCharArrayFromHeap(HeapReadBuffer heapReadBuffer) {
        byte b = nextValuableByteFromHeap(heapReadBuffer, true);
        if (b == (byte) ']') {
            return Utils.emptyCharArray();
        }
        char[] buf = charBuffer;
        final int maxArrayElements = option.maxArrayElements();
        for(int i = 0, idx = 0; i < maxArrayElements; i++) {
            checkStrStart(b);
            char v = deserializeCharFromHeap(heapReadBuffer);
            // no overflow, maxArrayElements is limited
            int newIdx = idx + 1;
            if(newIdx > buf.length) {
                buf = Arrays.copyOf(buf, buf.length << 1);
            }
            buf[idx] = v;
            idx = newIdx;
            b = nextValuableByteFromHeap(heapReadBuffer, true);
            if(b == (byte) ']') {
                charBuffer = buf;
                return Arrays.copyOf(buf, idx);
            } else if(b == (byte) ',') {
                b = nextValuableByteFromHeap(heapReadBuffer, true);
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, exceeded limit : " + maxArrayElements);
    }

    private char[] deserializeCharArrayFromSegment(SegmentReadBuffer segmentReadBuffer) {
        byte b = nextValuableByteFromSegment(segmentReadBuffer, true);
        if (b == (byte) ']') {
            return Utils.emptyCharArray();
        }
        char[] buf = charBuffer;
        final int maxArrayElements = option.maxArrayElements();
        for(int i = 0, idx = 0; i < maxArrayElements; i++) {
            checkStrStart(b);
            char v = deserializeCharFromSegment(segmentReadBuffer);
            // no overflow, maxArrayElements is limited
            int newIdx = idx + 1;
            if(newIdx > buf.length) {
                buf = Arrays.copyOf(buf, buf.length << 1);
            }
            buf[idx] = v;
            idx = newIdx;
            b = nextValuableByteFromSegment(segmentReadBuffer, true);
            if(b == (byte) ']') {
                charBuffer = buf;
                return Arrays.copyOf(buf, idx);
            } else if(b == (byte) ',') {
                b = nextValuableByteFromSegment(segmentReadBuffer, true);
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, exceeded limit : " + maxArrayElements);
    }

    public char[] deserializeCharArray(byte firstByte) {
        checkArrayStart(firstByte);
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> deserializeCharArrayFromHeap(heapReadBuffer);
            case SegmentReadBuffer segmentReadBuffer -> deserializeCharArrayFromSegment(segmentReadBuffer);
        };
    }

    private int[] deserializeIntArrayFromHeap(HeapReadBuffer heapReadBuffer) {
        byte b = nextValuableByteFromHeap(heapReadBuffer, true);
        if (b == (byte) ']') {
            return Utils.emptyIntArray();
        }
        byte[] buf = byteBuffer;
        final int maxArrayElements = option.maxArrayElements();
        for(int i = 0, idx = index; i < maxArrayElements; i++) {
            int v = deserializeInt(b);
            // no overflow, maxArrayElements is limited
            int newIdx = idx + 4;
            if(newIdx > buf.length) {
                buf = Arrays.copyOf(buf, buf.length << 1);
            }
            ArrayAccess.setInt(buf, idx, v);
            idx = newIdx;
            b = nextValuableByteFromHeap(heapReadBuffer, true);
            if(b == (byte) ']') {
                byteBuffer = buf;
                int len = (idx - index) >> 2;
                int[] r = new int[len];
                MemorySegment.copy(MemorySegment.ofArray(buf), ValueLayout.JAVA_INT_UNALIGNED, index, r, 0, len);
                return r;
            } else if(b == (byte) ',') {
                b = nextValuableByteFromHeap(heapReadBuffer, true);
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, exceeded limit : " + maxArrayElements);
    }

    private int[] deserializeIntArrayFromSegment(SegmentReadBuffer segmentReadBuffer) {
        byte b = nextValuableByteFromSegment(segmentReadBuffer, true);
        if (b == (byte) ']') {
            return Utils.emptyIntArray();
        }
        byte[] buf = byteBuffer;
        final int maxArrayElements = option.maxArrayElements();
        for(int i = 0, idx = index; i < maxArrayElements; i++) {
            int v = deserializeInt(b);
            // no overflow, maxArrayElements is limited
            int newIdx = idx + 4;
            if(newIdx > buf.length) {
                buf = Arrays.copyOf(buf, buf.length << 1);
            }
            ArrayAccess.setInt(buf, idx, v);
            idx = newIdx;
            b = nextValuableByteFromSegment(segmentReadBuffer, true);
            if(b == (byte) ']') {
                byteBuffer = buf;
                int len = (idx - index) >> 2;
                int[] r = new int[len];
                MemorySegment.copy(MemorySegment.ofArray(buf), ValueLayout.JAVA_INT_UNALIGNED, index, r, 0, len);
                return r;
            } else if(b == (byte) ',') {
                b = nextValuableByteFromSegment(segmentReadBuffer, true);
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, exceeded limit : " + maxArrayElements);
    }

    public int[] deserializeIntArray(byte firstByte) {
        checkArrayStart(firstByte);
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> deserializeIntArrayFromHeap(heapReadBuffer);
            case SegmentReadBuffer segmentReadBuffer -> deserializeIntArrayFromSegment(segmentReadBuffer);
        };
    }

    private long[] deserializeLongArrayFromHeap(HeapReadBuffer heapReadBuffer) {
        byte b = nextValuableByteFromHeap(heapReadBuffer, true);
        if (b == (byte) ']') {
            return Utils.emptyLongArray();
        }
        byte[] buf = byteBuffer;
        final int maxArrayElements = option.maxArrayElements();
        for(int i = 0, idx = index; i < maxArrayElements; i++) {
            long v = deserializeLong(b);
            // no overflow, maxArrayElements is limited
            int newIdx = idx + 8;
            if(newIdx > buf.length) {
                buf = Arrays.copyOf(buf, buf.length << 1);
            }
            ArrayAccess.setLong(buf, idx, v);
            idx = newIdx;
            b = nextValuableByteFromHeap(heapReadBuffer, true);
            if(b == (byte) ']') {
                byteBuffer = buf;
                int len = (idx - index) >> 3;
                long[] r = new long[len];
                MemorySegment.copy(MemorySegment.ofArray(buf), ValueLayout.JAVA_LONG_UNALIGNED, index, r, 0, len);
                return r;
            } else if(b == (byte) ',') {
                b = nextValuableByteFromHeap(heapReadBuffer, true);
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, exceeded limit : " + maxArrayElements);
    }

    private long[] deserializeLongArrayFromSegment(SegmentReadBuffer segmentReadBuffer) {
        byte b = nextValuableByteFromSegment(segmentReadBuffer, true);
        if (b == (byte) ']') {
            return Utils.emptyLongArray();
        }
        byte[] buf = byteBuffer;
        final int maxArrayElements = option.maxArrayElements();
        for(int i = 0, idx = index; i < maxArrayElements; i++) {
            long v = deserializeLong(b);
            // no overflow, maxArrayElements is limited
            int newIdx = idx + 8;
            if(newIdx > buf.length) {
                buf = Arrays.copyOf(buf, buf.length << 1);
            }
            ArrayAccess.setLong(buf, idx, v);
            idx = newIdx;
            b = nextValuableByteFromSegment(segmentReadBuffer, true);
            if(b == (byte) ']') {
                byteBuffer = buf;
                int len = (idx - index) >> 3;
                long[] r = new long[len];
                MemorySegment.copy(MemorySegment.ofArray(buf), ValueLayout.JAVA_LONG_UNALIGNED, index, r, 0, len);
                return r;
            } else if(b == (byte) ',') {
                b = nextValuableByteFromSegment(segmentReadBuffer, true);
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, exceeded limit : " + maxArrayElements);
    }

    public long[] deserializeLongArray(byte firstByte) {
        checkArrayStart(firstByte);
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> deserializeLongArrayFromHeap(heapReadBuffer);
            case SegmentReadBuffer segmentReadBuffer -> deserializeLongArrayFromSegment(segmentReadBuffer);
        };
    }

    private float[] deserializeFloatArrayFromHeap(HeapReadBuffer heapReadBuffer) {
        byte b = nextValuableByteFromHeap(heapReadBuffer, true);
        if (b == (byte) ']') {
            return Utils.emptyFloatArray();
        }
        byte[] buf = byteBuffer;
        final int maxArrayElements = option.maxArrayElements();
        for(int i = 0, idx = index; i < maxArrayElements; i++) {
            float v = deserializeFloat(b);
            // no overflow, maxArrayElements is limited
            int newIdx = idx + 4;
            if(newIdx > buf.length) {
                buf = Arrays.copyOf(buf, buf.length << 1);
            }
            ArrayAccess.setFloat(buf, idx, v);
            idx = newIdx;
            b = nextValuableByteFromHeap(heapReadBuffer, true);
            if(b == (byte) ']') {
                byteBuffer = buf;
                int len = (idx - index) >> 2;
                float[] r = new float[len];
                MemorySegment.copy(MemorySegment.ofArray(buf), ValueLayout.JAVA_FLOAT_UNALIGNED, index, r, 0, len);
                return r;
            } else if(b == (byte) ',') {
                b = nextValuableByteFromHeap(heapReadBuffer, true);
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, exceeded limit : " + maxArrayElements);
    }

    private float[] deserializeFloatArrayFromSegment(SegmentReadBuffer segmentReadBuffer) {
        byte b = nextValuableByteFromSegment(segmentReadBuffer, true);
        if (b == (byte) ']') {
            return Utils.emptyFloatArray();
        }
        byte[] buf = byteBuffer;
        final int maxArrayElements = option.maxArrayElements();
        for(int i = 0, idx = index; i < maxArrayElements; i++) {
            float v = deserializeFloat(b);
            // no overflow, maxArrayElements is limited
            int newIdx = idx + 4;
            if(newIdx > buf.length) {
                buf = Arrays.copyOf(buf, buf.length << 1);
            }
            ArrayAccess.setFloat(buf, idx, v);
            idx = newIdx;
            b = nextValuableByteFromSegment(segmentReadBuffer, true);
            if(b == (byte) ']') {
                byteBuffer = buf;
                int len = (idx - index) >> 2;
                float[] r = new float[len];
                MemorySegment.copy(MemorySegment.ofArray(buf), ValueLayout.JAVA_FLOAT_UNALIGNED, index, r, 0, len);
                return r;
            } else if(b == (byte) ',') {
                b = nextValuableByteFromSegment(segmentReadBuffer, true);
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, exceeded limit : " + maxArrayElements);
    }

    public float[] deserializeFloatArray(byte firstByte) {
        checkArrayStart(firstByte);
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> deserializeFloatArrayFromHeap(heapReadBuffer);
            case SegmentReadBuffer segmentReadBuffer -> deserializeFloatArrayFromSegment(segmentReadBuffer);
        };
    }

    private double[] deserializeDoubleArrayFromHeap(HeapReadBuffer heapReadBuffer) {
        byte b = nextValuableByteFromHeap(heapReadBuffer, true);
        if (b == (byte) ']') {
            return Utils.emptyDoubleArray();
        }
        byte[] buf = byteBuffer;
        final int maxArrayElements = option.maxArrayElements();
        for(int i = 0, idx = index; i < maxArrayElements; i++) {
            double v = deserializeDouble(b);
            // no overflow, maxArrayElements is limited
            int newIdx = idx + 8;
            if(newIdx > buf.length) {
                buf = Arrays.copyOf(buf, buf.length << 1);
            }
            ArrayAccess.setDouble(buf, idx, v);
            idx = newIdx;
            b = nextValuableByteFromHeap(heapReadBuffer, true);
            if(b == (byte) ']') {
                byteBuffer = buf;
                int len = (idx - index) >> 3;
                double[] r = new double[len];
                MemorySegment.copy(MemorySegment.ofArray(buf), ValueLayout.JAVA_DOUBLE_UNALIGNED, index, r, 0, len);
                return r;
            } else if(b == (byte) ',') {
                b = nextValuableByteFromHeap(heapReadBuffer, true);
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, exceeded limit : " + maxArrayElements);
    }

    private double[] deserializeDoubleArrayFromSegment(SegmentReadBuffer segmentReadBuffer) {
        byte b = nextValuableByteFromSegment(segmentReadBuffer, true);
        if (b == (byte) ']') {
            return Utils.emptyDoubleArray();
        }
        byte[] buf = byteBuffer;
        final int maxArrayElements = option.maxArrayElements();
        for(int i = 0, idx = index; i < maxArrayElements; i++) {
            double v = deserializeDouble(b);
            // no overflow, maxArrayElements is limited
            int newIdx = idx + 8;
            if(newIdx > buf.length) {
                buf = Arrays.copyOf(buf, buf.length << 1);
            }
            ArrayAccess.setDouble(buf, idx, v);
            idx = newIdx;
            b = nextValuableByteFromSegment(segmentReadBuffer, true);
            if(b == (byte) ']') {
                byteBuffer = buf;
                int len = (idx - index) >> 3;
                double[] r = new double[len];
                MemorySegment.copy(MemorySegment.ofArray(buf), ValueLayout.JAVA_DOUBLE_UNALIGNED, index, r, 0, len);
                return r;
            } else if(b == (byte) ',') {
                b = nextValuableByteFromSegment(segmentReadBuffer, true);
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, exceeded limit : " + maxArrayElements);
    }

    public double[] deserializeDoubleArray(byte firstByte) {
        checkArrayStart(firstByte);
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> deserializeDoubleArrayFromHeap(heapReadBuffer);
            case SegmentReadBuffer segmentReadBuffer -> deserializeDoubleArrayFromSegment(segmentReadBuffer);
        };
    }

    @FunctionalInterface
    public interface ElementDeserializer<T> {
        T deserialize(JsonDeserializerContext c, byte firstByte);
    }

    private <T> T[] deserializeObjArrayFromHeap(ElementDeserializer<T> deserializer, HeapReadBuffer heapReadBuffer, IntFunction<T[]> arrayFactory) {
        byte b = nextValuableByteFromHeap(heapReadBuffer, true);
        if(b == (byte) ']') {
            return arrayFactory.apply(0);
        }
        Object[] buf = arr;
        if(buf == null) {
            buf = new Object[OBJ_ARR_INITIAL_SIZE];
        }
        final int maxArrayElements = option.maxArrayElements();
        for (int i = 0; i < maxArrayElements; ) {
            // no overflow, maxArrayElements is limited
            if(i == buf.length) {
                buf = Arrays.copyOf(buf, buf.length << 1);
            }
            if(b == (byte) 'n') {
                deserializeNullFromHeap(heapReadBuffer);
                buf[i++] = null;
            } else {
                buf[i++] = deserializer.deserialize(this, b);
            }
            b = nextValuableByteFromHeap(heapReadBuffer, true);
            if(b == (byte) ']') {
                arr = buf;
                T[] r = arrayFactory.apply(i);
                //noinspection SuspiciousSystemArraycopy
                System.arraycopy(buf, 0, r, 0, i);
                return r;
            } else if(b == (byte) ',') {
                b = nextValuableByteFromHeap(heapReadBuffer, true);
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + maxArrayElements);
    }

    private <T> T[] deserializeObjArrayFromSegment(ElementDeserializer<T> deserializer, SegmentReadBuffer segmentReadBuffer, IntFunction<T[]> arrayFactory) {
        byte b = nextValuableByteFromSegment(segmentReadBuffer, true);
        if(b == (byte) ']') {
            return arrayFactory.apply(0);
        }
        Object[] buf = arr;
        if(buf == null) {
            buf = new Object[OBJ_ARR_INITIAL_SIZE];
        }
        final int maxArrayElements = option.maxArrayElements();
        for (int i = 0; i < maxArrayElements; ) {
            // no overflow, maxArrayElements is limited
            if(i == buf.length) {
                buf = Arrays.copyOf(buf, buf.length << 1);
            }
            if(b == (byte) 'n') {
                deserializeNullFromSegment(segmentReadBuffer);
                buf[i++] = null;
            } else {
                buf[i++] = deserializer.deserialize(this, b);
            }
            b = nextValuableByteFromSegment(segmentReadBuffer, true);
            if(b == (byte) ']') {
                arr = buf;
                T[] r = arrayFactory.apply(i);
                //noinspection SuspiciousSystemArraycopy
                System.arraycopy(buf, 0, r, 0, i);
            } else if(b == (byte) ',') {
                b = nextValuableByteFromSegment(segmentReadBuffer, true);
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + maxArrayElements);
    }

    private <T> T[] deserializeObjArray(ElementDeserializer<T> deserializer, IntFunction<T[]> arrayFactory) {
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> deserializeObjArrayFromHeap(deserializer, heapReadBuffer, arrayFactory);
            case SegmentReadBuffer segmentReadBuffer -> deserializeObjArrayFromSegment(deserializer, segmentReadBuffer, arrayFactory);
        };
    }

    public Byte[] deserializeByteWrapperArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(JsonDeserializerContext::deserializeByte, Byte[]::new);
    }

    public Boolean[] deserializeBooleanWrapperArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(JsonDeserializerContext::deserializeBoolean, Boolean[]::new);
    }

    public Short[] deserializeShortWrapperArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(JsonDeserializerContext::deserializeShort, Short[]::new);
    }

    public Character[] deserializeCharWrapperArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(JsonDeserializerContext::deserializeChar, Character[]::new);
    }

    public Integer[] deserializeIntWrapperArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(JsonDeserializerContext::deserializeInt, Integer[]::new);
    }

    public Long[] deserializeLongWrapperArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(JsonDeserializerContext::deserializeLong, Long[]::new);
    }

    public Float[] deserializeFloatWrapperArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(JsonDeserializerContext::deserializeFloat, Float[]::new);
    }

    public Double[] deserializeDoubleWrapperArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(JsonDeserializerContext::deserializeDouble, Double[]::new);
    }

    private static int copyAsciiFromHeap(char[] buf, byte[] bytes, int position, int end) {
        final int avail = Math.min(buf.length, end - position);
        int i = 0;
        for( ; i <= avail - BYTE_SPECIES.length(); i += BYTE_SPECIES.length()) {
            ByteVector byteVector = ByteVector.fromArray(BYTE_SPECIES, bytes, position + i);
            ShortVector part0 = (ShortVector) byteVector.convertShape(VectorOperators.B2S, SHORT_SPECIES, 0);
            ShortVector part1 = (ShortVector) byteVector.convertShape(VectorOperators.B2S, SHORT_SPECIES, 1);
            part0.intoCharArray(buf, i);
            part1.intoCharArray(buf, i + SHORT_SPECIES.length());
            int matched = byteVector.lt((byte) 0x20)
                    .or(byteVector.eq((byte) '\\'))
                    .or(byteVector.eq((byte) '"')).firstTrue();
            if(matched != BYTE_SPECIES.length()) {
                return i + matched;
            }
        }
        return i;
    }

    private static int copyAsciiFromSegment(char[] buf, MemorySegment segment, long position, long end) {
        final long avail = Math.min(buf.length, end - position);
        int i = 0;
        for( ; i <= avail - BYTE_SPECIES.length(); i += BYTE_SPECIES.length()) {
            ByteVector byteVector = ByteVector.fromMemorySegment(BYTE_SPECIES, segment, position + i, ByteOrder.nativeOrder()); // byteOrder will be ignored
            ShortVector part0 = (ShortVector) byteVector.convertShape(VectorOperators.B2S, SHORT_SPECIES, 0);
            ShortVector part1 = (ShortVector) byteVector.convertShape(VectorOperators.B2S, SHORT_SPECIES, 1);
            part0.intoCharArray(buf, i);
            part1.intoCharArray(buf, i + SHORT_SPECIES.length());
            int matched = byteVector.lt((byte) 0x20)
                    .or(byteVector.eq((byte) '\\'))
                    .or(byteVector.eq((byte) '"')).firstTrue();
            if(matched != BYTE_SPECIES.length()) {
                return i + matched;
            }
        }
        return i;
    }

    private static boolean parseUnicodeFromHeap(char[] buf, int bufIndex, byte[] bytes, int position, int end) {
        if (position > end - 4) {
            throw new JsonDeserializerException("illegal escaped unicode sequence");
        }
        char c = parseHexFromHeap(bytes, position);
        buf[bufIndex] = c;
        if(Character.isHighSurrogate(c)) {
            if(position > end - 10 ||
                    bytes[position + 4] != (byte) '\\' ||
                    bytes[position + 5] != (byte) 'u') {
                throw new JsonDeserializerException("illegal escaped low surrogate unicode sequence");
            }
            char c2 = parseHexFromHeap(bytes, position + 6);
            if(!Character.isLowSurrogate(c2)) {
                throw new JsonDeserializerException("illegal escaped low surrogate unicode sequence");
            }
            buf[bufIndex + 1] = c2;
            return true;
        }
        return false;
    }

    private static boolean parseUnicodeFromSegment(char[] buf, int bufIndex, MemorySegment segment, long position, long end) {
        if (position > end - 4L) {
            throw new JsonDeserializerException("illegal escaped unicode sequence");
        }
        char c = parseHexFromSegment(segment, position);
        buf[bufIndex] = c;
        if(Character.isHighSurrogate(c)) {
            if(position > end - 10L ||
                    SegmentAccess.getByte(segment, position + 4L) != (byte) '\\' ||
                    SegmentAccess.getByte(segment, position + 5L) != (byte) 'u') {
                throw new JsonDeserializerException("illegal escaped low surrogate unicode sequence");
            }
            char c2 = parseHexFromSegment(segment, position + 6L);
            if(!Character.isLowSurrogate(c2)) {
                throw new JsonDeserializerException("illegal escaped low surrogate unicode sequence");
            }
            buf[bufIndex + 1] = c2;
            return true;
        }
        return false;
    }

    private static char parseHexFromHeap(byte[] bytes, int position) {
        int i1 = HEX_TABLE[Byte.toUnsignedInt(bytes[position])];
        int i2 = HEX_TABLE[Byte.toUnsignedInt(bytes[position + 1])];
        int i3 = HEX_TABLE[Byte.toUnsignedInt(bytes[position + 2])];
        int i4 = HEX_TABLE[Byte.toUnsignedInt(bytes[position + 3])];
        if((i1 | i2 | i3 | i4) < 0) {
            throw new JsonDeserializerException("illegal escaped unicode sequence");
        }
        return (char) ((i1 << 12) | (i2 << 8) | (i3 << 4) | i4);
    }

    private static char parseHexFromSegment(MemorySegment segment, long position) {
        int i1 = HEX_TABLE[Byte.toUnsignedInt(SegmentAccess.getByte(segment, position))];
        int i2 = HEX_TABLE[Byte.toUnsignedInt(SegmentAccess.getByte(segment, position + 1L))];
        int i3 = HEX_TABLE[Byte.toUnsignedInt(SegmentAccess.getByte(segment, position + 2L))];
        int i4 = HEX_TABLE[Byte.toUnsignedInt(SegmentAccess.getByte(segment, position + 3L))];
        if((i1 | i2 | i3 | i4) < 0) {
            throw new JsonDeserializerException("illegal escaped unicode sequence");
        }
        return (char) ((i1 << 12) | (i2 << 8) | (i3 << 4) | i4);
    }

    private String deserializeStringFromHeap(HeapReadBuffer heapReadBuffer) {
        final byte[] bytes = heapReadBuffer.rawByteArray();
        int position = heapReadBuffer.intPosition();
        final int end = Math.min(option.maxStringBytes(), bytes.length - position) + position;
        char[] buf = this.charBuffer;
        int bufIndex = copyAsciiFromHeap(buf, bytes, position, end);
        position += bufIndex;
        while (position < end) {
            int nextBufIndex = bufIndex + 2;
            if(nextBufIndex > buf.length) {
                buf = Arrays.copyOf(buf, buf.length << 1); // no overflow
            }
            int i = Byte.toUnsignedInt(bytes[position]);
            if(i == '"') {
                charBuffer = buf;
                heapReadBuffer.setPosition(position + 1);
                return new String(buf, 0, bufIndex);
            } else if(i == '\\') {
                byte b = bytes[position + 1];
                if(b == (byte) 'u') {
                    if(parseUnicodeFromHeap(buf, bufIndex, bytes, position, end)) {
                        position += 12;
                        bufIndex += 2;
                    } else {
                        position += 6;
                        bufIndex += 1;
                    }
                } else {
                    byte sym = SYM_TABLE[Byte.toUnsignedInt(b)];
                    if(sym <= 0) {
                        throw new JsonDeserializerException("illegal escape sequence : " + sym);
                    }
                    buf[bufIndex++] = (char) sym;
                    position += 2;
                }
            } else if(i < 0x20) {
                throw new JsonDeserializerException("illegal unescaped ascii control byte : " + i);
            } else if(i < 0x80) {
                buf[bufIndex++] = (char) i;
                position += 1;
            } else if(i < 0xE0) {
                int i1 = bytes[position + 1] & 0x3F;
                buf[bufIndex++] = (char) (((i & 0x1F) << 6) | i1);
                position += 2;
            } else if(i < 0xF0) {
                int i1 = bytes[position + 1] & 0x3F;
                int i2 = bytes[position + 2] & 0x3F;
                buf[bufIndex++] = (char) (((i & 0x0F) << 12) | (i1 << 6) | i2);
                position += 3;
            } else {
                int i1 = bytes[position + 1];
                int i2 = bytes[position + 2];
                int i3 = bytes[position + 3];
                buf[bufIndex] = (char) (0xD800 | ((i & 0x07) << 8) | ((i1 & 0x3F) << 2) | ((i2 & 0x30) >>> 4));
                buf[bufIndex + 1] = (char) (0xDC00 | ((i2 & 0x0F) << 6) | (i3 & 0x3F));
                bufIndex += 2;
                position += 4;
            }
        }
        throw new JsonDeserializerException("illegal json string");
    }

    private String deserializeStringFromSegment(SegmentReadBuffer segmentReadBuffer) {
        final MemorySegment segment = segmentReadBuffer.rawSegment();
        long position = segmentReadBuffer.longPosition();
        final long end = Math.min(option.maxStringBytes(), segment.byteSize() - position) + position;
        char[] buf = this.charBuffer;
        int bufIndex = copyAsciiFromSegment(buf, segment, position, end);
        position += bufIndex;
        while (position < end) {
            int nextBufIndex = bufIndex + 2;
            if(nextBufIndex > buf.length) {
                buf = Arrays.copyOf(buf, buf.length << 1); // no overflow
            }
            int i = Byte.toUnsignedInt(SegmentAccess.getByte(segment, position));
            if(i == '"') {
                charBuffer = buf;
                segmentReadBuffer.setPosition(position + 1L);
                return new String(buf, 0, bufIndex);
            } else if(i == '\\') {
                byte b = SegmentAccess.getByte(segment, position + 1);
                if(b == (byte) 'u') {
                    if(parseUnicodeFromSegment(buf, bufIndex, segment, position, end)) {
                        position += 12L;
                        bufIndex += 2;
                    } else {
                        position += 6L;
                        bufIndex++;
                    }
                } else {
                    byte sym = SYM_TABLE[Byte.toUnsignedInt(b)];
                    if(sym <= 0) {
                        throw new JsonDeserializerException("illegal escape sequence : " + sym);
                    }
                    buf[bufIndex++] = (char) sym;
                    position += 2L;
                }
            } else if(i < 0x20) {
                throw new JsonDeserializerException("illegal unescaped ascii control byte : " + i);
            } else if(i < 0x80) {
                buf[bufIndex++] = (char) i;
                position += 1L;
            } else if(i < 0xE0) {
                int i1 = SegmentAccess.getByte(segment, position + 1) & 0x3F;
                buf[bufIndex++] = (char) (((i & 0x1F) << 6) | i1);
                position += 2L;
            } else if(i < 0xF0) {
                int i1 = SegmentAccess.getByte(segment, position + 1) & 0x3F;
                int i2 = SegmentAccess.getByte(segment, position + 2) & 0x3F;
                buf[bufIndex++] = (char) (((i & 0x0F) << 12) | (i1 << 6) | i2);
                position += 3L;
            } else {
                int i1 = SegmentAccess.getByte(segment, position + 1);
                int i2 = SegmentAccess.getByte(segment, position + 2);
                int i3 = SegmentAccess.getByte(segment, position + 3);
                buf[bufIndex] = (char) (0xD800 | ((i & 0x07) << 8) | ((i1 & 0x3F) << 2) | ((i2 & 0x30) >>> 4));
                buf[bufIndex + 1] = (char) (0xDC00 | ((i2 & 0x0F) << 6) | (i3 & 0x3F));
                bufIndex += 2;
                position += 4L;
            }
        }
        throw new JsonDeserializerException("illegal json string");
    }

    public String deserializeString(byte firstByte) {
        checkStrStart(firstByte);
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> deserializeStringFromHeap(heapReadBuffer);
            case SegmentReadBuffer segmentReadBuffer -> deserializeStringFromSegment(segmentReadBuffer);
        };
    }

    public String[] deserializeStringArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(JsonDeserializerContext::deserializeString, String[]::new);
    }

    public JsonPrimitiveType deserializeJsonPrimitiveType(byte firstByte) {
        if (firstByte == (byte) 't' || firstByte == (byte) 'f') {
            return deserializeJsonBoolType(firstByte);
        } else if(firstByte == '"') {
            return deserializeJsonStrType(firstByte);
        } else if(SYM_TABLE[Byte.toUnsignedInt(firstByte)] == NUM_SYM) {
            return deserializeJsonNumberType(firstByte);
        } else {
            throw new JsonDeserializerException("illegal first byte for json primitive type : " + firstByte);
        }
    }

    public JsonPrimitiveType[] deserializeJsonPrimitiveTypeArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(JsonDeserializerContext::deserializeJsonPrimitiveType, JsonPrimitiveType[]::new);
    }

    public JsonBoolType deserializeJsonBoolType(byte firstByte) {
        return new JsonBoolType(deserializeBoolean(firstByte));
    }

    public JsonBoolType[] deserializeJsonBoolTypeArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(JsonDeserializerContext::deserializeJsonBoolType, JsonBoolType[]::new);
    }
    
    private JsonNumberType deserializeJsonNumberTypeFromHeap(HeapReadBuffer heapReadBuffer, byte firstByte) {
        FpStrRep rep = JsonNumberUtil.readFpStrRepFromHeap(heapReadBuffer, option.maxNumberBytes(), firstByte);
        int len = rep.len();
        byte[] bytes = heapReadBuffer.rawByteArray();
        int position = heapReadBuffer.intPosition();
        byte[] numberBytes = Arrays.copyOfRange(bytes, position - len, position);
        return new JsonNumberType(numberBytes);
    }

    private JsonNumberType deserializeJsonNumberTypeFromSegment(SegmentReadBuffer segmentReadBuffer, byte firstByte) {
        FpStrRep rep = JsonNumberUtil.readFpStrRepFromSegment(segmentReadBuffer, option.maxNumberBytes(), firstByte);
        int len = rep.len();
        MemorySegment segment = segmentReadBuffer.rawSegment();
        long position = segmentReadBuffer.longPosition();
        byte[] r = new byte[len];
        MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, position - len, r, 0, len);
        return new JsonNumberType(r);
    }

    public JsonNumberType deserializeJsonNumberType(byte firstByte) {
        checkNumStart(firstByte);
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> deserializeJsonNumberTypeFromHeap(heapReadBuffer, firstByte);
            case SegmentReadBuffer segmentReadBuffer -> deserializeJsonNumberTypeFromSegment(segmentReadBuffer, firstByte);
        };
    }

    public JsonNumberType[] deserializeJsonNumberTypeArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(JsonDeserializerContext::deserializeJsonNumberType, JsonNumberType[]::new);
    }
    
    public JsonStrType deserializeJsonStrType(byte firstByte) {
        return new JsonStrType(deserializeString(firstByte));
    }

    public JsonStrType[] deserializeJsonStrTypeArray(byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray(JsonDeserializerContext::deserializeJsonStrType, JsonStrType[]::new);
    }

    private byte skipColonFromHeap(HeapReadBuffer heapReadBuffer) {
        final byte[] bytes = heapReadBuffer.rawByteArray();
        final int position = heapReadBuffer.intPosition();
        final int range = Math.min(option.maxEmptyBytes(), bytes.length - position);
        byte b = Byte.MIN_VALUE;
        int i = 0;
        for( ; i < range; i++) {
            b = bytes[position + i];
            if(SYM_TABLE[Byte.toUnsignedInt(b)] != EMPTY_SYM) {
                break ;
            }
        }
        if(i == range || b != (byte) ':') {
            throw new JsonDeserializerException("colon not found");
        }
        i += 1; // no overflow
        for( ; i < range; i++) {
            b = bytes[position + i];
            if(SYM_TABLE[Byte.toUnsignedInt(b)] != EMPTY_SYM) {
                heapReadBuffer.setPosition(position + i + 1);
                return b;
            }
        }
        throw new JsonDeserializerException("valuable byte not found");
    }

    private byte skipColonFromSegment(SegmentReadBuffer segmentReadBuffer) {
        final MemorySegment segment = segmentReadBuffer.rawSegment();
        final long position = segmentReadBuffer.longPosition();
        final int range = Math.min(option.maxEmptyBytes(), Math.toIntExact(segment.byteSize() - position));
        byte b = Byte.MIN_VALUE;
        int i = 0;
        for( ; i < range; i++) {
            b = SegmentAccess.getByte(segment, position + i);
            if(SYM_TABLE[Byte.toUnsignedInt(b)] != EMPTY_SYM) {
                break ;
            }
        }
        if(i == range || b != (byte) ':') {
            throw new JsonDeserializerException("colon not found");
        }
        i += 1; // no overflow
        for( ; i < range; i++) {
            b = SegmentAccess.getByte(segment, position + i);
            if(SYM_TABLE[Byte.toUnsignedInt(b)] != EMPTY_SYM) {
                segmentReadBuffer.setPosition(position + i + 1);
                return b;
            }
        }
        throw new JsonDeserializerException("valuable byte not found");
    }

    public byte skipColon() {
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> skipColonFromHeap(heapReadBuffer);
            case SegmentReadBuffer segmentReadBuffer -> skipColonFromSegment(segmentReadBuffer);
        };
    }

    public boolean skipAnyValue(byte firstByte) {
        if(firstByte == (byte) 'n') {
            skipFollowingNullValue();
        } else if(firstByte == (byte) 't' || firstByte == (byte) 'f') {
            skipBoolValue(firstByte);
        } else if(firstByte == (byte) '"') {
            skipStringValue();
        } else if(SYM_TABLE[Byte.toUnsignedInt(firstByte)] == NUM_SYM) {
            skipNumberValue(firstByte);
        } else {
            return false;
        }
        return true;
    }

    public void skipFollowingNullValue() {
        deserializeFollowingNull();
    }

    public void skipBoolValue(byte firstByte) {
        deserializeBoolean(firstByte);
    }

    public void skipNumberValue(byte firstByte) {
        checkNumStart(firstByte);
        FpStrRep _ = JsonNumberUtil.readFpStrRep(readBuffer, option.maxNumberBytes(), firstByte);
    }

    private static int skipStrFromHeap(byte[] bytes, int position, int end, boolean allowUtf) {
        for( ; position <= end - BYTE_SPECIES.length(); position += BYTE_SPECIES.length()) {
            ByteVector byteVector = ByteVector.fromArray(BYTE_SPECIES, bytes, position);
            int matched = byteVector.compare(allowUtf ? VectorOperators.ULT : VectorOperators.LT, (byte) 0x20)
                    .or(byteVector.eq((byte) '\\'))
                    .or(byteVector.eq((byte) '"')).firstTrue();
            if(matched != BYTE_SPECIES.length()) {
                return position + matched;
            }
        }
        return position;
    }

    private static long skipStrFromSegment(MemorySegment segment, long position, long end, boolean allowUtf) {
        for( ; position <= end - BYTE_SPECIES.length(); position += BYTE_SPECIES.length()) {
            ByteVector byteVector = ByteVector.fromMemorySegment(BYTE_SPECIES, segment, position, ByteOrder.nativeOrder()); // byteOrder will be ignored
            int matched = byteVector.compare(allowUtf ? VectorOperators.ULT : VectorOperators.LT, (byte) 0x20)
                    .or(byteVector.eq((byte) '\\'))
                    .or(byteVector.eq((byte) '"')).firstTrue();
            if(matched != BYTE_SPECIES.length()) {
                return position + matched;
            }
        }
        return position;
    }

    private static boolean skipUnicodeFromHeap(byte[] bytes, int position, int end) {
        if (position > end - 4) {
            throw new JsonDeserializerException("illegal escaped unicode sequence");
        }
        char c = parseHexFromHeap(bytes, position);
        if(Character.isHighSurrogate(c)) {
            if(position > end - 10 ||
                    bytes[position + 4] != (byte) '\\' ||
                    bytes[position + 5] != (byte) 'u') {
                throw new JsonDeserializerException("illegal escaped low surrogate unicode sequence");
            }
            char c2 = parseHexFromHeap(bytes, position + 6);
            if(!Character.isLowSurrogate(c2)) {
                throw new JsonDeserializerException("illegal escaped low surrogate unicode sequence");
            }
            return true;
        }
        return false;
    }

    private static boolean skipUnicodeFromSegment(MemorySegment segment, long position, long end) {
        if (position > end - 4L) {
            throw new JsonDeserializerException("illegal escaped unicode sequence");
        }
        char c = parseHexFromSegment(segment, position);
        if(Character.isHighSurrogate(c)) {
            if(position > end - 10L ||
                    SegmentAccess.getByte(segment, position + 4L) != (byte) '\\' ||
                    SegmentAccess.getByte(segment, position + 5L) != (byte) 'u') {
                throw new JsonDeserializerException("illegal escaped low surrogate unicode sequence");
            }
            char c2 = parseHexFromSegment(segment, position + 6L);
            if(!Character.isLowSurrogate(c2)) {
                throw new JsonDeserializerException("illegal escaped low surrogate unicode sequence");
            }
            return true;
        }
        return false;
    }

    private void skipStringFromHeap(HeapReadBuffer heapReadBuffer) {
        final byte[] bytes = heapReadBuffer.rawByteArray();
        int position = heapReadBuffer.intPosition();
        final int end = Math.min(option.maxStringBytes(), bytes.length - position) + position;
        position = skipStrFromHeap(bytes, position, end, false);
        while (position < end) {
            int i = Byte.toUnsignedInt(bytes[position]);
            if(i == '"') {
                heapReadBuffer.setPosition(position + 1);
                return ;
            } else if(i == '\\') {
                byte b = bytes[position + 1];
                if(b == (byte) 'u') {
                    position += skipUnicodeFromHeap(bytes, position, end) ? 12 : 6;
                } else {
                    byte sym = SYM_TABLE[Byte.toUnsignedInt(b)];
                    if(sym <= 0) {
                        throw new JsonDeserializerException("illegal escape sequence : " + sym);
                    }
                    position += 2;
                }
            } else if(i < 0x20) {
                throw new JsonDeserializerException("illegal unescaped ascii control byte : " + i);
            } else if(i < 0x80) {
                position += 1;
            } else if(i < 0xE0) {
                position += 2;
            } else if(i < 0xF0) {
                position += 3;
            } else {
                position += 4;
            }
        }
        throw new JsonDeserializerException("closing quote not found");
    }

    private void skipStringFromSegment(SegmentReadBuffer segmentReadBuffer) {
        final MemorySegment segment = segmentReadBuffer.rawSegment();
        long position = segmentReadBuffer.longPosition();
        final long end = Math.min(option.maxStringBytes(), segment.byteSize() - position) + position;
        position = skipStrFromSegment(segment, position, end, false);
        while (position < end) {
            int i = Byte.toUnsignedInt(SegmentAccess.getByte(segment, position));
            if(i == '"') {
                segmentReadBuffer.setPosition(position + 1L);
                return ;
            } else if(i == '\\') {
                byte b = SegmentAccess.getByte(segment, position + 1L);
                if(b == (byte) 'u') {
                    position += skipUnicodeFromSegment(segment, position, end) ? 12L : 6L;
                } else {
                    byte sym = SYM_TABLE[Byte.toUnsignedInt(b)];
                    if(sym <= 0) {
                        throw new JsonDeserializerException("illegal escape sequence : " + sym);
                    }
                    position += 2L;
                }
            } else if(i < 0x20) {
                throw new JsonDeserializerException("illegal unescaped ascii control byte : " + i);
            } else if(i < 0x80) {
                position += 1L;
            } else if(i < 0xE0) {
                position += 2L;
            } else if(i < 0xF0) {
                position += 3L;
            } else {
                position += 4L;
            }
        }
        throw new JsonDeserializerException("closing quote not found");
    }

    public void skipStringValue() {
        switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> skipStringFromHeap(heapReadBuffer);
            case SegmentReadBuffer segmentReadBuffer -> skipStringFromSegment(segmentReadBuffer);
        }
    }

    private MarshallInfo deserializeMarshallInfoFromHeap(MarshallFacade fc, HeapReadBuffer heapReadBuffer) {
        final byte[] bytes = heapReadBuffer.rawByteArray();
        final int position = heapReadBuffer.intPosition();
        final int end = Math.min(option.maxStringBytes(), bytes.length - position) + position;
        final int searched = skipStrFromHeap(bytes, position, end, true);
        if(searched == end) {
            throw new JsonDeserializerException("closing quote not found");
        }
        if(bytes[searched] == (byte) '"') {
            heapReadBuffer.setPosition(searched + 1);
            return fc.marshallInfoByMappedName(bytes, position, searched - position);
        }
        return fc.marshallInfoByMappedName(deserializeStringFromHeap(heapReadBuffer));
    }

    private MarshallInfo deserializeMarshallInfoFromSegment(MarshallFacade fc, SegmentReadBuffer segmentReadBuffer) {
        final MemorySegment segment = segmentReadBuffer.rawSegment();
        final long position = segmentReadBuffer.longPosition();
        final long end = Math.min(option.maxStringBytes(), segment.byteSize() - position) + position;
        final long searched = skipStrFromSegment(segment, position, end, true);
        if(searched == end) {
            throw new JsonDeserializerException("closing quote not found");
        }
        if(SegmentAccess.getByte(segment, searched) == (byte) '"') {
            segmentReadBuffer.setPosition(searched + 1L);
            return fc.marshallInfoByMappedName(segment, position, searched - position);
        }
        return fc.marshallInfoByMappedName(deserializeStringFromSegment(segmentReadBuffer));
    }

    public MarshallInfo deserializeMarshallInfo(MarshallFacade fc, byte firstByte) {
        checkStrStart(firstByte);
        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> deserializeMarshallInfoFromHeap(fc, heapReadBuffer);
            case SegmentReadBuffer segmentReadBuffer -> deserializeMarshallInfoFromSegment(fc, segmentReadBuffer);
        };
    }
    
    public Enum<?> deserializeEnum(Class<?> enumType, byte firstByte) {
        Enum<?>[] enumConstants = (Enum<?>[]) enumType.getEnumConstants();
        MarshallFacade fc = Marshalls.enumMarshallFacade(enumType);
        if(fc == null) {
            String name = deserializeString(firstByte);
            for (Enum<?> e : enumConstants) {
                if (e.name().equals(name)) {
                    return e;
                }
            }
            throw new JsonDeserializerException("enum item not found for item : " + name);
        }
        MarshallInfo inf = deserializeMarshallInfo(fc, firstByte);
        if(inf == null) {
            throw new JsonDeserializerException("enum item not found for type : " + enumType.getName());
        }
        return enumConstants[inf.index()];
    }

    public Enum<?>[] deserializeEnumArray(Class<?> enumType, byte firstByte) {
        checkArrayStart(firstByte);
        return deserializeObjArray((c, b) -> c.deserializeEnum(enumType, b), Enum<?>[]::new);
    }

    public static JsonDeserializeFunc builtinDeserializeObjFunc(Class<?> rawType) {
        return BUILTIN_DESERIALIZE_OBJ_FUNC_MAP.get(rawType);
    }

    public static JsonDeserializeFunc builtinDeserializeArrayFunc(Class<?> rawType) {
        return BUILTIN_DESERIALIZE_ARRAY_FUNC_MAP.get(rawType);
    }

    // builtin type has the highest priority
    // then check if current type could be override by option
    // enum must be specially treated
    // finally assuming marshallable
    public JsonDeserializeFunc valueDeserializeFunc(Class<?> rawType) {
        // builtin type has the highest priority
        if (rawType.isArray()) {
            JsonDeserializeFunc builtinDeserializeArrFunc = builtinDeserializeArrayFunc(rawType);
            if (builtinDeserializeArrFunc != null) {
                return builtinDeserializeArrFunc;
            }
            JsonDeserializeFunc customArrFunc = option.customArrFunc(rawType);
            if(customArrFunc != null) {
                return customArrFunc;
            }
            Class<?> componentType = rawType.componentType();
            if(componentType.isEnum()) {
                return (b, c) -> {
                    c.setObj(c.deserializeEnumArray(rawType, b));
                    return JsonDeserializeResult.Continue;
                };
            }
            return (b, c) -> {
                checkArrayStart(b);
                c.setType(componentType);
                return JsonDeserializeResult.NewArr;
            };
        }
        JsonDeserializeFunc builtinDeserializeFunc = builtinDeserializeObjFunc(rawType);
        if(builtinDeserializeFunc != null) {
            return builtinDeserializeFunc;
        }
        // check if current type could be override by option
        JsonDeserializeFunc customFunc = option.customFunc(rawType);
        if(customFunc != null) {
            return customFunc;
        }
        // enum must be specially treated
        if(rawType.isEnum()) {
            return (b, c) -> {
                c.setObj(c.deserializeEnum(rawType, b));
                return JsonDeserializeResult.Continue;
            };
        }
        return (b, c) -> {
            JsonDeserializerContext.checkObjStart(b);
            c.setType(rawType);
            return JsonDeserializeResult.NewMarshallable;
        };
    }
}
