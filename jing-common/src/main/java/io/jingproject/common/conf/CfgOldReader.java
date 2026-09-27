package io.jingproject.common.conf;

import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.common.conf.Cfg.CfgItem;
import io.jingproject.common.conf.Cfg.CfgList;
import io.jingproject.common.conf.Cfg.CfgObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public sealed interface CfgOldReader {
    CfgObject parse(int maxDepth) throws IOException;

    final class JsonCfgOldReader implements CfgOldReader {
        private record Frame(Map<String, Cfg> parent, String key) {}

        private final PushbackInputStream input;
        private final HeapWriteBuffer out = new HeapWriteBuffer(64);
        private final Deque<Frame> frames = new ArrayDeque<>();
        private Map<String, Cfg> current = new HashMap<>();
        private String key = null;

        public JsonCfgOldReader(InputStream input) {
            this.input = new PushbackInputStream(input);
        }

        private static void readJsonStrValue(InputStream input, HeapWriteBuffer output) throws IOException {
            boolean escaping = false;
            int b;
            while ((b = input.read()) != -1) {
                if (escaping) {
                    switch (b) {
                        case '\"' -> output.writeByte((byte) '\"');
                        case '\\' -> output.writeByte((byte) '\\');
                        case '/' -> output.writeByte((byte) '/');
                        case 'b' -> output.writeByte((byte) '\b');
                        case 'f' -> output.writeByte((byte) '\f');
                        case 'n' -> output.writeByte((byte) '\n');
                        case 'r' -> output.writeByte((byte) '\r');
                        case 't' -> output.writeByte((byte) '\t');
                        case 'u' -> {
                            int codePoint = CfgOldUtil.readUnicode(input, 4);
                            if (codePoint instanceof char highSurrogate && Character.isHighSurrogate(highSurrogate)) {
                                CfgOldUtil.assume(input, '\\');
                                CfgOldUtil.assume(input, 'u');
                                int lowSurrogateCodePoint = CfgOldUtil.readUnicode(input, 4);
                                if (lowSurrogateCodePoint instanceof char lowSurrogate && Character.isLowSurrogate(lowSurrogate) && Character.isSurrogatePair(highSurrogate, lowSurrogate)) {
                                    codePoint = Character.toCodePoint(highSurrogate, lowSurrogate);
                                }
                            }
                            output.writeCodePointInUtf8(codePoint);
                        }
                        default -> throw new CfgException("invalid escape sequence: " + b);
                    }
                    escaping = false;
                } else {
                    if (b == '\\') {
                        escaping = true;
                    } else if (b == '"') {
                        return;
                    } else {
                        output.writeByte((byte) b);
                    }
                }
            }
            throw new CfgException("EOF reached");
        }

        // the constructed reader can only parse once, further parse is undefined behavior
        public CfgObject parse(int maxDepth) throws IOException {
            if (maxDepth <= 0) {
                throw new CfgException("invalid max depth: " + maxDepth);
            }
            State state = State.INITIAL;
            while (state != State.DONE) {
                state = switch (state) {
                    case INITIAL -> onInitial();
                    case OBJ_START -> onObjStart(maxDepth);
                    case AFTER_VALUE -> onAfterValue();
                    case EXPECT_KEY -> onExpectKey();
                    case KEY_READ -> onKeyRead();
                    case COLON -> onColon();
                    case EXPECT_VALUE -> onExpectValue();
                    case ARRAY -> onArray();
                    case DONE -> throw new AssertionError();
                };
            }
            int tail = CfgOldUtil.ignore(input, ' ', '\t', '\r', '\n');
            if (tail != -1) {
                throw new CfgException("corrupted json configuration");
            }
            return new CfgObject(current);
        }

        private State onInitial() throws IOException {
            int b = CfgOldUtil.ignoreTillEOF(input, ' ', '\t', '\r', '\n');
            if (b != '{') {
                throw new CfgException("corrupted json configuration");
            }
            return State.OBJ_START;
        }

        private State onObjStart(int maxDepth) throws IOException {
            if (key != null) {
                if (frames.size() + 2 > maxDepth) {
                    throw new CfgException("maximum nesting depth exceeded");
                }
                frames.addLast(new Frame(current, key));
                current = new HashMap<>();
                key = null;
            }
            int b = CfgOldUtil.ignoreTillEOF(input, ' ', '\t', '\r', '\n');
            if (b == '}' && key == null && frames.isEmpty()) {
                // root object closed, exit the parse loop
                return State.DONE;
            }
            if (b == '}') {
                // empty nested object, resolve the frame that was pushed above
                if (!frames.isEmpty()) {
                    Frame frame = frames.pollLast();
                    if (frame.parent().putIfAbsent(frame.key(), new CfgObject(current)) != null) {
                        throw new CfgException("duplicate key: " + frame.key());
                    }
                    current = frame.parent();
                }
                return State.AFTER_VALUE;
            }
            if (b == '"') {
                return State.KEY_READ;
            }
            throw new CfgException("corrupted json configuration");
        }

        private State onAfterValue() throws IOException {
            int b = CfgOldUtil.ignore(input, ' ', '\t', '\r', '\n');
            if (b == ',') {
                return State.EXPECT_KEY;
            }
            if (b == '}') {
                Frame frame = frames.pollLast();
                if (frame == null) {
                    return State.DONE;
                }
                key = frame.key();
                if (frame.parent().putIfAbsent(key, new CfgObject(current)) != null) {
                    throw new CfgException("duplicate key: " + key);
                }
                current = frame.parent();
                key = null;
                return State.AFTER_VALUE;
            }
            if (b == -1) {
                throw new CfgException("EOF reached");
            }
            throw new CfgException("corrupted json configuration");
        }

        private State onExpectKey() throws IOException {
            int b = CfgOldUtil.ignoreTillEOF(input, ' ', '\t', '\r', '\n');
            if (b == '"') {
                return State.KEY_READ;
            }
            throw new CfgException("corrupted json configuration");
        }

        private State onKeyRead() throws IOException {
            CfgOldUtil.search(input, out, '"');
            key = CfgOldUtil.readCfgKey(out.rawByteArray(), 0, out.intPosition());
            out.reset();
            return State.COLON;
        }

        private State onColon() throws IOException {
            int b = CfgOldUtil.ignoreTillEOF(input, ' ', '\t', '\r', '\n');
            if (b != ':') {
                throw new CfgException("corrupted json configuration");
            }
            return State.EXPECT_VALUE;
        }

        private State onExpectValue() throws IOException {
            int b = CfgOldUtil.ignoreTillEOF(input, ' ', '\t', '\r', '\n');
            if (b == '{') {
                return State.OBJ_START;
            }
            if (b == '[') {
                return State.ARRAY;
            }
            storeItem(readScalarValue(b));
            return State.AFTER_VALUE;
        }

        private State onArray() throws IOException {
            int b = CfgOldUtil.ignoreTillEOF(input, ' ', '\t', '\r', '\n');
            if (b == ']') {
                storeList(new ArrayList<>());
                return State.AFTER_VALUE;
            }
            List<String> list = new ArrayList<>();
            for (;;) {
                list.add(readScalarValue(b));
                b = CfgOldUtil.ignoreTillEOF(input, ' ', '\t', '\r', '\n');
                if (b == ']') {
                    break;
                }
                if (b != ',') {
                    throw new CfgException("corrupted json configuration");
                }
                b = CfgOldUtil.ignoreTillEOF(input, ' ', '\t', '\r', '\n');
            }
            storeList(list);
            return State.AFTER_VALUE;
        }

        private String readScalarValue(int firstByte) throws IOException {
            if (firstByte == '"') {
                readJsonStrValue(input, out);
                String str = new String(out.rawByteArray(), 0, out.intPosition(), StandardCharsets.UTF_8);
                out.reset();
                return str;
            }
            if (firstByte == '-' || (firstByte >= '0' && firstByte <= '9')) {
                String num = CfgOldUtil.readJsonNumber(input, firstByte, out);
                out.reset();
                return num;
            }
            if (firstByte == 't' || firstByte == 'f') {
                String bool = CfgOldUtil.readJsonBool(input, firstByte, out);
                out.reset();
                return bool;
            }
            throw new CfgException("corrupted json configuration");
        }

        private void storeItem(String value) {
            if (current.putIfAbsent(key, new CfgItem(value)) != null) {
                throw new CfgException("duplicate key: " + key);
            }
            key = null;
        }

        private void storeList(List<String> value) {
            if (current.putIfAbsent(key, new CfgList(value)) != null) {
                throw new CfgException("duplicate key: " + key);
            }
            key = null;
        }

        enum State {
            INITIAL,       // find the opening '{' of the root object
            OBJ_START,     // entering an object: push a frame if a key is pending, expect '}' or '"'
            AFTER_VALUE,   // a value just completed: expect ',' or '}' (root '}' -> DONE)
            EXPECT_KEY,    // expect the opening '"' of a key
            KEY_READ,      // reading the key content up to the closing '"'
            COLON,         // expect the ':' separator
            EXPECT_VALUE,  // dispatch on the first byte of a value
            ARRAY,         // parsing array elements
            DONE,          // root object finished, exit the loop
        }
    }

    final class PropertiesCfgOldReader implements CfgOldReader {
        private final InputStream input;

        public PropertiesCfgOldReader(InputStream input) {
            this.input = input;
        }

        private static Cfg buildCfgObject(String value) {
            if (value.startsWith("[") && value.endsWith("]")) {
                String arrStr = value.substring(1, Math.subtractExact(value.length(), 1));
                List<String> arrItems = new ArrayList<>();
                for (String item : arrStr.split(",")) {
                    if (!item.isBlank()) {
                        arrItems.add(item);
                    }
                }
                return new CfgList(arrItems);
            }
            return new CfgItem(value);
        }

        public CfgObject parse(int maxDepth) throws IOException {
            if (maxDepth <= 0) {
                throw new CfgException("invalid max depth: " + maxDepth);
            }
            CfgObject r = new CfgObject(new HashMap<>());
            CfgObject current = r;
            Properties prop = new Properties();
            try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                prop.load(reader);
                for (String key : prop.stringPropertyNames()) {
                    if (key.isBlank()) {
                        continue;
                    }
                    String value = prop.getProperty(key);
                    if (value.isBlank()) {
                        continue;
                    }
                    byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
                    List<String> nestedKeys = CfgOldUtil.readCfgNestedKey(keyBytes, 0, keyBytes.length, maxDepth);
                    switch (nestedKeys.size()) {
                        case 0 -> throw new CfgException("invalid key: " + key);
                        case 1 -> {
                            if (current.value().putIfAbsent(nestedKeys.getLast(), buildCfgObject(value)) != null) {
                                throw new CfgException("duplicate key: " + key);
                            }
                        }
                        default -> {
                            for (String nestedKey : nestedKeys.subList(0, nestedKeys.size() - 1)) {
                                Map<String, Cfg> currentMap = current.value();
                                Cfg currentObj = currentMap.get(nestedKey);
                                if (currentObj == null) {
                                    CfgObject cm = new CfgObject(new HashMap<>());
                                    currentMap.put(nestedKey, cm);
                                    current = cm;
                                } else if (currentObj instanceof CfgObject cm) {
                                    current = cm;
                                } else {
                                    throw new CfgException("duplicate key: " + key);
                                }
                            }
                            if (current.value().putIfAbsent(nestedKeys.getLast(), buildCfgObject(value)) != null) {
                                throw new CfgException("duplicate key: " + key);
                            }
                            current = r;
                        }
                    }
                }
                return r;
            }
        }
    }

    final class TomlCfgOldReader implements CfgOldReader {
        private final PushbackInputStream input;
        private final HeapWriteBuffer out = new HeapWriteBuffer(64);
        private final CfgObject root = new CfgObject(new HashMap<>());
        private final Set<String> tables = new HashSet<>();
        private CfgObject current = root;
        private String key = null;

        public TomlCfgOldReader(InputStream input) {
            this.input = new PushbackInputStream(input);
        }

        // based on https://toml.io/en/v1.1.0#comment
        private static boolean rejectCommentControlCharacter(int b) {
            return (b >= 0x0000 && b <= 0x0008) || (b >= 0x000A && b <= 0x001F) || b == 0x007F;
        }

        private static void readTomlStrValue(InputStream input, HeapWriteBuffer output) throws IOException {
            boolean escaping = false;
            int b;
            while ((b = input.read()) != -1) {
                if (escaping) {
                    switch (b) {
                        case 'b' -> output.writeByte((byte) '\b');
                        case 't' -> output.writeByte((byte) '\t');
                        case 'n' -> output.writeByte((byte) '\n');
                        case 'f' -> output.writeByte((byte) '\f');
                        case 'r' -> output.writeByte((byte) '\r');
                        case 'e' -> output.writeByte((byte) '\u001B');
                        case '"' -> output.writeByte((byte) '\"');
                        case '\\' -> output.writeByte((byte) '\\');
                        case 'x' -> {
                            int codePoint = CfgOldUtil.readUnicode(input, 2);
                            if (codePoint instanceof char charCodePoint && Character.isSurrogate(charCodePoint)) {
                                throw new CfgException("invalid surrogate code point: " + codePoint);
                            }
                            output.writeCodePointInUtf8(codePoint);
                        }
                        case 'u' -> {
                            int codePoint = CfgOldUtil.readUnicode(input, 4);
                            if (codePoint instanceof char charCodePoint && Character.isSurrogate(charCodePoint)) {
                                throw new CfgException("invalid surrogate code point: " + codePoint);
                            }
                            output.writeCodePointInUtf8(codePoint);
                        }
                        case 'U' -> {
                            int codePoint = CfgOldUtil.readUnicode(input, 8);
                            if (!Character.isValidCodePoint(codePoint)) {
                                throw new CfgException("invalid code point: " + codePoint);
                            }
                            if (codePoint instanceof char charCodePoint && Character.isSurrogate(charCodePoint)) {
                                throw new CfgException("invalid surrogate code point: " + codePoint);
                            }
                            output.writeCodePointInUtf8(codePoint);
                        }
                        default -> throw new CfgException("invalid escape sequence: " + b);
                    }
                    escaping = false;
                } else {
                    if (b == '\\') {
                        escaping = true;
                    } else if (b == '"') {
                        return;
                    } else {
                        output.writeByte((byte) b);
                    }
                }
            }
            throw new CfgException("EOF reached");
        }

        public CfgObject parse(int maxDepth) throws IOException {
            if (maxDepth <= 0) {
                throw new CfgException("invalid max depth: " + maxDepth);
            }
            State state = State.INITIAL;
            while (state != State.DONE) {
                state = switch (state) {
                    case INITIAL -> onInitial();
                    case COMMENT -> onComment();
                    case TABLE_START -> onTableStart(maxDepth);
                    case VALUE_END -> onValueEnd();
                    case KEY_READ -> onKeyRead();
                    case VALUE_START -> onValueStart();
                    case ARRAY -> onArray();
                    case DONE -> throw new AssertionError();
                };
            }
            return root;
        }

        private State onInitial() throws IOException {
            int b = CfgOldUtil.ignore(input, ' ', '\t', '\r', '\n');
            if (b == -1) {
                return State.DONE;
            }
            if (b == '#') {
                return State.COMMENT;
            }
            if (b == '[') {
                return State.TABLE_START;
            }
            out.writeByte((byte) b);
            return State.KEY_READ;
        }

        private State onComment() throws IOException {
            int b;
            while ((b = input.read()) != -1 && b != '\r' && b != '\n') {
                if (rejectCommentControlCharacter(b)) {
                    throw new CfgException("corrupt toml configuration");
                }
            }
            return b == -1 ? State.DONE : State.INITIAL;
        }

        private State onTableStart(int maxDepth) throws IOException {
            current = root;
            CfgOldUtil.search(input, out, ']');
            int nestedKeyLen = out.intPosition();
            if (nestedKeyLen == 0) {
                throw new CfgException("corrupt toml configuration");
            }
            String nestedKeyStr = new String(out.rawByteArray(), 0, nestedKeyLen, StandardCharsets.US_ASCII);
            if (!tables.add(nestedKeyStr)) {
                throw new CfgException("duplicate table: " + nestedKeyStr);
            }
            for (String nestedKey : CfgOldUtil.readCfgNestedKey(out.rawByteArray(), 0, nestedKeyLen, maxDepth)) {
                Map<String, Cfg> currentMap = current.value();
                Cfg currentObj = currentMap.get(nestedKey);
                if (currentObj == null) {
                    CfgObject cm = new CfgObject(new HashMap<>());
                    currentMap.put(nestedKey, cm);
                    current = cm;
                } else if (currentObj instanceof CfgObject cm) {
                    current = cm;
                } else {
                    throw new CfgException("duplicate table: " + nestedKeyStr);
                }
            }
            out.reset();
            return State.VALUE_END;
        }

        private State onValueEnd() throws IOException {
            int b = CfgOldUtil.ignore(input, ' ', '\t');
            if (b == -1) {
                return State.DONE;
            }
            if (b == '\r' || b == '\n') {
                return State.INITIAL;
            }
            if (b == '#') {
                return State.COMMENT;
            }
            throw new CfgException("corrupt toml configuration");
        }

        private State onKeyRead() throws IOException {
            int b = CfgOldUtil.search(input, out, ' ', '\t', '=');
            key = CfgOldUtil.readCfgKey(out.rawByteArray(), 0, out.intPosition());
            out.reset();
            if (b != '=') {
                b = CfgOldUtil.ignoreTillEOF(input, ' ', '\t');
                if (b != '=') {
                    throw new CfgException("corrupt toml configuration");
                }
            }
            return State.VALUE_START;
        }

        private State onValueStart() throws IOException {
            int b = CfgOldUtil.ignoreTillEOF(input, ' ', '\t');
            if (b == '[') {
                return State.ARRAY;
            }
            storeItem(readScalarValue(b));
            return State.VALUE_END;
        }

        private State onArray() throws IOException {
            int b = CfgOldUtil.ignoreTillEOF(input, ' ', '\t');
            if (b == ']') {
                storeList(new ArrayList<>());
                return State.VALUE_END;
            }
            List<String> list = new ArrayList<>();
            for (;;) {
                list.add(readScalarValue(b));
                b = CfgOldUtil.ignoreTillEOF(input, ' ', '\t');
                if (b == ']') {
                    break;
                }
                if (b != ',') {
                    throw new CfgException("corrupt toml configuration");
                }
                b = CfgOldUtil.ignoreTillEOF(input, ' ', '\t');
            }
            storeList(list);
            return State.VALUE_END;
        }

        private String readScalarValue(int firstByte) throws IOException {
            if (firstByte == '"') {
                readTomlStrValue(input, out);
                String str = new String(out.rawByteArray(), 0, out.intPosition(), StandardCharsets.UTF_8);
                out.reset();
                return str;
            }
            if (firstByte == '-' || firstByte == '+' || (firstByte >= '0' && firstByte <= '9')) {
                String num = CfgOldUtil.readTomlNumber(input, firstByte, out);
                out.reset();
                return num;
            }
            if (firstByte == 't' || firstByte == 'f' || firstByte == 'i' || firstByte == 'n') {
                String word = CfgOldUtil.readTomlWord(input, firstByte, out);
                out.reset();
                return word;
            }
            throw new CfgException("corrupt toml configuration");
        }

        private void storeItem(String value) {
            if (current.value().putIfAbsent(key, new CfgItem(value)) != null) {
                throw new CfgException("duplicate key: " + key);
            }
            key = null;
        }

        private void storeList(List<String> value) {
            if (current.value().putIfAbsent(key, new CfgList(value)) != null) {
                throw new CfgException("duplicate key: " + key);
            }
            key = null;
        }

        enum State {
            INITIAL,       // skip whitespace, detect EOF / '#' / '[' / key start
            COMMENT,       // skip to EOL, validate control characters
            TABLE_START,   // read '[table.header]' and navigate `current`
            VALUE_END,     // after a value: expect newline, EOF, or '#'
            KEY_READ,      // read the key up to '='
            VALUE_START,   // dispatch on the first byte of a value
            ARRAY,         // parsing array elements
            DONE,          // EOF reached, exit the loop
        }
    }
}