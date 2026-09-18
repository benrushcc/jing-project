package io.jingproject.commontest;

import io.jingproject.common.conf.Cfg.CfgItem;
import io.jingproject.common.conf.Cfg.CfgList;
import io.jingproject.common.conf.Cfg.CfgObject;
import io.jingproject.common.conf.CfgException;
import io.jingproject.common.conf.CfgReader.TomlCfgReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
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
        Assertions.assertEquals(new CfgObject(Map.of("key", new CfgItem("value"))), parse("key = \"value\""));
    }

    @Test
    public void testMultipleKeyValues() throws Exception {
        Assertions.assertEquals(
            new CfgObject(Map.of("a", new CfgItem("1"), "b", new CfgItem("2"))),
            parse("a = \"1\"\nb = \"2\""));
    }

    @Test
    public void testTable() throws Exception {
        Assertions.assertEquals(
            new CfgObject(Map.of("server", new CfgObject(Map.of("port", new CfgItem("8080"))))),
            parse("[server]\nport = \"8080\""));
    }

    @Test
    public void testNestedTable() throws Exception {
        Assertions.assertEquals(
            new CfgObject(Map.of("a", new CfgObject(Map.of("b", new CfgObject(Map.of("c", new CfgItem("1"))))))),
            parse("[a.b]\nc = \"1\""));
    }

    @Test
    public void testArray() throws Exception {
        Assertions.assertEquals(
            new CfgObject(Map.of("key", new CfgList(List.of("a", "b")))),
            parse("key = [\"a\", \"b\"]"));
    }

    @Test
    public void testComment() throws Exception {
        Assertions.assertEquals(
            new CfgObject(Map.of("key", new CfgItem("value"))),
            parse("# comment\nkey = \"value\""));
    }

    @Test
    public void testUnicodeEscape() throws Exception {
        Assertions.assertEquals(new CfgObject(Map.of("key", new CfgItem("你"))), parse("key = \"\\u4F60\""));
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
        Assertions.assertEquals(
            new CfgObject(Map.of("server", new CfgObject(Map.of(
                "host", new CfgItem("localhost"),
                "ports", new CfgList(List.of("8080", "8081")),
                "tls", new CfgObject(Map.of("enabled", new CfgItem("true"))))))),
            parse("[server]\nhost = \"localhost\"\nports = [\"8080\", \"8081\"]\n[server.tls]\nenabled = \"true\""));
    }

    @Test
    public void testCommentAtEof() throws Exception {
        Assertions.assertEquals(
            new CfgObject(Map.of("a", new CfgItem("1"))),
            parse("a = 1\n# trailing comment"));
        Assertions.assertEquals(new CfgObject(Map.of()), parse("# only comment"));
    }

    @Test
    public void testInvalidInputsThrow() {
        Assertions.assertThrows(CfgException.class, () -> parse("a = 1b"));
        Assertions.assertThrows(CfgException.class, () -> parse("a = truex"));
        Assertions.assertThrows(CfgException.class, () -> parse("[t]x"));
        Assertions.assertThrows(CfgException.class, () -> parse("[]"));
    }

    @Test
    public void testEmptyArray() throws Exception {
        Assertions.assertEquals(
            new CfgObject(Map.of("a", new CfgList(List.of()))),
            parse("a = []"));
    }

    @Test
    public void testMaxDepthBoundary() throws Exception {
        Assertions.assertEquals(
            new CfgObject(Map.of("a", new CfgObject(Map.of("b", new CfgObject(Map.of("c", new CfgItem("1"))))))),
            parse("[a.b]\nc = \"1\"", 2));
        Assertions.assertThrows(CfgException.class, () -> parse("[a.b.c]\nc = \"1\"", 2));
    }

    @Test
    public void testZeroMaxDepthThrows() {
        Assertions.assertThrows(CfgException.class, () -> parse("a = 1", 0));
    }
}