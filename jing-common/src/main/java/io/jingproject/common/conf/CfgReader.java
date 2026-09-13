package io.jingproject.common.conf;

import io.jingproject.common.conf.Cfg.CfgItem;
import io.jingproject.common.conf.Cfg.CfgList;
import io.jingproject.common.conf.Cfg.CfgObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public sealed interface CfgReader {
    CfgObject parse(int maxDepth) throws IOException;

    final class JsonCfgReader implements CfgReader {
        private final InputStream input;
        private final Deque<Map<String, Cfg>> previous = new ArrayDeque<>();
        private final Deque<String> keys = new ArrayDeque<>();
        private Map<String, Cfg> current = new HashMap<>();
        private String key = null;

        public JsonCfgReader(InputStream input) {
            this.input = input;
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
                            if (!Character.isValidCodePoint(codePoint)) {
                                throw new CfgException("invalid code point: " + codePoint);
                            }
                            if (codePoint instanceof char highSurrogate && Character.isHighSurrogate(highSurrogate)) {
                                CfgUtil.assume(input, '\\');
                                CfgUtil.assume(input, 'u');
                                int lowSurrogateCodePoint = CfgUtil.readUnicode(input, 4);
                                if (lowSurrogateCodePoint instanceof char lowSurrogate && Character.isLowSurrogate(lowSurrogate) && Character.isSurrogatePair(highSurrogate, lowSurrogate)) {
                                    codePoint = Character.toCodePoint(highSurrogate, lowSurrogate);
                                }
                            }
                            CfgUtil.writeUnicodeInUtf8(output, codePoint);
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
                        output.write(b);
                    }
                }
            }
            throw new CfgException("EOF reached");
        }

        // the constructed reader can only parse once, further parse is undefined behavior
        public CfgObject parse(int maxDepth) throws IOException {
            State state = State.INITIAL;
            int b;
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                for (; ; ) {
                    switch (state) {
                        case INITIAL -> {
                            b = CfgUtil.ignoreTillEOF(input, ' ', '\t', '\r', '\n');
                            if (b != '{') {
                                throw new CfgException("Corrupted json configuration");
                            }
                            state = State.OBJ_START;
                        }
                        case OBJ_START -> {
                            if (key != null) {
                                if (previous.size() >= maxDepth) {
                                    throw new CfgException("maximum nesting depth exceeded");
                                }
                                keys.addLast(key);
                                key = null;
                                previous.addLast(current);
                                current = new HashMap<>();
                            }
                            b = CfgUtil.ignoreTillEOF(input, ' ', '\t', '\r', '\n');
                            if (b == '}') {
                                state = State.STR_ARR_OBJ_END;
                            } else if (b == '"') {
                                state = State.KEY_START;
                            } else {
                                throw new CfgException("Corrupted json configuration");
                            }
                        }
                        case STR_ARR_OBJ_END -> {
                            b = CfgUtil.ignore(input, ' ', '\t', '\r', '\n');
                            if (b == ',') {
                                state = State.EXPECT_KEY;
                            } else if (b == '}') {
                                Map<String, Cfg> parent = previous.pollLast();
                                if (parent == null) {
                                    return new CfgObject(current);
                                }
                                key = keys.pollLast();
                                if (parent.putIfAbsent(key, new CfgObject(current)) != null) {
                                    throw new CfgException("Duplicate key: " + key);
                                }
                                current = parent;
                                key = null;
                            } else if (b == -1) {
                                if (previous.isEmpty()) {
                                    return new CfgObject(current);
                                }
                                throw new CfgException("EOF reached");
                            } else {
                                throw new CfgException("Corrupted json configuration");
                            }
                        }
                        case EXPECT_KEY -> {
                            b = CfgUtil.ignoreTillEOF(input, ' ', '\t', '\r', '\n');
                            if (b == '"') {
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
                            if (b != ':') {
                                throw new CfgException("Corrupted json configuration");
                            }
                            state = State.EXPECT_VALUE;
                        }
                        case EXPECT_VALUE -> {
                            b = CfgUtil.ignoreTillEOF(input, ' ', '\t', '\r', '\n');
                            if (b == '"') {
                                state = State.STR_START;
                            } else if (b == '[') {
                                state = State.ARR_START;
                            } else if (b == '{') {
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
                            for (; ; ) {
                                b = CfgUtil.ignoreTillEOF(input, ' ', '\t', '\r', '\n');
                                if (b != '"') {
                                    throw new CfgException("Corrupted json configuration");
                                }
                                readJsonStrValue(input, output);
                                strList.add(output.toString(StandardCharsets.UTF_8));
                                output.reset();
                                b = CfgUtil.ignoreTillEOF(input, ' ', '\t', '\r', '\n');
                                if (b == ']') {
                                    if (current.putIfAbsent(key, new CfgList(strList)) != null) {
                                        throw new CfgException("Duplicate key: " + key);
                                    }
                                    key = null;
                                    state = State.STR_ARR_OBJ_END;
                                    break;
                                } else if (b != ',') {
                                    throw new CfgException("Corrupted json configuration");
                                }
                            }
                        }
                    }
                }
            }
        }

        enum State {
            INITIAL, // initial state, expecting the next '{' as the start of an object
            OBJ_START, // read the object start '{', expecting a key start '"' or the object end '}'
            STR_ARR_OBJ_END, // read the end of a string, array or object '"' ']' '}', expecting '}' or ','
            EXPECT_KEY, // expecting the opening quote of the next string key
            KEY_START, // read the opening quote of a key, now parsing the key content
            KEY_END, // read the closing quote of a key, expecting the separator ':'
            EXPECT_VALUE, // read ':', expecting the value start, one of '"' '[' '{'
            STR_START, // read the value start '"', expecting the value end '"'
            ARR_START, // read the array start '[', expecting the array end ']'
        }
    }

    final class PropertiesCfgReader implements CfgReader {
        private final InputStream input;

        public PropertiesCfgReader(InputStream input) {
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
                    List<String> nestedKeys = CfgUtil.readCfgNestedKey(key.getBytes(StandardCharsets.UTF_8), maxDepth);
                    switch (nestedKeys.size()) {
                        case 0 -> throw new AssertionError();
                        case 1 -> {
                            if (current.value().putIfAbsent(nestedKeys.getLast(), buildCfgObject(value)) != null) {
                                throw new CfgException("Duplicate key: " + key);
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
                                    throw new CfgException("Duplicate key: " + key);
                                }
                            }
                            if (current.value().putIfAbsent(nestedKeys.getLast(), buildCfgObject(value)) != null) {
                                throw new CfgException("Duplicate key: " + key);
                            }
                            current = r;
                        }
                    }
                }
                return r;
            }
        }
    }

    final class TomlCfgReader implements CfgReader {
        private final InputStream input;
        private final CfgObject root = new CfgObject(new HashMap<>());
        private final Set<String> tables = new HashSet<>();
        private CfgObject current = root;
        private String key = null;

        public TomlCfgReader(InputStream input) {
            this.input = input;
        }

        // based on https://toml.io/en/v1.1.0#comment
        private static boolean rejectCommentControlCharacter(int b) {
            return (b >= 0x0000 && b <= 0x0008) || (b >= 0x000A && b <= 0x001F) || b == 0x007F;
        }

        private static void readTomlStrValue(InputStream input, ByteArrayOutputStream output) throws IOException {
            boolean escaping = false;
            int b;
            while ((b = input.read()) != -1) {
                if (escaping) {
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
                            if (!Character.isValidCodePoint(codePoint)) {
                                throw new CfgException("invalid code point: " + codePoint);
                            }
                            if (codePoint instanceof char charCodePoint && Character.isSurrogate(charCodePoint)) {
                                throw new CfgException("invalid surrogate code point: " + codePoint);
                            }
                            CfgUtil.writeUnicodeInUtf8(output, codePoint);
                        }
                        case 'u' -> {
                            int codePoint = CfgUtil.readUnicode(input, 4);
                            if (!Character.isValidCodePoint(codePoint)) {
                                throw new CfgException("invalid code point: " + codePoint);
                            }
                            if (codePoint instanceof char charCodePoint && Character.isSurrogate(charCodePoint)) {
                                throw new CfgException("invalid surrogate code point: " + codePoint);
                            }
                            CfgUtil.writeUnicodeInUtf8(output, codePoint);
                        }
                        case 'U' -> {
                            int codePoint = CfgUtil.readUnicode(input, 8);
                            if (!Character.isValidCodePoint(codePoint)) {
                                throw new CfgException("invalid code point: " + codePoint);
                            }
                            if (codePoint instanceof char charCodePoint && Character.isSurrogate(charCodePoint)) {
                                throw new CfgException("invalid surrogate code point: " + codePoint);
                            }
                            CfgUtil.writeUnicodeInUtf8(output, codePoint);
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
                        output.write(b);
                    }
                }
            }
            throw new CfgException("EOF reached");
        }

        public CfgObject parse(int maxDepth) throws IOException {
            State state = State.INITIAL;
            int b;
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                for (; ; ) {
                    switch (state) {
                        case INITIAL -> {
                            b = CfgUtil.ignore(input, ' ', '\t', '\r', '\n');
                            if (b == -1) {
                                return root;
                            } else if (b == '#') {
                                state = State.COMMENT;
                            } else if (b == '[') {
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
                            if (!tables.add(nestedKeyStr)) {
                                throw new CfgException("Duplicate table: " + nestedKeyStr);
                            }
                            output.reset();
                            for (String nestedKey : CfgUtil.readCfgNestedKey(nestedKeyBytes, maxDepth)) {
                                Map<String, Cfg> currentMap = current.value();
                                Cfg currentObj = currentMap.get(nestedKey);
                                if (currentObj == null) {
                                    CfgObject cm = new CfgObject(new HashMap<>());
                                    currentMap.put(nestedKey, cm);
                                    current = cm;
                                } else if (currentObj instanceof CfgObject cm) {
                                    current = cm;
                                } else {
                                    throw new CfgException("Duplicate table: " + nestedKeyStr);
                                }
                            }
                            state = State.VALUE_END;
                        }
                        case VALUE_END -> {
                            b = CfgUtil.ignore(input, ' ', '\t');
                            if (b == -1) {
                                return root;
                            } else if (b == '\r' || b == '\n') {
                                state = State.INITIAL;
                            } else if (b == '#') {
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
                            if (b != '=') {
                                b = CfgUtil.ignoreTillEOF(input, ' ', '\t');
                                if (b != '=') {
                                    throw new CfgException("Corrupt toml configuration");
                                }
                            }
                            state = State.VALUE_START;
                        }
                        case VALUE_START -> {
                            b = CfgUtil.ignoreTillEOF(input, ' ', '\t');
                            if (b == '"') {
                                state = State.STR_START;
                            } else if (b == '[') {
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
                            for (; ; ) {
                                b = CfgUtil.ignoreTillEOF(input, ' ', '\t');
                                if (b != '"') {
                                    throw new CfgException("Corrupt toml configuration");
                                }
                                readTomlStrValue(input, output);
                                strList.add(output.toString(StandardCharsets.UTF_8));
                                output.reset();
                                b = CfgUtil.ignoreTillEOF(input, ' ', '\t');
                                if (b == ']') {
                                    if (current.value().putIfAbsent(key, new CfgList(strList)) != null) {
                                        throw new CfgException("Duplicate key: " + key);
                                    }
                                    key = null;
                                    state = State.VALUE_END;
                                    break;
                                } else if (b != ',') {
                                    throw new CfgException("Corrupt toml configuration");
                                }
                            }
                        }
                    }
                }
            }
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
    }
}