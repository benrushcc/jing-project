package io.jingproject.common.conf;

import io.jingproject.common.ConfigurationFacade;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 简化报错信息，可以分为几种，EOF，或者duplicate 或者corrupted 或者empty，核心就是这几个
public final class DefaultConfigurationFacade implements ConfigurationFacade {
    private static final int MAX_DEPTH = 128;
    // 搜索的优先级顺序是先toml，再json，最后properties
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
        for(String ext : fileExts) {
            if(!SUPPORTED_FILE_EXT.contains(fileExt)) {
                throw new CfgException("Unsupported configuration file extension: " + ext);
            }
            String fullName = fileName + "." + ext;
            InputStream rawStream = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream(fullName);
            if(rawStream == null) {
                continue ;
            }
            try (InputStream stream = new BufferedInputStream(rawStream)) {
                r = switch (ext) {
                    case "toml" -> new TomlCfgReader(stream).parse();
                    case "json" -> new JsonCfgReader(stream).parse();
                    case "properties" -> new PropertiesCfgReader(stream).parse();
                    default -> throw new AssertionError();
                };
                break;
            } catch (IOException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
        if (r == null) {
            throw new CfgException("Configuration file not found, filename : " + fileName + " , exts: " + fileExts);
        }
        return r;
    }

    @Override
    public String conf(String key) {
        CfgObject cfgObject = getConfiguration();
        List<String> nestedkeys = CfgUtil.readCfgNestedKey(key.getBytes(StandardCharsets.UTF_8));
        switch (nestedkeys.size()) {
            case 0 -> throw new CfgException("Invalid key: " + key);
            case 1 -> {
                Cfg cfg = cfgObject.value().get(nestedkeys.getLast());
                if(cfg == null) {
                    return null;
                }
                if(cfg instanceof CfgItem(String value)) {
                    return value;
                }
                throw new CfgException("Invalid key : " + key + ", type : " + cfg.type());
            }
            default -> {
                CfgObject current = cfgObject;
                for (String nestedKey : nestedkeys.subList(1, Math.subtractExact(nestedkeys.size(), 1))) {
                    Cfg cfg = current.value().get(nestedKey);
                    if(cfg == null) {
                        return null;
                    }
                    if(cfg instanceof CfgObject co) {
                        current = co;
                    } else {
                        throw new CfgException("Invalid key : " + key + ", current nested key: " + nestedKey + ", type : " + cfg.type());
                    }
                }
                Cfg cfg = current.value().get(nestedkeys.getLast());
                if(cfg == null) {
                    return null;
                }
                if(cfg instanceof CfgItem(String value)) {
                    return value;
                }
                throw new CfgException("Invalid key : " + key + ", type : " + cfg.type());
            }
        }
    }

    @Override
    public List<String> confList(String key) {
        CfgObject cfgObject = getConfiguration();
        List<String> nestedkeys = CfgUtil.readCfgNestedKey(key.getBytes(StandardCharsets.UTF_8));
        switch (nestedkeys.size()) {
            case 0 -> throw new CfgException("Invalid key: " + key);
            case 1 -> {
                Cfg cfg = cfgObject.value().get(nestedkeys.getLast());
                if(cfg == null) {
                    return null;
                }
                if(cfg instanceof CfgList(List<String> value)) {
                    return value;
                }
                throw new CfgException("Invalid key : " + key + ", type : " + cfg.type());
            }
            default -> {
                CfgObject current = cfgObject;
                for (String nestedKey : nestedkeys.subList(1, Math.subtractExact(nestedkeys.size(), 1))) {
                    Cfg cfg = current.value().get(nestedKey);
                    if(cfg == null) {
                        return null;
                    }
                    if(cfg instanceof CfgObject co) {
                        current = co;
                    } else {
                        throw new CfgException("Invalid key : " + key + ", current nested key: " + nestedKey + ", type : " + cfg.type());
                    }
                }
                Cfg cfg = current.value().get(nestedkeys.getLast());
                if(cfg == null) {
                    return null;
                }
                if(cfg instanceof CfgList(List<String> value)) {
                    return value;
                }
                throw new CfgException("Invalid key : " + key + ", type : " + cfg.type());
            }
        }
    }
}
