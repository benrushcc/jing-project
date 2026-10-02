package io.jingproject.common.conf;

import jdk.incubator.json.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.IntUnaryOperator;

public sealed abstract class CfgReader permits CfgReader.JsonCfgReader, CfgReader.PropertiesCfgReader, CfgReader.TomlCfgReader {

    public abstract Cfg.CfgObject parse(InputStream inputStream, int maxDepth);

    protected static void validateKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new CfgException("key cannot be empty");
        }
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                continue ;
            }
            if(c == '_' || c == '-') {
                if(i == 0 || i == key.length() - 1) {
                    throw new CfgException("key must not start or end with '_' or '-'");
                }
                char before = key.charAt(i - 1);
                char after = key.charAt(i + 1);
                if(before == '_' || before == '-' || after == '_' || after == '-') {
                    throw new CfgException("key must not contain consecutive '-' or '_' characters");
                }
            } else {
                throw new CfgException("illegal key : " + key);
            }
        }
    }

    public static final class JsonCfgReader extends CfgReader {

        @Override
        public Cfg.CfgObject parse(InputStream inputStream, int maxDepth) {
            String json;
            try {
                json = StandardCharsets.UTF_8
                        .newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(inputStream.readAllBytes()))
                        .toString();
            } catch (CharacterCodingException e) {
                throw new CfgException("invalid utf8 json configuration", e);
            } catch (IOException e) {
                throw new CfgException("failed to read json configuration", e);
            }
            JsonValue jsonValue;
            try {
                jsonValue = Json.parse(json);
            } catch (RuntimeException e) {
                throw new CfgException("failed to parse json configuration", e);
            }
            if(jsonValue instanceof JsonObject jsonObject) {
                return toCfgObject(jsonObject, maxDepth);
            }
            throw new CfgException("not a json object");
        }

        private value record Frame(
                JsonObject object,
                java.util.Iterator<Map.Entry<String, JsonValue>> iterator,
                int depth) {
        }

        private static List<String> toStrList(List<JsonValue> values) {
            List<String> result = new ArrayList<>(values.size());
            for (JsonValue value : values) {
                switch (value) {
                    case JsonString jsonString -> {
                        String v = jsonString.asString();
                        if(v == null || v.isBlank()) {
                            throw new CfgException("empty json string value");
                        }
                        result.add(v);
                    }
                    case JsonNumber jsonNumber -> {
                        String v = jsonNumber.toString();
                        if(v == null || v.isBlank()) {
                            throw new CfgException("empty json number value");
                        }
                        result.add(v);
                    }
                    case JsonBoolean jsonBoolean -> {
                        String v = jsonBoolean.toString();
                        if(v == null || v.isBlank()) {
                            throw new CfgException("empty json boolean value");
                        }
                        result.add(v);
                    }
                    case JsonNull _ -> throw new CfgException("null not allowed in json configuration");
                    default -> throw new CfgException("unsupported recursive array or object");
                }
            }
            return result;
        }

        private static Cfg toCfg(String key, JsonValue value, IdentityHashMap<JsonObject, Cfg.CfgObject> result) {
            return switch (value) {
                case JsonString jsonString -> {
                    String v = jsonString.asString();
                    if(v == null || v.isBlank()) {
                        throw new CfgException("empty json string value");
                    }
                    yield new Cfg.CfgItem(jsonString.asString());
                }
                case JsonNumber jsonNumber -> {
                    String v = jsonNumber.toString();
                    if(v == null || v.isBlank()) {
                        throw new CfgException("empty json number value");
                    }
                    yield new Cfg.CfgItem(v);
                }
                case JsonBoolean jsonBoolean -> {
                    String v = jsonBoolean.toString();
                    if(v == null || v.isBlank()) {
                        throw new CfgException("empty json number value");
                    }
                    yield new Cfg.CfgItem(jsonBoolean.toString());
                }
                case JsonArray jsonArray -> {
                    List<JsonValue> v = jsonArray.asList();
                    if(v == null || v.isEmpty()) {
                        throw new CfgException("empty json array value");
                    }
                    yield new Cfg.CfgList(toStrList(v));
                }
                case JsonObject jsonObject -> {
                    Cfg.CfgObject cfgObject = result.get(jsonObject);
                    if (cfgObject == null) {
                        throw new CfgException("child object has not been converted : " + key);
                    }
                    yield cfgObject;
                }
                case JsonNull _ -> throw new CfgException("null not allowed in json configuration");
            };
        }

        public static Cfg.CfgObject toCfgObject(JsonObject root, int maxDepth) {
            IdentityHashMap<JsonObject, Cfg.CfgObject> result = new IdentityHashMap<>();
            ArrayDeque<Frame> stack = new ArrayDeque<>();
            stack.push(new Frame(root, root.asMap().entrySet().iterator(), 1));
            while (!stack.isEmpty()) {
                Frame frame = stack.peek();
                if (frame.iterator().hasNext()) {
                    Map.Entry<String, JsonValue> entry = frame.iterator().next();
                    JsonValue value = entry.getValue();
                    if (value instanceof JsonObject child && !result.containsKey(child)) {
                        int childDepth = Math.incrementExact(frame.depth());
                        if (childDepth > maxDepth) {
                            throw new CfgException("maximum json object nesting depth exceeded : " + maxDepth);
                        }
                        stack.push(new Frame(child, child.asMap().entrySet().iterator(), childDepth));
                    }
                    continue;
                }
                Map<String, Cfg> cfgMap = new LinkedHashMap<>(frame.object().asMap().size());
                for (Map.Entry<String, JsonValue> entry : frame.object().asMap().entrySet()) {
                    String key = entry.getKey();
                    validateKey(key);
                    cfgMap.put(key, toCfg(key, entry.getValue(), result));
                }
                result.put(frame.object(), new Cfg.CfgObject(cfgMap));
                stack.pop();
            }
            return result.get(root);
        }
    }

    public static final class PropertiesCfgReader extends CfgReader {

        @Override
        public Cfg.CfgObject parse(InputStream inputStream, int maxDepth) {
            Properties properties = new Properties();
            try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IOException e) {
                throw new CfgException("failed to parse properties configuration", e);
            }
            Cfg.CfgObject root = new Cfg.CfgObject(new HashMap<>());
            for (String key : properties.stringPropertyNames()) {
                int start = 0;
                int end;
                while ((end = key.indexOf('.', start)) != -1) {
                    String part = key.substring(start, end);
                    validateKey(part);
                    start = end + 1;
                }
                validateKey(key.substring(start));
                String value = properties.getProperty(key);
                if (value == null || value.isBlank()) {
                    throw new CfgException("empty property value not supported, key : " + key);
                }
                insert(root, key, parseCfg(key, value), maxDepth);
            }
            return root;
        }

        private static void insert(Cfg.CfgObject root, String key, Cfg cfg, int maxDepth) {
            Cfg.CfgObject current = root;
            int start = 0;
            int depth = 1;
            for( ; ; ) {
                int index = key.indexOf('.', start);
                if (index < 0) {
                    if (start == key.length()) {
                        throw new CfgException("empty properties key segment : " + key);
                    }
                    String leaf = key.substring(start);
                    if (leaf.isBlank()) {
                        throw new CfgException("empty properties key segment : " + key);
                    }
                    if (current.value().putIfAbsent(leaf, cfg) != null) {
                        throw new CfgException("duplicate properties key : " + key);
                    }
                    return;
                }
                if (index == start) {
                    throw new CfgException("empty properties key segment : " + key);
                }
                if (++depth > maxDepth) {
                    throw new CfgException("maximum properties configuration depth exceeded : " + maxDepth);
                }
                String part = key.substring(start, index);
                if (part.isBlank()) {
                    throw new CfgException("empty properties key segment : " + key);
                }
                Cfg child = current.value().get(part);
                if (child == null) {
                    Cfg.CfgObject object = new Cfg.CfgObject(new HashMap<>());
                    current.value().put(part, object);
                    current = object;
                } else if (child instanceof Cfg.CfgObject object) {
                    current = object;
                } else {
                    throw new CfgException("conflicts properties key with existing : " + key);
                }
                start = index + 1;
            }
        }

        private static Cfg parseCfg(String key, String value) {
            if (value.length() >= 2 && value.charAt(0) == '[' && value.charAt(value.length() - 1) == ']') {
                return parseCfgList(key, value);
            }
            return new Cfg.CfgItem(value);
        }

        private static Cfg.CfgList parseCfgList(String key, String value) {
            int start = 1;
            int end = value.length() - 1;
            List<String> values = new ArrayList<>();
            for( ; ; ) {
                int comma = value.indexOf(',', start);
                if (comma < 0 || comma >= end) {
                    String item = value.substring(start, end);
                    if (item.isBlank()) {
                        throw new CfgException("empty properties list item for key : " + key);
                    }
                    values.add(item);
                    break;
                }
                String item = value.substring(start, comma);
                if (item.isBlank()) {
                    throw new CfgException("empty properties list item for key : " + key);
                }
                values.add(item);
                start = comma + 1;
            }
            return new Cfg.CfgList(values);
        }
    }

    public static final class TomlCfgReader extends CfgReader {
        @Override
        public Cfg.CfgObject parse(InputStream inputStream, int maxDepth) {
            byte[] src;
            try {
                src = inputStream.readAllBytes();
            } catch (IOException e) {
                throw new CfgException("failed to parse toml configuration", e);
            }
            return new TomlParser(src, maxDepth).parse();
        }

        private static class TomlParser {
            private final byte[] bytes;
            private final int maxDepth;
            private final Set<Cfg.CfgObject> explicitTables;
            private final Set<Cfg.CfgObject> stdExprTables;
            private int pos = 0;

            public TomlParser(byte[] bytes, int maxDepth) {
                this.bytes = bytes;
                this.maxDepth = maxDepth;
                this.explicitTables = Collections.newSetFromMap(new IdentityHashMap<>());
                this.stdExprTables = Collections.newSetFromMap(new IdentityHashMap<>());
            }

            public Cfg.CfgObject parse() {
                Cfg.CfgObject root = new Cfg.CfgObject(new LinkedHashMap<>());
                Cfg.CfgObject cur = root;
                int curDepth = 0;
                int value;
                for( ; ; ) {
                    skipWs();
                    if(pos == bytes.length) {
                        break ;
                    }
                    value = bytes[pos++] & 0xFF;
                    if(newLine(value)) {
                        continue ;
                    }
                    if(value == '#') {
                        skipComment();
                        if(pos == bytes.length) {
                            break ;
                        }
                        continue ;
                    }
                    if(value == '[') {
                        skipWs();
                        List<String> dottedKey = parseDottedKey(']');
                        curDepth = dottedKey.size();
                        if(curDepth > maxDepth) {
                            throw new CfgException("max depth exceeded : " + curDepth);
                        }
                        cur = defineTable(root, dottedKey);
                    } else {
                        pos--;
                        List<String> dottedKey = parseDottedKey('=');
                        int dp = Math.addExact(curDepth, dottedKey.size());
                        if(dp > maxDepth) {
                            throw new CfgException("max depth exceeded : " + dp);
                        }
                        skipWs();
                        setValue(cur, dottedKey, parseValue());
                    }
                    skipWs();
                    if(pos == bytes.length) {
                        break ;
                    }
                    value = bytes[pos++] & 0xFF;
                    if(value == '#') {
                        skipComment();
                        if(pos == bytes.length) {
                            break ;
                        }
                        continue ;
                    }
                    if(!newLine(value)) {
                        throw new CfgException("invalid line end : " + value);
                    }
                }
                return root;
            }

            private void skipWs() {
                while (pos < bytes.length) {
                    int value = bytes[pos] & 0xFF;
                    if(value != ' ' && value != '\t') {
                        return ;
                    }
                    pos++;
                }
            }

            private boolean newLine(int value) {
                if(value == '\r') {
                    if(pos == bytes.length || bytes[pos++] != (byte) '\n') {
                        throw new CfgException("single CR not allowed");
                    }
                    return true;
                }
                return value == '\n';
            }

            private boolean invalidCharacter(int value) {
                return (value < 0x20 && value != '\t') || value == 0x7f;
            }

            private void skipComment() {
                while (pos < bytes.length) {
                    int value = bytes[pos++] & 0xFF;
                    if(newLine(value)) {
                        return ;
                    }
                    if (invalidCharacter(value)) {
                        throw new CfgException("control character not allowed in comment : " + value);
                    }
                }
            }

            private List<String> parseDottedKey(int end) {
                List<String> r = new ArrayList<>();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                while (pos < bytes.length) {
                    int value = bytes[pos++] & 0xFF;
                    if(value == '"') {
                        parseBasicString(out);
                    }else if (value == '\'') {
                        parseLiteralString(out);
                    } else if (value == '.') {
                        if (out.size() == 0) {
                            throw new CfgException("empty key part not allowed");
                        }
                        r.add(out.toString(StandardCharsets.UTF_8));
                        out.reset();
                    } else if (value == end) {
                        if (out.size() == 0) {
                            throw new CfgException("empty key part not allowed");
                        }
                        r.add(out.toString(StandardCharsets.UTF_8));
                        r.forEach(CfgReader::validateKey);
                        return r;
                    } else if(value == ' ' || value == '\t') {
                        while (pos < bytes.length) {
                            value = bytes[pos] & 0xFF;
                            if(value != ' ' && value != '\t') {
                                break ;
                            }
                            pos++;
                        }
                        if (pos >= bytes.length || value != end) {
                            throw new CfgException("key closure not found after identation");
                        }
                    } else {
                        out.write(value);
                    }
                }
                throw new CfgException("eof reached while parsing dotted key");
            }

            private void parseBasicString(ByteArrayOutputStream out) {
                while (pos < bytes.length) {
                    int value = bytes[pos] & 0xFF;
                    if(value == '"') {
                        pos++;
                        return ;
                    }
                    if (value == '\\') {
                        pos++;
                        parseEscape(out);
                        continue ;
                    }
                    if(invalidCharacter(value)) {
                        throw new CfgException("invalid ctrl character in basic string");
                    }
                    out.write(value);
                    pos++;
                }
                throw new CfgException("eof reached while parsing basic string");
            }

            private void parseLiteralString(ByteArrayOutputStream out) {
                while (pos < bytes.length) {
                    int value = bytes[pos] & 0xFF;
                    if(value == '\'') {
                        pos++;
                        return ;
                    }
                    if(invalidCharacter(value)) {
                        throw new CfgException("invalid ctrl character in literal string");
                    }
                    pos++;
                    out.write(value);
                }
                throw new CfgException("eof reached while parsing literal string");
            }

            private void parseEscape(ByteArrayOutputStream out) {
                if (pos >= bytes.length) {
                    throw new CfgException("eof reached while parsing escaped sequence");
                }
                int value = bytes[pos++] & 0xFF;
                switch (value) {
                    case 'b'  -> out.write('\b');
                    case 't'  -> out.write('\t');
                    case 'n'  -> out.write('\n');
                    case 'f'  -> out.write('\f');
                    case 'r'  -> out.write('\r');
                    case 'e'  -> out.write('\u001B');
                    case '"'  -> out.write('"');
                    case '\\' -> out.write('\\');
                    case 'x'  -> {
                        int v = readHexDigits(2);
                        if(v < 0 || v > 0x10FFFF || (v >= 0xD800 && v <= 0xDFFF)) {
                            throw new CfgException("invalid surrogate in \\x escape");
                        }
                        writeUtf8(out, v);
                    }
                    case 'u'  -> {
                        int v = readHexDigits(4);
                        if(v < 0 || v > 0x10FFFF || (v >= 0xD800 && v <= 0xDFFF)) {
                            throw new CfgException("invalid surrogate in \\u escape");
                        }
                        writeUtf8(out, v);
                    }
                    case 'U' -> {
                        int v = readHexDigits(8);
                        if(v < 0 || v > 0x10FFFF || (v >= 0xD800 && v <= 0xDFFF)) {
                            throw new CfgException("invalid surrogate in \\U escape");
                        }
                        writeUtf8(out, v);
                    }
                    default  -> throw new CfgException("invalid escape sequence : " + value);
                }
            }

            private int readHexDigits(int n) {
                if (pos > bytes.length - n) {
                    throw new CfgException("eof reached while reading hex digits");
                }
                int v = 0;
                for (int i = 0; i < n; i++) {
                    int digit = hexDigit(bytes[pos++] & 0xFF);
                    if(digit < 0) {
                        throw new CfgException("invalid hex digit : " + digit);
                    }
                    v = (v << 4) | digit;
                }
                return v;
            }

            private static int binaryDigit(int value) {
                if(value == '0') {
                    return 0;
                } else if(value == '1') {
                    return 1;
                } else {
                    return -1;
                }
            }

            private static int octalDigit(int value) {
                if(value >= '0' && value <= '7') {
                    return value - '0';
                } else {
                    return -1;
                }
            }

            private static int hexDigit(int value) {
                if(value >= '0' && value <= '9') {
                    return value - '0';
                } else if(value >= 'A' && value <= 'F') {
                    return value - 'A' + 10;
                } else if(value >= 'a' && value <= 'f') {
                    return value - 'a' + 10;
                } else {
                    return -1;
                }
            }

            private static void writeUtf8(ByteArrayOutputStream out, int cp) {
                if (cp < 0x80) {
                    out.write(cp);
                } else if (cp < 0x800) {
                    out.write(0xC0 | (cp >> 6));
                    out.write(0x80 | (cp & 0x3F));
                } else if (cp < 0x10000) {
                    out.write(0xE0 | (cp >> 12));
                    out.write(0x80 | ((cp >> 6) & 0x3F));
                    out.write(0x80 | (cp & 0x3F));
                } else {
                    out.write(0xF0 | (cp >> 18));
                    out.write(0x80 | ((cp >> 12) & 0x3F));
                    out.write(0x80 | ((cp >> 6) & 0x3F));
                    out.write(0x80 | (cp & 0x3F));
                }
            }

            private Cfg.CfgObject defineTable(Cfg.CfgObject root, List<String> parts) {
                Cfg.CfgObject tab = root;
                for (int i = 0; i < parts.size() - 1; i++) {
                    tab = findOrCreate(tab, parts.get(i), true);
                }
                String lastKey = parts.getLast();
                Map<String, Cfg> m = tab.value();
                Cfg existing = m.get(lastKey);
                if (existing == null) {
                    Cfg.CfgObject nt = new Cfg.CfgObject(new LinkedHashMap<>());
                    m.put(lastKey, nt);
                    explicitTables.add(nt);
                    return nt;
                }
                if (existing instanceof Cfg.CfgObject obj) {
                    // explicit tables and dotted-key-created tables cannot be
                    // redefined by a header; only header-created intermediates can
                    if (explicitTables.contains(obj) || !stdExprTables.contains(obj)) {
                        throw new CfgException("table already defined: " + lastKey);
                    }
                    explicitTables.add(obj);
                    return obj;
                }
                throw new CfgException("cannot locate table at key " + lastKey);
            }

            private Cfg.CfgObject findOrCreate(Cfg.CfgObject tab, String part, boolean stdExpr) {
                Map<String, Cfg> m = tab.value();
                Cfg v = m.get(part);
                switch (v) {
                    case null -> {
                        Cfg.CfgObject nt = new Cfg.CfgObject(new LinkedHashMap<>());
                        m.put(part, nt);
                        if (stdExpr) {
                            stdExprTables.add(nt);
                        }
                        return nt;
                    }
                    case Cfg.CfgObject obj -> {
                        return obj;
                    }
                    case Cfg.CfgList _ -> throw new CfgException("key already defined as configuration list");
                    case Cfg.CfgItem _ -> throw new CfgException("key already defined as configuration item");
                }
            }

            private void setValue(Cfg.CfgObject curTab, List<String> parts, Cfg value) {
                Cfg.CfgObject tab = curTab;
                for (int i = 0; i < parts.size() - 1; i++) {
                    tab = findOrCreate(tab, parts.get(i), false);
                }
                String lastKey = parts.getLast();
                Map<String, Cfg> m = tab.value();
                if (m.containsKey(lastKey)) {
                    throw new CfgException("duplicate key: " + lastKey);
                }
                m.put(lastKey, value);
            }

            private List<String> parseArray() {
                List<String> r = new ArrayList<>();
                for( ; ; ) {
                    skipWs();
                    if(pos == bytes.length) {
                        throw new CfgException("eof reached while parsing array");
                    }
                    int value = bytes[pos++] & 0xFF;
                    switch (value) {
                        case '"' -> {
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            parseBasicString(out);
                            r.add(out.toString(StandardCharsets.UTF_8));
                        }
                        case '\'' -> {
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            parseLiteralString(out);
                            r.add(out.toString(StandardCharsets.UTF_8));
                        }
                        case 't' -> r.add(parseTrue());
                        case 'f' -> r.add(parseFalse());
                        case ']' -> {
                            return r;
                        }
                        default -> {
                            if ((value >= '0' && value <= '9') || value == '+' || value == '-' || value == 'i' || value == 'n') {
                                ByteArrayOutputStream out = new ByteArrayOutputStream();
                                parseNumber(value, out);
                                r.add(out.toString(StandardCharsets.UTF_8));
                            } else {
                                throw new CfgException("invalid value");
                            }
                        }
                    }
                    skipWs();
                    if(pos == bytes.length) {
                        throw new CfgException("eof reached while parsing array");
                    }
                    value = bytes[pos++] & 0xFF;
                    if(value == ']') {
                        return r;
                    } else if(value != ',') {
                        throw new CfgException("missing comma");
                    }
                }
            }

            private String parseTrue() {
                if(pos > bytes.length - 3) {
                    throw new CfgException("eof reached while parsing 'true' value");
                }
                if(bytes[pos] != (byte) 'r' || bytes[pos + 1] != (byte) 'u' || bytes[pos + 2] != (byte) 'e') {
                    throw new CfgException("invalid bool value for 'true'");
                }
                pos += 3;
                return "true";
            }

            private String parseFalse() {
                if(pos > bytes.length - 4) {
                    throw new CfgException("eof reached while parsing 'false' value");
                }
                if(bytes[pos] != (byte) 'a' || bytes[pos + 1] != (byte) 'l' || bytes[pos + 2] != (byte) 's' || bytes[pos + 3] != (byte) 'e') {
                    throw new CfgException("invalid bool value for 'false'");
                }
                pos += 4;
                return "false";
            }

            private Cfg parseValue() {
                if (pos >= bytes.length) {
                    throw new CfgException("eof reached while parsing value");
                }
                int value = bytes[pos++] & 0xFF;
                switch (value) {
                    case '"' -> {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        parseBasicString(out);
                        return new Cfg.CfgItem(out.toString(StandardCharsets.UTF_8));
                    }
                    case '\'' -> {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        parseLiteralString(out);
                        return new Cfg.CfgItem(out.toString(StandardCharsets.UTF_8));
                    }
                    case '{' -> throw new CfgException("inline tables not supported");
                    case '[' -> {
                        return new Cfg.CfgList(parseArray());
                    }
                    case 't' -> {
                        return new Cfg.CfgItem(parseTrue());
                    }
                    case 'f' -> {
                        return new Cfg.CfgItem(parseFalse());
                    }
                    default -> {
                        if ((value >= '0' && value <= '9') || value == '+' || value == '-' || value == 'i' || value == 'n') {
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            parseNumber(value, out);
                            return new Cfg.CfgItem(out.toString(StandardCharsets.UTF_8));
                        }
                        throw new CfgException("invalid value");
                    }
                }
            }

            private void parseInf(int sign, ByteArrayOutputStream out) {
                if(pos > bytes.length - 2) {
                    throw new CfgException("eof reached while parsing 'inf' value");
                }
                if(bytes[pos] != (byte) 'n' || bytes[pos + 1] != (byte) 'f') {
                    throw new CfgException("invalid value for 'inf'");
                }
                pos += 2;
                out.writeBytes(sign < 0 ? "-Infinity".getBytes(StandardCharsets.US_ASCII) : "Infinity".getBytes(StandardCharsets.US_ASCII));
            }

            private void parseNan(int sign, ByteArrayOutputStream out) {
                if(pos > bytes.length - 2) {
                    throw new CfgException("eof reached while parsing 'nan' value");
                }
                if(bytes[pos] != (byte) 'a' || bytes[pos + 1] != (byte) 'n') {
                    throw new CfgException("invalid value for 'nan'");
                }
                pos += 2;
                out.writeBytes(sign < 0 ? "-NaN".getBytes(StandardCharsets.US_ASCII) : "NaN".getBytes(StandardCharsets.US_ASCII));
            }

            private void parseNumber(int first, ByteArrayOutputStream out) {
                int sign = 0;
                if(first == '+' || first == '-') {
                    if(pos == bytes.length) {
                        throw new CfgException("eof reached while parsing number value");
                    }
                    sign = first == '-' ? -1 : 1;
                    first = bytes[pos++] & 0xFF;
                }
                if(first == '0') {
                    parseZeroOrRadix(sign, out);
                    return ;
                }
                if(first == 'i') {
                    parseInf(sign, out);
                    return ;
                }
                if(first == 'n') {
                    parseNan(sign, out);
                    return ;
                }
                parseDecimal(first, sign, out);
            }

            private void parseZeroOrRadix(int sign, ByteArrayOutputStream out) {
                if(pos == bytes.length) {
                    out.write('0');
                    return ;
                }
                int value = bytes[pos] & 0xFF;
                int radix;
                IntUnaryOperator op;
                if(value == 'x') {
                    radix = 16;
                    op = TomlParser::hexDigit;
                } else if(value == 'o') {
                    radix = 8;
                    op = TomlParser::octalDigit;
                } else if(value == 'b') {
                    radix = 2;
                    op = TomlParser::binaryDigit;
                } else if(value == '.' || value == 'e' || value == 'E') {
                    parseDecimal('0', sign, out);
                    return ;
                } else {
                    out.write('0');
                    return ;
                }
                if(sign != 0) {
                    throw new CfgException("sign not allowed for non-decimal value");
                }
                pos++;
                long acc = 0L;
                boolean expectDigit = true;
                while (pos < bytes.length) {
                    byte v = bytes[pos];
                    if (v == '_') {
                        if (expectDigit) {
                            throw new CfgException("underscore must follow a digit");
                        }
                        expectDigit = true;
                        pos++;
                        continue ;
                    }
                    int digit = op.applyAsInt(v);
                    if(digit < 0) {
                        break ;
                    }
                    try {
                        acc = Math.multiplyExact(acc, radix);
                        acc = Math.addExact(acc, digit);
                    } catch (ArithmeticException e) {
                        throw new CfgException("integer overflow");
                    }
                    expectDigit = false;
                    pos++;
                }
                if (expectDigit) {
                    throw new CfgException("missing digit");
                }
                out.writeBytes(Long.toString(acc).getBytes(StandardCharsets.US_ASCII));
            }

            private void parseDecimal(int first, int sign, ByteArrayOutputStream out) {
                if(sign == 1) {
                    out.write('+');
                } else if(sign == -1) {
                    out.write('-');
                }
                out.write(first);
                boolean expectDigit = false;
                while (pos < bytes.length) {
                    int value = bytes[pos] & 0xFF;
                    boolean special = value == '.' || value == 'e' || value == 'E' || value == '+' || value == '-';
                    if ((value >= '0' && value <= '9') || special) {
                        out.write(value == 'e' ? 'E' : value);
                        expectDigit = special;
                        pos++;
                    }else if(value == '_') {
                        if (expectDigit || pos >= bytes.length - 1 || bytes[pos + 1] < '0' || bytes[pos + 1] > '9') {
                            throw new CfgException("underscore must be surrounded by at least one digit on each side");
                        }
                        expectDigit = true;
                        pos++;
                    } else {
                        break ;
                    }
                }
                String str = out.toString(StandardCharsets.US_ASCII);
                if (expectDigit) {
                    throw new CfgException("corrupted toml number format : " + str);
                }
                try {
                    if (str.indexOf('.') > 0 || str.indexOf('E') > 0) {
                        double _ = Double.parseDouble(str);
                    } else {
                        long _ = Long.parseLong(str);
                    }
                } catch (NumberFormatException e) {
                    throw new CfgException("corrupted toml number format : " + str);
                }
            }
        }
    }
}
