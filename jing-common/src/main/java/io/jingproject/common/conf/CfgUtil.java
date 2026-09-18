package io.jingproject.common.conf;

import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.common.WriteBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class CfgUtil {
    private CfgUtil() {
        throw new UnsupportedOperationException("utility class");
    }

    public static void assume(InputStream input, int target) throws IOException {
        if (input == null) {
            throw new AssertionError();
        }
        int b = input.read();
        if (b == -1) {
            throw new CfgException("EOF reached");
        }
        if (b != target) {
            throw new CfgException("invalid target, assumed: " + target + " actual: " + b);
        }
    }

    public static int search(InputStream input, WriteBuffer output, int... targets) throws IOException {
        if (input == null) {
            throw new AssertionError();
        }
        int b;
        while ((b = input.read()) != -1) {
            for (int target : targets) {
                if (b == target) {
                    return target;
                }
            }
            output.writeByte((byte) b);
        }
        throw new CfgException("EOF reached");
    }

    public static int ignore(InputStream input, int... targets) throws IOException {
        if (input == null) {
            throw new AssertionError();
        }
        int b;
        while ((b = input.read()) != -1) {
            boolean matched = false;
            for (int target : targets) {
                if (b == target) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return b;
            }
        }
        return -1;
    }

    public static int ignoreTillEOF(InputStream input, int... targets) throws IOException {
        if (input == null) {
            throw new AssertionError();
        }
        int b = ignore(input, targets);
        if (b == -1) {
            throw new CfgException("EOF reached");
        }
        return b;
    }

    public static boolean rejectKey(byte b) {
        return (b < 'a' || b > 'z') && (b < 'A' || b > 'Z') && (b < '0' || b > '9') && b != '-' && b != '_';
    }

    public static String readCfgKey(byte[] data, int off, int len) {
        if (data == null || len == 0) {
            throw new CfgException("empty key");
        }
        for (int i = off; i < off + len; i++) {
            if (rejectKey(data[i])) {
                throw new CfgException("invalid key byte: " + data[i]);
            }
        }
        return new String(data, off, len, StandardCharsets.US_ASCII);
    }

    public static List<String> readCfgNestedKey(byte[] data, int off, int len, int maxDepth) {
        if (data == null || len == 0) {
            throw new CfgException("empty key");
        }
        for (int i = off; i < off + len; i++) {
            if (rejectKey(data[i]) && data[i] != (byte) '.') {
                throw new CfgException("invalid key byte: " + data[i]);
            }
        }
        List<String> r = new ArrayList<>();
        int start = off;
        for (int i = off; i < off + len; i++) {
            if (data[i] == '.') {
                if (i == start) {
                    throw new CfgException("invalid consecutive delimiters");
                }
                r.add(new String(data, start, i - start, StandardCharsets.US_ASCII));
                if (r.size() > maxDepth) {
                    throw new CfgException("maximum nesting depth exceeded");
                }
                start = i + 1;
            }
        }
        if (start < off + len) {
            r.add(new String(data, start, off + len - start, StandardCharsets.US_ASCII));
            if (r.size() > maxDepth) {
                throw new CfgException("maximum nesting depth exceeded");
            }
            return r;
        }
        throw new CfgException("invalid delimiters");
    }

    public static int readUnicode(InputStream input, int len) throws IOException {
        if (input == null) {
            throw new AssertionError();
        }
        if (len < 0 || len > Math.divideExact(Integer.SIZE, 4)) {
            throw new AssertionError();
        }
        int r = 0;
        for (int i = 0; i < len; i++) {
            int b = input.read();
            if (b == -1) {
                throw new CfgException("EOF reached");
            }
            if ((b >= '0' && b <= '9')) {
                r = (r << 4) | (b - '0');
            } else if ((b >= 'a' && b <= 'f')) {
                r = (r << 4) | (b - 'a' + 10);
            } else if ((b >= 'A' && b <= 'F')) {
                r = (r << 4) | (b - 'A' + 10);
            } else {
                throw new CfgException("bad hex character: " + b);
            }
        }
        return r;
    }

    public static String readJsonNumber(InputStream input, int firstByte, HeapWriteBuffer buf) throws IOException {
        PushbackInputStream p = asPushback(input);
        buf.reset();
        int b = firstByte;
        if (b == '-') {
            buf.writeByte((byte) b);
            b = p.read();
        }
        if (b >= '0' && b <= '9') {
            buf.writeByte((byte) b);
            if (b == '0') {
                b = p.read();
                if (b >= '0' && b <= '9') {
                    throw new CfgException("invalid json number: leading zero: " + b);
                }
            } else {
                b = p.read();
                while (b >= '0' && b <= '9') {
                    buf.writeByte((byte) b);
                    b = p.read();
                }
            }
        } else {
            throw new CfgException("invalid json number: " + b);
        }
        if (b == '.') {
            buf.writeByte((byte) b);
            b = p.read();
            if (b < '0' || b > '9') {
                throw new CfgException("invalid json number: " + b);
            }
            while (b >= '0' && b <= '9') {
                buf.writeByte((byte) b);
                b = p.read();
            }
        }
        if (b == 'e' || b == 'E') {
            buf.writeByte((byte) b);
            b = p.read();
            if (b == '+' || b == '-') {
                buf.writeByte((byte) b);
                b = p.read();
            }
            if (b < '0' || b > '9') {
                throw new CfgException("invalid json number: " + b);
            }
            while (b >= '0' && b <= '9') {
                buf.writeByte((byte) b);
                b = p.read();
            }
        }
        if (b != -1) {
            p.unread(b);
        }
        return new String(buf.rawByteArray(), 0, buf.intPosition(), StandardCharsets.US_ASCII);
    }

    public static String readJsonBool(InputStream input, int firstByte, HeapWriteBuffer buf) throws IOException {
        PushbackInputStream p = asPushback(input);
        buf.reset();
        String literal;
        if (firstByte == 't') {
            literal = "true";
        } else if (firstByte == 'f') {
            literal = "false";
        } else {
            throw new CfgException("invalid json boolean: " + firstByte);
        }
        for (int i = 1; i < literal.length(); i++) {
            int b = p.read();
            if (b != literal.charAt(i)) {
                throw new CfgException("invalid json boolean: " + b);
            }
        }
        return literal;
    }

    public static String readTomlNumber(InputStream input, int firstByte, HeapWriteBuffer buf) throws IOException {
        PushbackInputStream p = asPushback(input);
        buf.reset();
        boolean negative = false;
        boolean hasSign = false;
        int b = firstByte;
        if (b == '-' || b == '+') {
            hasSign = true;
            negative = (b == '-');
            b = p.read();
            if (b == -1) {
                throw new CfgException("invalid toml number: " + b);
            }
        }
        if (b == 'i' || b == 'n') {
            return readTomlInfNan(p, negative, b, buf);
        }
        if (b == '0') {
            int d = p.read();
            if (isRadixPrefix(d)) {
                if (hasSign) {
                    throw new CfgException("invalid toml integer: sign not allowed for radix literal");
                }
                return accumulateRadix(p, d);
            }
            if (d >= '0' && d <= '9') {
                throw new CfgException("invalid toml integer: leading zero: " + d);
            }
            if (d == '_') {
                throw new CfgException("invalid toml integer: misplaced underscore: " + d);
            }
            if (d == '.' || d == 'e' || d == 'E') {
                if (negative) {
                    buf.writeByte((byte) '-');
                }
                buf.writeByte((byte) '0');
                return readTomlFloatContinue(p, d, buf);
            }
            if (d != -1) {
                p.unread(d);
            }
            return "0";
        }
        if (b >= '1' && b <= '9') {
            if (negative) {
                buf.writeByte((byte) '-');
            }
            buf.writeByte((byte) b);
            long acc = -(b - '0');
            boolean overflow = false;
            boolean lastWasUnderscore = false;
            int d = p.read();
            while (d >= '0' && d <= '9' || d == '_') {
                if (d == '_') {
                    if (lastWasUnderscore) {
                        throw new CfgException("invalid toml integer: adjacent underscore");
                    }
                    lastWasUnderscore = true;
                } else {
                    lastWasUnderscore = false;
                    buf.writeByte((byte) d);
                    if (!overflow) {
                        int digit = d - '0';
                        if (acc < Long.MIN_VALUE / 10) {
                            overflow = true;
                        } else {
                            acc = acc * 10;
                            if (acc < Long.MIN_VALUE + digit) {
                                overflow = true;
                            } else {
                                acc = acc - digit;
                            }
                        }
                    }
                }
                d = p.read();
            }
            if (lastWasUnderscore) {
                throw new CfgException("invalid toml integer: trailing underscore");
            }
            if (d == '.' || d == 'e' || d == 'E') {
                return readTomlFloatContinue(p, d, buf);
            }
            if (d != -1) {
                p.unread(d);
            }
            if (overflow) {
                throw new CfgException("integer literal out of range: " + new String(buf.rawByteArray(), 0, buf.intPosition(), StandardCharsets.US_ASCII));
            }
            long value;
            if (negative) {
                value = acc;
            } else if (acc == Long.MIN_VALUE) {
                throw new CfgException("integer literal out of range");
            } else {
                value = -acc;
            }
            return Long.toString(value);
        }
        throw new CfgException("invalid toml number: " + b);
    }

    public static String readTomlWord(InputStream input, int firstByte, HeapWriteBuffer buf) throws IOException {
        PushbackInputStream p = asPushback(input);
        buf.reset();
        if (firstByte == 't' || firstByte == 'f') {
            String literal = firstByte == 't' ? "true" : "false";
            for (int i = 1; i < literal.length(); i++) {
                int b = p.read();
                if (b != literal.charAt(i)) {
                    throw new CfgException("invalid toml value: " + b);
                }
            }
            return literal;
        }
        if (firstByte == 'i' || firstByte == 'n') {
            return readTomlInfNan(p, false, firstByte, buf);
        }
        throw new CfgException("invalid toml value: " + firstByte);
    }

    private static PushbackInputStream asPushback(InputStream input) {
        if (input instanceof PushbackInputStream p) {
            return p;
        }
        throw new AssertionError();
    }

    private static boolean isRadixPrefix(int b) {
        return b == 'x' || b == 'X' || b == 'o' || b == 'O' || b == 'b' || b == 'B';
    }

    private static String readTomlInfNan(PushbackInputStream p, boolean negative, int firstByte, HeapWriteBuffer buf) throws IOException {
        buf.reset();
        buf.writeByte((byte) firstByte);
        int b = p.read();
        while (b >= 'a' && b <= 'z' || b >= 'A' && b <= 'Z') {
            buf.writeByte((byte) b);
            b = p.read();
        }
        if (b != -1) {
            p.unread(b);
        }
        String word = new String(buf.rawByteArray(), 0, buf.intPosition(), StandardCharsets.US_ASCII);
        if (word.equals("inf")) {
            if (negative) {
                return "-Infinity";
            }
            return "Infinity";
        }
        if (word.equals("nan")) {
            return "NaN";
        }
        throw new CfgException("invalid toml value: " + word);
    }

    private static String accumulateRadix(PushbackInputStream p, int prefixByte) throws IOException {
        int radix;
        if (prefixByte == 'x' || prefixByte == 'X') {
            radix = 16;
        } else if (prefixByte == 'o' || prefixByte == 'O') {
            radix = 8;
        } else {
            radix = 2;
        }
        long acc = 0;
        boolean hasDigit = false;
        boolean lastWasUnderscore = false;
        int b = p.read();
        while (true) {
            int digit = radixDigitValue(b, radix);
            if (digit >= 0) {
                hasDigit = true;
                lastWasUnderscore = false;
                if (acc < Long.MIN_VALUE / radix) {
                    throw new CfgException("integer literal out of range: " + b);
                }
                acc = acc * radix;
                if (acc < Long.MIN_VALUE + digit) {
                    throw new CfgException("integer literal out of range: " + b);
                }
                acc = acc - digit;
            } else if (b == '_') {
                if (lastWasUnderscore || !hasDigit) {
                    throw new CfgException("invalid toml integer: misplaced underscore");
                }
                lastWasUnderscore = true;
            } else {
                break;
            }
            b = p.read();
        }
        if (lastWasUnderscore) {
            throw new CfgException("invalid toml integer: trailing underscore");
        }
        if (!hasDigit) {
            throw new CfgException("invalid toml integer: missing digits after radix prefix");
        }
        if (b != -1) {
            p.unread(b);
        }
        if (acc == Long.MIN_VALUE) {
            throw new CfgException("integer literal out of range");
        }
        return Long.toString(-acc);
    }

    private static int radixDigitValue(int b, int radix) {
        if (b >= '0' && b <= '9') {
            int v = b - '0';
            return v < radix ? v : -1;
        }
        if (radix == 16) {
            if (b >= 'a' && b <= 'f') {
                return b - 'a' + 10;
            }
            if (b >= 'A' && b <= 'F') {
                return b - 'A' + 10;
            }
        }
        return -1;
    }

    private static String readTomlFloatContinue(PushbackInputStream p, int dotOrExp, HeapWriteBuffer buf) throws IOException {
        int b = dotOrExp;
        if (b == '.') {
            buf.writeByte((byte) b);
            b = p.read();
            if (b < '0' || b > '9') {
                throw new CfgException("invalid toml float: " + b);
            }
            boolean lastWasUnderscore = false;
            while (b >= '0' && b <= '9' || b == '_') {
                if (b == '_') {
                    if (lastWasUnderscore) {
                        throw new CfgException("invalid toml float: adjacent underscore: " + b);
                    }
                    lastWasUnderscore = true;
                } else {
                    lastWasUnderscore = false;
                    buf.writeByte((byte) b);
                }
                b = p.read();
            }
            if (lastWasUnderscore) {
                throw new CfgException("invalid toml float: trailing underscore");
            }
        }
        if (b == 'e' || b == 'E') {
            buf.writeByte((byte) b);
            b = p.read();
            if (b == '+' || b == '-') {
                buf.writeByte((byte) b);
                b = p.read();
            }
            if (b < '0' || b > '9') {
                throw new CfgException("invalid toml float: " + b);
            }
            while (b >= '0' && b <= '9') {
                buf.writeByte((byte) b);
                b = p.read();
            }
            if (b == '_') {
                throw new CfgException("invalid toml float: underscore in exponent");
            }
        }
        if (b != -1) {
            p.unread(b);
        }
        return new String(buf.rawByteArray(), 0, buf.intPosition(), StandardCharsets.US_ASCII);
    }
}