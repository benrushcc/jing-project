package io.jingproject.commontest;

import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.common.conf.Cfg.CfgItem;
import io.jingproject.common.conf.Cfg.CfgList;
import io.jingproject.common.conf.Cfg.CfgObject;
import io.jingproject.common.conf.CfgException;
import io.jingproject.common.conf.CfgUtil;
import io.jingproject.common.conf.CfgReader.JsonCfgReader;
import io.jingproject.common.conf.CfgReader.TomlCfgReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class CfgTest {
    private record JsonCase(String input, CfgObject expected) {
    }

    private record TomlCase(String input, CfgObject expected) {
    }

    private static CfgObject parseJson(String json) throws Exception {
        return new JsonCfgReader(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))).parse(128);
    }

    private static CfgObject parseToml(String toml) throws Exception {
        return new TomlCfgReader(new ByteArrayInputStream(toml.getBytes(StandardCharsets.UTF_8))).parse(128);
    }

    @Test
    public void testReadCfgKey() {
        byte[] d = "abc".getBytes(StandardCharsets.US_ASCII);
        Assertions.assertEquals("abc", CfgUtil.readCfgKey(d, 0, d.length));
        byte[] d2 = "a-b_c1".getBytes(StandardCharsets.US_ASCII);
        Assertions.assertEquals("a-b_c1", CfgUtil.readCfgKey(d2, 0, d2.length));
        byte[] d3 = "a b".getBytes(StandardCharsets.US_ASCII);
        Assertions.assertThrows(CfgException.class, () -> CfgUtil.readCfgKey(d3, 0, d3.length));
        byte[] d4 = "a.b".getBytes(StandardCharsets.US_ASCII);
        Assertions.assertThrows(CfgException.class, () -> CfgUtil.readCfgKey(d4, 0, d4.length));
    }

    @Test
    public void testReadCfgNestedKey() {
        byte[] d = "a.b.c".getBytes(StandardCharsets.US_ASCII);
        Assertions.assertEquals(List.of("a", "b", "c"), CfgUtil.readCfgNestedKey(d, 0, d.length, 128));
        byte[] d2 = "a".getBytes(StandardCharsets.US_ASCII);
        Assertions.assertEquals(List.of("a"), CfgUtil.readCfgNestedKey(d2, 0, d2.length, 128));
        byte[] d3 = ".a".getBytes(StandardCharsets.US_ASCII);
        Assertions.assertThrows(CfgException.class, () -> CfgUtil.readCfgNestedKey(d3, 0, d3.length, 128));
        byte[] d4 = "a.".getBytes(StandardCharsets.US_ASCII);
        Assertions.assertThrows(CfgException.class, () -> CfgUtil.readCfgNestedKey(d4, 0, d4.length, 128));
        byte[] d5 = "a..b".getBytes(StandardCharsets.US_ASCII);
        Assertions.assertThrows(CfgException.class, () -> CfgUtil.readCfgNestedKey(d5, 0, d5.length, 128));
    }

    @Test
    public void testReadUnicode() throws Exception {
        Assertions.assertEquals(0x4F60, CfgUtil.readUnicode(
            new ByteArrayInputStream("4F60".getBytes(StandardCharsets.US_ASCII)), 4));
        Assertions.assertThrows(CfgException.class, () -> CfgUtil.readUnicode(
            new ByteArrayInputStream("zz".getBytes(StandardCharsets.US_ASCII)), 2));
    }

    @Test
    public void testWriteCodePointInUtf8() {
        HeapWriteBuffer buf = new HeapWriteBuffer(64);
        buf.writeCodePointInUtf8(0x4F60);
        byte[] actual = new byte[buf.intPosition()];
        System.arraycopy(buf.rawByteArray(), 0, actual, 0, buf.intPosition());
        Assertions.assertArrayEquals("你".getBytes(StandardCharsets.UTF_8), actual);
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

    // --- json accept ---

    @Test
    public void testJsonAccepts() throws Exception {
        List<JsonCase> cases = List.of(
            new JsonCase("{\"a\": 42}", new CfgObject(Map.of("a", new CfgItem("42")))),
            new JsonCase("{\"a\": -7}", new CfgObject(Map.of("a", new CfgItem("-7")))),
            new JsonCase("{\"a\": 0}", new CfgObject(Map.of("a", new CfgItem("0")))),
            new JsonCase("{\"a\": -0}", new CfgObject(Map.of("a", new CfgItem("-0")))),
            new JsonCase("{\"a\": 123}", new CfgObject(Map.of("a", new CfgItem("123")))),
            new JsonCase("{\"a\": 12345678901234567890}",
                new CfgObject(Map.of("a", new CfgItem("12345678901234567890")))),
            new JsonCase("{\"a\": 3.14}", new CfgObject(Map.of("a", new CfgItem("3.14")))),
            new JsonCase("{\"a\": 0.5}", new CfgObject(Map.of("a", new CfgItem("0.5")))),
            new JsonCase("{\"a\": -0.5}", new CfgObject(Map.of("a", new CfgItem("-0.5")))),
            new JsonCase("{\"a\": 1e10}", new CfgObject(Map.of("a", new CfgItem("1e10")))),
            new JsonCase("{\"a\": 0e0}", new CfgObject(Map.of("a", new CfgItem("0e0")))),
            new JsonCase("{\"a\": 1E2}", new CfgObject(Map.of("a", new CfgItem("1E2")))),
            new JsonCase("{\"a\": 2.5E-3}", new CfgObject(Map.of("a", new CfgItem("2.5E-3")))),
            new JsonCase("{\"a\": 1.5E+10}", new CfgObject(Map.of("a", new CfgItem("1.5E+10")))),
            new JsonCase("{\"a\": true}", new CfgObject(Map.of("a", new CfgItem("true")))),
            new JsonCase("{\"a\": false}", new CfgObject(Map.of("a", new CfgItem("false")))),
            new JsonCase("{\"a\": [1, \"two\", true]}",
                new CfgObject(Map.of("a", new CfgList(List.of("1", "two", "true")))))
        );
        for (JsonCase c : cases) {
            Assertions.assertEquals(c.expected(), parseJson(c.input()), c.input());
        }
    }

    // --- json reject ---

    @Test
    public void testJsonRejects() {
        List<String> inputs = List.of(
            "{\"a\": 007}",
            "{\"a\": 01}",
            "{\"a\": .5}",
            "{\"a\": .}",
            "{\"a\": 5.}",
            "{\"a\": 1e}",
            "{\"a\": 1e+}",
            "{\"a\": 1e-}",
            "{\"a\": +1}",
            "{\"a\": -}",
            "{\"a\": 1_0}",
            "{\"a\": 1..2}",
            "{\"a\": 0xFF}",
            "{\"a\": 0x1}",
            ""
        );
        for (String input : inputs) {
            Assertions.assertThrows(CfgException.class, () -> parseJson(input), "expected CfgException for: " + input);
        }
    }

    // --- toml accept ---

    @Test
    public void testTomlAccepts() throws Exception {
        List<TomlCase> cases = List.of(
            new TomlCase("", new CfgObject(Map.of())),
            new TomlCase("a = 42", new CfgObject(Map.of("a", new CfgItem("42")))),
            new TomlCase("a = -7", new CfgObject(Map.of("a", new CfgItem("-7")))),
            new TomlCase("a = +3", new CfgObject(Map.of("a", new CfgItem("3")))),
            new TomlCase("a = 1_000_000", new CfgObject(Map.of("a", new CfgItem("1000000")))),
            new TomlCase("a = 0", new CfgObject(Map.of("a", new CfgItem("0")))),
            new TomlCase("a = -0", new CfgObject(Map.of("a", new CfgItem("0")))),
            new TomlCase("a = +0", new CfgObject(Map.of("a", new CfgItem("0")))),
            new TomlCase("a = -9223372036854775808",
                new CfgObject(Map.of("a", new CfgItem("-9223372036854775808")))),
            new TomlCase("a = 0xFF", new CfgObject(Map.of("a", new CfgItem("255")))),
            new TomlCase("a = 0xF_F", new CfgObject(Map.of("a", new CfgItem("255")))),
            new TomlCase("a = 0x0", new CfgObject(Map.of("a", new CfgItem("0")))),
            new TomlCase("a = 0o77", new CfgObject(Map.of("a", new CfgItem("63")))),
            new TomlCase("a = 0b1010", new CfgObject(Map.of("a", new CfgItem("10")))),
            new TomlCase("a = 0b10_10", new CfgObject(Map.of("a", new CfgItem("10")))),
            new TomlCase("a = 0b1_0", new CfgObject(Map.of("a", new CfgItem("2")))),
            new TomlCase("a = 3.14", new CfgObject(Map.of("a", new CfgItem("3.14")))),
            new TomlCase("a = 0.5", new CfgObject(Map.of("a", new CfgItem("0.5")))),
            new TomlCase("a = 1e10", new CfgObject(Map.of("a", new CfgItem("1e10")))),
            new TomlCase("a = 5e0", new CfgObject(Map.of("a", new CfgItem("5e0")))),
            new TomlCase("a = 1e+2", new CfgObject(Map.of("a", new CfgItem("1e+2")))),
            new TomlCase("a = 2.5E-3", new CfgObject(Map.of("a", new CfgItem("2.5E-3")))),
            new TomlCase("a = 1_0.5", new CfgObject(Map.of("a", new CfgItem("10.5")))),
            new TomlCase("a = inf", new CfgObject(Map.of("a", new CfgItem("Infinity")))),
            new TomlCase("a = -inf", new CfgObject(Map.of("a", new CfgItem("-Infinity")))),
            new TomlCase("a = +inf", new CfgObject(Map.of("a", new CfgItem("Infinity")))),
            new TomlCase("a = nan", new CfgObject(Map.of("a", new CfgItem("NaN")))),
            new TomlCase("a = -nan", new CfgObject(Map.of("a", new CfgItem("NaN")))),
            new TomlCase("a = +nan", new CfgObject(Map.of("a", new CfgItem("NaN")))),
            new TomlCase("a = true", new CfgObject(Map.of("a", new CfgItem("true")))),
            new TomlCase("a = false", new CfgObject(Map.of("a", new CfgItem("false")))),
            new TomlCase("a = [1, \"two\", true]",
                new CfgObject(Map.of("a", new CfgList(List.of("1", "two", "true"))))),
            new TomlCase("[t]\nn = 1\nb = true",
                new CfgObject(Map.of("t", new CfgObject(Map.of("n", new CfgItem("1"), "b", new CfgItem("true"))))))
        );
        for (TomlCase c : cases) {
            Assertions.assertEquals(c.expected(), parseToml(c.input()), c.input());
        }
    }

    // --- toml reject ---

    @Test
    public void testTomlRejects() {
        List<String> inputs = List.of(
            "a = 007",
            "a = _1",
            "a = 0_1",
            "a = 1__0",
            "a = 1_",
            "a = +",
            "a = +0xFF",
            "a = -0xFF",
            "a = 0x",
            "a = 0xG",
            "a = 0o8",
            "a = 0b2",
            "a = 0xFF_",
            "a = 1e1_0",
            "a = .5",
            "a = 5.",
            "a = 1.",
            "a = 1e+",
            "a = 1._0",
            "a = 9223372036854775808"
        );
        for (String input : inputs) {
            Assertions.assertThrows(CfgException.class, () -> parseToml(input), "expected CfgException for: " + input);
        }
    }

    // --- jdk-convertibility ---

    @Test
    public void testJdkConvertibility() {
        List<String> intStrings = List.of("42", "-7", "3", "1000000", "255", "63", "10", "0", "-0");
        for (String s : intStrings) {
            Assertions.assertDoesNotThrow(() -> Integer.parseInt(s), "Integer.parseInt failed for: " + s);
            Assertions.assertDoesNotThrow(() -> Long.parseLong(s), "Long.parseLong failed for: " + s);
        }
        String minLong = "-9223372036854775808";
        Assertions.assertDoesNotThrow(() -> Long.parseLong(minLong), "Long.parseLong failed for: " + minLong);
        List<String> doubleStrings = List.of("3.14", "1e10", "2.5E-3", "10.5", "-0.5", "Infinity", "-Infinity", "NaN");
        for (String s : doubleStrings) {
            Assertions.assertDoesNotThrow(() -> Double.parseDouble(s), "Double.parseDouble failed for: " + s);
        }
        Assertions.assertTrue(Boolean.parseBoolean("true"));
        Assertions.assertFalse(Boolean.parseBoolean("false"));
    }
}