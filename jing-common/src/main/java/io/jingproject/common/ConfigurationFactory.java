package io.jingproject.common;

import io.jingproject.common.conf.DefaultConfigurationFacade;

import java.util.List;
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

    public static String conf(String key) {
        return instance().conf(key);
    }

    public static List<String> confList(String key) {
        return instance().confList(key);
    }

    public static String conf(String key, String defaultValue) {
        String value = conf(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    public static boolean valueAsBoolean(String value, boolean defaultValue) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        } else if ("false".equalsIgnoreCase(value)) {
            return false;
        } else {
            return defaultValue;
        }
    }

    public static boolean confAsBoolean(String key, boolean defaultValue) {
        return valueAsBoolean(conf(key), defaultValue);
    }

    public static int valueAsInt(String value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    public static int confAsInt(String key, int defaultValue) {
        return valueAsInt(conf(key), defaultValue);
    }

    public static long valueAsLong(String value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return Long.parseLong(value);
    }

    public static long confAsLong(String key, long defaultValue) {
        return valueAsLong(conf(key), defaultValue);
    }

    public static float valueAsFloat(String value, float defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return Float.parseFloat(value);
    }

    public static float confAsFloat(String key, float defaultValue) {
        return valueAsFloat(conf(key), defaultValue);
    }

    public static double valueAsDouble(String value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return Double.parseDouble(value);
    }

    public static double confAsDouble(String key, double defaultValue) {
        return valueAsDouble(conf(key), defaultValue);
    }
}
