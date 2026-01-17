package io.jingproject.common.conf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CfgUtil {
    private static final int MAX_DEPTH = 128;
    private CfgUtil() {
        throw new AssertionError();
    }

    public static void assume(InputStream input, int target) throws IOException {
        if(input == null) {
            throw new AssertionError();
        }
        int b = input.read();
        if(b == -1) {
            throw new CfgException("EOF reached");
        }
        if(b != target) {
            throw new CfgException("Invalid target, assumed: " + target + " actual: " + b);
        }
    }

    public static int search(InputStream input, OutputStream output, int... targets) throws IOException {
        if(input == null) {
            throw new AssertionError();
        }
        int b;
        while ((b = input.read()) != -1) {
            for (int target : targets) {
                if (b == target) {
                    return target;
                }
            }
            if(output != null) {
                output.write(b);
            }
        }
        throw new CfgException("EOF reached");
    }

    public static int ignore(InputStream input, int... targets) throws IOException {
        if(input == null) {
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
        if(input == null) {
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

    public static String readCfgKey(byte[] content) {
        if(content == null || content.length == 0) {
            throw new AssertionError();
        }
        for (byte b : content) {
            if (rejectKey(b)) {
                throw new CfgException("Invalid key byte: " + b);
            }
        }
        return new String(content, StandardCharsets.US_ASCII);
    }

    public static List<String> readCfgNestedKey(byte[] content) {
        if(content == null || content.length == 0) {
            throw new AssertionError();
        }
        for (byte b : content) {
            if(rejectKey(b) && b != (byte) '.') {
                throw new CfgException("Invalid key byte: " + b);
            }
        }
        List<String> r = new ArrayList<>();
        int start = 0;
        for(int i = 0; i < content.length; i++) {
            if(content[i] == '.') {
                if(i == start) {
                    throw new CfgException("Invalid consecutive delimiters");
                }
                r.add(new String(content, start, i - start, StandardCharsets.US_ASCII));
                if(r.size() > MAX_DEPTH) {
                    throw new CfgException("Maximum nesting depth exceeded");
                }
                start = i + 1;
            }
        }
        if (start < content.length) {
            r.add(new String(content, start, content.length - start, StandardCharsets.US_ASCII));
            if(r.size() > MAX_DEPTH) {
                throw new CfgException("Maximum nesting depth exceeded");
            }
            return r;
        }
        throw new CfgException("Invalid delimiters");
    }

    public static int readUnicode(InputStream input, int len) throws IOException {
        if(input == null) {
            throw new AssertionError();
        }
        if(len < 0 || len > Math.divideExact(Integer.SIZE, 4)) {
            throw new AssertionError();
        }
        int r = 0;
        for (int i = 0; i < len; i++) {
            int b = input.read();
            if(b == -1) {
                throw new CfgException("EOF reached");
            }
            if((b >= '0' && b <= '9')) {
                r = (r << 4) | (b - '0');
            } else if((b >= 'a' && b <= 'f')) {
                r = (r << 4) | (b - 'a' + 10);
            }  else if((b >= 'A' && b <= 'F')) {
                r = (r << 4) | (b - 'A' + 10);
            } else {
                throw new CfgException("Bad hex character: " + b);
            }
        }
        return r;
    }

    public static void writeUnicodeInUtf8(ByteArrayOutputStream out, int codePoint) {
        if (!Character.isValidCodePoint(codePoint)) {
            throw new CfgException("Invalid code point: " + codePoint);
        }
        if (codePoint < 0x80) {
            out.write(codePoint);
        } else if (codePoint < 0x800) {
            out.write(0xC0 | (codePoint >> 6));
            out.write(0x80 | (codePoint & 0x3F));
        } else if (codePoint < 0x10000) {
            out.write(0xE0 | (codePoint >> 12));
            out.write(0x80 | ((codePoint >> 6) & 0x3F));
            out.write(0x80 | (codePoint & 0x3F));
        } else {
            out.write(0xF0 | (codePoint >> 18));
            out.write(0x80 | ((codePoint >> 12) & 0x3F));
            out.write(0x80 | ((codePoint >> 6) & 0x3F));
            out.write(0x80 | (codePoint & 0x3F));
        }
    }
}
