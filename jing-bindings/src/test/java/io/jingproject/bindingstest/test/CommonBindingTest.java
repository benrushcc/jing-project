package io.jingproject.bindingstest.test;

import io.jingproject.bindings.CommonBinding;
import io.jingproject.common.Utils;
import io.jingproject.ffm.Libs;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CommonBindingTest {
    private static final CommonBinding COMMON_BINDING = Libs.impl(CommonBinding.class);

    static {
        if(COMMON_BINDING == null) {
            throw new ExceptionInInitializerError("native library not available");
        }
    }

    @Test
    public void versionTest() {
        Assertions.assertEquals(COMMON_BINDING.majorVersion(), Utils.majorVersion());
        Assertions.assertEquals(COMMON_BINDING.minorVersion(), Utils.minorVersion());
        Assertions.assertEquals(COMMON_BINDING.patchVersion(), Utils.patchVersion());
        Assertions.assertEquals(COMMON_BINDING.versionString(), Utils.versionString());
    }
}
