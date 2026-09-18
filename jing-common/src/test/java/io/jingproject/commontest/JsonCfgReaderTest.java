package io.jingproject.commontest;

import io.jingproject.common.conf.Cfg.CfgItem;
import io.jingproject.common.conf.Cfg.CfgList;
import io.jingproject.common.conf.Cfg.CfgObject;
import io.jingproject.common.conf.CfgException;
import io.jingproject.common.conf.CfgReader.JsonCfgReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class JsonCfgReaderTest {
    private static CfgObject parse(String json) throws Exception {
        return parse(json, 128);
    }

    private static CfgObject parse(String json, int maxDepth) throws Exception {
        return new JsonCfgReader(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))).parse(maxDepth);
    }

    @Test
    public void testSingleKeyValue() throws Exception {
        Assertions.assertEquals(new CfgObject(Map.of("key", new CfgItem("value"))), parse("{\"key\": \"value\"}"));
    }

    @Test
    public void testMultipleKeys() throws Exception {
        Assertions.assertEquals(
            new CfgObject(Map.of("a", new CfgItem("1"), "b", new CfgItem("2"))),
            parse("{\"a\": \"1\", \"b\": \"2\"}"));
    }

    @Test
    public void testNestedObject() throws Exception {
        Assertions.assertEquals(
            new CfgObject(Map.of("a", new CfgObject(Map.of("b", new CfgItem("c"))))),
            parse("{\"a\": {\"b\": \"c\"}}"));
    }

    @Test
    public void testArray() throws Exception {
        Assertions.assertEquals(
            new CfgObject(Map.of("a", new CfgList(List.of("x", "y")))),
            parse("{\"a\": [\"x\", \"y\"]}"));
    }

    @Test
    public void testUnicodeEscape() throws Exception {
        Assertions.assertEquals(new CfgObject(Map.of("a", new CfgItem("你"))), parse("{\"a\": \"\\u4F60\"}"));
    }

    @Test
    public void testSurrogatePair() throws Exception {
        Assertions.assertEquals(new CfgObject(Map.of("a", new CfgItem("😀"))), parse("{\"a\": \"\\uD83D\\uDE00\"}"));
    }

    @Test
    public void testDuplicateKey() {
        Assertions.assertThrows(CfgException.class, () -> parse("{\"a\": \"1\", \"a\": \"2\"}"));
    }

    @Test
    public void testCorrupted() {
        Assertions.assertThrows(CfgException.class, () -> parse("{\"a\": }"));
    }

    @Test
    public void testMaxDepthExceeded() {
        Assertions.assertThrows(CfgException.class,
            () -> parse("{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{}}}}}}}", 5));
    }

    @Test
    public void testComplexMixed() throws Exception {
        Assertions.assertEquals(
            new CfgObject(Map.of("server", new CfgObject(Map.of(
                "host", new CfgItem("localhost"),
                "ports", new CfgList(List.of("8080", "8081")),
                "tls", new CfgObject(Map.of("enabled", new CfgItem("true"))))))),
            parse(
                "{\"server\":{\"host\":\"localhost\",\"ports\":[\"8080\",\"8081\"],\"tls\":{\"enabled\":\"true\"}}}"));
    }

    @Test
    public void testTruncatedThrows() {
        Assertions.assertThrows(CfgException.class, () -> parse("{\"a\": 1"));
    }

    @Test
    public void testEmptyObject() throws Exception {
        Assertions.assertEquals(new CfgObject(Map.of()), parse("{}"));
    }

    @Test
    public void testTrailingGarbageThrows() {
        Assertions.assertThrows(CfgException.class, () -> parse("{\"a\":1}x"));
        Assertions.assertThrows(CfgException.class, () -> parse("{\"a\":1}}"));
    }

    @Test
    public void testEmptyArray() throws Exception {
        Assertions.assertEquals(
            new CfgObject(Map.of("a", new CfgList(List.of()))),
            parse("{\"a\":[]}"));
    }

    @Test
    public void testMaxDepthBoundary() throws Exception {
        Assertions.assertEquals(
            new CfgObject(Map.of("a", new CfgObject(Map.of("b", new CfgItem("c"))))),
            parse("{\"a\":{\"b\":\"c\"}}", 2));
        Assertions.assertThrows(CfgException.class, () -> parse("{\"a\":{\"b\":{\"c\":\"d\"}}}", 2));
    }

    @Test
    public void testZeroMaxDepthThrows() {
        Assertions.assertThrows(CfgException.class, () -> parse("{\"a\": 1}", 0));
    }
}