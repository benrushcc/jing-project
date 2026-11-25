package io.jingproject.common;

import io.jingproject.common.conf.CfgException;
import io.jingproject.common.conf.DefaultConfigurationFacade;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

public final class ConfigurationFactory {
    private static final LazyConstant<ConfigurationFacade> FACTORY = LazyConstant.of(() -> Anchor.compute(ConfigurationFacade.class, () -> {
        Optional<ConfigurationFacade> cf = ServiceLoader.load(ConfigurationFacade.class).findFirst();
        return cf.orElseGet(DefaultConfigurationFacade::new);
    }));

    private ConfigurationFactory() {
        throw new UnsupportedOperationException("utility class");
    }

    public static String conf(String key) {
        return FACTORY.get().conf(key);
    }

    public static List<String> confList(String key) {
        return FACTORY.get().confList(key);
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
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new CfgException("illegal number format for int value : " + value);
        }
    }

    public static int confAsInt(String key, int defaultValue) {
        return valueAsInt(conf(key), defaultValue);
    }

    public static long valueAsLong(String value, long defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new CfgException("illegal number format for long value : " + value);
        }
    }

    public static long confAsLong(String key, long defaultValue) {
        return valueAsLong(conf(key), defaultValue);
    }

    public static float valueAsFloat(String value, float defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            throw new CfgException("illegal number format for float value : " + value);
        }
    }

    public static float confAsFloat(String key, float defaultValue) {
        return valueAsFloat(conf(key), defaultValue);
    }

    public static double valueAsDouble(String value, double defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new CfgException("illegal number format for double value : " + value);
        }
    }

    public static double confAsDouble(String key, double defaultValue) {
        return valueAsDouble(conf(key), defaultValue);
    }


}
