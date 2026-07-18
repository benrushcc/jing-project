package io.jingproject.marshalljson;

import jdk.incubator.vector.*;

import java.nio.charset.StandardCharsets;

public final class StringSerializer {
    private static final boolean ESCAPE_SLASH =
            Boolean.parseBoolean(System.getProperty("jing.marshalljson.escapeslash", "false"));
    private static final VectorSpecies<Short> SHORT_SPECIES;
    private static final VectorSpecies<Byte> BYTE_SPECIES;
    private static final int LANE_COUNT;
    private static final int NO_MOVE_COUNT;
    private static final byte[] WRITER_ESCAPE_TABLE = makeWriterEscapeTable();
    private static final byte[] HEX_BYTES = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
    private static final byte BYTE_quote = (byte) '"';
    private static final byte BYTE_rsolidus = (byte) '\\';
    private static final byte BYTE_u = (byte) 'u';
    private static final byte BYTE_zero = (byte) '0';

    static {
        int vecSize = Integer.parseInt(System.getProperty("jing.marshalljson.vecsize", "-1"));
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
            default -> throw new UnsupportedOperationException("vector size too small");
        }
        LANE_COUNT = SHORT_SPECIES.length();
        NO_MOVE_COUNT = Math.multiplyExact(LANE_COUNT, 4096); // for avx-512, string more than 128kb will not be moved
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

    private char[] buffer;

    public char[] acquireBuffer(int len) {
        assert len > 0;
        if(len < LANE_COUNT || len > NO_MOVE_COUNT) {
            return null;
        }
        if(buffer == null || buffer.length < len) {
            buffer = new char[Integer.highestOneBit(len) << 1]; // no overflow
        }
        return buffer;
    }

    public int serializeToBytes(String str, byte[] bytes, int offset) {
        bytes[offset++] = BYTE_quote;
        final int len = str.length();
        final char[] buffer = acquireBuffer(len);
        if(buffer == null) {
            offset = process(str, len, bytes, offset);
        } else {
            str.getChars(0, len, buffer, 0);
            int index = 0;
            while (len - index >= LANE_COUNT) {
                ShortVector shortVector = ShortVector.fromCharArray(SHORT_SPECIES, buffer, index);
                ByteVector byteVector = (ByteVector) shortVector.convertShape(VectorOperators.S2B, BYTE_SPECIES, 0);
                byteVector.intoArray(bytes, offset);
                int matched = asciiCount(shortVector);
                offset += matched;
                index += matched;
                if(matched != LANE_COUNT) {
                    break ;
                }
            }
            if(index < len) {
                offset = processRemaining(buffer, index, len, bytes, offset);
            }
        }
        bytes[offset++] = BYTE_quote;
        return offset;
    }

    private static int asciiCount(ShortVector shortVector) {
        VectorMask<Short> mask = shortVector.compare(VectorOperators.LT, (short) 0x20)
                .or(shortVector.compare(VectorOperators.GT, (short) 0x7E))
                .or(shortVector.compare(VectorOperators.EQ, (short) 0x22))
                .or(shortVector.compare(VectorOperators.EQ, (short) 0x5C));
        if (ESCAPE_SLASH) {
            mask = mask.or(shortVector.compare(VectorOperators.EQ, (short) 0x2F));
        }
        // return mask.firstTrue();
        return Long.numberOfTrailingZeros(mask.toLong());
    }

    private static int process(String str, int len, byte[] bytes, int offset) {
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

    private static int processRemaining(char[] buffer, int index, int len, byte[] bytes, int offset) {
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

    private static int serializeCharToBytes(char c, byte[] bytes, int offset) {
        int v = WRITER_ESCAPE_TABLE[c];
        if (v == 0) {
            bytes[offset++] = (byte) c;
        } else if (v > 0) {
            bytes[offset++] = BYTE_rsolidus;
            bytes[offset++] = (byte) v;
        } else {
            bytes[offset++] = BYTE_rsolidus;
            bytes[offset++] = BYTE_u;
            bytes[offset++] = BYTE_zero;
            bytes[offset++] = BYTE_zero;
            bytes[offset++] = HEX_BYTES[c >>> 4];
            bytes[offset++] = HEX_BYTES[c & 0xF];
        }
        return offset;
    }

    private static int serializeCharToBytes2(char c, byte[] bytes, int offset) {
        bytes[offset]     = (byte) (0xC0 | (c >> 6));
        bytes[offset + 1] = (byte) (0x80 | (c & 0x3F));
        return offset + 2;
    }

    private static int serializeCharToBytes3(char c, byte[] bytes, int offset) {
        bytes[offset]     = (byte) (0xE0 | (c >> 12));
        bytes[offset + 1] = (byte) (0x80 | ((c >> 6) & 0x3F));
        bytes[offset + 2] = (byte) (0x80 | (c & 0x3F));
        return offset + 3;
    }

    private static int serializeCharToBytes4(char highSurrogate, char lowSurrogate, byte[] bytes, int offset) {
        int cp = Character.toCodePoint(highSurrogate, lowSurrogate);
        bytes[offset]     = (byte) (0xF0 | (cp >> 18));
        bytes[offset + 1] = (byte) (0x80 | ((cp >> 12) & 0x3F));
        bytes[offset + 2] = (byte) (0x80 | ((cp >> 6) & 0x3F));
        bytes[offset + 3] = (byte) (0x80 | (cp & 0x3F));
        return offset + 4;
    }
}
