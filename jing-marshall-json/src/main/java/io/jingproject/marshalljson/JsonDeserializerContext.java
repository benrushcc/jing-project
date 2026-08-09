package io.jingproject.marshalljson;

import io.jingproject.common.*;
import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.MarshallInfo;
import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class JsonDeserializerContext {
    private static final int BITMAP_INITIAL_SIZE = 16;
    private static final VectorSpecies<Short> SHORT_SPECIES;
    private static final VectorSpecies<Byte> BYTE_SPECIES;
    private static final int COMPACT_TRUE = Utils.compact(Utils.compact((byte) 't', (byte) 'r'), Utils.compact((byte) 'u', (byte) 'e'));
    private static final int COMPACT_ALSE = Utils.compact(Utils.compact((byte) 'a', (byte) 'l'), Utils.compact((byte) 's', (byte) 'e'));
    private static final int COMPACT_NULL = Utils.compact(Utils.compact((byte) 'n', (byte) 'u'), Utils.compact((byte) 'l', (byte) 'l'));
    private static final int OBJ_ARR_INITIAL_SIZE = 8;
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
        JsonDeserializeFunc strFunc = (b, c) -> {
            c.setObj(c.deserializeStringArray(b));
            return JsonDeserializeResult.Continue;
        };
        r.put(CharSequence[].class, strFunc);
        r.put(String[].class, strFunc);
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

    private final JsonDeserializerOption option;
    private final ReadBuffer readBuffer;
    private char[] chars;
    private int charsIndex = 0;
    private byte[] bytes;
    private int bytesIndex = 0;
    private byte[] bitmap;
    private int bitmapIndex = 0;
    private Object[] objArr = null;
    private Object obj = null;
    private boolean asArray = false;
    private Class<?> type = null;

    public JsonDeserializerContext(JsonDeserializerOption option, ReadBuffer readBuffer) {
        this.option = option;
        this.readBuffer = readBuffer;
        this.chars = new char[option.charBufferSize()];
        this.bytes = new byte[option.byteBufferSize()];
        this.bitmap = new byte[BITMAP_INITIAL_SIZE];
    }

    private static char parseUnicode(ReadBuffer readBuffer) {

        int i1 = parseHex(readBuffer.readByte()) << 12;
        int i2 = parseHex(readBuffer.readByte()) << 8;
        int i3 = parseHex(readBuffer.readByte()) << 4;
        int i4 = parseHex(readBuffer.readByte());
        return (char) (i1 | i2 | i3 | i4);
    }

    private static char parseUnicode(byte[] bytes, int index) {

        int i1 = parseHex(bytes[index]) << 12;
        int i2 = parseHex(bytes[index + 1]) << 8;
        int i3 = parseHex(bytes[index + 2]) << 4;
        int i4 = parseHex(bytes[index + 3]);
        return (char) (i1 | i2 | i3 | i4);
    }

    private static char parseUnicode(MemorySegment segment, long index) {

        int i1 = parseHex(SegmentAccess.getByte(segment, index)) << 12;
        int i2 = parseHex(SegmentAccess.getByte(segment, index + 1)) << 8;
        int i3 = parseHex(SegmentAccess.getByte(segment, index + 2)) << 4;
        int i4 = parseHex(SegmentAccess.getByte(segment, index + 3));
        return (char) (i1 | i2 | i3 | i4);
    }

    private static int parseHex(byte b) {
        if ((b >= '0' && b <= '9')) {
            return b - '0';
        } else if ((b >= 'a' && b <= 'f')) {
            return b - 'a' + 10;
        } else if ((b >= 'A' && b <= 'F')) {
            return b - 'A' + 10;
        } else {
            throw new JsonDeserializerException("illegal hex character: " + b);
        }
    }

    // TODO 拆表，和数字的做一起合并，一个表实现
    public static boolean validateJsonNonnullValueStart(byte firstByte) {
        return (firstByte >= (byte) '0' && firstByte <= (byte) '9')
                || firstByte == '-' || firstByte == ' ' || firstByte == 't' || firstByte == 'f' || firstByte == '"';
    }

    public static JsonDeserializeFunc builtinDeserializeObjFunc(Class<?> rawType) {

        return BUILTIN_DESERIALIZE_OBJ_FUNC_MAP.get(rawType);
    }

    public static JsonDeserializeFunc builtinDeserializeArrayFunc(Class<?> rawType) {

        return BUILTIN_DESERIALIZE_ARRAY_FUNC_MAP.get(rawType);
    }

    public JsonDeserializerOption option() {
        return option;
    }

    public ReadBuffer readBuffer() {
        return readBuffer;
    }

    public Object obj() {
        return obj;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }

    public Class<?> type() {
        final Class<?> r = type;
        type = null;
        return r;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public boolean asArray() {
        return asArray;
    }

    public void setAsArray(boolean asArray) {
        this.asArray = asArray;
    }

    public void ensureCharsCapacity(int capacity) {
        final char[] crs = chars;
        final int required = Math.addExact(capacity, charsIndex); // no overflow
        if (crs.length < required) {
            int newLen = 1 << (32 - Integer.numberOfLeadingZeros(required));
            if (newLen < 0) {
                throw new JsonSerializerException("char buffer overflow");
            }
            chars = Arrays.copyOf(crs, newLen);
        }
    }

    public void appendChar(char c) {

        ensureCharsCapacity(1);
        final char[] crs = chars;
        final int idx = charsIndex;
        crs[idx] = c;
        charsIndex = idx + 1;
    }

    public void appendChars(char c1, char c2) {

        ensureCharsCapacity(2);
        final char[] crs = chars;
        final int idx = charsIndex;
        crs[idx] = c1;
        crs[idx + 1] = c2;
        charsIndex = idx + 2;
    }

    public char asChar() {
        if (charsIndex != 1) {
            throw new JsonDeserializerException("not a single char");
        }
        charsIndex = 0;
        return chars[0];
    }

    public char[] asCharArray() {
        if (charsIndex == 0) {
            throw new JsonDeserializerException("no char value present");
        }
        char[] r = Arrays.copyOf(chars, charsIndex);
        charsIndex = 0;
        return r;
    }

    public String asString() {
        // the constructor already has an internal fast path for length 0, so no explicit check is needed.
        String r = new String(chars, 0, charsIndex);
        charsIndex = 0;
        return r;
    }

    public byte[] bytes() {
        return bytes;
    }

    public void setBytesIndex(int bytesIndex) {

        this.bytesIndex = bytesIndex;
    }

    public void ensureBytesCapacity(int capacity) {
        final int required = Math.addExact(capacity, bytesIndex);
        final byte[] bs = this.bytes;
        if (bs.length < required) {
            int newLen = 1 << (32 - Integer.numberOfLeadingZeros(required));
            if (newLen < 0) {
                throw new JsonSerializerException("byte buffer overflow");
            }
            bytes = Arrays.copyOf(bs, newLen);
        }
    }

    public void appendByte(byte b) {
        ensureBytesCapacity(1);
        bytes[bytesIndex++] = b;
    }

    public void appendShort(short s) {
        ensureBytesCapacity(2);
        ArrayAccess.setShort(bytes, bytesIndex, s);
        bytesIndex += 2;
    }

    public void appendInt(int i) {
        ensureBytesCapacity(4);
        ArrayAccess.setInt(bytes, bytesIndex, i);
        bytesIndex += 4;
    }

    public void appendLong(long l) {
        ensureBytesCapacity(8);
        ArrayAccess.setLong(bytes, bytesIndex, l);
        bytesIndex += 8;
    }

    public void appendFloat(float f) {
        ensureBytesCapacity(4);
        ArrayAccess.setFloat(bytes, bytesIndex, f);
        bytesIndex += 4;
    }

    public void appendDouble(double d) {
        ensureBytesCapacity(8);
        ArrayAccess.setDouble(bytes, bytesIndex, d);
        bytesIndex += 8;
    }

    public void appendNonSurr(char c) {

        if (c < 0x80) {
            ensureBytesCapacity(1);
            bytes[bytesIndex++] = (byte) c;
        } else if (c < 0x800) {
            ensureBytesCapacity(2);
            bytes[bytesIndex] = (byte) (0xC0 | (c >> 6));
            bytes[bytesIndex + 1] = (byte) (0x80 | (c & 0x3F));
            bytesIndex += 2;
        } else {
            ensureBytesCapacity(3);
            bytes[bytesIndex] = (byte) (0xE0 | (c >> 12));
            bytes[bytesIndex + 1] = (byte) (0x80 | ((c >> 6) & 0x3F));
            bytes[bytesIndex + 2] = (byte) (0x80 | (c & 0x3F));
            bytesIndex += 3;
        }
    }

    public void appendSurr(char high, char low) {

        ensureBytesCapacity(4);
        int cp = ((high - 0xD800) << 10) + (low - 0xDC00) + 0x10000;
        bytes[bytesIndex] = (byte) (0xF0 | (cp >> 18));
        bytes[bytesIndex + 1] = (byte) (0x80 | ((cp >> 12) & 0x3F));
        bytes[bytesIndex + 2] = (byte) (0x80 | ((cp >> 6) & 0x3F));
        bytes[bytesIndex + 3] = (byte) (0x80 | (cp & 0x3F));
        bytesIndex += 4;
    }

    public void appendBytes(byte[] data, int offset, int len) {

        ensureBytesCapacity(len);
        System.arraycopy(data, offset, bytes, bytesIndex, len);
        bytesIndex += len;
    }

    public void appendSegment(MemorySegment segment, long offset, int len) {

        ensureBytesCapacity(len);
        MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, offset, bytes, bytesIndex, len);
        bytesIndex += len;
    }

    public MarshallInfo asMarshallInfo(MarshallFacade fc) {
        if (bytesIndex == 0) {
            throw new JsonDeserializerException("key is empty");
        }
        MarshallInfo r = fc.marshallInfoByMappedName(bytes, 0, bytesIndex);
        bytesIndex = 0;
        return r;
    }

    public byte[] asByteArray() {
        if (bytesIndex == 0) {
            throw new JsonDeserializerException("no byte value present");
        }
        byte[] r = Arrays.copyOf(bytes, bytesIndex);
        bytesIndex = 0;
        return r;
    }

    public boolean[] asBooleanArray() {
        final int bIndex = bytesIndex;
        if (bIndex == 0) {
            throw new JsonDeserializerException("no boolean value present");
        }
        boolean[] r = new boolean[bIndex];
        for (int i = 0; i < bIndex; i++) {
            r[i] = bytes[i] > 0;
        }
        bytesIndex = 0;
        return r;
    }

    public short[] asShortArray() {
        final int bIndex = bytesIndex;
        if (bIndex == 0) {
            throw new JsonDeserializerException("no short value present");
        }

        int len = bIndex >> 1;
        short[] r = new short[len];
        MemorySegment.copy(MemorySegment.ofArray(bytes), ValueLayout.JAVA_SHORT_UNALIGNED, 0L, r, 0, len);
        bytesIndex = 0;
        return r;
    }

    public int[] asIntArray() {
        final int bIndex = bytesIndex;
        if (bIndex == 0) {
            throw new JsonDeserializerException("no int value present");
        }

        int len = bIndex >> 2;
        int[] r = new int[len];
        MemorySegment.copy(MemorySegment.ofArray(bytes), ValueLayout.JAVA_INT_UNALIGNED, 0L, r, 0, len);
        bytesIndex = 0;
        return r;
    }

    public long[] asLongArray() {
        final int bIndex = bytesIndex;
        if (bIndex == 0) {
            throw new JsonDeserializerException("no long value present");
        }

        int len = bIndex >> 3;
        long[] r = new long[len];
        MemorySegment.copy(MemorySegment.ofArray(bytes), ValueLayout.JAVA_LONG_UNALIGNED, 0L, r, 0, len);
        bytesIndex = 0;
        return r;
    }

    public float[] asFloatArray() {
        final int bIndex = bytesIndex;
        if (bIndex == 0) {
            throw new JsonDeserializerException("no float value present");
        }

        int len = bIndex >> 2;
        float[] r = new float[len];
        MemorySegment.copy(MemorySegment.ofArray(bytes), ValueLayout.JAVA_FLOAT_UNALIGNED, 0L, r, 0, len);
        bytesIndex = 0;
        return r;
    }

    public double[] asDoubleArray() {
        final int bIndex = bytesIndex;
        if (bIndex == 0) {
            throw new JsonDeserializerException("no double value present");
        }

        int len = bIndex >> 3;
        double[] r = new double[len];
        MemorySegment.copy(MemorySegment.ofArray(bytes), ValueLayout.JAVA_DOUBLE_UNALIGNED, 0L, r, 0, len);
        bytesIndex = 0;
        return r;
    }

    private void ensureBitmapCapacity(int capacity) {
        final int required = Math.addExact(capacity, bitmapIndex);
        if (bitmap.length < required) {
            final int newLen = 1 << (32 - Integer.numberOfLeadingZeros(required));
            if (newLen < 0) {
                throw new JsonSerializerException("bitmap buffer overflow");
            }
            bitmap = Arrays.copyOf(bitmap, newLen);
        }
    }

    public int bitmapIndex(int fieldCount) {
        // a class have at most 65535 fields in jvm
        final int requiredBytes = (fieldCount + 7) >> 3;
        ensureBitmapCapacity(requiredBytes);
        final int bIndex = bitmapIndex;
        bitmapIndex = bIndex + requiredBytes; // no overflow
        return bIndex;
    }

    public boolean assign(int bIndex, int fieldIndex) {

        final int byteOffset = bIndex + (fieldIndex >> 3);
        final int bitOffset = fieldIndex & 0x7;
        final byte mask = (byte) (1 << bitOffset);
        final byte[] bm = bitmap;
        final byte val = bm[byteOffset];
        bm[byteOffset] = (byte) (val | mask);
        return (val & mask) != 0;
    }

    public MarshallInfo filter(int bIndex, int fieldCount, boolean allPresent, MarshallFacade fc) {
        // a class have at most 65535 fields in jvm
        final byte[] bm = bitmap;
        for (int i = 0; i < fieldCount; i++) {
            final int byteOffset = bIndex + (i >> 3);
            final int bitOffset = i & 0x7;
            final byte mask = (byte) (1 << bitOffset);
            if ((bm[byteOffset] & mask) == 0) {
                MarshallInfo inf = fc.marshallInfoByIndex(i);
                if (allPresent || inf.rawType().isPrimitive()) {
                    return inf;
                }
            }
        }
        throw new AssertionError();
    }

    public void rewind(int bIndex) {

        Arrays.fill(bitmap, bIndex, bitmapIndex, (byte) 0);
        bitmapIndex = bIndex;
    }

    public byte nextFirstValuableByte() {
        final int maxEmptyBytes = option.maxEmptyBytes();
        switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> {
                final byte[] bytes = heapReadBuffer.rawByteArray();
                final int position = heapReadBuffer.intPosition();
                final int end = position + Math.min(bytes.length - position, maxEmptyBytes); // no overflow
                for (int i = position; i < end; i++) {
                    byte b = bytes[i];
                    switch (b) {
                        case (byte) ' ', (byte) '\n', (byte) '\r', (byte) '\t' -> {
                        }
                        default -> {
                            heapReadBuffer.setPosition(i + 1); // no overflow
                            return b;
                        }
                    }
                }
            }
            case SegmentReadBuffer segmentReadBuffer -> {
                final MemorySegment segment = segmentReadBuffer.rawSegment();
                final long position = segmentReadBuffer.longPosition();
                final long end = position + Math.min(segment.byteSize() - position, maxEmptyBytes); // no overflow
                for (long i = position; i < end; i++) {
                    byte b = SegmentAccess.getByte(segment, i);
                    switch (b) {
                        case (byte) ' ', (byte) '\n', (byte) '\r', (byte) '\t' -> {
                        }
                        default -> {
                            segmentReadBuffer.setPosition(i + 1L); // no overflow
                            return b;
                        }
                    }
                }
            }
        }
        throw new JsonDeserializerException("too many empty bytes");
    }

    public void deserializeNull(byte firstByte) {

        final ReadBuffer r = this.readBuffer;
        switch (r) {
            case HeapReadBuffer heapReadBuffer -> {
                final byte[] bytes = heapReadBuffer.rawByteArray();
                final int position = heapReadBuffer.intPosition();
                final int newPosition = Math.addExact(position, 3);
                if (newPosition > bytes.length) {
                    throw new JsonDeserializerException("eof reached while deserializing null");
                }
                if (ArrayAccess.getInt(bytes, position - 1) != COMPACT_NULL) {
                    throw new JsonDeserializerException("illegal null token, position : " + position);
                }
                r.setPosition(newPosition);
            }
            case SegmentReadBuffer segmentReadBuffer -> {
                final MemorySegment segment = segmentReadBuffer.rawSegment();
                final long position = segmentReadBuffer.longPosition();
                final long newPosition = Math.addExact(position, 3L);
                if (newPosition > segment.byteSize()) {
                    throw new JsonDeserializerException("eof reached while deserializing null");
                }
                if (SegmentAccess.getInt(segment, position - 1L) != COMPACT_NULL) {
                    throw new JsonDeserializerException("illegal null token, position : " + position);
                }
                r.setPosition(newPosition);
            }
        }
    }

    public byte deserializeByte(byte firstByte) {

        int v = JsonNumberUtil.readInt(readBuffer, firstByte);
        if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
            return (byte) v;
        }
        throw new JsonDeserializerException("byte value overflow : " + v);
    }

    public boolean deserializeBoolean(byte firstByte) {

        return switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> {
                final byte[] bytes = heapReadBuffer.rawByteArray();
                final int position = heapReadBuffer.intPosition();
                if (firstByte == 't') {
                    final int newPosition = Math.addExact(position, 3);
                    if (newPosition > bytes.length) {
                        throw new JsonDeserializerException("eof reached while deserializing boolean literal 'true' value");
                    }
                    if (ArrayAccess.getInt(bytes, position - 1) != COMPACT_TRUE) {
                        throw new JsonDeserializerException("illegal boolean literal 'true' value");
                    }
                    heapReadBuffer.setPosition(newPosition);
                    yield true;
                } else {
                    final int newPosition = Math.addExact(position, 4);
                    if (newPosition > bytes.length) {
                        throw new JsonDeserializerException("eof reached while deserializing boolean literal 'false' value");
                    }
                    if (ArrayAccess.getInt(bytes, position) != COMPACT_ALSE) {
                        throw new JsonDeserializerException("illegal boolean literal 'false' value");
                    }
                    heapReadBuffer.setPosition(newPosition);
                    yield false;
                }
            }
            case SegmentReadBuffer segmentReadBuffer -> {
                final MemorySegment segment = segmentReadBuffer.rawSegment();
                final long position = segmentReadBuffer.longPosition();
                if (firstByte == 't') {
                    final long newPosition = Math.addExact(position, 3L);
                    if (newPosition > segment.byteSize()) {
                        throw new JsonDeserializerException("eof reached while deserializing boolean literal 'true' value");
                    }
                    if (SegmentAccess.getInt(segment, position - 1L) != COMPACT_TRUE) {
                        throw new JsonDeserializerException("illegal boolean literal 'true' value");
                    }
                    segmentReadBuffer.setPosition(newPosition);
                    yield true;
                } else {
                    final long newPosition = Math.addExact(position, 4L);
                    if (newPosition > segment.byteSize()) {
                        throw new JsonDeserializerException("eof reached while deserializing boolean literal 'false' value");
                    }
                    if (SegmentAccess.getInt(segment, position) != COMPACT_ALSE) {
                        throw new JsonDeserializerException("illegal boolean literal 'false' value");
                    }
                    segmentReadBuffer.setPosition(newPosition);
                    yield false;
                }
            }
        };
    }

    public short deserializeShort(byte firstByte) {

        int v = JsonNumberUtil.readInt(readBuffer, firstByte);
        if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
            return (short) v;
        }
        throw new JsonDeserializerException("short value overflow : " + v);
    }

    public char deserializeChar(byte firstByte) {

        parseStringIntoChars(firstByte);
        return asChar();
    }

    public int deserializeInt(byte firstByte) {

        return JsonNumberUtil.readInt(readBuffer, firstByte);
    }

    public long deserializeLong(byte firstByte) {

        return JsonNumberUtil.readLong(readBuffer, firstByte);
    }

    public float deserializeFloat(byte firstByte) {

        return JsonNumberUtil.readFloat(readBuffer, option.maxNumberBytes(), firstByte);
    }

    public double deserializeDouble(byte firstByte) {

        return JsonNumberUtil.readDouble(readBuffer, option.maxNumberBytes(), firstByte);
    }

    public void parseStringIntoBytes(byte firstByte) {

        switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> parseHeapStringIntoBytes(heapReadBuffer);
            case SegmentReadBuffer segmentReadBuffer -> parseSegmentStringIntoBytes(segmentReadBuffer);
            case null, default -> throw new AssertionError();
        }
    }

    private int parseNonEscapedHeapStringIntoBytes(byte[] bytes, int position, int avail) {

        final byte[] buf = bytes();
        final int upper = BYTE_SPECIES.loopBound(Math.min(avail, buf.length));
        for (int i = 0; i < upper; i += BYTE_SPECIES.length(), position += BYTE_SPECIES.length()) {
            ByteVector byteVector = ByteVector.fromArray(BYTE_SPECIES, bytes, position);
            byteVector.intoArray(buf, i);
            long mask = byteVector.compare(VectorOperators.ULT, (byte) 0x20)
                    .or(byteVector.eq((byte) '\\'))
                    .or(byteVector.eq((byte) '"')).toLong();
            if (mask != 0L) {
                int range = Long.numberOfTrailingZeros(mask);
                setBytesIndex(i + range); // no overflow
                return position + range; // no overflow
            }
        }
        setBytesIndex(upper);
        return position;
    }

    private int parseEscapedHeapStringIntoBytes(byte[] bytes, int position, int end) {
        byte b = bytes[position++];
        switch (b) {
            case '\"' -> appendByte((byte) '\"');
            case '\\' -> appendByte((byte) '\\');
            case '/' -> appendByte((byte) '/');
            case 'b' -> appendByte((byte) '\b');
            case 'f' -> appendByte((byte) '\f');
            case 'n' -> appendByte((byte) '\n');
            case 'r' -> appendByte((byte) '\r');
            case 't' -> appendByte((byte) '\t');
            case 'u' -> {
                if (end - position < 4) {
                    throw new JsonDeserializerException("illegal unicode");
                }
                char c = parseUnicode(bytes, position);
                position += 4;
                if (c >= 0xDC00 && c <= 0xDFFF) {
                    throw new JsonDeserializerException("illegal low surrogate : " + c);
                }
                if (c >= 0xD800 && c <= 0xDBFF) {
                    if (end - position < 6 || bytes[position] != '\\' || bytes[position + 1] != 'u') {
                        throw new JsonDeserializerException("illegal high surrogate without low surrogate");
                    }
                    char c1 = parseUnicode(bytes, position + 2);
                    position += 6;
                    if (c1 < 0xDC00 || c1 > 0xDFFF) {
                        throw new JsonDeserializerException("illegal low surrogate : " + c1);
                    }
                    appendSurr(c, c1);
                } else {
                    appendNonSurr(c);
                }
            }
            default -> throw new JsonDeserializerException("illegal escaped byte : " + b);
        }
        return position;
    }

    private void parseHeapStringIntoBytes(HeapReadBuffer heapReadBuffer) {

        final byte[] bytes = heapReadBuffer.rawByteArray();
        final int position = heapReadBuffer.intPosition();
        final int avail = Math.min(bytes.length - position, option.maxStringBytes());
        if (avail == 0) {
            throw new JsonDeserializerException("no string avaliable");
        }
        if (bytes[position] == '"') {
            heapReadBuffer.setPosition(position + 1);
            return;
        }
        int p1 = parseNonEscapedHeapStringIntoBytes(bytes, position, avail);
        int p2 = p1;
        final int end = position + avail;
        while (p1 < end) {
            byte b = bytes[p1++];
            if (b == '\\') {
                int len = p1 - p2 - 1;
                if (len > 0) {
                    appendBytes(bytes, p2, len);
                }
                if (p1 == end) {
                    throw new JsonDeserializerException("illegal escape at end of string");
                }
                p1 = parseEscapedHeapStringIntoBytes(bytes, p1, end);
                p2 = p1;
            } else if (b == '"') {
                int len = p1 - p2 - 1;
                if (len > 0) {
                    appendBytes(bytes, p2, len);
                }
                heapReadBuffer.setPosition(p1);
                return;
            } else if (b >= (byte) 0 && b < (byte) 0x20) {
                throw new JsonDeserializerException("illegal unescaped ascii control byte : " + b);
            }
        }
        throw new JsonDeserializerException("illegal json string");
    }

    private void parseSegmentStringIntoBytes(SegmentReadBuffer segmentReadBuffer) {

        final MemorySegment segment = segmentReadBuffer.rawSegment();
        final long position = segmentReadBuffer.longPosition();
        final int avail = Math.min(Math.toIntExact(segment.byteSize() - position), option.maxStringBytes());
        if (avail == 0) {
            throw new JsonDeserializerException("no string avaliable");
        }
        if (SegmentAccess.getByte(segment, position) == '"') {
            segmentReadBuffer.setPosition(position + 1L);
            return;
        }
        long p1 = parseNonEscapedSegmentStringToBytes(segment, position, avail);
        long p2 = p1;
        final long end = position + avail; // no overflow
        while (p1 < end) {
            byte b = SegmentAccess.getByte(segment, p1++);
            if (b == '\\') {
                int len = Math.toIntExact(p1 - p2 - 1L);
                if (len > 0) {
                    appendSegment(segment, p2, len);
                }
                if (p1 == end) {
                    throw new JsonDeserializerException("illegal escape at end of string");
                }
                p1 = parseEscapedSegmentSequenceToBytes(segment, p1, end);
                p2 = p1;
            } else if (b == '"') {
                int len = Math.toIntExact(p1 - p2 - 1L);
                if (len > 0) {
                    appendSegment(segment, p2, len);
                }
                segmentReadBuffer.setPosition(p1);
                return;
            } else if (b >= (byte) 0 && b < (byte) 0x20) {
                throw new JsonDeserializerException("illegal unescaped ascii control byte : " + b);
            }
        }
        throw new JsonDeserializerException("illegal json string");
    }

    private long parseNonEscapedSegmentStringToBytes(MemorySegment segment, long position, int avail) {

        final byte[] buf = bytes();
        final int upper = BYTE_SPECIES.loopBound(Math.min(avail, buf.length));
        for (int i = 0; i < upper; i += BYTE_SPECIES.length(), position += BYTE_SPECIES.length()) {
            ByteVector byteVector = ByteVector.fromMemorySegment(BYTE_SPECIES, segment, position, ByteOrder.nativeOrder()); // byteOrder will be ignored
            byteVector.intoArray(buf, i);
            long mask = byteVector.compare(VectorOperators.ULT, (byte) 0x20)
                    .or(byteVector.eq((byte) '\\'))
                    .or(byteVector.eq((byte) '"')).toLong();
            if (mask != 0L) {
                int range = Long.numberOfTrailingZeros(mask);
                setBytesIndex(i + range); // no overflow
                return position + range; // no overflow
            }
        }
        setBytesIndex(upper);
        return position;
    }

    private long parseEscapedSegmentSequenceToBytes(MemorySegment segment, long position, long end) {
        byte b = SegmentAccess.getByte(segment, position++);
        switch (b) {
            case '\"' -> appendByte((byte) '\"');
            case '\\' -> appendByte((byte) '\\');
            case '/' -> appendByte((byte) '/');
            case 'b' -> appendByte((byte) '\b');
            case 'f' -> appendByte((byte) '\f');
            case 'n' -> appendByte((byte) '\n');
            case 'r' -> appendByte((byte) '\r');
            case 't' -> appendByte((byte) '\t');
            case 'u' -> {
                if (end - position < 4) {
                    throw new JsonDeserializerException("illegal unicode");
                }
                char c = parseUnicode(segment, position);
                position += 4;
                if (c >= 0xDC00 && c <= 0xDFFF) {
                    throw new JsonDeserializerException("illegal low surrogate : " + c);
                }
                if (c >= 0xD800 && c <= 0xDBFF) {
                    if (end - position < 6 || SegmentAccess.getByte(segment, position) != '\\' || SegmentAccess.getByte(segment, position + 1L) != 'u') {
                        throw new JsonDeserializerException("illegal high surrogate without low surrogate");
                    }
                    char c1 = parseUnicode(segment, position + 2L);
                    position += 6;
                    if (c1 < 0xDC00 || c1 > 0xDFFF) {
                        throw new JsonDeserializerException("illegal low surrogate : " + c1);
                    }
                    appendSurr(c, c1);
                } else {
                    appendNonSurr(c);
                }
            }
            default -> throw new JsonDeserializerException("illegal escaped byte : " + b);
        }
        return position;
    }

    public void parseStringIntoChars(byte firstByte) {

        switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> parseHeapStringIntoChars(heapReadBuffer);
            case SegmentReadBuffer segmentReadBuffer -> parseSegmentStringIntoChars(segmentReadBuffer);
            case null, default -> throw new AssertionError();
        }
    }

    private int parseNonEscapedHeapStringIntoChars(byte[] bytes, int position, int avail) {

        final char[] buf = chars;
        final int upper = BYTE_SPECIES.loopBound(Math.min(avail, buf.length));
        for (int i = 0; i < upper; i += BYTE_SPECIES.length(), position += BYTE_SPECIES.length()) {
            ByteVector byteVector = ByteVector.fromArray(BYTE_SPECIES, bytes, position);
            ShortVector part0 = (ShortVector) byteVector.convertShape(VectorOperators.B2S, SHORT_SPECIES, 0);
            ShortVector part1 = (ShortVector) byteVector.convertShape(VectorOperators.B2S, SHORT_SPECIES, 1);
            part0.intoCharArray(buf, i);
            part1.intoCharArray(buf, i + SHORT_SPECIES.length()); // no overflow
            long mask = byteVector.lt((byte) 0x20)
                    .or(byteVector.eq((byte) '\\'))
                    .or(byteVector.eq((byte) '"')).toLong();
            if (mask != 0L) {
                int range = Long.numberOfTrailingZeros(mask);
                charsIndex = i + range; // no overflow
                return position + range; // no overflow
            }
        }
        charsIndex = upper;
        return position;
    }

    private int parseEscapedHeapStringIntoChars(byte[] bytes, int position, int end) {
        byte b = bytes[position++];
        switch (b) {
            case '\"' -> appendChar('\"');
            case '\\' -> appendChar('\\');
            case '/' -> appendChar('/');
            case 'b' -> appendChar('\b');
            case 'f' -> appendChar('\f');
            case 'n' -> appendChar('\n');
            case 'r' -> appendChar('\r');
            case 't' -> appendChar('\t');
            case 'u' -> {
                if (end - position < 4) {
                    throw new JsonDeserializerException("illegal unicode");
                }
                char c = parseUnicode(bytes, position);
                position += 4;
                if (c >= 0xDC00 && c <= 0xDFFF) {
                    throw new JsonDeserializerException("illegal low surrogate : " + c);
                }
                if (c >= 0xD800 && c <= 0xDBFF) {
                    if (end - position < 6 || bytes[position] != '\\' || bytes[position + 1] != 'u') {
                        throw new JsonDeserializerException("illegal high surrogate without low surrogate");
                    }
                    char c1 = parseUnicode(bytes, position + 2);
                    position += 6;
                    if (c1 < 0xDC00 || c1 > 0xDFFF) {
                        throw new JsonDeserializerException("illegal low surrogate : " + c1);
                    }
                    appendChars(c, c1);
                } else {
                    appendChar(c);
                }
            }
            default -> throw new JsonDeserializerException("illegal escaped byte : " + b);
        }
        return position;
    }

    private void parseHeapStringIntoChars(HeapReadBuffer heapReadBuffer) {

        final byte[] bytes = heapReadBuffer.rawByteArray();
        final int position = heapReadBuffer.intPosition();
        final int avail = Math.min(bytes.length - position, option.maxStringBytes());
        if (avail == 0) {
            throw new JsonDeserializerException("no string avaliable");
        }
        if (bytes[position] == '"') {
            heapReadBuffer.setPosition(position + 1);
            return;
        }
        int index = parseNonEscapedHeapStringIntoChars(bytes, position, avail);
        final int end = position + avail;
        while (index < end) {
            char c = (char) (bytes[index++] & 0xFF);
            if (c == '\\') {
                index = parseEscapedHeapStringIntoChars(bytes, index, end);
            } else if (c == '"') {
                heapReadBuffer.setPosition(index);
                return;
            } else if (c < 0x20) {
                throw new JsonDeserializerException("illegal unescaped ascii control byte : " + c);
            } else if (c < 0x80) {
                appendChar(c);
            } else if (c < 0xE0) {
                char c1 = (char) (bytes[index++] & 0xFF);
                appendChar((char) (((c & 0x1F) << 6) | (c1 & 0x3F)));
            } else if (c < 0xF0) {
                char c1 = (char) (bytes[index++] & 0xFF);
                char c2 = (char) (bytes[index++] & 0xFF);
                appendChar((char) (((c & 0x0F) << 12) | ((c1 & 0x3F) << 6) | (c2 & 0x3F)));
            } else {
                char c1 = (char) (bytes[index++] & 0xFF);
                char c2 = (char) (bytes[index++] & 0xFF);
                char c3 = (char) (bytes[index++] & 0xFF);
                char high = (char) (0xD800 | ((c & 0x07) << 8) | ((c1 & 0x3F) << 2) | ((c2 & 0x30) >>> 4));
                char low = (char) (0xDC00 | ((c2 & 0x0F) << 6) | (c3 & 0x3F));
                appendChars(high, low);
            }
        }
        throw new JsonDeserializerException("illegal json string");
    }

    private long parseNonEscapedSegmentStringIntoChars(MemorySegment segment, long position, int avail) {

        final char[] buf = chars;
        final int upper = BYTE_SPECIES.loopBound(Math.min(avail, buf.length));
        for (int i = 0; i < upper; i += BYTE_SPECIES.length(), position += BYTE_SPECIES.length()) {
            ByteVector byteVector = ByteVector.fromMemorySegment(BYTE_SPECIES, segment, position, ByteOrder.nativeOrder()); // byteOrder will be ignored
            ShortVector part0 = (ShortVector) byteVector.convertShape(VectorOperators.B2S, SHORT_SPECIES, 0);
            ShortVector part1 = (ShortVector) byteVector.convertShape(VectorOperators.B2S, SHORT_SPECIES, 1);
            part0.intoCharArray(buf, i);
            part1.intoCharArray(buf, i + SHORT_SPECIES.length()); // no overflow
            long mask = byteVector.lt((byte) 0x20)
                    .or(byteVector.eq((byte) '\\'))
                    .or(byteVector.eq((byte) '"')).toLong();
            if (mask != 0L) {
                int range = Long.numberOfTrailingZeros(mask);
                charsIndex = i + range; // no overflow
                return position + range; // no overflow
            }
        }
        charsIndex = upper;
        return position;
    }

    private long parseEscapedSegmentStringIntoChars(MemorySegment segment, long position, long end) {
        byte b = SegmentAccess.getByte(segment, position++);
        switch (b) {
            case '\"' -> appendChar('\"');
            case '\\' -> appendChar('\\');
            case '/' -> appendChar('/');
            case 'b' -> appendChar('\b');
            case 'f' -> appendChar('\f');
            case 'n' -> appendChar('\n');
            case 'r' -> appendChar('\r');
            case 't' -> appendChar('\t');
            case 'u' -> {
                if (end - position < 4) {
                    throw new JsonDeserializerException("illegal unicode");
                }
                char c = parseUnicode(segment, position);
                position += 4;
                if (c >= 0xDC00 && c <= 0xDFFF) {
                    throw new JsonDeserializerException("illegal low surrogate : " + c);
                }
                if (c >= 0xD800 && c <= 0xDBFF) {
                    if (end - position < 6L || SegmentAccess.getByte(segment, position) != '\\' || SegmentAccess.getByte(segment, position + 1L) != 'u') {
                        throw new JsonDeserializerException("illegal high surrogate without low surrogate");
                    }
                    char c1 = parseUnicode(segment, position + 2L);
                    position += 6;
                    if (c1 < 0xDC00 || c1 > 0xDFFF) {
                        throw new JsonDeserializerException("illegal low surrogate : " + c1);
                    }
                    appendChars(c, c1);
                } else {
                    appendChar(c);
                }
            }
            default -> throw new JsonDeserializerException("illegal escaped byte : " + b);
        }
        return position;
    }

    private void parseSegmentStringIntoChars(SegmentReadBuffer segmentReadBuffer) {

        final MemorySegment segment = segmentReadBuffer.rawSegment();
        final long position = segmentReadBuffer.longPosition();
        final int avail = Math.min(Math.toIntExact(segment.byteSize() - position), option.maxStringBytes());
        if (avail == 0) {
            throw new JsonDeserializerException("no string avaliable");
        }
        if (SegmentAccess.getByte(segment, position) == '"') {
            segmentReadBuffer.setPosition(position + 1L);
            return;
        }
        long index = parseNonEscapedSegmentStringIntoChars(segment, position, avail);
        final long end = position + avail;
        while (index < end) {
            char c = (char) (SegmentAccess.getByte(segment, index++) & 0xFF);
            if (c == '\\') {
                index = parseEscapedSegmentStringIntoChars(segment, index, end);
            } else if (c == '"') {
                segmentReadBuffer.setPosition(index);
                return;
            } else if (c < 0x20) {
                throw new JsonDeserializerException("illegal unescaped ascii control byte : " + c);
            } else if (c < 0x80) {
                appendChar(c);
            } else if (c < 0xE0) {
                char c1 = (char) (SegmentAccess.getByte(segment, index++) & 0xFF);
                appendChar((char) (((c & 0x1F) << 6) | (c1 & 0x3F)));
            } else if (c < 0xF0) {
                char c1 = (char) (SegmentAccess.getByte(segment, index++) & 0xFF);
                char c2 = (char) (SegmentAccess.getByte(segment, index++) & 0xFF);
                appendChar((char) (((c & 0x0F) << 12) | ((c1 & 0x3F) << 6) | (c2 & 0x3F)));
            } else {
                char c1 = (char) (SegmentAccess.getByte(segment, index++) & 0xFF);
                char c2 = (char) (SegmentAccess.getByte(segment, index++) & 0xFF);
                char c3 = (char) (SegmentAccess.getByte(segment, index++) & 0xFF);
                char high = (char) (0xD800 | ((c & 0x07) << 8) | ((c1 & 0x3F) << 2) | ((c2 & 0x30) >>> 4));
                char low = (char) (0xDC00 | ((c2 & 0x0F) << 6) | (c3 & 0x3F));
                appendChars(high, low);
            }
        }
        throw new JsonDeserializerException("illegal json string");
    }

    public byte[] deserializeByteArray(byte firstByte) {

        byte b = nextFirstValuableByte();
        if (b == (byte) ']') {
            return Utils.emptyByteArray();
        }
        int maxArrayElements = option.maxArrayElements();
        for (int index = 0; index < maxArrayElements; index++) {
            if (!JsonNumberUtil.validateNumberStart(b)) {
                throw new JsonDeserializerException("not a number start : " + b);
            }
            byte v = deserializeByte(b);
            appendByte(v);
            b = nextFirstValuableByte();
            if (b == (byte) ']') {
                return asByteArray();
            } else if (b == (byte) ',') {
                b = nextFirstValuableByte();
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + maxArrayElements);
    }

    public boolean[] deserializeBooleanArray(byte firstByte) {

        byte b = nextFirstValuableByte();
        if (b == (byte) ']') {
            return Utils.emptyBooleanArray();
        }
        final int maxArrayElements = option.maxArrayElements();
        for (int index = 0; index < maxArrayElements; index++) {
            if (b != (byte) 't' && b != (byte) 'f') {
                throw new JsonDeserializerException("not a bool start : " + b);
            }
            boolean v = deserializeBoolean(b);
            appendByte(v ? Byte.MAX_VALUE : Byte.MIN_VALUE);
            b = nextFirstValuableByte();
            if (b == (byte) ']') {
                return asBooleanArray();
            } else if (b == (byte) ',') {
                b = nextFirstValuableByte();
            } else {
                throw new JsonDeserializerException("array separator expected, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + maxArrayElements);
    }

    public short[] deserializeShortArray(byte firstByte) {

        byte b = nextFirstValuableByte();
        if (b == (byte) ']') {
            return Utils.emptyShortArray();
        }
        final int maxArrayElements = option.maxArrayElements();
        for (int index = 0; index < maxArrayElements; index++) {
            if (!JsonNumberUtil.validateNumberStart(b)) {
                throw new JsonDeserializerException("not a number start : " + b);
            }
            short v = deserializeShort(b);
            appendShort(v);
            b = nextFirstValuableByte();
            if (b == (byte) ']') {
                return asShortArray();
            } else if (b == (byte) ',') {
                b = nextFirstValuableByte();
            } else {
                throw new JsonDeserializerException("array separator expected, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + maxArrayElements);
    }

    public char[] deserializeCharArray(byte firstByte) {

        byte b = nextFirstValuableByte();
        if (b == (byte) ']') {
            return Utils.emptyCharArray();
        }
        final int maxArrayElements = option.maxArrayElements();
        for (int index = 0; index < maxArrayElements; index++) {
            if (b != (byte) '"') {
                throw new JsonDeserializerException("not a string start : " + b);
            }
            char v = deserializeChar(b);
            appendChar(v);
            b = nextFirstValuableByte();
            if (b == (byte) ']') {
                return asCharArray();
            } else if (b == (byte) ',') {
                b = nextFirstValuableByte();
            } else {
                throw new JsonDeserializerException("array separator expected, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + maxArrayElements);
    }

    public int[] deserializeIntArray(byte firstByte) {

        byte b = nextFirstValuableByte();
        if (b == (byte) ']') {
            return Utils.emptyIntArray();
        }
        final int maxArrayElements = option.maxArrayElements();
        for (int index = 0; index < maxArrayElements; index++) {
            if (!JsonNumberUtil.validateNumberStart(b)) {
                throw new JsonDeserializerException("not a number start : " + b);
            }
            int v = deserializeInt(b);
            appendInt(v);
            b = nextFirstValuableByte();
            if (b == (byte) ']') {
                return asIntArray();
            } else if (b == (byte) ',') {
                b = nextFirstValuableByte();
            } else {
                throw new JsonDeserializerException("array separator expected, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + maxArrayElements);
    }

    public long[] deserializeLongArray(byte firstByte) {

        byte b = nextFirstValuableByte();
        if (b == (byte) ']') {
            return Utils.emptyLongArray();
        }
        final int maxArrayElements = option.maxArrayElements();
        for (int index = 0; index < maxArrayElements; index++) {
            if (!JsonNumberUtil.validateNumberStart(b)) {
                throw new JsonDeserializerException("not a number start : " + b);
            }
            long v = deserializeLong(b);
            appendLong(v);
            b = nextFirstValuableByte();
            if (b == (byte) ']') {
                return asLongArray();
            } else if (b == (byte) ',') {
                b = nextFirstValuableByte();
            } else {
                throw new JsonDeserializerException("array separator expected, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + maxArrayElements);
    }

    public float[] deserializeFloatArray(byte firstByte) {

        byte b = nextFirstValuableByte();
        if (b == (byte) ']') {
            return Utils.emptyFloatArray();
        }
        final int maxArrayElements = option.maxArrayElements();
        for (int index = 0; index < maxArrayElements; index++) {
            if (!JsonNumberUtil.validateNumberStart(b)) {
                throw new JsonDeserializerException("not a number start : " + b);
            }
            float v = deserializeFloat(b);
            appendFloat(v);
            b = nextFirstValuableByte();
            if (b == (byte) ']') {
                return asFloatArray();
            } else if (b == (byte) ',') {
                b = nextFirstValuableByte();
            } else {
                throw new JsonDeserializerException("array separator expected, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + maxArrayElements);
    }

    public double[] deserializeDoubleArray(byte firstByte) {

        byte b = nextFirstValuableByte();
        if (b == (byte) ']') {
            return Utils.emptyDoubleArray();
        }
        final int maxArrayElements = option.maxArrayElements();
        for (int index = 0; index < maxArrayElements; index++) {
            if (!JsonNumberUtil.validateNumberStart(b)) {
                throw new JsonDeserializerException("not a number start : " + b);
            }
            double v = deserializeDouble(b);
            appendDouble(v);
            b = nextFirstValuableByte();
            if (b == (byte) ']') {
                return asDoubleArray();
            } else if (b == (byte) ',') {
                b = nextFirstValuableByte();
            } else {
                throw new JsonDeserializerException("array separator expected, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + maxArrayElements);
    }

    @SuppressWarnings("unchecked")
    private <T> T[] deserializeObjectArray(byte firstByte, Class<T> componentType, ObjectDeserializer<T> deserializer) {

        byte b = nextFirstValuableByte();
        if (b == ']') {
            return (T[]) Utils.emptyObjectArray();
        }
        final Object[] arr = objArr;
        T[] r = (T[]) arr;
        if (r == null) {
            r = (T[]) Array.newInstance(componentType, OBJ_ARR_INITIAL_SIZE);
        }
        final int maxArrayElements = option.maxArrayElements();
        for (int index = 0; index < maxArrayElements; ) {
            T v = null;
            if (b == (byte) 'n') {
                deserializeNull(b);
            } else {
                v = deserializer.deserialize(b);
            }
            if (index == r.length) {
                int newLength = Math.min(r.length << 1, maxArrayElements); // no overflow
                r = Arrays.copyOf(r, newLength);
            }
            r[index++] = v;
            b = nextFirstValuableByte();
            if (b == ']') {
                if (r != arr) {
                    objArr = r;
                }
                return Arrays.copyOf(r, index);
            } else if (b == ',') {
                b = nextFirstValuableByte();
            } else {
                throw new JsonDeserializerException("array sep not found, got : " + b);
            }
        }
        throw new JsonDeserializerException("too many array elements, limit : " + maxArrayElements);
    }

    public Byte[] deserializeByteWrapperArray(byte firstByte) {

        return deserializeObjectArray(firstByte, Byte.class, b -> {
            if (!JsonNumberUtil.validateNumberStart(b)) {
                throw new JsonDeserializerException("not a number start : " + b);
            }
            return deserializeByte(b);
        });
    }

    public Boolean[] deserializeBooleanWrapperArray(byte firstByte) {

        return deserializeObjectArray(firstByte, Boolean.class, b -> {
            if (b != (byte) 't' && b != (byte) 'f') {
                throw new JsonDeserializerException("not a bool start : " + b);
            }
            return deserializeBoolean(b);
        });
    }

    public Short[] deserializeShortWrapperArray(byte firstByte) {

        return deserializeObjectArray(firstByte, Short.class, b -> {
            if (!JsonNumberUtil.validateNumberStart(b)) {
                throw new JsonDeserializerException("not a number start : " + b);
            }
            return deserializeShort(b);
        });
    }

    public Character[] deserializeCharWrapperArray(byte firstByte) {

        return deserializeObjectArray(firstByte, Character.class, b -> {
            if (b != (byte) '"') {
                throw new JsonDeserializerException("not a string start : " + b);
            }
            return deserializeChar(b);
        });
    }

    public Integer[] deserializeIntWrapperArray(byte firstByte) {

        return deserializeObjectArray(firstByte, Integer.class, b -> {
            if (!JsonNumberUtil.validateNumberStart(b)) {
                throw new JsonDeserializerException("not a number start : " + b);
            }
            return deserializeInt(b);
        });
    }

    public Long[] deserializeLongWrapperArray(byte firstByte) {

        return deserializeObjectArray(firstByte, Long.class, b -> {
            if (!JsonNumberUtil.validateNumberStart(b)) {
                throw new JsonDeserializerException("not a number start : " + b);
            }
            return deserializeLong(b);
        });
    }

    public Float[] deserializeFloatWrapperArray(byte firstByte) {

        return deserializeObjectArray(firstByte, Float.class, b -> {
            if (!JsonNumberUtil.validateNumberStart(b)) {
                throw new JsonDeserializerException("not a number start : " + b);
            }
            return deserializeFloat(b);
        });
    }

    public Double[] deserializeDoubleWrapperArray(byte firstByte) {

        return deserializeObjectArray(firstByte, Double.class, b -> {
            if (!JsonNumberUtil.validateNumberStart(b)) {
                throw new JsonDeserializerException("not a number start : " + b);
            }
            return deserializeDouble(b);
        });
    }

    public String deserializeString(byte firstByte) {

        parseStringIntoChars(firstByte);
        return asString();
    }

    public String[] deserializeStringArray(byte firstByte) {

        return deserializeObjectArray(firstByte, String.class, b -> {
            if (b != (byte) '"') {
                throw new JsonDeserializerException("not a string start : " + b);
            }
            return deserializeString(b);
        });
    }

    public JsonPrimitiveType deserializeJsonPrimitiveType(byte firstByte) {

        if (firstByte == (byte) 't' || firstByte == 'f') {
            return deserializeJsonBoolType(firstByte);
        } else if (firstByte == '"') {
            return new JsonStrType(deserializeString(firstByte));
        } else {
            return deserializeJsonNumberType(firstByte);
        }
    }

    public JsonPrimitiveType[] deserializeJsonPrimitiveTypeArray(byte firstByte) {

        return deserializeObjectArray(firstByte, JsonPrimitiveType.class, b -> {
            if (!validateJsonNonnullValueStart(b)) {
                throw new JsonDeserializerException("not a value start : " + b);
            }
            return deserializeJsonPrimitiveType(b);
        });
    }

    public JsonBoolType deserializeJsonBoolType(byte firstByte) {

        return new JsonBoolType(deserializeBoolean(firstByte));
    }

    public JsonBoolType[] deserializeJsonBoolTypeArray(byte firstByte) {

        return deserializeObjectArray(firstByte, JsonBoolType.class, b -> {
            if (b != (byte) 't' && b != (byte) 'f') {
                throw new JsonDeserializerException("not a bool start : " + b);
            }
            return deserializeJsonBoolType(b);
        });
    }

    public JsonNumberType deserializeJsonNumberType(byte firstByte) {

        final ReadBuffer r = this.readBuffer;
        FpStrRep rep = JsonNumberUtil.parseFpStrRep(r, option.maxNumberBytes(), firstByte);
        return switch (r) {
            case HeapReadBuffer heapReadBuffer -> {
                final byte[] bytes = heapReadBuffer.rawByteArray();
                final int position = heapReadBuffer.intPosition();
                yield new JsonNumberType(Arrays.copyOfRange(bytes, position - 1, position + rep.len())); // no overflow
            }
            case SegmentReadBuffer segmentReadBuffer -> {
                final MemorySegment segment = segmentReadBuffer.rawSegment();
                final long position = segmentReadBuffer.longPosition();
                yield new JsonNumberType(segment.asSlice(position - 1L, rep.len() + 1L).toArray(ValueLayout.JAVA_BYTE)); // overflow guarded
            }
        };
    }

    public JsonNumberType[] deserializeJsonNumberTypeArray(byte firstByte) {

        return deserializeObjectArray(firstByte, JsonNumberType.class, b -> {
            if (!JsonNumberUtil.validateNumberStart(b)) {
                throw new JsonDeserializerException("not a number start : " + b);
            }
            return deserializeJsonNumberType(b);
        });
    }

    public JsonStrType deserializeJsonStrType(byte firstByte) {

        parseStringIntoChars(firstByte);
        return new JsonStrType(asString());
    }

    public JsonStrType[] deserializeJsonStrTypeArray(byte firstByte) {

        return deserializeObjectArray(firstByte, JsonStrType.class, b -> {
            if (b != (byte) '"') {
                throw new JsonDeserializerException("not a string start : " + b);
            }
            return deserializeJsonStrType(b);
        });
    }

    public byte skipColon() {
        final int maxEmptyBytes = option.maxEmptyBytes();
        switch (readBuffer) {
            case HeapReadBuffer heapReadBuffer -> {
                final byte[] bytes = heapReadBuffer.rawByteArray();
                final int position = heapReadBuffer.intPosition();
                final int end = position + Math.min(bytes.length - position, maxEmptyBytes); // no overflow
                int i = position;
                for (; i < end; i++) {
                    byte b = bytes[i];
                    if (b == (byte) ':') {
                        i++;
                        break;
                    }
                }
                for (; i < end; i++) {
                    byte b = bytes[i];
                    switch (b) {
                        case (byte) ' ', (byte) '\n', (byte) '\r', (byte) '\t' -> {
                        }
                        default -> {
                            heapReadBuffer.setPosition(i + 1); // no overflow
                            return b;
                        }
                    }
                }
            }
            case SegmentReadBuffer segmentReadBuffer -> {
                final MemorySegment segment = segmentReadBuffer.rawSegment();
                final long position = segmentReadBuffer.longPosition();
                final long end = position + Math.min(segment.byteSize() - position, maxEmptyBytes); // no overflow
                long i = position;
                for (; i < end; i++) {
                    byte b = SegmentAccess.getByte(segment, i);
                    if (b == (byte) ':') {
                        i++;
                        break;
                    }
                }
                for (; i < end; i++) {
                    byte b = SegmentAccess.getByte(segment, i);
                    switch (b) {
                        case (byte) ' ', (byte) '\n', (byte) '\r', (byte) '\t' -> {
                        }
                        default -> {
                            segmentReadBuffer.setPosition(i + 1L); // no overflow
                            return b;
                        }
                    }
                }
            }
        }
        throw new JsonDeserializerException("colon not found");
    }

    public void skipNullValue(byte firstByte) {

        final ReadBuffer r = this.readBuffer;
        if (r.readByte() != (byte) 'u' || r.readByte() != (byte) 'l' || r.readByte() != (byte) 'l') {
            throw new JsonDeserializerException("skipped illegal null value");
        }
    }

    public void skipBoolValue(byte firstByte) {

        final ReadBuffer r = this.readBuffer;
        if (firstByte == (byte) 't') {
            if (r.readByte() != (byte) 'u' || r.readByte() != (byte) 'l' || r.readByte() != (byte) 'l') {
                throw new JsonDeserializerException("skipped illegal true value");
            }
        } else {
            if (r.readByte() != (byte) 'a' || r.readByte() != (byte) 'l' || r.readByte() != (byte) 's' || r.readByte() != (byte) 'e') {
                throw new JsonDeserializerException("skipped illegal false value");
            }
        }
    }

    public void skipNumberValue(byte firstByte) {

        final ReadBuffer r = this.readBuffer;
        FpStrRep rep = JsonNumberUtil.parseFpStrRep(r, option.maxNumberBytes(), firstByte);
        r.setPosition(r.intPosition() + rep.len()); // no overflow
    }

    public void skipStringValue(byte firstByte) {

        final ReadBuffer r = this.readBuffer;
        final int len = Math.min(option.maxStringBytes(), r.intLength() - r.intPosition());
        int i = 0;
        while (i++ < len) {
            byte b = r.readByte();
            if (b == (byte) '\\') {
                if (i++ == len) {
                    throw new JsonDeserializerException("illegal escape");
                }
                switch (r.readByte()) {
                    case '\"', '\\', '/', 'b', 'f', 'n', 'r', 't' -> {
                    }
                    case 'u' -> {
                        if (len - i < 4) {
                            throw new JsonDeserializerException("illegal unicode");
                        }
                        char c = parseUnicode(r);
                        i += 4;
                        if (c >= 0xDC00 && c <= 0xDFFF) {
                            throw new JsonDeserializerException("illegal low surrogate : " + c);
                        }
                        if (c >= 0xD800 && c <= 0xDBFF) {
                            if (len - i < 6 || r.readByte() != '\\' || r.readByte() != 'u') {
                                throw new JsonDeserializerException("illegal high surrogate without low surrogate");
                            }
                            char c1 = parseUnicode(r);
                            i += 6;
                            if (c1 < 0xDC00 || c1 > 0xDFFF) {
                                throw new JsonDeserializerException("illegal low surrogate : " + c1);
                            }
                        }
                    }
                    default -> throw new JsonDeserializerException("illegal escaped byte : " + b);
                }
            } else if (b == (byte) '"') {
                return;
            } else if (b < (byte) 0x20) {
                throw new JsonDeserializerException("illegal unescaped byte : " + b);
            }
        }
        throw new JsonDeserializerException("illegal json string");
    }

    public boolean skipAnyValue(byte firstByte) {

        if (firstByte == (byte) 'n') {
            skipNullValue(firstByte);
        } else if (firstByte == (byte) 't' || firstByte == (byte) 'f') {
            skipBoolValue(firstByte);
        } else if (firstByte == (byte) '"') {
            skipStringValue(firstByte);
        } else if (JsonNumberUtil.validateNumberStart(firstByte)) {
            skipNumberValue(firstByte);
        } else {
            return false;
        }
        return true;
    }

    public JsonDeserializeFunc valueDeserializeFunc(Class<?> rawType) {

        // builtin type has the highest priority
        if (rawType.isArray()) {
            JsonDeserializeFunc builtinDeserializeArrFunc = builtinDeserializeArrayFunc(rawType);
            if (builtinDeserializeArrFunc != null) {
                return builtinDeserializeArrFunc;
            }
            return (b, c) -> {
                if (b != (byte) '[') {
                    throw new JsonDeserializerException("array start not found, got : " + b);
                }
                return JsonDeserializeResult.NewList;
            };
        }
        // TODO
        return null;
    }

    @FunctionalInterface
    interface ObjectDeserializer<T> {
        T deserialize(byte firstByte);
    }
}
