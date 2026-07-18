package io.jingproject.marshalljson;

import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.MarshallInfo;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public final class JsonDeserializerContext {
    private static final boolean VALIDATE_UTF8 = true;
    public static final int DEFAULT_BUFFER_SIZE = 64;
    private byte[] buffer; // combined storage for bitmap and temp buffer
    private int index;     // allocated region end (0..index are allocated)
    private int len;       // temp buffer len (index..index + len used), 0 if temp not active

    public JsonDeserializerContext() {
        this.buffer = new byte[DEFAULT_BUFFER_SIZE];
        this.index = 0;
        this.len = 0;
    }

    public JsonDeserializerContext(JsonDeserializerOption option) {
        this.buffer = new byte[option.bufferSize()];
        this.index = 0;
        this.len = 0;
    }

    public MarshallInfo asMarshallInfo(MarshallFacade fc) {
        assert len > 0;
        if (VALIDATE_UTF8 && !JsonDeserializeUtil.validateUtf8(buffer, index, len)) {
            throw new JsonDeserializerException("illegal utf-8 encoded string : " + Arrays.toString(Arrays.copyOfRange(buffer, index, index + len)));
        }
        MarshallInfo r = fc.marshallInfoByMappedName(buffer, index, len);
        len = -1;
        return r;
    }

    public char asSingleChar() {
        assert len > 0;
        if (VALIDATE_UTF8 && !JsonDeserializeUtil.validateUtf8(buffer, index, len)) {
            throw new JsonDeserializerException("illegal utf-8 encoded string : " + Arrays.toString(Arrays.copyOfRange(buffer, index, index + len)));
        }
        char c = switch (len - index) {
            case 1 -> (char) buffer[index];
            case 2 -> (char) (((buffer[index] & 0x1F) << 6) | (buffer[index + 1] & 0x3F));
            case 3 -> (char) (((buffer[index] & 0x0F) << 12) | ((buffer[index + 1] & 0x3F) << 6) | (buffer[index + 2] & 0x3F));
            default -> throw new JsonDeserializerException("string cannot be safely represented as exactly one character : " + Arrays.toString(Arrays.copyOfRange(buffer, index, index + len)));
        };
        len = 0;
        return c;
    }

    public String asUtf8String() {
        if (VALIDATE_UTF8 && !JsonDeserializeUtil.validateUtf8(buffer, index, len)) {
            throw new JsonDeserializerException("illegal utf-8 encoded string : " + Arrays.toString(Arrays.copyOfRange(buffer, index, index + len)));
        }
        String r = new String(buffer, index, len, StandardCharsets.UTF_8);
        len = 0;
        return r;
    }

    public void ensureCapacity(int capacity) {
        int required = Math.addExact(capacity, index + len); // no overflow
        if(buffer.length < required) {
            int newLen = 1 << (32 - Integer.numberOfLeadingZeros(required));
            if(newLen < 0) {
                throw new JsonSerializerException("buffer overflow");
            }
            buffer = Arrays.copyOf(buffer, newLen);
        }
    }

    public void appendByte(byte b) {
        ensureCapacity(1);
        buffer[index + len] = b;
        len += 1;
    }
    
    public void appendutf8CodePoint(int cp) {
        if (cp < 0x80) {
            ensureCapacity(1);
            buffer[index + len] = (byte) cp;
            len += 1;
        } else if (cp < 0x800) {
            ensureCapacity(2);
            buffer[index + len] = (byte) (0xC0 | (cp >> 6));
            buffer[index + len + 1] = (byte) (0x80 | (cp & 0x3F));
            len += 2;
        } else if (cp < 0x10000) {
            ensureCapacity(3);
            buffer[index + len] = (byte) (0xE0 | (cp >> 12));
            buffer[index + len + 1] = (byte) (0x80 | ((cp >> 6) & 0x3F));
            buffer[index + len + 2] = (byte) (0x80 | (cp & 0x3F));
            len += 3;
        } else {
            ensureCapacity(4);
            buffer[index + len] = (byte) (0xF0 | (cp >> 18));
            buffer[index + len + 1] = (byte) (0x80 | ((cp >> 12) & 0x3F));
            buffer[index + len + 2] = (byte) (0x80 | ((cp >> 6) & 0x3F));
            buffer[index + len + 3] = (byte) (0x80 | (cp & 0x3F));
            len += 4;
        }
    }

    public void appendBytes(byte[] bytes, int offset, int length) {
        assert bytes != null && Objects.checkFromIndexSize(offset, length, bytes.length) >= 0;
        ensureCapacity(length);
        System.arraycopy(bytes, offset, buffer, index + len, length);
        len += length;
    }

    public void appendSegment(MemorySegment segment, long offset, int length) {
        assert segment != null && Objects.checkFromIndexSize(offset, length, segment.byteSize()) >= 0;
        ensureCapacity(length);
        MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, offset, buffer, index + len, length);
        len += length;
    }

    public static int bitmapBytes(int fieldCount) {
        assert fieldCount >= 0 && fieldCount <= 8192;
        return (fieldCount + 7) >> 3;
    }

    public int bitmapIndex(int bytes) {
        assert bytes > 0 && bytes < 8192; // a class have at most 65535 fields in jvm
        assert len == 0;
        ensureCapacity(bytes);
        int r = index;
        index += bytes;
        return r;
    }

    public boolean assign(int bitmapIndex, int fieldIndex) {
        assert fieldIndex >= 0 && fieldIndex < 8192;
        assert bitmapIndex >= 0 && bitmapIndex < index && len == 0;
        int byteOffset = bitmapIndex + (fieldIndex >> 3);
        int bitOffset = fieldIndex & 0x7;
        byte mask = (byte) (1 << bitOffset);
        byte val = buffer[byteOffset];
        buffer[byteOffset] = (byte) (val | mask);
        return (val & mask) != 0;
    }

    public boolean assigned(int bitmapIndex, int fieldCount) {
        assert fieldCount >= 0 && fieldCount <= 8192;
        assert bitmapIndex >= 0 && bitmapIndex < index && len == 0;
        int fullBytes = fieldCount >> 3;
        int remainingBits = fieldCount & 0x7;
        for (int i = 0; i < fullBytes; i++) {
            if (buffer[bitmapIndex + i] != (byte) 0xFF) {
                return false;
            }
        }
        if (remainingBits > 0) {
            byte lastByte = buffer[bitmapIndex + fullBytes];
            byte mask = (byte) ((1 << remainingBits) - 1);
            return (lastByte & mask) == mask;
        }
        return true;
    }

    public void rewind(int bitmapIndex) {
        assert bitmapIndex >= 0 && bitmapIndex < index;
        assert len == 0;
        index = bitmapIndex;
    }
}
