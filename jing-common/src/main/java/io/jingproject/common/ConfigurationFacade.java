package io.jingproject.common;

import java.util.List;

public interface ConfigurationFacade {
    String conf(String key);

    List<String> confList(String key);
}
