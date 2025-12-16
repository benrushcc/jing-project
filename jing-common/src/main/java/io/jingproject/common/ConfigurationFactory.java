package io.jingproject.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

public final class ConfigurationFactory {

    private ConfigurationFactory() {
        throw new UnsupportedOperationException("utility class");
    }

    private static ConfigurationFacade instance() {
        class Holder {
            static final ConfigurationFacade INSTANCE = Anchor.compute(ConfigurationFacade.class, () -> {
                Optional<ConfigurationFacade> cf = ServiceLoader.load(ConfigurationFacade.class).findFirst();
                return cf.orElseGet(DefaultConfigurationFacade::new);
            });
        }
        return Holder.INSTANCE;
    }

    public static String item(String key) {
        return instance().item(key);
    }

    public static Map<String, String> items(String prefix) {
        return instance().items(prefix);
    }

    public static String item(String key, String defaultValue) {
        String value = item(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    public static boolean valueAsBoolean(String value, boolean defaultValue) {
        if("true".equalsIgnoreCase(value)) {
            return true;
        } else if("false".equalsIgnoreCase(value)) {
            return false;
        } else {
            return defaultValue;
        }
    }

    public static boolean itemAsBoolean(String key, boolean defaultValue) {
        return valueAsBoolean(item(key), defaultValue);
    }

    public static int valueAsInt(String value, int defaultValue) {
        if(value == null) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    public static int itemAsInt(String key, int defaultValue) {
        return valueAsInt(item(key), defaultValue);
    }

    public static long valueAsLong(String value, long defaultValue) {
        if(value == null) {
            return defaultValue;
        }
        return Long.parseLong(value);
    }

    public static long itemAsLong(String key, long defaultValue) {
        return valueAsLong(item(key), defaultValue);
    }

    public static float valueAsFloat(String value, float defaultValue) {
        if(value == null) {
            return defaultValue;
        }
        return Float.parseFloat(value);
    }

    public static float itemAsFloat(String key, float defaultValue) {
        return valueAsFloat(item(key), defaultValue);
    }

    public static double valueAsDouble(String value, double defaultValue) {
        if(value == null) {
            return defaultValue;
        }
        return Double.parseDouble(value);
    }

    public static double itemAsDouble(String key, double defaultValue) {
        return valueAsDouble(item(key), defaultValue);
    }

    sealed interface ConfigrationObject {

    }

    record ConfigurationItem(String value) implements ConfigrationObject {

    }

    record ConfigurationSet(Map<String, ConfigrationObject> value) implements ConfigrationObject {

    }

    /**
     *   Default configuration facade would read JSON file as input
     */
    static final class DefaultConfigurationFacade implements ConfigurationFacade {

        private static final int MAX_NESTED_LIMITS = 128;

        private static final ConfigurationSet ROOT;

        static {
            String fileName = System.getProperty("jing.config.file", "jing-config.json");
            try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName)) {
                if(stream == null) {
                    // Given an empty configuration by default
                    ROOT = new ConfigurationSet(Map.of());
                } else {
                    byte[] content = stream.readAllBytes();
                    ROOT = parseConfigurationSet(content, 0, new HashMap<>(), 0);
                }
            } catch (IOException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        @FunctionalInterface
        interface ByteConsumer{
            boolean accept(byte b);
        }

        private static int searchByte(byte[] content, int fromIndex, ByteConsumer consumer) throws IOException {
            if(fromIndex < 0 || fromIndex >= content.length) {
                throw new IOException("Json file format corrupted");
            }
            for(int i = fromIndex; i < content.length; i++) {
                if(consumer.accept(content[i])) {
                    return i + 1;
                }
            }
            throw new IOException("Invalid json structure");
        }


        private static ConfigurationSet parseConfigurationSet(byte[] content, int index, Map<String, ConfigrationObject> current, int steps) throws IOException {
            if(steps > MAX_NESTED_LIMITS) {
                throw new IOException("Json file max nested limit exceeded");
            }
            int nextIndex;
            for( ; ; ) {
                index = searchByte(content, index, b -> b == (byte) '\"' || b == (byte) '}');
                if(content[index - 1] == (byte) '}') {
                    throw new IOException("Empty json is not allowed");
                }
                nextIndex = searchByte(content, index, b -> b == (byte) '"');
                String key = readJsonString(content, index, nextIndex - 1);
                index = searchByte(content, nextIndex, b -> b == (byte) ':');
                index = searchByte(content, index, b -> b == (byte) '\"' || b == (byte) '{');
                if(content[index - 1] == '{') {
                    ConfigurationSet value = parseConfigurationSet(content, index, new HashMap<>(), steps + 1);
                    current.put(key, value);
                } else if(content[index - 1] == '\"') {
                    nextIndex = searchByte(content, index, b -> b == (byte) '\"');
                    String value = new String(content, index, nextIndex - index - 1, StandardCharsets.UTF_8);
                    current.put(key, new ConfigurationItem(value));
                }
                index = searchByte(content, index, b -> b == (byte) ',' || b == (byte) '}');
                if(content[index - 1] == '}') {
                    return new ConfigurationSet(Map.copyOf(current));
                }
            }
        }

        private static String readJsonString(byte[] content, int startIndex, int endIndex) throws IOException {
            byte[] r = new byte[endIndex - startIndex];
            int rIndex = 0;
            boolean escaping = false;
            for(int i = startIndex; i < endIndex; i++) {
                byte b = content[i];
                if(escaping) {
                    switch (b) {
                        case (byte) '\"'-> r[rIndex++] = (byte)  '\"';
                        case (byte) '\\'-> r[rIndex++] = (byte)  '\\';
                        case (byte) '/' -> r[rIndex++] = (byte)  '/';
                        case (byte) 'b' -> r[rIndex++] = (byte)  '\b';
                        case (byte) 'f' -> r[rIndex++] = (byte)  '\f';
                        case (byte) 'n' -> r[rIndex++] = (byte)  '\n';
                        case (byte) 'r' -> r[rIndex++] = (byte)  '\r';
                        case (byte) 't' -> r[rIndex++] = (byte)  '\t';
                        case (byte) 'u' -> {
                            if (i + 4 >= endIndex) break;
                            int c = 0;
                            for (int j = 0; j < 4; ++j) {
                                byte hb = content[++i];
                                if (isHexDigit(hb)) {
                                    c = (c << 4) | hexToInt(hb);
                                } else {
                                    throw new IOException("Bad unicode character");
                                }
                            }
                            if (c < 0x80) {
                                r[rIndex++] = (byte) c;
                            } else if (c < 0x800) {
                                r[rIndex++] = (byte) (0xC0 | (c >> 6));
                                r[rIndex++] = (byte) (0x80 | (c & 0x3F));
                            } else {
                                r[rIndex++] = (byte) (0xE0 | (c >> 12));
                                r[rIndex++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                                r[rIndex++] = (byte) (0x80 | (c & 0x3F));
                            }
                        }
                        default -> {
                            r[rIndex++] = '\\';
                            r[rIndex++] = b;
                        }
                    }
                    escaping = false;
                } else {
                    if(b == (byte) '\\') {
                        escaping = true;
                    }else {
                        r[rIndex++] = b;
                    }
                }
            }
            return new String(r, 0, rIndex, StandardCharsets.UTF_8);
        }

        private static boolean isHexDigit(byte b) {
            return (b >= '0' && b <= '9') ||
                    (b >= 'a' && b <= 'f') ||
                    (b >= 'A' && b <= 'F');
        }

        private static int hexToInt(byte b) {
            if (b >= '0' && b <= '9') return b - '0';
            if (b >= 'a' && b <= 'f') return b - 'a' + 10;
            return b - 'A' + 10;
        }

        @Override
        public String item(String key) {
            ConfigurationSet current = ROOT;
            String[] ss = key.split("\\.");
            for (int i = 0; i < ss.length; i++) {
                ConfigrationObject o = current.value().get(ss[i]);
                switch (o) {
                    case ConfigurationSet cs -> current = cs;
                    case ConfigurationItem(String value) -> {
                        return i == ss.length - 1 ? value : null;
                    }
                    default -> {
                        return null;
                    }
                }
            }
            return null;
        }

        @Override
        public Map<String, String> items(String prefix) {
            ConfigurationSet current = ROOT;
            String[] ss = prefix.split("\\.");
            for (String string : ss) {
                ConfigrationObject o = current.value().get(string);
                if (o instanceof ConfigurationSet cs) {
                    current = cs;
                } else {
                    return Map.of();
                }
            }
            if(current == null) {
                return Map.of();
            }
            Map<String, String> r = new HashMap<>();
            current.value().forEach((k, v) -> {
                if(v instanceof ConfigurationItem(String s)) {
                    r.put(k, s);
                }
            });
            return Map.copyOf(r);
        }
    }
}
