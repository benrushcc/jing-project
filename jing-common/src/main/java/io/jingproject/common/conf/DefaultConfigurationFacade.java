package io.jingproject.common.conf;

import io.jingproject.common.ConfigurationFacade;
import io.jingproject.common.conf.Cfg.CfgItem;
import io.jingproject.common.conf.Cfg.CfgList;
import io.jingproject.common.conf.Cfg.CfgObject;
import io.jingproject.common.conf.CfgReader.JsonCfgReader;
import io.jingproject.common.conf.CfgReader.PropertiesCfgReader;
import io.jingproject.common.conf.CfgReader.TomlCfgReader;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

// simplified error messages: EOF, duplicate, corrupted, or empty
public final class DefaultConfigurationFacade implements ConfigurationFacade {
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

    // search priority: toml, then json, then properties
    private static final List<String> SUPPORTED_FILE_EXT = List.of("toml", "json", "properties");

    private static CfgObject getConfiguration() {
        class Holder {
            static final CfgObject INSTANCE = createConfiguration();
        }
        return Holder.INSTANCE;
    }

    private static CfgObject createConfiguration() {
        String fileName = System.getProperty("jing.config.file", "jing-config");
        String fileExt = System.getProperty("jing.config.ext", "").trim();
        List<String> fileExts = fileExt.isBlank() ? SUPPORTED_FILE_EXT : List.of(fileExt);
        CfgObject r = null;
        for (String ext : fileExts) {
            if (!SUPPORTED_FILE_EXT.contains(ext)) {
                throw new CfgException("unsupported configuration file extension: " + ext);
            }
            String fullName = fileName + "." + ext;
            InputStream rawStream = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream(fullName);
            if (rawStream == null) {
                continue;
            }
            try (InputStream stream = new BufferedInputStream(rawStream)) {
                r = switch (ext) {
                    case "toml" -> new TomlCfgReader(stream).parse(MAX_DEPTH);
                    case "json" -> new JsonCfgReader(stream).parse(MAX_DEPTH);
                    case "properties" -> new PropertiesCfgReader(stream).parse(MAX_DEPTH);
                    default -> throw new AssertionError();
                };
                break;
            } catch (IOException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
        if (r == null) {
            throw new CfgException("configuration file not found, filename : " + fileName + " , exts: " + fileExts);
        }
        return r;
    }

    @Override
    public String conf(String key) {
        CfgObject cfgObject = getConfiguration();
        List<String> nestedkeys = CfgUtil.readCfgNestedKey(key.getBytes(StandardCharsets.UTF_8), MAX_DEPTH);
        switch (nestedkeys.size()) {
            case 0 -> throw new CfgException("invalid key: " + key);
            case 1 -> {
                Cfg cfg = cfgObject.value().get(nestedkeys.getLast());
                if (cfg == null) {
                    return null;
                }
                if (cfg instanceof CfgItem(String value)) {
                    return value;
                }
                throw new CfgException("invalid key : " + key + ", type : " + cfg.getClass().getSimpleName());
            }
            default -> {
                CfgObject current = cfgObject;
                for (String nestedKey : nestedkeys.subList(0, Math.subtractExact(nestedkeys.size(), 1))) {
                    Cfg cfg = current.value().get(nestedKey);
                    if (cfg == null) {
                        return null;
                    }
                    if (cfg instanceof CfgObject co) {
                        current = co;
                    } else {
                        throw new CfgException("invalid key : " + key + ", current nested key: " + nestedKey + ", type : " + cfg.getClass().getSimpleName());
                    }
                }
                Cfg cfg = current.value().get(nestedkeys.getLast());
                if (cfg == null) {
                    return null;
                }
                if (cfg instanceof CfgItem(String value)) {
                    return value;
                }
                throw new CfgException("invalid key : " + key + ", type : " + cfg.getClass().getSimpleName());
            }
        }
    }

    @Override
    public List<String> confList(String key) {
        CfgObject cfgObject = getConfiguration();
        List<String> nestedkeys = CfgUtil.readCfgNestedKey(key.getBytes(StandardCharsets.UTF_8), MAX_DEPTH);
        switch (nestedkeys.size()) {
            case 0 -> throw new CfgException("invalid key: " + key);
            case 1 -> {
                Cfg cfg = cfgObject.value().get(nestedkeys.getLast());
                if (cfg == null) {
                    return null;
                }
                if (cfg instanceof CfgList(List<String> value)) {
                    return value;
                }
                throw new CfgException("invalid key : " + key + ", type : " + cfg.getClass().getSimpleName());
            }
            default -> {
                CfgObject current = cfgObject;
                for (String nestedKey : nestedkeys.subList(0, Math.subtractExact(nestedkeys.size(), 1))) {
                    Cfg cfg = current.value().get(nestedKey);
                    if (cfg == null) {
                        return null;
                    }
                    if (cfg instanceof CfgObject co) {
                        current = co;
                    } else {
                        throw new CfgException("invalid key : " + key + ", current nested key: " + nestedKey + ", type : " + cfg.getClass().getSimpleName());
                    }
                }
                Cfg cfg = current.value().get(nestedkeys.getLast());
                if (cfg == null) {
                    return null;
                }
                if (cfg instanceof CfgList(List<String> value)) {
                    return value;
                }
                throw new CfgException("invalid key : " + key + ", type : " + cfg.getClass().getSimpleName());
            }
        }
    }
}
