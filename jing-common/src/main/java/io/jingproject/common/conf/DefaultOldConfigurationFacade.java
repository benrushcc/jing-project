package io.jingproject.common.conf;

import io.jingproject.common.ConfigurationFacade;
import io.jingproject.common.conf.Cfg.CfgItem;
import io.jingproject.common.conf.Cfg.CfgList;
import io.jingproject.common.conf.Cfg.CfgObject;
import io.jingproject.common.conf.CfgOldReader.JsonCfgOldReader;
import io.jingproject.common.conf.CfgOldReader.PropertiesCfgOldReader;
import io.jingproject.common.conf.CfgOldReader.TomlCfgOldReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

// simplified error messages: EOF, duplicate, corrupted, or empty
public final class DefaultOldConfigurationFacade implements ConfigurationFacade {
    // search priority: toml, then json, then properties
    private static final List<String> SUPPORTED_FILE_EXT = List.of("toml", "json", "properties");
    private static final int DEFAULT_MAX_DEPTH = 128;
    private static final int MIN_MAX_DEPTH = 4;
    private static final int MAX_DEPTH;

    static {
        String raw = System.getenv("JING_CONFIG_MAX_DEPTH");
        int value = DEFAULT_MAX_DEPTH;
        if (raw != null && !raw.isBlank()) {
            value = Integer.parseInt(raw.trim());
        }
        if (value <= MIN_MAX_DEPTH) {
            throw new ExceptionInInitializerError("max depth must be greater than " + MIN_MAX_DEPTH + ", actual: " + value);
        }
        MAX_DEPTH = value;
    }

    private final CfgObject root = createConfiguration();

    private static CfgObject createConfiguration() {
        String fileName = System.getProperty("jing.config.file", "jing-config").trim();
        String fileExt = System.getProperty("jing.config.ext", "").trim();
        List<String> fileExts = fileExt.isBlank() ? SUPPORTED_FILE_EXT : List.of(fileExt);
        CfgObject r = null;
        for (String ext : fileExts) {
            String fullName = fileName + "." + ext;
            try (InputStream stream = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream(fullName)) {
                if(stream == null) {
                    continue ;
                }
                r = switch (ext) {
                    case "toml" -> new TomlCfgOldReader(stream).parse(MAX_DEPTH);
                    case "json" -> new JsonCfgOldReader(stream).parse(MAX_DEPTH);
                    case "properties" -> new PropertiesCfgOldReader(stream).parse(MAX_DEPTH);
                    default -> throw new CfgException("unsupported configuration file extension : " + ext);
                };
                break;
            } catch (IOException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
        if (r == null) {
            throw new CfgException("configuration file not found, filename : " + fileName + " , exts: " + fileExts);
        }
        return r.asImmutable();
    }

    @Override
    public String conf(String key) {
        Cfg cfg = navigate(key);
        if (cfg == null) {
            return null;
        }
        if (cfg instanceof CfgItem(String value)) {
            return value;
        }
        throw new CfgException("invalid key: " + key + ", type : " + cfg.getClass().getSimpleName());
    }

    private Cfg navigate(String key) {
        if (key == null || key.isBlank()) {
            throw new CfgException("empty key not supported");
        }
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        List<String> nestedkeys = CfgOldUtil.readCfgNestedKey(keyBytes, 0, keyBytes.length, MAX_DEPTH);
        CfgObject current = root;
        for (String nestedKey : nestedkeys.subList(0, Math.subtractExact(nestedkeys.size(), 1))) {
            Cfg cfg = current.value().get(nestedKey);
            if (cfg == null) {
                return null;
            }
            if (cfg instanceof CfgObject co) {
                current = co;
            } else {
                throw new CfgException("invalid key: " + key + ", current nested key: " + nestedKey
                        + ", type : " + cfg.getClass().getSimpleName());
            }
        }
        return current.value().get(nestedkeys.getLast());
    }

    @Override
    public List<String> confList(String key) {
        Cfg cfg = navigate(key);
        if (cfg == null) {
            return null;
        }
        if (cfg instanceof CfgList(List<String> value)) {
            return List.copyOf(value);
        }
        throw new CfgException("invalid key: " + key + ", type : " + cfg.getClass().getSimpleName());
    }
}
