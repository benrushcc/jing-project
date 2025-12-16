package io.jingproject.common;

import java.util.Map;

public interface ConfigurationFacade {
    String item(String key);

    Map<String, String> items(String prefix);
}
