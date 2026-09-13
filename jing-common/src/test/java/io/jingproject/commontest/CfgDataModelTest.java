package io.jingproject.commontest;

import io.jingproject.common.conf.Cfg.CfgItem;
import io.jingproject.common.conf.Cfg.CfgList;
import io.jingproject.common.conf.Cfg.CfgObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CfgDataModelTest {
    @Test
    public void testAsImmutable() {
        CfgItem item = new CfgItem("x");
        Assertions.assertSame(item, item.asImmutable());

        CfgList list = new CfgList(new ArrayList<>(List.of("a")));
        CfgList immutableList = list.asImmutable();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> immutableList.value().add("b"));

        CfgObject object = new CfgObject(new HashMap<>());
        CfgObject immutableObject = object.asImmutable();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> immutableObject.value().put("k", new CfgItem("v")));
    }
}