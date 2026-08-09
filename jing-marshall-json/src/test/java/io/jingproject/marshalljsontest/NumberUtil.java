package io.jingproject.marshalljsontest;

import io.jingproject.common.*;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;

public final class NumberUtil {
    public static final byte BYTE_ZERO = (byte) '0';
    public static final byte BYTE_MINUS = (byte) '-';
    private static final byte[] MIN_INT_BYTES = String.valueOf(Integer.MIN_VALUE).getBytes(StandardCharsets.US_ASCII);
    private static final byte[] MIN_LONG_BYTES = String.valueOf(Long.MIN_VALUE).getBytes(StandardCharsets.US_ASCII);
    private static final short[] ITOA_LUT_TABLE = makeItoaLutTable();
    private static final int[] LEN_TABLE = makeLenTable();
    private static final long[] POW_TABLE = makePowTable();
    private NumberUtil() {
        throw new UnsupportedOperationException("utility class");
    }

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

    // no overflow
    private static int[] makeLenTable() {
        int[] r = new int[64];
        for (int i = 1; i <= 63; i++) {
            long maxNum = -1L >>> i;
            r[i] = String.valueOf(maxNum).getBytes(StandardCharsets.US_ASCII).length;
        }
        return r;
    }

    // no overflow
    private static long[] makePowTable() {
        long[] r = new long[20];
        for (int i = 2; i <= 19; i++) {
            r[i] = Math.powExact(10L, i - 1);
        }
        return r;
    }

    private static int digitCount(int n) {
        int leadingZeros = Integer.numberOfLeadingZeros(n);
        int count = LEN_TABLE[leadingZeros + Long.SIZE - Integer.SIZE];
        if (n < POW_TABLE[count]) {
            return count - 1;
        }
        return count;
    }

    private static int digitCount(long n) {
        int leadingZeros = Long.numberOfLeadingZeros(n);
        int count = LEN_TABLE[leadingZeros];
        if (n < POW_TABLE[count]) {
            return count - 1;
        }
        return count;
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

    public static void writeInt3(int value, WriteBuffer writeBuffer) {
        writeBuffer.ensureCapacity(MIN_INT_BYTES.length);
        switch (writeBuffer) {
            case HeapWriteBuffer heapWriteBuffer -> writeIntToHeap(value, heapWriteBuffer);
            case SegmentWriteBuffer segmentWriteBuffer -> writeIntToSegment(value, segmentWriteBuffer);
        }
    }

    private static void writeIntToHeap(int value, HeapWriteBuffer heapWriteBuffer) {
        final byte[] bytes = heapWriteBuffer.rawByteArray();
        int position = heapWriteBuffer.intPosition();
        if(value == 0L) {
            bytes[position] = BYTE_ZERO;
            heapWriteBuffer.setPosition(position + 1);
            return ;
        }
        if(value == Integer.MIN_VALUE) {
            System.arraycopy(MIN_INT_BYTES, 0, bytes, position, MIN_INT_BYTES.length);
            heapWriteBuffer.setPosition(position + MIN_INT_BYTES.length);
            return ;
        }
        if(value < 0L){
            bytes[position++] = BYTE_MINUS;
            value = -value;
        }
        heapWriteBuffer.setPosition(writePositiveIntToHeap(value, digitCount(value), bytes, position));
    }

    private static void writeIntToSegment(int value, SegmentWriteBuffer segmentWriteBuffer) {
        final MemorySegment segment = segmentWriteBuffer.rawSegment();
        int position = segmentWriteBuffer.intPosition();
        if(value == 0L) {
            SegmentAccess.setByte(segment, position, BYTE_ZERO);
            segmentWriteBuffer.setPosition(position + 1);
            return ;
        }
        if(value == Integer.MIN_VALUE) {
            MemorySegment.copy(MIN_INT_BYTES, 0, segment, ValueLayout.JAVA_BYTE, position, MIN_INT_BYTES.length);
            segmentWriteBuffer.setPosition(position + MIN_INT_BYTES.length);
            return ;
        }
        if(value < 0L){
            SegmentAccess.setByte(segment, position++, BYTE_MINUS);
            value = -value;
        }
        segmentWriteBuffer.setPosition(writePositiveIntToSegment(value, digitCount(value), segment, position));
    }

    private static int writePositiveIntToHeap(int value, int digitCount, byte[] bytes, int position) {
        int v;
        switch (digitCount) {
            case 10:
                v = value / 100000000;
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 100000000;
            case 8:
                v = value / 1000000;
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1000000;
            case 6:
                v = value / 10000;
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 10000;
            case 4:
                v = value / 100;
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 100;
            case 2:
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[value]);
                position += 2;
                break;
            case 9:
                v = value / 10000000;
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 10000000;
            case 7:
                v = value / 100000;
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 100000;
            case 5:
                v = value / 1000;
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1000;
            case 3:
                v = value / 10;
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 10;
            case 1:
                bytes[position] = (byte) (BYTE_ZERO + value);
                position++;
        }
        return position;
    }

