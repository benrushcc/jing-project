package io.jingproject.commontest;

import io.jingproject.common.conf.CfgException;
import io.jingproject.common.conf.CfgUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CfgUtilTest {
    @Test
    public void testReadCfgKey() {
        Assertions.assertEquals("abc", CfgUtil.readCfgKey("abc".getBytes(StandardCharsets.US_ASCII)));
        Assertions.assertEquals("a-b_c1", CfgUtil.readCfgKey("a-b_c1".getBytes(StandardCharsets.US_ASCII)));
        Assertions.assertThrows(CfgException.class, () -> CfgUtil.readCfgKey("a b".getBytes(StandardCharsets.US_ASCII)));
        Assertions.assertThrows(CfgException.class, () -> CfgUtil.readCfgKey("a.b".getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    public void testReadCfgNestedKey() {
        Assertions.assertEquals(List.of("a", "b", "c"), CfgUtil.readCfgNestedKey("a.b.c".getBytes(StandardCharsets.US_ASCII), 128));
        Assertions.assertEquals(List.of("a"), CfgUtil.readCfgNestedKey("a".getBytes(StandardCharsets.US_ASCII), 128));
        Assertions.assertThrows(CfgException.class, () -> CfgUtil.readCfgNestedKey(".a".getBytes(StandardCharsets.US_ASCII), 128));
        Assertions.assertThrows(CfgException.class, () -> CfgUtil.readCfgNestedKey("a.".getBytes(StandardCharsets.US_ASCII), 128));
        Assertions.assertThrows(CfgException.class, () -> CfgUtil.readCfgNestedKey("a..b".getBytes(StandardCharsets.US_ASCII), 128));
    }

    @Test
    public void testReadUnicode() throws Exception {
        Assertions.assertEquals(0x4F60, CfgUtil.readUnicode(new ByteArrayInputStream("4F60".getBytes(StandardCharsets.US_ASCII)), 4));
        Assertions.assertThrows(CfgException.class, () -> CfgUtil.readUnicode(new ByteArrayInputStream("zz".getBytes(StandardCharsets.US_ASCII)), 2));
    }

    @Test
    public void testWriteUnicodeInUtf8() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CfgUtil.writeUnicodeInUtf8(out, 0x4F60);
        Assertions.assertArrayEquals("你".getBytes(StandardCharsets.UTF_8), out.toByteArray());
    }

    @Test
    public void testRejectKey() {
        Assertions.assertFalse(CfgUtil.rejectKey((byte) 'a'));
        Assertions.assertFalse(CfgUtil.rejectKey((byte) 'Z'));
        Assertions.assertFalse(CfgUtil.rejectKey((byte) '0'));
        Assertions.assertFalse(CfgUtil.rejectKey((byte) '-'));
        Assertions.assertFalse(CfgUtil.rejectKey((byte) '_'));
        Assertions.assertTrue(CfgUtil.rejectKey((byte) '.'));
        Assertions.assertTrue(CfgUtil.rejectKey((byte) ' '));
    }
}