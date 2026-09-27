package io.jingproject.common.conf;

import io.jingproject.common.ConfigurationFacade;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public final class DefaultConfigurationFacade implements ConfigurationFacade {
    private static final int DEFAULT_MAX_DEPTH = 16;
    private static final int MIN_MAX_DEPTH = 4;

    private final Cfg.CfgObject root = createConfiguration();

    // search priority: toml, then json, then properties
    private static Cfg.CfgObject createConfiguration() {
        int maxDepth = Integer.getInteger("jing.config.maxdepth", DEFAULT_MAX_DEPTH);
        if(maxDepth < MIN_MAX_DEPTH) {
            throw new CfgException("configuration depth out of range : " + maxDepth);
        }
        String fileName = System.getProperty("jing.config.file", "jing-config").trim();
        String fileExt = System.getProperty("jing.config.ext", "").trim();
        List<String> fileExts = fileExt.isBlank() ? List.of("toml", "json", "properties") : List.of(fileExt);
        for (String ext : fileExts) {
            String fullName = fileName + "." + ext;
            try (InputStream stream = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream(fullName)) {
                if(stream == null) {
                    continue ;
                }
                return switch (ext) {
                    case "toml" -> new CfgReader.TomlCfgReader().parse(stream, maxDepth);
                    case "json" -> new CfgReader.JsonCfgReader().parse(stream, maxDepth);
                    case "properties" -> new CfgReader.PropertiesCfgReader().parse(stream, maxDepth);
                    default -> throw new CfgException("unknown file extension format : " + ext);
                };
            } catch (IOException e) {
                throw new CfgException("failed to init root configuration", e);
            }
        }
        throw new CfgException("configuration file not found for all the extension format");
    }

    private Cfg nav(String key) {
        if(key == null || key.isBlank()) {
            throw new CfgException("empty key not supported");
        }
        Cfg.CfgObject current = root;
        int index = 0;
        for( ; ; ) {
            int end = key.indexOf('.', index);
            if(end < 0) {
                break ;
            }
            if (end == index) {
                throw new CfgException("invalid consecutive delimiters");
            }
            String nestedKey = key.substring(index, end);
            Cfg cfg = current.value().get(nestedKey);
            if (cfg == null) {
                return null;
            }
            if(cfg instanceof Cfg.CfgObject co) {
                current = co;
                index = end + 1;
            } else {
                throw new CfgException("invalid key: " + key);
            }
        }
        if (index == key.length()) {
            throw new CfgException("invalid delimiters");
        }
        return current.value().get(key.substring(index));
    }

    @Override
    public String conf(String key) {
        Cfg cfg = nav(key);
        if (cfg == null) {
            return null;
        }
        if (cfg instanceof Cfg.CfgItem(String value)) {
            return value;
        }
        throw new CfgException("invalid key: " + key + ", type : " + cfg.getClass().getSimpleName());
    }

    @Override
    public List<String> confList(String key) {
        Cfg cfg = nav(key);
        if (cfg == null) {
            return null;
        }
        if (cfg instanceof Cfg.CfgList(List<String> value)) {
            return value;
        }
        throw new CfgException("invalid key: " + key + ", type : " + cfg.getClass().getSimpleName());
    }
}
