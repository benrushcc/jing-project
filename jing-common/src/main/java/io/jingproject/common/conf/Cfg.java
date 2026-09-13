package io.jingproject.common.conf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public sealed interface Cfg {
    record CfgItem(String value) implements Cfg {
        public CfgItem asImmutable() {
            return this;
        }
    }

    record CfgList(List<String> value) implements Cfg {
        public CfgList asImmutable() {
            return new CfgList(List.copyOf(value));
        }
    }

    record CfgObject(Map<String, Cfg> value) implements Cfg {
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
}