package io.jingproject.common.conf;

public sealed interface Cfg permits CfgItem, CfgList, CfgObject {
    String type();
}
