package io.jingproject.log;

import io.jingproject.ffm.NativeSegmentAccess;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

// a growing cursor-style utf-8 writer over a native memory region.
//
// it implements both Appendable and OutputStream so it can serve as the write
// target of java.text.DateTimeFormatter.formatTo and of a PrintStream used for
// stack traces. all writes stay off-heap, the writer only tracks a cursor.
//
// capacity is fixed at construction. every write checks the remaining space
// first and, instead of throwing, marks the overflow flag and drops the write.
// this lets the pattern engine (and Appendable consumers such as DateTimeFormatter)
// run to completion and then ask overflowed() once, rather than unwinding
// mid-format. an overflowed writer can be cleared (used for the "%ex" truncation
// marker that must not fail the event).
public final class RegionOut extends OutputStream implements Appendable {
    private final MemorySegment region;
    private long pos;
    private final long capacity;
    private boolean overflow;

    // creates a writer over the whole region, starting at offset 0
    public RegionOut(MemorySegment region) {
        if (region == null) {
            throw new IllegalArgumentException("region must not be null");
        }
        this.region = region;
        this.capacity = region.byteSize();
        this.pos = 0;
        this.overflow = false;
    }

    // returns the number of bytes written so far
    public long written() {
        return pos;
    }

    // returns true when a write request exceeded the remaining capacity
    public boolean overflowed() {
        return overflow;
    }

    // clears the overflow flag, allowing a small follow-up write. used by the
    // "%ex" node to append the truncation marker after the region filled up
    public void clearOverflow() {
        overflow = false;
    }

    // utf-8 encodes a substring of the given string directly into the region
    public void writeUtf8(String s, int from, int to) {
        if (s == null) {
            return;
        }
        int end = Math.min(to, s.length());
        for (int i = Math.max(0, from); i < end; ) {
            char c = s.charAt(i);
            if (c < 0x80) {
                if (!ensure(1)) {
                    return;
                }
                putByte((byte) c);
                i++;
            } else if (c < 0x800) {
                if (!ensure(2)) {
                    return;
                }
                putByte((byte) (0xC0 | (c >> 6)));
                putByte((byte) (0x80 | (c & 0x3F)));
                i++;
            } else if (Character.isHighSurrogate(c) && i + 1 < end && Character.isLowSurrogate(s.charAt(i + 1))) {
                int cp = Character.toCodePoint(c, s.charAt(i + 1));
                if (!ensure(4)) {
                    return;
                }
                putByte((byte) (0xF0 | (cp >> 18)));
                putByte((byte) (0x80 | ((cp >> 12) & 0x3F)));
                putByte((byte) (0x80 | ((cp >> 6) & 0x3F)));
                putByte((byte) (0x80 | (cp & 0x3F)));
                i += 2;
            } else if (Character.isSurrogate(c)) {
                // a lone surrogate has no pair here, write the replacement char
                if (!ensure(3)) {
                    return;
                }
                putByte((byte) 0xEF);
                putByte((byte) 0xBF);
                putByte((byte) 0xBD);
                i++;
            } else {
                if (!ensure(3)) {
                    return;
                }
                putByte((byte) (0xE0 | (c >> 12)));
                putByte((byte) (0x80 | ((c >> 6) & 0x3F)));
                putByte((byte) (0x80 | (c & 0x3F)));
                i++;
            }
        }
    }

    // utf-8 encodes the whole string directly into the region
    public void writeUtf8(String s) {
        writeUtf8(s, 0, s.length());
    }

    @Override
    public void write(int b) {
        if (ensure(1)) {
            putByte((byte) b);
        }
    }

    @Override
    public void write(byte[] b) {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        if (b == null) {
            throw new NullPointerException("bytes must not be null");
        }
        if (off < 0 || len < 0 || off > b.length - len) {
            throw new IndexOutOfBoundsException("invalid range, off : " + off + ", len : " + len
                    + ", bytes : " + b.length);
        }
        if (len == 0) {
            return;
        }
        if (!ensure(len)) {
            return;
        }
        MemorySegment.copy(b, off, region, ValueLayout.JAVA_BYTE, pos, len);
        pos += len;
    }

    @Override
    public RegionOut append(char c) {
        if (c < 0x80) {
            if (ensure(1)) {
                putByte((byte) c);
            }
        } else if (c < 0x800) {
            if (ensure(2)) {
                putByte((byte) (0xC0 | (c >> 6)));
                putByte((byte) (0x80 | (c & 0x3F)));
            }
        } else if (Character.isSurrogate(c)) {
            // a lone surrogate has no pair here, write the replacement char
            if (ensure(3)) {
                putByte((byte) 0xEF);
                putByte((byte) 0xBF);
                putByte((byte) 0xBD);
            }
        } else {
            if (ensure(3)) {
                putByte((byte) (0xE0 | (c >> 12)));
                putByte((byte) (0x80 | ((c >> 6) & 0x3F)));
                putByte((byte) (0x80 | (c & 0x3F)));
            }
        }
        return this;
    }

    @Override
    public RegionOut append(CharSequence csq) {
        return append(csq, 0, csq.length());
    }

    @Override
    public RegionOut append(CharSequence csq, int start, int end) {
        if (csq == null) {
            csq = "null";
        }
        if (csq instanceof String s) {
            writeUtf8(s, start, end);
            return this;
        }
        int sEnd = Math.min(end, csq.length());
        for (int i = Math.max(0, start); i < sEnd; i++) {
            char c = csq.charAt(i);
            if (!appendChar(c)) {
                return this;
            }
        }
        return this;
    }

    // encodes a single char, returns false when the region overflowed
    private boolean appendChar(char c) {
        if (c < 0x80) {
            if (!ensure(1)) {
                return false;
            }
            putByte((byte) c);
        } else if (c < 0x800) {
            if (!ensure(2)) {
                return false;
            }
            putByte((byte) (0xC0 | (c >> 6)));
            putByte((byte) (0x80 | (c & 0x3F)));
        } else if (Character.isSurrogate(c)) {
            if (!ensure(3)) {
                return false;
            }
            putByte((byte) 0xEF);
            putByte((byte) 0xBF);
            putByte((byte) 0xBD);
        } else {
            if (!ensure(3)) {
                return false;
            }
            putByte((byte) (0xE0 | (c >> 12)));
            putByte((byte) (0x80 | ((c >> 6) & 0x3F)));
            putByte((byte) (0x80 | (c & 0x3F)));
        }
        return true;
    }

    // checks capacity first, never throws on overflow
    private boolean ensure(long n) {
        if (overflow) {
            return false;
        }
        if (n < 0 || n > capacity - pos) {
            overflow = true;
            return false;
        }
        return true;
    }

    private void putByte(byte b) {
        NativeSegmentAccess.setByte(region, pos, b);
        pos++;
    }

    @Override
    public void flush() {
        // region is already the final destination, nothing to flush
    }

    @Override
    public void close() throws IOException {
        // no owned resource to release
    }
}