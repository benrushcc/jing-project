package io.jingproject.common.conf;

public record CfgItem(String value) implements Cfg {
    @Override
    public String type() {
        return "Item";
    }

    public CfgItem asImmutable() {
        return this;
    }
}
