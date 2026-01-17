package io.jingproject.common.conf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class JsonCfgReader {
    private final InputStream input;
    private final Deque<Map<String, Cfg>> previous = new ArrayDeque<>();
    private final Deque<String> keys = new ArrayDeque<>();
    private Map<String, Cfg> current = new HashMap<>();
    private String key = null;

    public JsonCfgReader(InputStream input) {
        this.input = input;
    }

    enum State {
        INITIAL, // 初始状态，想要读取下一个'{'作为对象的开始
        OBJ_START, // 已经读取到了对象的开始'{'，想要找下一个key的启动'"'或者对象的结束'}'
        STR_ARR_OBJ_END, // 已经读取到了字符串，数组，对象的结束'"' ']' '}'，接下来找'}'或者','
        EXPECT_KEY, // 想要读取下一个字符串key的起始双引号
        KEY_START, // 已经读取到了key的开头'"'，接下来开始解析key的部分
        KEY_END, // 已经读取到了key的结束'"'，接下来寻找分隔符':'
        EXPECT_VALUE, // 已经读取到了':'，接下来找值的起始部分，可能是'"' '[' '{'中的一种
        STR_START, // 已经读取到了值的起始'"'，接下来要找值结束'"'
        ARR_START, // 已经读取到了数组的起始'['，接下来要读到数组的结束']'
    }

    // 构建出来的Reader只能parse一次，继续parse是ub
    public CfgObject parse() throws IOException {
        State state = State.INITIAL;
        int b;
        try(ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for( ; ; ) {
                switch (state) {
                    case INITIAL -> {
                        b = CfgUtil.ignoreTillEOF(input, ' ', '\t', '\r', '\n');
                        if(b != '{') {
                            throw new CfgException("Corrupted json configuration");
                        }
                        state = State.OBJ_START;
                    }
                    case OBJ_START -> {
                        if(key != null) {
                            keys.addLast(key);
                            key = null;
                            previous.addLast(current);
                            current = new HashMap<>();
                        }
                        b = CfgUtil.ignoreTillEOF(input, ' ', '\t', '\r', '\n');
                        if(b == '}') {
                            state = State.STR_ARR_OBJ_END;
                        } else if(b == '"') {
                            state = State.KEY_START;
                        } else {
                            throw new CfgException("Corrupted json configuration");
                        }
                    }
                    case STR_ARR_OBJ_END -> {
                        Map<String, Cfg> parent = previous.pollLast();
                        if(parent == null) {
                            return new CfgObject(current);
                        } else {
                            key = keys.pollLast();
                            if (parent.putIfAbsent(key, new CfgObject(current)) != null) {
                                throw new CfgException("Duplicate key: " + key);
                            }
                            current = parent;
                            key = null;
                            b = CfgUtil.ignoreTillEOF(input, ' ', '\t', '\r', '\n');
                            if(b == ',') {
                                state = State.EXPECT_KEY;
                            } else if(b != '}') {
                                throw new CfgException("Corrupted json configuration");
                            }
                        }
                    }
                    case EXPECT_KEY -> {
                        b = CfgUtil.ignoreTillEOF(input, ' ', '\t', '\r', '\n');
                        if(b == '"') {
                            state = State.KEY_START;
                        } else {
                            throw new CfgException("Corrupted json configuration");
                        }
                    }
                    case KEY_START -> {
                        CfgUtil.search(input, output, '"');
                        byte[] keyBytes = output.toByteArray();
                        output.reset();
                        key = CfgUtil.readCfgKey(keyBytes);
                        state = State.KEY_END;
                    }
                    case KEY_END -> {
                        b = CfgUtil.ignoreTillEOF(input, ' ', '\t', '\r', '\n');
                        if(b != ':') {
                            throw new CfgException("Corrupted json configuration");
                        }
                        state = State.EXPECT_VALUE;
                    }
                    case EXPECT_VALUE -> {
                        b = CfgUtil.ignoreTillEOF(input, ' ', '\t', '\r', '\n');
                        if(b == '"') {
                            state = State.STR_START;
                        } else if(b == '[') {
                            state = State.ARR_START;
                        } else if(b == '{') {
                            state = State.OBJ_START;
                        } else {
                            throw new CfgException("Corrupted json configuration");
                        }
                    }
                    case STR_START -> {
                        readJsonStrValue(input, output);
                        String str = output.toString(StandardCharsets.UTF_8);
                        output.reset();
                        if (current.putIfAbsent(key, new CfgItem(str)) != null) {
                            throw new CfgException("Duplicate key: " + key);
                        }
                        key = null;
                        state = State.STR_ARR_OBJ_END;
                    }
                    case ARR_START -> {
                        List<String> strList = new ArrayList<>();
                        for( ; ; ) {
                            b = CfgUtil.ignoreTillEOF(input, ' ', '\t', '\r', '\n');
                            if(b != '"') {
                                throw new CfgException("Corrupted json configuration");
                            }
                            readJsonStrValue(input, output);
                            strList.add(output.toString(StandardCharsets.UTF_8));
                            output.reset();
                            b = CfgUtil.ignoreTillEOF(input, ' ', '\t', '\r', '\n');
                            if(b == ']') {
                                if (current.putIfAbsent(key, new CfgList(strList)) != null) {
                                    throw new CfgException("Duplicate key: " + key);
                                }
                                key = null;
                                state = State.STR_ARR_OBJ_END;
                                break ;
                            } else if(b != ',') {
                                throw new CfgException("Corrupted json configuration");
                            }
                        }

                    }
                }
            }
        }
    }

    private static void readJsonStrValue(InputStream input, ByteArrayOutputStream output) throws IOException {
        boolean escaping = false;
        int b;
        while ((b = input.read()) != -1) {
            if (escaping) {
                switch (b) {
                    case '\"' -> output.write('\"');
                    case '\\' -> output.write('\\');
                    case '/' -> output.write('/');
                    case 'b' -> output.write('\b');
                    case 'f' -> output.write('\f');
                    case 'n' -> output.write('\n');
                    case 'r' -> output.write('\r');
                    case 't' -> output.write('\t');
                    case 'u' -> {
                        int codePoint = CfgUtil.readUnicode(input, 4);
                        if(!Character.isValidCodePoint(codePoint)) {
                            throw new CfgException("Invalid code point: " + codePoint);
                        }
                        if(codePoint instanceof char highSurrogate && Character.isHighSurrogate(highSurrogate)) {
                            CfgUtil.assume(input, '\\');
                            CfgUtil.assume(input, 'u');
                            int lowSurrogateCodePoint = CfgUtil.readUnicode(input, 4);
                            if(lowSurrogateCodePoint instanceof char lowSurrogate && Character.isLowSurrogate(lowSurrogate) && Character.isSurrogatePair(highSurrogate, lowSurrogate)) {
                                codePoint = Character.toCodePoint(highSurrogate, lowSurrogate);
                            }
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
