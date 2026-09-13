package io.jingproject.commontest;

import io.jingproject.common.conf.Cfg;
import io.jingproject.common.conf.Cfg.CfgItem;
import io.jingproject.common.conf.Cfg.CfgList;
import io.jingproject.common.conf.Cfg.CfgObject;
import io.jingproject.common.conf.CfgException;
import io.jingproject.common.conf.CfgReader.TomlCfgReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TomlCfgReaderTest {
    private static CfgObject parse(String toml) throws Exception {
        return parse(toml, 128);
    }

    private static CfgObject parse(String toml, int maxDepth) throws Exception {
        return new TomlCfgReader(new ByteArrayInputStream(toml.getBytes(StandardCharsets.UTF_8))).parse(maxDepth);
    }

    @Test
    public void testSingleKeyValue() throws Exception {
        Map<String, Cfg> expected = new HashMap<>();
        expected.put("key", new CfgItem("value"));
        Assertions.assertEquals(new CfgObject(expected), parse("key = \"value\""));
    }

    @Test
    public void testMultipleKeyValues() throws Exception {
        Map<String, Cfg> expected = new HashMap<>();
        expected.put("a", new CfgItem("1"));
        expected.put("b", new CfgItem("2"));
        Assertions.assertEquals(new CfgObject(expected), parse("a = \"1\"\nb = \"2\""));
    }

    @Test
    public void testTable() throws Exception {
        Map<String, Cfg> server = new HashMap<>();
        server.put("port", new CfgItem("8080"));
        Map<String, Cfg> expected = new HashMap<>();
        expected.put("server", new CfgObject(server));
        Assertions.assertEquals(new CfgObject(expected), parse("[server]\nport = \"8080\""));
    }

    @Test
    public void testNestedTable() throws Exception {
        Map<String, Cfg> b = new HashMap<>();
        b.put("c", new CfgItem("1"));
        Map<String, Cfg> a = new HashMap<>();
        a.put("b", new CfgObject(b));
        Map<String, Cfg> expected = new HashMap<>();
        expected.put("a", new CfgObject(a));
        Assertions.assertEquals(new CfgObject(expected), parse("[a.b]\nc = \"1\""));
    }

    @Test
    public void testArray() throws Exception {
        Map<String, Cfg> expected = new HashMap<>();
        expected.put("key", new CfgList(List.of("a", "b")));
        Assertions.assertEquals(new CfgObject(expected), parse("key = [\"a\", \"b\"]"));
    }

    @Test
    public void testComment() throws Exception {
        Map<String, Cfg> expected = new HashMap<>();
        expected.put("key", new CfgItem("value"));
        Assertions.assertEquals(new CfgObject(expected), parse("# comment\nkey = \"value\""));
    }

    @Test
    public void testUnicodeEscape() throws Exception {
        Map<String, Cfg> expected = new HashMap<>();
        expected.put("key", new CfgItem("你"));
        Assertions.assertEquals(new CfgObject(expected), parse("key = \"\\u4F60\""));
    }

    @Test
    public void testDuplicateKey() {
        Assertions.assertThrows(CfgException.class, () -> parse("key = \"a\"\nkey = \"b\""));
    }

    @Test
    public void testDuplicateTable() {
        Assertions.assertThrows(CfgException.class, () -> parse("[a]\nx = \"1\"\n[a]\ny = \"2\""));
    }

    @Test
    public void testInvalidKey() {
        Assertions.assertThrows(CfgException.class, () -> parse("a b = \"1\""));
    }

    @Test
    public void testMaxDepthExceeded() {
        Assertions.assertThrows(CfgException.class, () -> parse("[a.b.c.d.e.f]\nkey = \"value\"", 5));
    }

    @Test
    public void testComplexMixed() throws Exception {
        Map<String, Cfg> tls = new HashMap<>();
        tls.put("enabled", new CfgItem("true"));
        Map<String, Cfg> server = new HashMap<>();
        server.put("host", new CfgItem("localhost"));
        server.put("ports", new CfgList(List.of("8080", "8081")));
        server.put("tls", new CfgObject(tls));
        Map<String, Cfg> expected = new HashMap<>();
        expected.put("server", new CfgObject(server));
        Assertions.assertEquals(new CfgObject(expected), parse("[server]\nhost = \"localhost\"\nports = [\"8080\", \"8081\"]\n[server.tls]\nenabled = \"true\""));
    }
}