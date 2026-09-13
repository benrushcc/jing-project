package io.jingproject.commontest;

import io.jingproject.common.conf.Cfg;
import io.jingproject.common.conf.Cfg.CfgItem;
import io.jingproject.common.conf.Cfg.CfgList;
import io.jingproject.common.conf.Cfg.CfgObject;
import io.jingproject.common.conf.CfgException;
import io.jingproject.common.conf.CfgReader.JsonCfgReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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
        Map<String, Cfg> expected = new HashMap<>();
        expected.put("key", new CfgItem("value"));
        Assertions.assertEquals(new CfgObject(expected), parse("{\"key\": \"value\"}"));
    }

    @Test
    public void testMultipleKeys() throws Exception {
        Map<String, Cfg> expected = new HashMap<>();
        expected.put("a", new CfgItem("1"));
        expected.put("b", new CfgItem("2"));
        Assertions.assertEquals(new CfgObject(expected), parse("{\"a\": \"1\", \"b\": \"2\"}"));
    }

    @Test
    public void testNestedObject() throws Exception {
        Map<String, Cfg> inner = new HashMap<>();
        inner.put("b", new CfgItem("c"));
        Map<String, Cfg> expected = new HashMap<>();
        expected.put("a", new CfgObject(inner));
        Assertions.assertEquals(new CfgObject(expected), parse("{\"a\": {\"b\": \"c\"}}"));
    }

    @Test
    public void testArray() throws Exception {
        Map<String, Cfg> expected = new HashMap<>();
        expected.put("a", new CfgList(List.of("x", "y")));
        Assertions.assertEquals(new CfgObject(expected), parse("{\"a\": [\"x\", \"y\"]}"));
    }

    @Test
    public void testUnicodeEscape() throws Exception {
        Map<String, Cfg> expected = new HashMap<>();
        expected.put("a", new CfgItem("你"));
        Assertions.assertEquals(new CfgObject(expected), parse("{\"a\": \"\\u4F60\"}"));
    }

    @Test
    public void testSurrogatePair() throws Exception {
        Map<String, Cfg> expected = new HashMap<>();
        expected.put("a", new CfgItem("😀"));
        Assertions.assertEquals(new CfgObject(expected), parse("{\"a\": \"\\uD83D\\uDE00\"}"));
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
        Assertions.assertThrows(CfgException.class, () -> parse("{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{}}}}}}}", 5));
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
        Assertions.assertEquals(new CfgObject(expected), parse("{\"server\":{\"host\":\"localhost\",\"ports\":[\"8080\",\"8081\"],\"tls\":{\"enabled\":\"true\"}}}"));
    }
}