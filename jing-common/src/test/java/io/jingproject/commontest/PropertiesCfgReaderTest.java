package io.jingproject.commontest;

import io.jingproject.common.conf.Cfg.CfgItem;
import io.jingproject.common.conf.Cfg.CfgList;
import io.jingproject.common.conf.Cfg.CfgObject;
import io.jingproject.common.conf.CfgException;
import io.jingproject.common.conf.CfgReader.PropertiesCfgReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class PropertiesCfgReaderTest {
    private static CfgObject parse(String properties) throws Exception {
        return parse(properties, 128);
    }

    private static CfgObject parse(String properties, int maxDepth) throws Exception {
        return new PropertiesCfgReader(
            new ByteArrayInputStream(properties.getBytes(StandardCharsets.UTF_8))).parse(maxDepth);
    }

    @Test
    public void testSimple() throws Exception {
        Assertions.assertEquals(
            new CfgObject(Map.of("a", new CfgItem("1"), "b", new CfgItem("2"))),
            parse("a=1\nb=2"));
    }

    @Test
    public void testNested() throws Exception {
        Assertions.assertEquals(
            new CfgObject(Map.of("a", new CfgObject(Map.of("b", new CfgItem("1"))))),
            parse("a.b=1"));
    }

    @Test
    public void testArray() throws Exception {
        Assertions.assertEquals(new CfgObject(Map.of("key", new CfgList(List.of("a", " b")))), parse("key=[a, b]"));
    }

    @Test
    public void testComment() throws Exception {
        Assertions.assertEquals(new CfgObject(Map.of("a", new CfgItem("1"))), parse("# comment\na=1"));
    }

    @Test
    public void testDuplicateLastWins() throws Exception {
        Assertions.assertEquals(new CfgObject(Map.of("a", new CfgItem("2"))), parse("a=1\na=2"));
    }

    @Test
    public void testMaxDepthExceeded() {
        Assertions.assertThrows(CfgException.class, () -> parse("a.b.c.d.e.f=value", 5));
    }

    @Test
    public void testComplexMixed() throws Exception {
        Assertions.assertEquals(
            new CfgObject(Map.of("server", new CfgObject(Map.of(
                "host", new CfgItem("localhost"),
                "ports", new CfgList(List.of("8080", "8081")),
                "tls", new CfgObject(Map.of("enabled", new CfgItem("true"))))))),
            parse("server.host=localhost\nserver.ports=[8080,8081]\nserver.tls.enabled=true"));
    }

    @Test
    public void testMaxDepthBoundary() throws Exception {
        Assertions.assertEquals(
            new CfgObject(Map.of("a", new CfgObject(Map.of("b", new CfgItem("1"))))),
            parse("a.b=1", 2));
        Assertions.assertThrows(CfgException.class, () -> parse("a.b.c=1", 2));
    }

    @Test
    public void testNestedConflictThrows() {
        Assertions.assertThrows(CfgException.class, () -> parse("a.b=1\na=2"));
        Assertions.assertThrows(CfgException.class, () -> parse("a=1\na.b=2"));
    }

    @Test
    public void testZeroMaxDepthThrows() {
        Assertions.assertThrows(CfgException.class, () -> parse("a=1", 0));
    }
}