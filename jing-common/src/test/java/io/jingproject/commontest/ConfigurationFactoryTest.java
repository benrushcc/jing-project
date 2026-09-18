package io.jingproject.commontest;

import io.jingproject.common.ConfigurationFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ConfigurationFactoryTest {
    @Test
    public void testValueAsIntBlankIsDefault() {
        Assertions.assertEquals(7, ConfigurationFactory.valueAsInt("", 7));
        Assertions.assertEquals(7, ConfigurationFactory.valueAsInt("   ", 7));
        Assertions.assertEquals(42, ConfigurationFactory.valueAsInt("42", 7));
        Assertions.assertEquals(7, ConfigurationFactory.valueAsInt(null, 7));
    }

    @Test
    public void testValueAsLongBlankIsDefault() {
        Assertions.assertEquals(9L, ConfigurationFactory.valueAsLong(" ", 9L));
    }

    @Test
    public void testValueAsFloatBlankIsDefault() {
        Assertions.assertEquals(1.5f, ConfigurationFactory.valueAsFloat("", 1.5f));
    }

    @Test
    public void testValueAsDoubleBlankIsDefault() {
        Assertions.assertEquals(2.5, ConfigurationFactory.valueAsDouble(" ", 2.5));
    }

    @Test
    public void testValueAsBooleanDefaultBehavior() {
        Assertions.assertTrue(ConfigurationFactory.valueAsBoolean(null, true));
        Assertions.assertFalse(ConfigurationFactory.valueAsBoolean("", false));
    }
}