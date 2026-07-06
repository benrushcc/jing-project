package io.jingproject.marshalljson;

import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.MarshallInfo;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public final class JsonDeserializerContext {
    private static final int DEFAULT_BUFFER_SIZE = 64;
    private byte[] strBuffer;
    private int strIndex;
    private byte[] seenBuffer;
    private int seenIndex;

    public JsonDeserializerContext() {
        this(DEFAULT_BUFFER_SIZE);
    }

    public JsonDeserializerContext(int size) {
        this.strBuffer = new byte[size];
        this.strIndex = 0;
    }

    public byte[] strBuffer() {
        return strBuffer;
    }

    public int strIndex() {
        return strIndex;
    }

    public MarshallInfo asMarshallInfo(MarshallFacade fc) {
        MarshallInfo r = fc.marshallInfoByMappedName(strBuffer, 0, strIndex);
        strIndex = 0;
        return r;
    }

    public char asChar() {
        if (!JsonDeserializeUtil.validateUtf8(strBuffer, 0, strIndex)) {
            throw new JsonDeserializerException("illegal utf-8 encoded string : " + Arrays.toString(Arrays.copyOfRange(strBuffer, 0, strIndex)));
        }
        char c = switch (strIndex) {
            case 1 -> (char) strBuffer[0];
            case 2 -> (char) (((strBuffer[0] & 0x1F) << 6) | (strBuffer[1] & 0x3F));
            case 3 -> (char) (((strBuffer[0] & 0x0F) << 12) | ((strBuffer[1] & 0x3F) << 6) | (strBuffer[2] & 0x3F));
            default -> throw new JsonDeserializerException("string cannot be safely represented as exactly one character : " + Arrays.toString(Arrays.copyOfRange(strBuffer, 0, strIndex)));
        };
        strIndex = 0;
        return c;
    }

    public String asString() {
        if (!JsonDeserializeUtil.validateUtf8(strBuffer, 0, strIndex)) {
            throw new JsonDeserializerException("illegal utf-8 encoded string : " + Arrays.toString(Arrays.copyOfRange(strBuffer, 0, strIndex)));
        }
        String r = new String(strBuffer, 0, strIndex, StandardCharsets.UTF_8);
        strIndex = 0;
        return r;
    }

    public void ensureStrBufferCapacity(int capacity) {
        int required = Math.addExact(capacity, strIndex);
        if(strBuffer.length < required) {
            int newLen = 1 << (32 - Integer.numberOfLeadingZeros(required));
            if(newLen < 0) {
                throw new JsonSerializerException("buffer overflow");
            }
            strBuffer = Arrays.copyOf(strBuffer, newLen);
        }
    }

    public void appendByte(byte b) {
        ensureStrBufferCapacity(1);
        strBuffer[strIndex++] = b;
    }
    
    public void appendutf8CodePoint(int cp) {
        if (cp < 0x80) {
            ensureStrBufferCapacity(1);
            strBuffer[strIndex++] = (byte) cp;
        } else if (cp < 0x800) {
            ensureStrBufferCapacity(2);
            strBuffer[strIndex++] = (byte) (0xC0 | (cp >> 6));
            strBuffer[strIndex++] = (byte) (0x80 | (cp & 0x3F));
        } else if (cp < 0x10000) {
            ensureStrBufferCapacity(3);
            strBuffer[strIndex++] = (byte) (0xE0 | (cp >> 12));
            strBuffer[strIndex++] = (byte) (0x80 | ((cp >> 6) & 0x3F));
            strBuffer[strIndex++] = (byte) (0x80 | (cp & 0x3F));
        } else {
            ensureStrBufferCapacity(4);
            strBuffer[strIndex++] = (byte) (0xF0 | (cp >> 18));
            strBuffer[strIndex++] = (byte) (0x80 | ((cp >> 12) & 0x3F));
            strBuffer[strIndex++] = (byte) (0x80 | ((cp >> 6) & 0x3F));
            strBuffer[strIndex++] = (byte) (0x80 | (cp & 0x3F));
        }
    }

    public void appendBytes(byte[] bytes, int offset, int length) {
        assert bytes != null && Objects.checkFromIndexSize(offset, length, bytes.length) >= 0;
        ensureStrBufferCapacity(length);
        System.arraycopy(bytes, offset, strBuffer, strIndex, length);
        strIndex += length;
    }

    public void appendSegment(MemorySegment segment, long offset, int length) {
        assert segment != null && Objects.checkFromIndexSize(offset, length, segment.byteSize()) >= 0;
        ensureStrBufferCapacity(length);
        MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, offset, strBuffer, strIndex, length);
        strIndex += length;
    }

    public int seenIndex(int bytes) {
        assert bytes > 0 && bytes < 8192; // jvm have at most 65535 fields
        if(seenBuffer == null) {
            int newLen = 1 << (32 - Integer.numberOfLeadingZeros(bytes));
            seenBuffer = new byte[newLen];
            seenIndex = bytes;
            return 0;
        }
        int curIndex = seenIndex;
        int newIndex = Math.addExact(curIndex, bytes);
        if(newIndex > seenBuffer.length) {
            int newLen = 1 << (32 - Integer.numberOfLeadingZeros(newIndex));
            if(newLen < 0) {
                throw new JsonSerializerException("seen buffer overflow");
            }
            seenBuffer = Arrays.copyOf(seenBuffer, newLen);
        }
        seenIndex = newIndex;
        return curIndex;
    }

    public boolean seen(int sIndex, int fieldIndex) {
        assert sIndex >= 0 && sIndex < seenBuffer.length && fieldIndex >= 0 && fieldIndex <= 65535;
        int byteIndex = Math.addExact(sIndex, fieldIndex >>> 3);
        int mask = 1 << (fieldIndex & 7);
        byte b = seenBuffer[byteIndex];
        if ((b & mask) != 0) {
            return true;
        }
        seenBuffer[byteIndex] = (byte) (b | mask);
        return false;
    }

    public void finishSeen(int sIndex) {
        assert sIndex >= 0 && sIndex < seenBuffer.length;
        if(sIndex > seenIndex) {
            throw new JsonDeserializerException("corrupted state");
        }
        seenIndex = sIndex;
    }
}
