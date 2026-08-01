package io.jingproject.marshalljson;

import io.jingproject.common.ArrayAccess;
import io.jingproject.common.Os;
import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.MarshallInfo;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Objects;

public final class JsonDeserializerContext {

    static {
        try {
            Class<Os> _ = MethodHandles.lookup().ensureInitialized(Os.class);
        } catch (IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final int BITMAP_INITIAL_SIZE = 16;
    private char[] chars;
    private int charsIndex = 0;
    private byte[] bytes;
    private int bytesIndex = 0;
    private byte[] bitmap;
    private int bitmapIndex = 0;
    private Object[] objArr = null;
    private Object obj = null;
    private Class<?> type = null;

    public JsonDeserializerContext(int charBufferSize, int byteBufferSize) {
        if(charBufferSize > 0) {
            this.chars = new char[charBufferSize];
        }
        if(byteBufferSize > 0) {
            this.bytes = new byte[byteBufferSize];
        }
        this.bitmap = null;
    }

    public JsonDeserializerContext(JsonDeserializerOption option) {
        this.chars = new char[option.charBufferSize()];
        this.bytes = new byte[option.byteBufferSize()];
        this.bitmap = new byte[BITMAP_INITIAL_SIZE];
    }

    public Object[] objArr() {
        return objArr;
    }

    public void setObjArr(Object[] objArr) {
        this.objArr = objArr;
    }

    public Object obj() {
        Object r =  obj;
        obj = null;
        return r;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }

    public Class<?> type() {
        Class<?> r = type;
        type = null;
        return r;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public char[] chars() {
        return chars;
    }

    public void setCharsIndex(int charsIndex) {
        assert charsIndex >= 0;
        this.charsIndex = charsIndex;
    }

    public void ensureCharsCapacity(int capacity) {
        int required = Math.addExact(capacity, charsIndex); // no overflow
        if(chars.length < required) {
            int newLen = 1 << (32 - Integer.numberOfLeadingZeros(required));
            if(newLen < 0) {
                throw new JsonSerializerException("char buffer overflow");
            }
            chars = Arrays.copyOf(chars, newLen);
        }
    }

    public void appendChar(char c) {
        assert !Character.isSurrogate(c);
        ensureCharsCapacity(1);
        chars[charsIndex++] = c;
    }

    public void appendChars(char c1, char c2) {
        assert Character.isHighSurrogate(c1) &&  Character.isLowSurrogate(c2);
        ensureCharsCapacity(2);
        chars[charsIndex] = c1;
        chars[charsIndex + 1] = c2;
        charsIndex += 2;
    }

    public char asChar() {
        if(charsIndex != 1) {
            throw new JsonDeserializerException("not a single char");
        }
        charsIndex = 0;
        return chars[0];
    }

    public char[] asCharArray() {
        if(charsIndex == 0) {
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
        assert bytesIndex >= 0;
        this.bytesIndex = bytesIndex;
    }

    public void ensureBytesCapacity(int capacity) {
        int required = Math.addExact(capacity, bytesIndex); // no overflow
        if(bytes.length < required) {
            int newLen = 1 << (32 - Integer.numberOfLeadingZeros(required));
            if(newLen < 0) {
                throw new JsonSerializerException("byte buffer overflow");
            }
            bytes = Arrays.copyOf(bytes, newLen);
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
        assert !Character.isSurrogate(c);
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
        assert Character.isHighSurrogate(high) && Character.isLowSurrogate(low);
        ensureBytesCapacity(4);
        int cp = ((high - 0xD800) << 10) + (low - 0xDC00) + 0x10000;
        bytes[bytesIndex] = (byte) (0xF0 | (cp >> 18));
        bytes[bytesIndex + 1] = (byte) (0x80 | ((cp >> 12) & 0x3F));
        bytes[bytesIndex + 2] = (byte) (0x80 | ((cp >> 6) & 0x3F));
        bytes[bytesIndex + 3] = (byte) (0x80 | (cp & 0x3F));
        bytesIndex += 4;
    }

    public void appendBytes(byte[] data, int offset, int len) {
        assert data != null && Objects.checkFromIndexSize(offset, len, data.length) >= 0;
        ensureBytesCapacity(len);
        System.arraycopy(data, offset, bytes, bytesIndex, len);
        bytesIndex += len;
    }

    public void appendSegment(MemorySegment segment, long offset, int len) {
        assert segment != null && Objects.checkFromIndexSize(offset, len, segment.byteSize()) >= 0;
        ensureBytesCapacity(len);
        MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, offset, bytes, bytesIndex, len);
        bytesIndex += len;
    }

    public MarshallInfo asMarshallInfo(MarshallFacade fc) {
        if(bytesIndex == 0) {
            throw new JsonDeserializerException("key is empty");
        }
        MarshallInfo r = fc.marshallInfoByMappedName(bytes, 0, bytesIndex);
        bytesIndex = 0;
        return r;
    }

    public byte[] asByteArray() {
        if(bytesIndex == 0) {
            throw new JsonDeserializerException("no byte value present");
        }
        byte[] r = Arrays.copyOf(bytes, bytesIndex);
        bytesIndex = 0;
        return r;
    }

    public boolean[] asBooleanArray() {
        if(bytesIndex == 0) {
            throw new JsonDeserializerException("no boolean value present");
        }
        boolean[] r = new boolean[bytesIndex];
        for(int i = 0; i < bytesIndex; i++) {
            r[i] = bytes[i] > 0;
        }
        bytesIndex = 0;
        return r;
    }

    public short[] asShortArray() {
        if(bytesIndex == 0) {
            throw new JsonDeserializerException("no short value present");
        }
        assert bytesIndex % 2 == 0;
        int len = bytesIndex >> 1;
        short[] r = new short[len];
        MemorySegment.copy(MemorySegment.ofArray(bytes), ValueLayout.JAVA_SHORT_UNALIGNED, 0L, r, 0, len);
        bytesIndex = 0;
        return r;
    }

    public int[] asIntArray() {
        if(bytesIndex == 0) {
            throw new JsonDeserializerException("no int value present");
        }
        assert bytesIndex % 4 == 0;
        int len = bytesIndex >> 2;
        int[] r = new int[len];
        MemorySegment.copy(MemorySegment.ofArray(bytes), ValueLayout.JAVA_INT_UNALIGNED, 0L, r, 0, len);
        bytesIndex = 0;
        return r;
    }

    public long[] asLongArray() {
        if(bytesIndex == 0) {
            throw new JsonDeserializerException("no long value present");
        }
        assert bytesIndex % 8 == 0;
        int len = bytesIndex >> 3;
        long[] r = new long[len];
        MemorySegment.copy(MemorySegment.ofArray(bytes), ValueLayout.JAVA_LONG_UNALIGNED, 0L, r, 0, len);
        bytesIndex = 0;
        return r;
    }

    public float[] asFloatArray() {
        if(bytesIndex == 0) {
            throw new JsonDeserializerException("no float value present");
        }
        assert bytesIndex % 4 == 0;
        int len = bytesIndex >> 2;
        float[] r = new float[len];
        MemorySegment.copy(MemorySegment.ofArray(bytes), ValueLayout.JAVA_FLOAT_UNALIGNED, 0L, r, 0, len);
        bytesIndex = 0;
        return r;
    }

    public double[] asDoubleArray() {
        if(bytesIndex == 0) {
            throw new JsonDeserializerException("no double value present");
        }
        assert bytesIndex % 8 == 0;
        int len = bytesIndex >> 3;
        double[] r = new double[len];
        MemorySegment.copy(MemorySegment.ofArray(bytes), ValueLayout.JAVA_DOUBLE_UNALIGNED, 0L, r, 0, len);
        bytesIndex = 0;
        return r;
    }

    private void ensureBitmapCapacity(int capacity) {
        int required = Math.addExact(capacity, bitmapIndex); // no overflow
        if(bitmap.length < required) {
            int newLen = 1 << (32 - Integer.numberOfLeadingZeros(required));
            if(newLen < 0) {
                throw new JsonSerializerException("bitmap buffer overflow");
            }
            bitmap = Arrays.copyOf(bitmap, newLen);
        }
    }

    public int bitmapIndex(int fieldCount) {
        assert fieldCount >= 0 && fieldCount <= 8192; // a class have at most 65535 fields in jvm
        int requiredBytes = (fieldCount + 7) >> 3;
        ensureBitmapCapacity(requiredBytes);
        int r = bitmapIndex;
        bitmapIndex += requiredBytes;
        return r;
    }

    public boolean assign(int bIndex, int fieldIndex) {
        assert bIndex >= 0 && bIndex < bitmapIndex && fieldIndex >= 0 && fieldIndex < 8192;
        int byteOffset = bIndex + (fieldIndex >> 3);
        int bitOffset = fieldIndex & 0x7;
        byte mask = (byte) (1 << bitOffset);
        byte val = bitmap[byteOffset];
        bitmap[byteOffset] = (byte) (val | mask);
        return (val & mask) != 0;
    }

    public boolean allPrimitiveFieldPresent(MarshallFacade fc, int bIndex, int fieldCount) {
        assert bIndex >= 0 && bIndex < bitmapIndex && fieldCount >= 0 && fieldCount <= 8192;
        for(int i = 0; i < fieldCount; i++) {
            MarshallInfo marshallInfo = fc.marshallInfoByIndex(i);
            if(marshallInfo.rawType().isPrimitive()) {
                int byteOffset = bIndex + (i >> 3);
                int bitOffset = i & 0x7;
                byte mask = (byte) (1 << bitOffset);
                byte val = bitmap[byteOffset];
                if((val & mask) == 0) {
                    return false;
                }
            }
        }
        return true;
    }


    public boolean allPresent(int bIndex, int fieldCount) {
        assert bIndex >= 0 && bIndex < bitmapIndex && fieldCount >= 0 && fieldCount <= 8192;
        int fullBytes = fieldCount >> 3;
        int remainingBits = fieldCount & 0x7;
        for (int i = 0; i < fullBytes; i++) {
            if (bitmap[bIndex + i] != (byte) 0xFF) {
                return false;
            }
        }
        if (remainingBits > 0) {
            byte lastByte = bitmap[bIndex + fullBytes];
            byte mask = (byte) ((1 << remainingBits) - 1);
            return (lastByte & mask) == mask;
        }
        return true;
    }

    public void rewind(int bIndex) {
        assert bIndex >= 0 && bIndex < bitmapIndex;
        bitmapIndex = bIndex;
    }
}
