package io.jingproject.common.conf;

import java.util.List;

public record CfgList(List<String> value) implements Cfg {
    @Override
    public String type() {
        return "List";
    }

    public CfgList asImmutable() {
        return new CfgList(List.copyOf(value));
    }
}
