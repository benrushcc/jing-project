package io.jingproject.marshallcbor;

import io.jingproject.common.ArrayAccess;
import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.common.SegmentAccess;
import io.jingproject.common.SegmentWriteBuffer;
import io.jingproject.common.WriteBuffer;
import io.jingproject.marshall.MarshallInfo;
import io.jingproject.marshall.Marshalls;
import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// sealed context with a heap and a segment implementation, mirroring the json serializer.
// the container layout and value semantics are shared here, while the subclasses only provide
// unchecked buffer writes and growth. every value is encoded directly into the caller's buffer,
// so no intermediate write buffer and no extra copy is required.
public sealed abstract class CborSerializerContext
        permits CborSerializerContext.CborSerializerHeapContext, CborSerializerContext.CborSerializerSegmentContext {
    protected static final int MAX_HEAD_BYTES = 9;
    private static final VectorSpecies<Short> SHORT_SPECIES;
    private static final VectorSpecies<Byte> BYTE_SPECIES;
    private static final Map<Class<?>, CborSerializeFunc> BUILTIN_SERIALIZE_OBJ_FUNC_MAP;
    private static final Map<Class<?>, CborSerializeFunc> BUILTIN_SERIALIZE_ARRAY_FUNC_MAP;

    static {
        int vectorBits = ShortVector.SPECIES_PREFERRED.vectorBitSize();
        switch (vectorBits) {
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
            default -> throw new UnsupportedOperationException("unsupported vector size : " + vectorBits);
        }
    }

    static {
        Map<Class<?>, CborSerializeFunc> r = new HashMap<>();
        r.put(Byte.class, (o, c) -> {
            c.serializeByte((Byte) o);
            return CborSerializeResult.Continue;
        });
        r.put(Boolean.class, (o, c) -> {
            c.serializeBoolean((Boolean) o);
            return CborSerializeResult.Continue;
        });
        r.put(Short.class, (o, c) -> {
            c.serializeShort((Short) o);
            return CborSerializeResult.Continue;
        });
        r.put(Character.class, (o, c) -> {
            c.serializeChar((Character) o);
            return CborSerializeResult.Continue;
        });
        r.put(Integer.class, (o, c) -> {
            c.serializeInt((Integer) o);
            return CborSerializeResult.Continue;
        });
        r.put(Long.class, (o, c) -> {
            c.serializeLong((Long) o);
            return CborSerializeResult.Continue;
        });
        r.put(Float.class, (o, c) -> {
            c.serializeFloat((Float) o);
            return CborSerializeResult.Continue;
        });
        r.put(Double.class, (o, c) -> {
            c.serializeDouble((Double) o);
            return CborSerializeResult.Continue;
        });
        r.put(CharSequence.class, (o, c) -> {
            c.serializeStr((CharSequence) o);
            return CborSerializeResult.Continue;
        });
        r.put(String.class, (o, c) -> {
            c.serializeStr((String) o);
            return CborSerializeResult.Continue;
        });
        r.put(CborValue.class, (o, c) -> {
            c.serializeCborValue((CborValue) o);
            return CborSerializeResult.Continue;
        });
        r.put(CborTaggedType.class, (o, c) -> {
            c.serializeCborTaggedType((CborTaggedType) o);
            return CborSerializeResult.Continue;
        });
        r.put(CborBytesType.class, (o, c) -> {
            c.serializeCborBytesType((CborBytesType) o);
            return CborSerializeResult.Continue;
        });
        BUILTIN_SERIALIZE_OBJ_FUNC_MAP = Map.copyOf(r);
    }

    static {
        Map<Class<?>, CborSerializeFunc> r = new HashMap<>();
        r.put(byte[].class, (o, c) -> {
            c.serializeBytes((byte[]) o);
            return CborSerializeResult.Continue;
        });
        r.put(boolean[].class, (o, c) -> {
            c.serializeBooleanArray((boolean[]) o);
            return CborSerializeResult.Continue;
        });
        r.put(short[].class, (o, c) -> {
            c.serializeShortArray((short[]) o);
            return CborSerializeResult.Continue;
        });
        r.put(char[].class, (o, c) -> {
            c.serializeCharArray((char[]) o);
            return CborSerializeResult.Continue;
        });
        r.put(int[].class, (o, c) -> {
            c.serializeIntArray((int[]) o);
            return CborSerializeResult.Continue;
        });
        r.put(long[].class, (o, c) -> {
            c.serializeLongArray((long[]) o);
            return CborSerializeResult.Continue;
        });
        r.put(float[].class, (o, c) -> {
            c.serializeFloatArray((float[]) o);
            return CborSerializeResult.Continue;
        });
        r.put(double[].class, (o, c) -> {
            c.serializeDoubleArray((double[]) o);
            return CborSerializeResult.Continue;
        });
        r.put(Byte[].class, (o, c) -> {
            c.serializeByteWrapperArray((Byte[]) o);
            return CborSerializeResult.Continue;
        });
        r.put(Boolean[].class, (o, c) -> {
            c.serializeBooleanWrapperArray((Boolean[]) o);
            return CborSerializeResult.Continue;
        });
        r.put(Short[].class, (o, c) -> {
            c.serializeShortWrapperArray((Short[]) o);
            return CborSerializeResult.Continue;
        });
        r.put(Character[].class, (o, c) -> {
            c.serializeCharWrapperArray((Character[]) o);
            return CborSerializeResult.Continue;
        });
        r.put(Integer[].class, (o, c) -> {
            c.serializeIntWrapperArray((Integer[]) o);
            return CborSerializeResult.Continue;
        });
        r.put(Long[].class, (o, c) -> {
            c.serializeLongWrapperArray((Long[]) o);
            return CborSerializeResult.Continue;
        });
        r.put(Float[].class, (o, c) -> {
            c.serializeFloatWrapperArray((Float[]) o);
            return CborSerializeResult.Continue;
        });
        r.put(Double[].class, (o, c) -> {
            c.serializeDoubleWrapperArray((Double[]) o);
            return CborSerializeResult.Continue;
        });
        r.put(CharSequence[].class, (o, c) -> {
            c.serializeStrArray((CharSequence[]) o);
            return CborSerializeResult.Continue;
        });
        r.put(String[].class, (o, c) -> {
            c.serializeStrArray((String[]) o);
            return CborSerializeResult.Continue;
        });

        BUILTIN_SERIALIZE_ARRAY_FUNC_MAP = Map.copyOf(r);
    }

    protected final CborSerializerOption option;
    protected Object obj;
    protected Class<?> type;
    protected byte[] utf8Scratch;
    protected char[] asciiScratch;

    protected CborSerializerContext(CborSerializerOption option) {
        this.option = option;
    }

    public static CborSerializerContext newCtx(CborSerializerOption option, WriteBuffer writeBuffer) {
        return switch (writeBuffer) {
            case HeapWriteBuffer heapWriteBuffer -> new CborSerializerHeapContext(option, heapWriteBuffer);
            case SegmentWriteBuffer segmentWriteBuffer -> new CborSerializerSegmentContext(option, segmentWriteBuffer);
        };
    }

    public final CborSerializerOption option() {
        return option;
    }

    public final Object obj() {
        return obj;
    }

    public final void setObj(Object obj) {
        this.obj = obj;
    }

    public final Class<?> type() {
        return type;
    }

    public final void setType(Class<?> type) {
        this.type = type;
    }

    // reserves space for the next writes; subclasses may grow the underlying buffer
    protected abstract void ensureCapacity(int requiredCapacity);

    // unchecked writes: the caller must have reserved enough space with ensureCapacity
    protected abstract void putByte(byte b);

    protected abstract void putShortBigEndian(short value);

    protected abstract void putIntBigEndian(int value);

    protected abstract void putLongBigEndian(long value);

    protected abstract void putBytes(byte[] bytes, int offset, int length);

    protected abstract long currentPosition();

    protected abstract void resetPosition(long position);

    protected abstract void moveBytes(long from, long to, int length);

    protected abstract void putAsciiVector(ShortVector vector, long dataStart, int offset);

    public abstract void commit();

    public final void append(byte b) {
        ensureCapacity(1);
        putByte(b);
    }

    public final void append(byte b1, byte b2) {
        ensureCapacity(2);
        putByte(b1);
        putByte(b2);
    }

    // writes a major-type head using the preferred (shortest) argument encoding
    public final void writeHead(int majorType, long argument) {
        final int head = (majorType << 5) & 0xE0;
        if (Long.compareUnsigned(argument, 24L) < 0) {
            ensureCapacity(1);
            putByte((byte) (head | (int) argument));
        } else if (Long.compareUnsigned(argument, 0x100L) < 0) {
            ensureCapacity(2);
            putByte((byte) (head | 24));
            putByte((byte) argument);
        } else if (Long.compareUnsigned(argument, 0x10000L) < 0) {
            ensureCapacity(3);
            putByte((byte) (head | 25));
            putShortBigEndian((short) argument);
        } else if (Long.compareUnsigned(argument, 0x100000000L) < 0) {
            ensureCapacity(5);
            putByte((byte) (head | 26));
            putIntBigEndian((int) argument);
        } else {
            ensureCapacity(MAX_HEAD_BYTES);
            putByte((byte) (head | 27));
            putLongBigEndian(argument);
        }
    }

    // null is the simple value 0xf6
    public final void serializeNull() {
        append((byte) 0xF6);
    }

    // true is 0xf5, false is 0xf4
    public final void serializeBoolean(boolean value) {
        append(value ? (byte) 0xF5 : (byte) 0xF4);
    }

    public final void serializeByte(byte value) {
        serializeInt(value);
    }

    public final void serializeShort(short value) {
        serializeInt(value);
    }

    public final void serializeInt(int value) {
        if (value >= 0) {
            writeHead(CborNumberUtil.TYPE_UNSIGNED, value);
        } else {
            writeHead(CborNumberUtil.TYPE_NEGATIVE, -1L - value);
        }
    }

    public final void serializeLong(long value) {
        if (value >= 0L) {
            writeHead(CborNumberUtil.TYPE_UNSIGNED, value);
        } else {
            writeHead(CborNumberUtil.TYPE_NEGATIVE, -1L - value);
        }
    }

    // float is encoded as 0xfa plus a big-endian float32 payload
    public final void serializeFloat(float value) {
        ensureCapacity(5);
        putByte((byte) 0xFA);
        putIntBigEndian(Float.floatToRawIntBits(value));
    }

    // double is encoded as 0xfb plus a big-endian float64 payload
    public final void serializeDouble(double value) {
        ensureCapacity(9);
        putByte((byte) 0xFB);
        putLongBigEndian(Double.doubleToRawLongBits(value));
    }

    // a char is a text string holding exactly one unicode scalar value
    public final void serializeChar(char value) {
        if (value >= 0xD800 && value <= 0xDFFF) {
            throw new CborSerializerException("surrogate not supported as single char : " + (int) value);
        }
        if (value < 0x80) {
            writeHead(CborNumberUtil.TYPE_TEXT, 1L);
            append((byte) value);
        } else if (value < 0x800) {
            writeHead(CborNumberUtil.TYPE_TEXT, 2L);
            append((byte) (0xC0 | (value >> 6)), (byte) (0x80 | (value & 0x3F)));
        } else {
            writeHead(CborNumberUtil.TYPE_TEXT, 3L);
            ensureCapacity(3);
            putByte((byte) (0xE0 | (value >> 12)));
            putByte((byte) (0x80 | ((value >> 6) & 0x3F)));
            putByte((byte) (0x80 | (value & 0x3F)));
        }
    }

    // text strings are encoded as raw utf-8 bytes; no escaping is needed in cbor.
    // ascii is copied with vectors directly into the target, and the first non-ascii
    // lane switches to scalar encoding from that byte without discarding the prefix.
    public final void serializeStr(String str) {
        final int length = str.length();
        if (length == 0) {
            writeHead(CborNumberUtil.TYPE_TEXT, 0L);
            return;
        }
        final long start = currentPosition();
        final int initialHeadSize = headSize(length);
        ensureCapacity(Math.addExact(initialHeadSize, length));
        writeHead(CborNumberUtil.TYPE_TEXT, length);
        final long dataStart = currentPosition();
        final int byteLength = encodeUtf8FromPosition(str);
        if (byteLength == length) {
            return;
        }
        final long end = currentPosition();
        final int headDelta = headSize(byteLength) - initialHeadSize;
        if (headDelta == 0) {
            resetPosition(start);
            writeHead(CborNumberUtil.TYPE_TEXT, byteLength);
            resetPosition(end);
            return;
        }
        // move the already encoded bytes right, then prepend the wider head
        ensureCapacity(headDelta);
        moveBytes(dataStart, dataStart + headDelta, byteLength);
        resetPosition(start);
        writeHead(CborNumberUtil.TYPE_TEXT, byteLength);
        resetPosition(dataStart + headDelta + byteLength);
    }

    private static int headSize(int length) {
        if (length < 24) {
            return 1;
        }
        if (length < 0x100) {
            return 2;
        }
        if (length < 0x10000) {
            return 3;
        }
        if (Long.compareUnsigned(length, 0x100000000L) < 0) {
            return 5;
        }
        return 9;
    }

    // assumes the provisional text head for the char length was already written.
    // ascii vectors are copied eagerly; on the first non-ascii lane the scalar
    // encoder continues from that byte and overwrites the provisional vector tail.
    private int encodeUtf8FromPosition(String str) {
        final int length = str.length();
        final long dataStart = currentPosition();
        final int vectorLength = SHORT_SPECIES.length();
        if (length < vectorLength) {
            for (int i = 0; i < length; i++) {
                char c = str.charAt(i);
                if (c >= 0x80) {
                    return encodeUtf8ScalarFrom(str, i, length, dataStart);
                }
                putByte((byte) c);
            }
            return length;
        }
        if (str.charAt(0) >= 0x80) {
            return encodeUtf8ScalarFrom(str, 0, length, dataStart);
        }
        if (asciiScratch == null) {
            asciiScratch = new char[Math.max(length, 256)];
        } else if (asciiScratch.length < length) {
            asciiScratch = new char[Math.max(length, asciiScratch.length + (asciiScratch.length >> 1))];
        }
        final char[] buf = asciiScratch;
        str.getChars(0, length, buf, 0);
        int index = 0;
        for (; index <= length - vectorLength; index += vectorLength) {
            ShortVector shortVector = ShortVector.fromCharArray(SHORT_SPECIES, buf, index);
            putAsciiVector(shortVector, dataStart, index);
            VectorMask<Short> nonAscii = shortVector.lanewise(VectorOperators.AND, (short) 0xFF80)
                    .compare(VectorOperators.NE, (short) 0);
            if (nonAscii.anyTrue()) {
                return encodeUtf8ScalarFrom(str, index + nonAscii.firstTrue(), length, dataStart);
            }
        }
        resetPosition(dataStart + index);
        for (; index < length; index++) {
            char c = buf[index];
            if (c >= 0x80) {
                return encodeUtf8ScalarFrom(str, index, length, dataStart);
            }
            putByte((byte) c);
        }
        return length;
    }

    private int encodeUtf8ScalarFrom(String str, int index, int length, long dataStart) {
        final int remaining = length - index;
        final int required = Math.multiplyExact(remaining, 3);
        if (utf8Scratch == null || utf8Scratch.length < required) {
            utf8Scratch = new byte[Math.max(required, utf8Scratch == null ? 256 : utf8Scratch.length * 2)];
        }
        final byte[] buffer = utf8Scratch;
        int encodedLength = 0;
        for (int i = index; i < length; i++) {
            char c = str.charAt(i);
            if (c < 0x80) {
                buffer[encodedLength++] = (byte) c;
            } else if (c < 0x800) {
                buffer[encodedLength++] = (byte) (0xC0 | (c >> 6));
                buffer[encodedLength++] = (byte) (0x80 | (c & 0x3F));
            } else if (c >= 0xD800 && c <= 0xDBFF) {
                int nextIndex = i + 1;
                if (nextIndex == length || (str.charAt(nextIndex) & 0xFC00) != 0xDC00) {
                    throw new CborSerializerException("illegal high surrogate without low surrogate");
                }
                char low = str.charAt(nextIndex);
                int codePoint = ((c - 0xD800) << 10) + (low - 0xDC00) + 0x10000;
                buffer[encodedLength++] = (byte) (0xF0 | (codePoint >> 18));
                buffer[encodedLength++] = (byte) (0x80 | ((codePoint >> 12) & 0x3F));
                buffer[encodedLength++] = (byte) (0x80 | ((codePoint >> 6) & 0x3F));
                buffer[encodedLength++] = (byte) (0x80 | (codePoint & 0x3F));
                i = nextIndex;
            } else if (c >= 0xDC00 && c <= 0xDFFF) {
                throw new CborSerializerException("illegal low surrogate without high surrogate");
            } else {
                buffer[encodedLength++] = (byte) (0xE0 | (c >> 12));
                buffer[encodedLength++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                buffer[encodedLength++] = (byte) (0x80 | (c & 0x3F));
            }
        }
        resetPosition(dataStart + index);
        ensureCapacity(encodedLength);
        putBytes(buffer, 0, encodedLength);
        return index + encodedLength;
    }

    public final void serializeStr(CharSequence charSequence) {
        serializeStr(charSequence.toString());
    }

    // pre-encoded UTF-8 text (mapped names, enum mapped names) written directly
    public final void serializeUtf8BytesAsStr(byte[] utf8Bytes) {
        ensureCapacity(Math.addExact(MAX_HEAD_BYTES, utf8Bytes.length));
        writeHead(CborNumberUtil.TYPE_TEXT, utf8Bytes.length);
        putBytes(utf8Bytes, 0, utf8Bytes.length);
    }

    // a byte string (major type 2) with a length head and raw bytes
    public final void serializeBytes(byte[] bytes) {
        ensureCapacity(Math.addExact(MAX_HEAD_BYTES, bytes.length));
        writeHead(CborNumberUtil.TYPE_BYTES, bytes.length);
        putBytes(bytes, 0, bytes.length);
    }

    public final void writeArrayHead(long count) {
        writeHead(CborNumberUtil.TYPE_ARRAY, count);
    }

    public final void writeMapHead(long count) {
        writeHead(CborNumberUtil.TYPE_MAP, count);
    }

    // indefinite-length forms: 0x9f array start, 0xbf map start, 0xff break
    public final void writeIndefiniteArrayStart() {
        append((byte) 0x9F);
    }

    public final void writeIndefiniteMapStart() {
        append((byte) 0xBF);
    }

    public final void writeBreak() {
        append((byte) 0xFF);
    }

    public final void serializeBooleanArray(boolean[] arr) {
        writeArrayHead(arr.length);
        for (boolean value : arr) {
            serializeBoolean(value);
        }
    }

    public final void serializeShortArray(short[] arr) {
        writeArrayHead(arr.length);
        for (short value : arr) {
            serializeShort(value);
        }
    }

    public final void serializeCharArray(char[] arr) {
        writeArrayHead(arr.length);
        for (char value : arr) {
            serializeChar(value);
        }
    }

    public final void serializeIntArray(int[] arr) {
        writeArrayHead(arr.length);
        for (int value : arr) {
            serializeInt(value);
        }
    }

    public final void serializeLongArray(long[] arr) {
        writeArrayHead(arr.length);
        for (long value : arr) {
            serializeLong(value);
        }
    }

    public final void serializeFloatArray(float[] arr) {
        writeArrayHead(arr.length);
        for (float value : arr) {
            serializeFloat(value);
        }
    }

    public final void serializeDoubleArray(double[] arr) {
        writeArrayHead(arr.length);
        for (double value : arr) {
            serializeDouble(value);
        }
    }

    public final void serializeByteWrapperArray(Byte[] arr) {
        serializeObjArray(arr, CborSerializerContext::serializeByte);
    }

    public final void serializeBooleanWrapperArray(Boolean[] arr) {
        serializeObjArray(arr, CborSerializerContext::serializeBoolean);
    }

    public final void serializeShortWrapperArray(Short[] arr) {
        serializeObjArray(arr, CborSerializerContext::serializeShort);
    }

    public final void serializeCharWrapperArray(Character[] arr) {
        serializeObjArray(arr, CborSerializerContext::serializeChar);
    }

    public final void serializeIntWrapperArray(Integer[] arr) {
        serializeObjArray(arr, CborSerializerContext::serializeInt);
    }

    public final void serializeLongWrapperArray(Long[] arr) {
        serializeObjArray(arr, CborSerializerContext::serializeLong);
    }

    public final void serializeFloatWrapperArray(Float[] arr) {
        serializeObjArray(arr, CborSerializerContext::serializeFloat);
    }

    public final void serializeDoubleWrapperArray(Double[] arr) {
        serializeObjArray(arr, CborSerializerContext::serializeDouble);
    }

    public final void serializeStrArray(CharSequence[] arr) {
        serializeObjArray(arr, CborSerializerContext::serializeStr);
    }

    public final void serializeStrArray(String[] arr) {
        writeArrayHead(arr.length);
        for (String str : arr) {
            if (str == null) {
                serializeNull();
            } else {
                serializeStr(str);
            }
        }
    }

    public final void serializeEnum(Enum<?> enumValue) {
        MarshallInfo inf = Marshalls.enumItemMarshallInfo(enumValue);
        if (inf == null) {
            serializeStr(enumValue.name());
        } else {
            serializeUtf8BytesAsStr(inf.mappedNameUtf8Bytes());
        }
    }

    public final void serializeEnumArray(Enum<?>[] arr) {
        serializeObjArray(arr, CborSerializerContext::serializeEnum);
    }

    public final void serializeCborBytesType(CborBytesType cborBytesType) {
        serializeBytes(cborBytesType.data());
    }

    public final void serializeCborTaggedType(CborTaggedType taggedType) {
        serializeCborValue(taggedType);
    }

    // encodes a complete value tree without recursion: containers push their
    // children in reverse order so the explicit stack yields the wire order
    public final void serializeCborValue(CborValue value) {
        if (value == null) {
            throw new CborSerializerException("value must not be null");
        }
        Deque<CborValue> stack = new ArrayDeque<>();
        stack.push(value);
        while (!stack.isEmpty()) {
            CborValue current = stack.pop();
            switch (current) {
                case CborBool v -> serializeBoolean(v.value());
                case CborInteger v -> serializeLong(v.value());
                case CborFloat v -> serializeCborFloat(v);
                case CborText v -> serializeStr(v.value());
                case CborNull ignored -> serializeNull();
                case CborBytesType v -> serializeBytes(v.data());
                case CborArray v -> {
                    List<CborValue> elements = v.elements();
                    writeArrayHead(elements.size());
                    for (int i = elements.size() - 1; i >= 0; i--) {
                        stack.push(elements.get(i));
                    }
                }
                case CborMap v -> {
                    List<CborMapEntry> entries = v.entries();
                    writeMapHead(entries.size());
                    for (int i = entries.size() - 1; i >= 0; i--) {
                        CborMapEntry entry = entries.get(i);
                        stack.push(entry.value());
                        stack.push(entry.key());
                    }
                }
                case CborTaggedType v -> {
                    writeHead(CborNumberUtil.TYPE_TAG, v.tag());
                    stack.push(v.value());
                }
            }
        }
    }

    // CborFloat already carries the exact wire bit pattern for its width
    private void serializeCborFloat(CborFloat value) {
        switch (value.width()) {
            case HALF -> {
                ensureCapacity(3);
                putByte((byte) 0xF9);
                putShortBigEndian((short) value.bits());
            }
            case SINGLE -> {
                ensureCapacity(5);
                putByte((byte) 0xFA);
                putIntBigEndian((int) value.bits());
            }
            case DOUBLE -> {
                ensureCapacity(9);
                putByte((byte) 0xFB);
                putLongBigEndian(value.bits());
            }
        }
    }

    private <T> void serializeObjArray(T[] arr, ElementSerializer<T> elementSerializer) {
        writeArrayHead(arr.length);
        for (T element : arr) {
            if (element == null) {
                serializeNull();
            } else {
                elementSerializer.serialize(this, element);
            }
        }
    }

    public static CborSerializeFunc builtinSerializeObjFunc(Class<?> rawType) {
        return BUILTIN_SERIALIZE_OBJ_FUNC_MAP.get(rawType);
    }

    public static CborSerializeFunc builtinSerializeArrayFunc(Class<?> rawType) {
        return BUILTIN_SERIALIZE_ARRAY_FUNC_MAP.get(rawType);
    }

    // builtin type has the highest priority
    // then check if current type could be overridden by option
    // enum must be specially treated
    // finally assuming marshallable
    public final CborSerializeFunc valueSerializeFunc(Class<?> rawType) {
        if (rawType.isArray()) {
            CborSerializeFunc builtinSerializeArrFunc = builtinSerializeArrayFunc(rawType);
            if (builtinSerializeArrFunc != null) {
                return builtinSerializeArrFunc;
            }
            Class<?> componentType = rawType.componentType();
            if (componentType.isEnum()) {
                return (o, c) -> {
                    c.serializeEnumArray((Enum<?>[]) o);
                    return CborSerializeResult.Continue;
                };
            }
            return (o, c) -> {
                c.setObj(o);
                return CborSerializeResult.NewArray;
            };
        }
        CborSerializeFunc builtinSerializeFunc = builtinSerializeObjFunc(rawType);
        if (builtinSerializeFunc != null) {
            return builtinSerializeFunc;
        }
        CborSerializeFunc customFunc = option.customFunc(rawType);
        if (customFunc != null) {
            return customFunc;
        }
        if (rawType.isEnum()) {
            return (o, c) -> {
                c.serializeEnum((Enum<?>) o);
                return CborSerializeResult.Continue;
            };
        }
        return (o, c) -> {
            c.setObj(o);
            return CborSerializeResult.NewMarshallable;
        };
    }

    @FunctionalInterface
    interface ElementSerializer<T> {
        void serialize(CborSerializerContext c, T t);
    }

    public static final class CborSerializerHeapContext extends CborSerializerContext {
        private final HeapWriteBuffer writeBuffer;
        private byte[] bytes;
        private int position;

        public CborSerializerHeapContext(CborSerializerOption option, HeapWriteBuffer writeBuffer) {
            super(option);
            this.writeBuffer = writeBuffer;
            this.bytes = writeBuffer.rawByteArray();
            this.position = writeBuffer.intPosition();
        }

        @Override
        protected void ensureCapacity(int requiredCapacity) {
            int nextPosition = Math.addExact(position, requiredCapacity);
            if (nextPosition > bytes.length) {
                int newLength = Math.max(Math.addExact(bytes.length, bytes.length), nextPosition);
                int limit = writeBuffer.rawLimit();
                if (newLength > limit) {
                    throw new CborSerializerException("reaching heap buffer limit : " + limit);
                }
                byte[] newBytes = new byte[newLength];
                System.arraycopy(bytes, 0, newBytes, 0, position);
                bytes = newBytes;
            }
        }

        @Override
        protected void putByte(byte b) {
            bytes[position++] = b;
        }

        @Override
        protected void putShortBigEndian(short value) {
            ArrayAccess.setShort(bytes, position, value, ByteOrder.BIG_ENDIAN);
            position += 2;
        }

        @Override
        protected void putIntBigEndian(int value) {
            ArrayAccess.setInt(bytes, position, value, ByteOrder.BIG_ENDIAN);
            position += 4;
        }

        @Override
        protected void putLongBigEndian(long value) {
            ArrayAccess.setLong(bytes, position, value, ByteOrder.BIG_ENDIAN);
            position += 8;
        }

        @Override
        protected void putBytes(byte[] src, int offset, int length) {
            System.arraycopy(src, offset, bytes, position, length);
            position += length;
        }

        @Override
        protected long currentPosition() {
            return position;
        }

        @Override
        protected void resetPosition(long position) {
            this.position = (int) position;
        }

        @Override
        protected void moveBytes(long from, long to, int length) {
            System.arraycopy(bytes, (int) from, bytes, (int) to, length);
        }

        @Override
        protected void putAsciiVector(ShortVector vector, long dataStart, int offset) {
            ByteVector byteVector = (ByteVector) vector.convertShape(
                    VectorOperators.S2B, BYTE_SPECIES, 0);
            byteVector.intoArray(bytes, (int) dataStart + offset);
        }

        @Override
        public void commit() {
            writeBuffer.setBufferAndPosition(bytes, position);
        }
    }

    public static final class CborSerializerSegmentContext extends CborSerializerContext {
        private final SegmentWriteBuffer writeBuffer;
        private MemorySegment segment;
        private long position;

        public CborSerializerSegmentContext(CborSerializerOption option, SegmentWriteBuffer writeBuffer) {
            super(option);
            this.writeBuffer = writeBuffer;
            this.segment = writeBuffer.rawSegment();
            this.position = writeBuffer.longPosition();
        }

        @Override
        protected void ensureCapacity(int requiredCapacity) {
            long nextPosition = Math.addExact(position, requiredCapacity);
            long segmentByteSize = segment.byteSize();
            if (nextPosition > segmentByteSize) {
                long newLength = Math.max(Math.addExact(segmentByteSize, segmentByteSize), nextPosition);
                long limit = writeBuffer.rawLimit();
                if (newLength > limit) {
                    throw new CborSerializerException("reaching segment buffer limit : " + limit);
                }
                MemorySegment newSegment = writeBuffer.rawAlloc().allocate(newLength);
                MemorySegment.copy(segment, 0L, newSegment, 0L, position);
                segment = newSegment;
            }
        }

        @Override
        protected void putByte(byte b) {
            SegmentAccess.setByte(segment, position++, b);
        }

        @Override
        protected void putShortBigEndian(short value) {
            SegmentAccess.setShort(segment, position, value, ByteOrder.BIG_ENDIAN);
            position += 2L;
        }

        @Override
        protected void putIntBigEndian(int value) {
            SegmentAccess.setInt(segment, position, value, ByteOrder.BIG_ENDIAN);
            position += 4L;
        }

        @Override
        protected void putLongBigEndian(long value) {
            SegmentAccess.setLong(segment, position, value, ByteOrder.BIG_ENDIAN);
            position += 8L;
        }

        @Override
        protected void putBytes(byte[] src, int offset, int length) {
            MemorySegment.copy(src, offset, segment, ValueLayout.JAVA_BYTE, position, length);
            position += length;
        }

        @Override
        protected long currentPosition() {
            return position;
        }

        @Override
        protected void resetPosition(long position) {
            this.position = position;
        }

        @Override
        protected void moveBytes(long from, long to, int length) {
            for (int i = length - 1; i >= 0; i--) {
                SegmentAccess.setByte(segment, to + i, SegmentAccess.getByte(segment, from + i));
            }
        }

        @Override
        protected void putAsciiVector(ShortVector vector, long dataStart, int offset) {
            ByteVector byteVector = (ByteVector) vector.convertShape(
                    VectorOperators.S2B, BYTE_SPECIES, 0);
            byteVector.intoMemorySegment(segment, dataStart + offset, ByteOrder.nativeOrder());
        }

        @Override
        public void commit() {
            writeBuffer.setSegmentAndPosition(segment, position);
        }
    }
}
