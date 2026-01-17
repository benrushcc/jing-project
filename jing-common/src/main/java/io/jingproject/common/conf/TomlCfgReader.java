package io.jingproject.common.conf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class TomlCfgReader {
    private final InputStream input;
    private final CfgObject root = new CfgObject(new HashMap<>());
    private final Set<String> tables = new HashSet<>();
    private CfgObject current = root;
    private String key = null;

    public TomlCfgReader(InputStream input) {
        this.input = input;
    }

    enum State {
        INITIAL,
        COMMENT,
        TABLE_START,
        VALUE_END,
        KEY_START,
        VALUE_START,
        STR_START,
        ARR_START,
    }

    public CfgObject parse() throws IOException {
        State state = State.INITIAL;
        int b;
        try(ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for( ; ; ) {
                switch (state) {
                    case INITIAL -> {
                        b = CfgUtil.ignore(input, ' ', '\t', '\r', '\n');
                        if(b == -1) {
                            return root;
                        } else if(b == '#') {
                            state = State.COMMENT;
                        } else if(b == '[') {
                            state = State.TABLE_START;
                        } else {
                            output.write(b);
                            state = State.KEY_START;
                        }
                    }
                    case COMMENT -> {
                        CfgUtil.search(input, output, '\r', '\n');
                        String commentStr = output.toString(StandardCharsets.UTF_8);
                        output.reset();
                        commentStr.chars().forEach(c -> {
                            if (Character.getType(c) == Character.CONTROL && rejectCommentControlCharacter(c)) {
                                throw new CfgException("Corrupt toml configuration");
                            }
                        });
                        state = State.INITIAL;
                    }
                    case TABLE_START -> {
                        current = root;
                        CfgUtil.search(input, output, ']');
                        byte[] nestedKeyBytes = output.toByteArray();
                        String nestedKeyStr = new String(nestedKeyBytes, StandardCharsets.US_ASCII);
                        if(!tables.add(nestedKeyStr)) {
                            throw new CfgException("Duplicate table: " + nestedKeyStr);
                        }
                        output.reset();
                        for (String nestedKey : CfgUtil.readCfgNestedKey(nestedKeyBytes)) {
                            Map<String, Cfg> currentMap = current.value();
                            Cfg currentObj = currentMap.get(nestedKey);
                            if(currentObj == null) {
                                CfgObject cm = new CfgObject(new HashMap<>());
                                currentMap.put(nestedKey, cm);
                                current = cm;
                            } else if(currentObj instanceof CfgObject cm) {
                                current = cm;
                            } else {
                                throw new CfgException("Duplicate table: " + nestedKeyStr);
                            }
                        }
                        state = State.VALUE_END;
                    }
                    case VALUE_END -> {
                        b = CfgUtil.ignore(input, '\r', '\n', '#');
                        if(b == -1) {
                            return root;
                        } else if(b == '\r' || b == '\n') {
                            state = State.INITIAL;
                        } else if(b == '#') {
                            state = State.COMMENT;
                        } else {
                            throw new AssertionError();
                        }
                    }
                    case KEY_START -> {
                        b = CfgUtil.search(input, output, ' ', '\t', '=');
                        byte[] keyBytes = output.toByteArray();
                        output.reset();
                        key = CfgUtil.readCfgKey(keyBytes);
                        if(b != '=') {
                            b = CfgUtil.ignoreTillEOF(input, ' ', '\t');
                            if(b != '=') {
                                throw new CfgException("Corrupt toml configuration");
                            }
                        }
                        state = State.VALUE_START;
                    }
                    case VALUE_START -> {
                        b = CfgUtil.ignoreTillEOF(input, ' ', '\t');
                        if(b == '"') {
                            state = State.STR_START;
                        } else if(b == '[') {
                            state = State.ARR_START;
                        } else {
                            throw new CfgException("Corrupt toml configuration");
                        }
                    }
                    case STR_START -> {
                        readTomlStrValue(input, output);
                        String str = output.toString(StandardCharsets.UTF_8);
                        output.reset();
                        if (current.value().putIfAbsent(key, new CfgItem(str)) != null) {
                            throw new CfgException("Duplicate key: " + key);
                        }
                        key = null;
                        state = State.VALUE_END;
                    }
                    case ARR_START -> {
                        List<String> strList = new ArrayList<>();
                        for( ; ; ) {
                            b = CfgUtil.ignoreTillEOF(input, ' ', '\t');
                            if(b != '"') {
                                throw new CfgException("Corrupt toml configuration");
                            }
                            readTomlStrValue(input, output);
                            strList.add(output.toString(StandardCharsets.UTF_8));
                            output.reset();
                            b = CfgUtil.ignoreTillEOF(input, ' ', '\t');
                            if(b == ']') {
                                if (current.value().putIfAbsent(key, new CfgList(strList)) != null) {
                                    throw new CfgException("Duplicate key: " + key);
                                }
                                key = null;
                                state = State.VALUE_END;
                                break ;
                            } else if(b != ',') {
                                throw new CfgException("Corrupt toml configuration");
                            }
                        }
                    }
                }
            }
        }
    }

    // based on https://toml.io/en/v1.1.0#comment
    private static boolean rejectCommentControlCharacter(int b) {
        return (b >= 0x0000 && b <= 0x0008) || (b >= 0x000A && b <= 0x001F) || b == 0x007F;
    }

    private static void readTomlStrValue(InputStream input, ByteArrayOutputStream output) throws IOException {
        boolean escaping = false;
        int b;
        while ((b = input.read()) != -1) {
            if(escaping) {
                switch (b) {
                    case 'b' -> output.write('\b');
                    case 't' -> output.write('\t');
                    case 'n' -> output.write('\n');
                    case 'f' -> output.write('\f');
                    case 'r' -> output.write('\r');
                    case 'e' -> output.write('\u001B');
                    case '"' -> output.write('\"');
                    case '\\' -> output.write('\\');
                    case 'x' -> {
                        int codePoint = CfgUtil.readUnicode(input, 2);
                        if(!Character.isValidCodePoint(codePoint)) {
                            throw new CfgException("Invalid code point: " + codePoint);
                        }
                        if(codePoint instanceof char charCodePoint && Character.isSurrogate(charCodePoint)) {
                            throw new CfgException("Invalid surrogate code point: " + codePoint);
                        }
                        CfgUtil.writeUnicodeInUtf8(output, codePoint);
                    }
                    case 'u' -> {
                        int codePoint = CfgUtil.readUnicode(input, 4);
                        if(!Character.isValidCodePoint(codePoint)) {
                            throw new CfgException("Invalid code point: " + codePoint);
                        }
                        if(codePoint instanceof char charCodePoint && Character.isSurrogate(charCodePoint)) {
                            throw new CfgException("Invalid surrogate code point: " + codePoint);
                        }
                        CfgUtil.writeUnicodeInUtf8(output, codePoint);
                    }
                    case 'U' -> {
                        int codePoint = CfgUtil.readUnicode(input, 8);
                        if(!Character.isValidCodePoint(codePoint)) {
                            throw new CfgException("Invalid code point: " + codePoint);
                        }
                        if(codePoint instanceof char charCodePoint && Character.isSurrogate(charCodePoint)) {
                            throw new CfgException("Invalid surrogate code point: " + codePoint);
                        }
                        CfgUtil.writeUnicodeInUtf8(output, codePoint);
                    }
                    default -> throw new CfgException("Invalid escape sequence: " + b);
                }
                escaping = false;
            } else {
                if (b == '\\') {
                    escaping = true;
                } else if (b == '"') {
                    return ;
                } else {
                    output.write(b);
                }
            }
        }
        throw new CfgException("EOF reached");
    }
}
