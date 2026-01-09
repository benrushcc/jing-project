package io.jingproject.common.conf;

import io.jingproject.common.ConfigurationFacade;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DefaultConfigurationFacade implements ConfigurationFacade {
    private static final int MAX_DEPTH = 128;
    private static final List<String> SUPPORTED_FILE_EXT = List.of("toml", "json", "properties");
    private static final CfgObject.CfgMap CFG_MAP;
    static {
        String configFileName = System.getProperty("jing.config.file", "jing-config");
        String configFileExt = System.getProperty("jing.config.ext", "");
        CfgObject.CfgMap cfgMap = null;
        if(configFileExt.isBlank()) {
            for(String ext : SUPPORTED_FILE_EXT) {
                try(InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(configFileName + "." + ext)) {
                    if(stream != null) {
                        cfgMap = createRootMap(stream, ext);
                        break;
                    }
                } catch (IOException e) {
                    throw new ExceptionInInitializerError(e);
                }
            }
        } else {
            try(InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(configFileName + "." + configFileExt)) {
                if(stream != null) {
                    cfgMap = createRootMap(stream, configFileExt);
                }
            } catch (IOException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
        if(cfgMap == null) {
            throw new ExceptionInInitializerError("Configuration file not found, filename : " + configFileName + " , ext: " + configFileExt);
        }
        CFG_MAP = cfgMap;
    }

    private static CfgObject.CfgMap createRootMap(InputStream stream, String configFileExt) throws IOException {
        return switch(configFileExt) {
            case "toml" -> createTomlRootMap(stream);
            case "json" -> createJsonRootMap(stream);
            case "properties" -> createPropertiesRootMap(stream);
            default -> throw new IllegalArgumentException("Unknown config file extension: " + configFileExt);
        };
    }

    private static CfgObject.CfgMap createTomlRootMap(InputStream stream) throws IOException {
        // TODO
        return null;
    }

    private static CfgObject.CfgMap createJsonRootMap(InputStream stream) throws IOException {
        return parseJsonCfgMap(stream, new HashMap<>(), 0);
    }

    private static CfgObject.CfgMap createPropertiesRootMap(InputStream stream) throws IOException {
        // TODO
        return null;
    }

    record SearchResult(byte[] content, byte target) {

    }

    private static CfgObject.CfgMap parseJsonCfgMap(InputStream stream, Map<String, CfgObject> current, int steps) throws IOException {
        if(steps > MAX_DEPTH) {
            throw new IOException("Maximum json nesting depth exceeded");
        }
        for( ; ; ) {
            if (search(stream, (byte) '"', (byte) '}').target() == (byte) '}') {
                return new CfgObject.CfgMap(Map.copyOf(current));
            }
            byte[] keyBytes = search(stream, (byte) '"').content();
            String key = readKey(keyBytes);
            search(stream, (byte) ':');
            SearchResult r = search(stream, (byte) '{', (byte) '[', (byte) '"');
            if(r.target() == (byte) '{') {
                CfgObject.CfgMap m = parseJsonCfgMap(stream, new HashMap<>(), Math.addExact(steps, 1));
                current.put(key, m);
            } else if(r.target() == (byte) '[') {
                CfgObject.CfgList m = parseJsonCfgList(stream, new ArrayList<>());
                current.put(key, m);
            } else if(r.target() == (byte) '"') {
                CfgObject.CfgItem m = parseJsonCfgItem(stream);
                current.put(key, m);
            } else {
                throw new CfgFormatException("Should never be reached");
            }
            if (search(stream, (byte) ',', (byte) '}').target() == (byte) '}') {
                return new CfgObject.CfgMap(Map.copyOf(current));
            }
        }
    }

    private static CfgObject.CfgList parseJsonCfgList(InputStream stream, List<String> current) throws IOException {
        for( ; ; ) {
            search(stream, (byte) '"');
            byte[] valueBytes = search(stream, (byte) '"').content();
            current.add(readJsonValueString(valueBytes));
            if (search(stream, (byte) ',', (byte) ']').target() == (byte) ']') {
                return new CfgObject.CfgList(List.copyOf(current));
            }
        }
    }

    private static CfgObject.CfgItem parseJsonCfgItem(InputStream stream) throws IOException {
        byte[] bytes = search(stream, (byte) '"').content();
        return new CfgObject.CfgItem(readJsonValueString(bytes));
    }

    private static SearchResult search(InputStream stream, byte... targets) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int b;
        while((b = stream.read()) != -1) {
            for (byte target : targets) {
                if(b == Byte.toUnsignedInt(target)) {
                    return new SearchResult(baos.toByteArray(), target);
                }
            }
            baos.write(b);
        }
        throw new CfgFormatException("Invalid search result, EOF reached");
    }

    private static String readKey(byte[] content) {
        for (byte b : content) {
            if(!validateKey(b)) {
                throw new CfgFormatException("Invalid key byte occurred: " + b);
            }
        }
        return new String(content, StandardCharsets.UTF_8);
    }

    private static boolean validateKey(byte b) {
        return (b >= 'a' && b <= 'z') || (b >= 'A' && b <= 'Z') || (b >= '0' && b <= '9') || b == '-' || b == '_';
    }

    private static String readJsonValueString(byte[] content) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(content.length);
        boolean escaping = false;
        for(int i = 0; i < content.length; i++) {
            byte b = content[i];
            if(escaping) {
                switch (b) {
                    case (byte) '\"'-> baos.write('\"');
                    case (byte) '\\'-> baos.write('\\');
                    case (byte) '/' -> baos.write('/');
                    case (byte) 'b' -> baos.write('\b');
                    case (byte) 'f' -> baos.write('\f');
                    case (byte) 'n' -> baos.write('\n');
                    case (byte) 'r' -> baos.write('\r');
                    case (byte) 't' -> baos.write('\t');
                    case (byte) 'u' -> {
                        if (i + 4 >= content.length) {
                            throw new IOException("Invalid json content, unicode must be exactly four hexadecimal digits");
                        }
                        int c = 0;
                        for (int j = 0; j < 4; ++j) {
                            byte hb = content[++i];
                            if (isHexDigit(hb)) {
                                c = (c << 4) | hexToInt(hb);
                            } else {
                                throw new IOException("Bad unicode character");
                            }
                        }
                        writeUtf8Codepoint(baos, c);
                    }
                    default -> {
                        baos.write('\\');
                        baos.write(b);
                    }
                }
                escaping = false;
            } else {
                if(b == (byte) '\\') {
                    escaping = true;
                }else {
                    baos.write(b);
                }
            }
        }
        return baos.toString(StandardCharsets.UTF_8);
    }

    private static boolean isHexDigit(byte b) {
        return (b >= '0' && b <= '9') ||
                (b >= 'a' && b <= 'f') ||
                (b >= 'A' && b <= 'F');
    }

    private static int hexToInt(byte b) {
        if (b >= '0' && b <= '9') {
            return b - '0';
        } else if (b >= 'a' && b <= 'f') {
            return b - 'a' + 10;
        } else {
            return b - 'A' + 10;
        }
    }

    private static void writeUtf8Codepoint(ByteArrayOutputStream out, int c) {
        if (c < 0x80) {
            out.write(c);
        } else if (c < 0x800) {
            out.write(0xC0 | (c >> 6));
            out.write(0x80 | (c & 0x3F));
        } else if (c < 0x10000) {
            out.write(0xE0 | (c >> 12));
            out.write(0x80 | ((c >> 6) & 0x3F));
            out.write(0x80 | (c & 0x3F));
        } else {
            out.write(0xF0 | (c >> 18));
            out.write(0x80 | ((c >> 12) & 0x3F));
            out.write(0x80 | ((c >> 6) & 0x3F));
            out.write(0x80 | (c & 0x3F));
        }
    }

    @Override
    public String item(String key) {
        return "";
    }

    @Override
    public List<String> itemList(String key) {
        return List.of();
    }

    @Override
    public Map<String, String> itemMap(String key) {
        return Map.of();
    }
}