    private static int writePositiveIntToSegment(int value, int digitCount, MemorySegment segment, int position) {
        long lp = position;
        int v;
        switch (digitCount) {
            case 10:
                v = value / 100000000;
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 100000000;
            case 8:
                v = value / 1000000;
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1000000;
            case 6:
                v = value / 10000;
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 10000;
            case 4:
                v = value / 100;
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 100;
            case 2:
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[value]);
                lp += 2;
                break;
            case 9:
                v = value / 10000000;
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 10000000;
            case 7:
                v = value / 100000;
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 100000;
            case 5:
                v = value / 1000;
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1000;
            case 3:
                v = value / 10;
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 10;
            case 1:
                SegmentAccess.setByte(segment, lp, (byte) (BYTE_ZERO + value));
                lp++;
        }
        return Math.toIntExact(lp);
    }

    public static void writeLong3(long value, WriteBuffer writeBuffer) {
        writeBuffer.ensureCapacity(MIN_LONG_BYTES.length);
        switch (writeBuffer) {
            case HeapWriteBuffer heapWriteBuffer -> writeLongToHeap(value, heapWriteBuffer);
            case SegmentWriteBuffer segmentWriteBuffer -> writeLongToSegment(value, segmentWriteBuffer);
        }
    }

    private static void writeLongToHeap(long value, HeapWriteBuffer heapWriteBuffer) {
        final byte[] bytes = heapWriteBuffer.rawByteArray();
        int position = heapWriteBuffer.intPosition();
        if(value == 0L) {
            bytes[position] = BYTE_ZERO;
            heapWriteBuffer.setPosition(position + 1);
            return ;
        }
        if(value == Long.MIN_VALUE) {
            System.arraycopy(MIN_LONG_BYTES, 0, bytes, position, MIN_LONG_BYTES.length);
            heapWriteBuffer.setPosition(position + MIN_LONG_BYTES.length);
            return ;
        }
        if(value < 0L){
            bytes[position++] = BYTE_MINUS;
            value = -value;
        }
        heapWriteBuffer.setPosition(writePositiveLongToHeap(value, digitCount(value), bytes, position));
    }

    private static void writeLongToSegment(long value, SegmentWriteBuffer segmentWriteBuffer) {
        final MemorySegment segment = segmentWriteBuffer.rawSegment();
        int position = segmentWriteBuffer.intPosition();
        if(value == 0L) {
            SegmentAccess.setByte(segment, position, BYTE_ZERO);
            segmentWriteBuffer.setPosition(position + 1);
            return ;
        }
        if(value == Long.MIN_VALUE) {
            MemorySegment.copy(MIN_LONG_BYTES, 0, segment, ValueLayout.JAVA_BYTE, position, MIN_LONG_BYTES.length);
            segmentWriteBuffer.setPosition(position + MIN_LONG_BYTES.length);
            return ;
        }
        if(value < 0L){
            SegmentAccess.setByte(segment, position++, BYTE_MINUS);
            value = -value;
        }
        segmentWriteBuffer.setPosition(writePositiveLongToSegment(value, digitCount(value), segment, position));
    }

    private static int writePositiveLongToHeap(long value, int digitCount, byte[] bytes, int position) {
        int v;
        switch (digitCount) {
            case 18:
                v = (int) (value / 1_000_000_000_000_000_0L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_000_000_0L;
            case 16:
                v = (int) (value / 1_000_000_000_000_00L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_000_00L;
            case 14:
                v = (int) (value / 1_000_000_000_000L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_000L;
            case 12:
                v = (int) (value / 1_000_000_000_0L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_0L;
            case 10:
                v = (int) (value / 1_000_000_00L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_00L;
            case 8:
                v = (int) (value / 1_000_000L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000L;
            case 6:
                v = (int) (value / 1_000_0L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_0L;
            case 4:
                v = (int) (value / 100L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 100L;
            case 2:
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[(int) value]);
                position += 2;
                break;
            case 19:
                v = (int) (value / 1_000_000_000_000_000_00L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_000_000_00L;
            case 17:
                v = (int) (value / 1_000_000_000_000_000L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_000_000L;
            case 15:
                v = (int) (value / 1_000_000_000_000_0L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_000_0L;
            case 13:
                v = (int) (value / 1_000_000_000_00L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000_00L;
            case 11:
                v = (int) (value / 1_000_000_000L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_000L;
            case 9:
                v = (int) (value / 1_000_000_0L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_000_0L;
            case 7:
                v = (int) (value / 1_000_00L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000_00L;
            case 5:
                v = (int) (value / 1_000L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 1_000L;
            case 3:
                v = (int) (value / 10L);
                ArrayAccess.setShort(bytes, position, ITOA_LUT_TABLE[v]);
                position += 2;
                value -= v * 10L;
            case 1:
                bytes[position] = (byte) (BYTE_ZERO + value);
                position++;
        }
        return position;
    }

    private static int writePositiveLongToSegment(long value, int digitCount, MemorySegment segment, int position) {
        long lp = position;
        int v;
        switch (digitCount) {
            case 18:
                v = (int) (value / 1_000_000_000_000_000_0L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1_000_000_000_000_000_0L;
            case 16:
                v = (int) (value / 1_000_000_000_000_00L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1_000_000_000_000_00L;
            case 14:
                v = (int) (value / 1_000_000_000_000L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1_000_000_000_000L;
            case 12:
                v = (int) (value / 1_000_000_000_0L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1_000_000_000_0L;
            case 10:
                v = (int) (value / 1_000_000_00L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1_000_000_00L;
            case 8:
                v = (int) (value / 1_000_000L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1_000_000L;
            case 6:
                v = (int) (value / 1_000_0L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1_000_0L;
            case 4:
                v = (int) (value / 100L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 100L;
            case 2:
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[(int) value]);
                lp += 2;
                break;
            case 19:
                v = (int) (value / 1_000_000_000_000_000_00L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1_000_000_000_000_000_00L;
            case 17:
                v = (int) (value / 1_000_000_000_000_000L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1_000_000_000_000_000L;
            case 15:
                v = (int) (value / 1_000_000_000_000_0L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1_000_000_000_000_0L;
            case 13:
                v = (int) (value / 1_000_000_000_00L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1_000_000_000_00L;
            case 11:
                v = (int) (value / 1_000_000_000L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1_000_000_000L;
            case 9:
                v = (int) (value / 1_000_000_0L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1_000_000_0L;
            case 7:
                v = (int) (value / 1_000_00L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1_000_00L;
            case 5:
                v = (int) (value / 1_000L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 1_000L;
            case 3:
                v = (int) (value / 10L);
                SegmentAccess.setShort(segment, lp, ITOA_LUT_TABLE[v]);
                lp += 2;
                value -= v * 10L;
            case 1:
                SegmentAccess.setByte(segment, lp, (byte) (BYTE_ZERO + value));
                lp++;
        }
        return Math.toIntExact(lp);
    }
}
