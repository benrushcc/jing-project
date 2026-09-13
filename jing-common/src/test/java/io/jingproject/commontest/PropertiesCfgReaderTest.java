package io.jingproject.commontest;

import io.jingproject.common.conf.Cfg;
import io.jingproject.common.conf.Cfg.CfgItem;
import io.jingproject.common.conf.Cfg.CfgList;
import io.jingproject.common.conf.Cfg.CfgObject;
import io.jingproject.common.conf.CfgException;
import io.jingproject.common.conf.CfgReader.PropertiesCfgReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropertiesCfgReaderTest {
    private static CfgObject parse(String properties) throws Exception {
        return parse(properties, 128);
    }

    private static CfgObject parse(String properties, int maxDepth) throws Exception {
        return new PropertiesCfgReader(new ByteArrayInputStream(properties.getBytes(StandardCharsets.UTF_8))).parse(maxDepth);
    }

    @Test
    public void testSimple() throws Exception {
        Map<String, Cfg> expected = new HashMap<>();
        expected.put("a", new CfgItem("1"));
        expected.put("b", new CfgItem("2"));
        Assertions.assertEquals(new CfgObject(expected), parse("a=1\nb=2"));
    }

    @Test
    public void testNested() throws Exception {
        Map<String, Cfg> inner = new HashMap<>();
        inner.put("b", new CfgItem("1"));
        Map<String, Cfg> expected = new HashMap<>();
        expected.put("a", new CfgObject(inner));
        Assertions.assertEquals(new CfgObject(expected), parse("a.b=1"));
    }

    @Test
    public void testArray() throws Exception {
        Map<String, Cfg> expected = new HashMap<>();
        expected.put("key", new CfgList(List.of("a", " b")));
        Assertions.assertEquals(new CfgObject(expected), parse("key=[a, b]"));
    }

    @Test
    public void testComment() throws Exception {
        Map<String, Cfg> expected = new HashMap<>();
        expected.put("a", new CfgItem("1"));
        Assertions.assertEquals(new CfgObject(expected), parse("# comment\na=1"));
    }

    @Test
    public void testDuplicateLastWins() throws Exception {
        Map<String, Cfg> expected = new HashMap<>();
        expected.put("a", new CfgItem("2"));
        Assertions.assertEquals(new CfgObject(expected), parse("a=1\na=2"));
    }

    @Test
    public void testMaxDepthExceeded() {
        Assertions.assertThrows(CfgException.class, () -> parse("a.b.c.d.e.f=value", 5));
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
        Assertions.assertEquals(new CfgObject(expected), parse("server.host=localhost\nserver.ports=[8080,8081]\nserver.tls.enabled=true"));
    }
}