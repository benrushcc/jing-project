package io.jingproject.marshalljsontest;

import io.jingproject.common.ArrayAccess;
import io.jingproject.common.Utils;
import io.jingproject.common.WriteBuffer;

import java.nio.charset.StandardCharsets;

// experiments
public final class NumberUtil {
    private NumberUtil() {
        throw new UnsupportedOperationException("utility class");
    }

    public static final byte BYTE_ZERO = (byte) '0';
    public static final byte BYTE_MINUS = (byte) '-';
    private static final byte[] MIN_INT_BYTES = String.valueOf(Integer.MIN_VALUE).getBytes(StandardCharsets.US_ASCII);
    private static final byte[] MIN_LONG_BYTES = String.valueOf(Long.MIN_VALUE).getBytes(StandardCharsets.US_ASCII);
    private static final short[] ITOA_LUT_TABLE = makeItoaLutTable();

    // no overflow
    private static short[] makeItoaLutTable() {
        short[] r = new short[100];
        for (int i = 0; i < 100; i++) {
            byte b0 = (byte) (BYTE_ZERO + (i / 10));
            byte b1 = (byte) (BYTE_ZERO + (i % 10));
            r[i] = Utils.compact(b0, b1);
        }
        return r;
    }

    /**
     * using jdk string conversion to write integer number to writeBuffer, slower but guaranteed to be safe, no overflow
     */
    public static void writeInt0(int number, WriteBuffer writeBuffer) {
        String s = Integer.toString(number);
        writeBuffer.writeBytes(s.getBytes(StandardCharsets.US_ASCII));
    }

    public static void writeLong0(long number, WriteBuffer writeBuffer) {
        String s = Long.toString(number);
        writeBuffer.writeBytes(s.getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * write 1 digit to writeBuffer each time, significantly faster than jdk string version, no overflow
     */
    public static void writeInt1(int number, WriteBuffer writeBuffer) {
        if (number == 0) {
            writeBuffer.writeByte(BYTE_ZERO);
            return;
        }
        byte[] buffer = new byte[MIN_INT_BYTES.length];
        int index = buffer.length;
        boolean negative = number < 0;
        int sum = negative ? number : -number;
        while (sum != 0) {
            int i = sum % 10;
            buffer[--index] = (byte) (BYTE_ZERO - i);
            sum = sum / 10;
        }
        if (negative) {
            buffer[--index] = BYTE_MINUS;
        }
        writeBuffer.writeBytes(buffer, index, buffer.length - index);
    }

    public static void writeLong1(long number, WriteBuffer writeBuffer) {
        if (number == 0) {
            writeBuffer.writeByte(BYTE_ZERO);
            return;
        }
        byte[] buffer = new byte[MIN_LONG_BYTES.length];
        int index = buffer.length;
        boolean negative = number < 0L;
        long sum = negative ? number : -number;
        while (sum != 0) {
            int i = (int) (sum % 10L);
            buffer[--index] = (byte) (BYTE_ZERO - i);
            sum = sum / 10L;
        }
        if (negative) {
            buffer[--index] = BYTE_MINUS;
        }
        writeBuffer.writeBytes(buffer, index, buffer.length - index);
    }

    /**
     * using lookup table to write 2 digits to writeBuffer each time, slightly faster than the 1 digit version, no overflow
     */
    public static void writeInt2(int number, WriteBuffer writeBuffer) {
        if (number == 0) {
            writeBuffer.writeByte(BYTE_ZERO);
            return;
        }
        byte[] buffer = new byte[MIN_INT_BYTES.length];
        int index = buffer.length;
        boolean negative = number < 0;
        if (!negative) {
            number = -number;
        }
        int q;
        while (number <= -100) {
            q = number / 100;
            index -= 2;
            ArrayAccess.setShort(buffer, index, ITOA_LUT_TABLE[(q * 100) - number]);
            number = q;
        }
        if (number <= -10) {
            index -= 2;
            ArrayAccess.setShort(buffer, index, ITOA_LUT_TABLE[-number]);
        } else {
            buffer[--index] = (byte) (BYTE_ZERO - number);
        }
        if (negative) {
            buffer[--index] = BYTE_MINUS;
        }
        writeBuffer.writeBytes(buffer, index, buffer.length - index);
    }

    public static void writeLong2(long number, WriteBuffer writeBuffer) {
        if (number == 0L) {
            writeBuffer.writeByte(BYTE_ZERO);
            return;
        }
        byte[] buffer = new byte[MIN_LONG_BYTES.length];
        int index = buffer.length;
        boolean negative = number < 0L;
        if (!negative) {
            number = -number;
        }
        long q;
        while (number <= -100L) {
            q = number / 100L;
            index = index - 2;
            ArrayAccess.setShort(buffer, index, ITOA_LUT_TABLE[(int) ((q * 100L) - number)]);
            number = q;
        }
        if (number <= -10L) {
            index = index - 2;
            ArrayAccess.setShort(buffer, index, ITOA_LUT_TABLE[(int) (-number)]);
        } else {
            buffer[--index] = (byte) (BYTE_ZERO - number);
        }
        if (negative) {
            buffer[--index] = BYTE_MINUS;
        }
        writeBuffer.writeBytes(buffer, index, buffer.length - index);
    }
}
