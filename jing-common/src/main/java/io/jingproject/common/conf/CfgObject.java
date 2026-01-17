package io.jingproject.common.conf;

import java.util.HashMap;
import java.util.Map;

public record CfgObject(Map<String, Cfg> value) implements Cfg {
    @Override
    public String type() {
        return "Object";
    }

    public CfgObject asImmutable() {
        Map<String, Cfg> r = new HashMap<>(value.size());
        for (Map.Entry<String, Cfg> entry : value.entrySet()) {
            switch (entry.getValue()) {
                case CfgItem cfgItem -> r.put(entry.getKey(), cfgItem.asImmutable());
                case CfgList cfgList -> r.put(entry.getKey(), cfgList.asImmutable());
                case CfgObject cfgObject -> r.put(entry.getKey(), cfgObject.asImmutable());
                default -> throw new AssertionError();
            }
        }
        return new CfgObject(Map.copyOf(r));
    }
}
