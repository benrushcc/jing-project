package io.jingproject.common;

import java.util.List;
import java.util.Map;

public interface ConfigurationFacade {
    String item(String key);

    List<String> itemList(String key);

    Map<String, String> itemMap(String key);
}
