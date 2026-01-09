package io.jingproject.common.conf;

import java.util.List;
import java.util.Map;

public sealed interface CfgObject permits CfgObject.CfgItem, CfgObject.CfgList, CfgObject.CfgMap {
    record CfgItem(String value) implements CfgObject {

    }

    record CfgList(List<String> value) implements CfgObject {


    }

    record CfgMap(Map<String, CfgObject> value) implements CfgObject {

    }
}
