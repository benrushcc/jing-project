package io.jingproject.commontest;

import io.jingproject.common.conf.Cfg;
import io.jingproject.common.conf.CfgException;
import io.jingproject.common.conf.CfgReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class CfgTest {

    private static byte[] readCase(String path) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("corpus resource not found: " + path);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    protected static List<String> caseFiles(String dir) {
        try {
            List<String> r = new ArrayList<>();
            Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources(dir);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                switch (url.getProtocol()) {
                    case "file" -> {
                        Path path = Path.of(url.toURI());
                        try (var s = Files.list(path)) {
                            s.filter(Files::isRegularFile)
                                    .map(p -> p.getFileName().toString())
                                    .forEach(r::add);
                        }
                    }
                    case "jar" -> {
                        JarURLConnection connection = (JarURLConnection) url.openConnection();
                        connection.setUseCaches(false);
                        String prefix = dir.endsWith("/") ? dir : dir + "/";
                        try (JarFile jar = connection.getJarFile()) {
                            jar.stream()
                                    .filter(e -> !e.isDirectory())
                                    .map(JarEntry::getName)
                                    .filter(name -> name.startsWith(prefix))
                                    .map(name -> name.substring(prefix.length()))
                                    .filter(name -> !name.contains("/"))
                                    .forEach(r::add);
                        }
                    }
                    default -> throw new AssertionError("unsupported classpath protocol : " + url.getProtocol());
                }
            }
            return r;
        } catch (RuntimeException | IOException | URISyntaxException e) {
            throw new AssertionError(e);
        }
    }

    private static String itemValue(Cfg.CfgObject obj, String key) {
        if(obj.value().get(key) instanceof Cfg.CfgItem(String value)) {
            return value;
        }
        throw new AssertionError();
    }

    private static Cfg.CfgObject object(Cfg.CfgObject obj, String key) {
        if(obj.value().get(key) instanceof Cfg.CfgObject cfgObject) {
            return cfgObject;
        }
        throw new AssertionError();
    }

    private static Cfg.CfgList list(Cfg.CfgObject obj, String key) {
        if(obj.value().get(key) instanceof Cfg.CfgList cfgList) {
            return cfgList;
        }
        throw new AssertionError();
    }

    @Nested
    class JsonCfgTest {
        private static Cfg.CfgObject parseJson(byte[] bytes) {
            return parseJson(bytes, Integer.MAX_VALUE);
        }

        private static Cfg.CfgObject parseJson(byte[] bytes, int maxDepth) {
            return new CfgReader.JsonCfgReader().parse(new ByteArrayInputStream(bytes), maxDepth);
        }

        @Test
        void scalars() {
            Cfg.CfgObject j = parseJson(readCase("cases/json/valid/scalars.json"));
            Assertions.assertEquals("hello", itemValue(j, "str"));
            Assertions.assertEquals("héllo", itemValue(j, "str_unicode"));
            Assertions.assertEquals("42", itemValue(j, "int"));
            Assertions.assertEquals("-17", itemValue(j, "neg"));
            Assertions.assertEquals("3.14", itemValue(j, "float"));
            Assertions.assertEquals("1.5e3", itemValue(j, "exp"));
            Assertions.assertEquals("1.5E3", itemValue(j, "exp_upper"));
            Assertions.assertEquals("1e+5", itemValue(j, "exp_plus"));
            Assertions.assertEquals("1e-5", itemValue(j, "exp_minus"));
            Assertions.assertEquals("9223372036854775807", itemValue(j, "big"));
            Assertions.assertEquals("-0.0", itemValue(j, "neg_zero"));
            Assertions.assertEquals("1.0", itemValue(j, "one"));
            Assertions.assertEquals("0", itemValue(j, "zero"));
            Assertions.assertEquals("true", itemValue(j, "bool_true"));
            Assertions.assertEquals("false", itemValue(j, "bool_false"));
        }

        @Test
        void arrays() {
            Cfg.CfgObject j = parseJson(readCase("cases/json/valid/arrays.json"));
            Assertions.assertEquals(List.of("1", "2", "3"), list(j, "ints").value());
            Assertions.assertEquals(List.of("a", "b"), list(j, "strings").value());
            Assertions.assertEquals(List.of("1", "two", "true"), list(j, "mixed").value());
            Assertions.assertEquals(List.of("1.5", "-0.5", "1e3"), list(j, "floats").value());
            Assertions.assertEquals(List.of("only"), list(j, "single").value());
        }

        @Test
        void nested() {
            Cfg.CfgObject j = parseJson(readCase("cases/json/valid/nested.json"));
            Cfg.CfgObject server = object(j, "server");
            Assertions.assertEquals("localhost", itemValue(server, "host"));
            Assertions.assertEquals("8080", itemValue(server, "port"));
            Assertions.assertEquals("true", itemValue(object(server, "tls"), "enabled"));
            Assertions.assertEquals("10", itemValue(object(object(j, "db"), "pool"), "size"));
        }

        @Test
        void emptyObject() {
            Assertions.assertTrue(parseJson(readCase("cases/json/valid/empty.json")).value().isEmpty());
        }

        @Test
        void keys() {
            Cfg.CfgObject j = parseJson(readCase("cases/json/valid/keys.json"));
            Assertions.assertEquals("1", itemValue(j, "keyone"));
            Assertions.assertEquals("2", itemValue(j, "with-dash"));
            Assertions.assertEquals("3", itemValue(j, "with_underscore"));
            Assertions.assertEquals("4", itemValue(j, "a-b-c"));
        }

        @Test
        void unicode() {
            Cfg.CfgObject j = parseJson(readCase("cases/json/valid/unicode.json"));
            Assertions.assertEquals("héllo", itemValue(j, "escaped"));
            Assertions.assertEquals("😀", itemValue(j, "emoji"));
            Assertions.assertEquals("café", itemValue(j, "raw"));
        }

        @Test
        void invalid() {
            for (String fileName : caseFiles("cases/json/invalid")) {
                Assertions.assertThrows(CfgException.class, () -> parseJson(readCase("cases/json/invalid/" + fileName)), fileName + " failed");
            }
        }

        @Test
        void maxDepth() {
            // shallow corpus files (depth <= 3) pass at maxDepth=4
            Assertions.assertDoesNotThrow(() -> parseJson(readCase("cases/json/valid/scalars.json"), 4));
            Assertions.assertDoesNotThrow(() -> parseJson(readCase("cases/json/valid/nested.json"), 4));
            Assertions.assertDoesNotThrow(() -> parseJson(readCase("cases/json/valid/empty.json"), 4));
            // deep.json nests 5 levels: rejected at maxDepth=4, accepted at 5
            Assertions.assertThrows(CfgException.class, () -> parseJson(readCase("cases/json/valid/deep.json"), 4));
            Assertions.assertDoesNotThrow(() -> parseJson(readCase("cases/json/valid/deep.json"), 5));
        }

        @Test
        void immutableCopyIsDeep() {
            // nested lists are copied and made immutable
            Cfg.CfgObject json = parseJson("{\"b\": [\"x\", \"y\"]}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            Cfg.CfgObject immutable = json.asImmutable();
            Cfg.CfgList list = (Cfg.CfgList) immutable.value().get("b");
            Assertions.assertThrows(UnsupportedOperationException.class, () -> list.value().add("z"));
        }
    }

    @Nested
    class PropCfgTest {
        private static Cfg.CfgObject parseProps(byte[] bytes) {
            return parseProps(bytes, Integer.MAX_VALUE);
        }

        private static Cfg.CfgObject parseProps(byte[] bytes, int maxDepth) {
            return new CfgReader.PropertiesCfgReader().parse(new ByteArrayInputStream(bytes), maxDepth);
        }

        @Test
        void basic() {
            Cfg.CfgObject p = parseProps(readCase("cases/props/valid/basic.properties"));
            Assertions.assertEquals("1", itemValue(p, "a"));
            Assertions.assertEquals("hello", itemValue(p, "b"));
            Assertions.assertEquals("true", itemValue(p, "c"));
            Assertions.assertEquals("3.14", itemValue(p, "d"));
            Assertions.assertEquals("1e5", itemValue(p, "e"));
            Assertions.assertEquals("-17", itemValue(p, "f"));
            Assertions.assertEquals("0x1F", itemValue(p, "g"));
        }

        @Test
        void dotted() {
            Cfg.CfgObject p = parseProps(readCase("cases/props/valid/dotted.properties"));
            Assertions.assertEquals("1", itemValue(object(p, "a"), "b"));
            Assertions.assertEquals("2", itemValue(object(p, "a"), "c"));
            Assertions.assertEquals("3", itemValue(object(object(p, "x"), "y"), "z"));
        }

        @Test
        void lists() {
            // Properties.load keeps the spaces after each comma inside the value
            Cfg.CfgObject p = parseProps(readCase("cases/props/valid/lists.properties"));
            Assertions.assertEquals(List.of("1", " 2", " 3"), list(p, "arr").value());
            Assertions.assertEquals(List.of("a", " b", " c"), list(p, "strs").value());
            Assertions.assertEquals(List.of("only"), list(p, "single").value());
            Assertions.assertEquals(List.of("1", " x", " true"), list(p, "mixed").value());
        }

        @Test
        void specialKeys() {
            Cfg.CfgObject p = parseProps(readCase("cases/props/valid/special.properties"));
            Assertions.assertEquals("value one", itemValue(p, "keyone"));
            Assertions.assertEquals("two", itemValue(p, "with-dash"));
            Assertions.assertEquals("three", itemValue(p, "with_underscore"));
            Assertions.assertEquals("four", itemValue(p, "a-b-c"));
        }

        @Test
        void emptyDocument() {
            Assertions.assertTrue(parseProps(readCase("cases/props/valid/empty.properties")).value().isEmpty());
        }

        @Test
        void escapes() {
            // Properties.load decodes backslash and \\uXXXX escapes
            Cfg.CfgObject p = parseProps(readCase("cases/props/valid/escaped.properties"));
            Assertions.assertEquals("C:\\temp\\file", itemValue(p, "path"));
            Assertions.assertEquals("é", itemValue(p, "unicode"));
        }

        @Test
        void comments() {
            // # and ! only start comments at the start of a line
            Cfg.CfgObject p = parseProps(readCase("cases/props/valid/comments.properties"));
            Assertions.assertEquals("1 # not a comment in properties", itemValue(p, "a"));
        }

        @Test
        void invalidDocuments() {
            for (String fileName : caseFiles("cases/props/invalid")) {
                Assertions.assertThrows(CfgException.class, () -> parseProps(readCase("cases/props/invalid/" + fileName)), fileName + " failed");
            }
        }

        @Test
        void maxDepth() {
            // shallow corpus files (depth <= 3) pass at maxDepth=4
            Assertions.assertDoesNotThrow(() -> parseProps(readCase("cases/props/valid/basic.properties"), 4));
            Assertions.assertDoesNotThrow(() -> parseProps(readCase("cases/props/valid/dotted.properties"), 4));
            Assertions.assertDoesNotThrow(() -> parseProps(readCase("cases/props/valid/empty.properties"), 4));
            // deep.properties has 5 key segments: rejected at maxDepth=4, accepted at 5
            Assertions.assertThrows(CfgException.class, () -> parseProps(readCase("cases/props/valid/deep.properties"), 4));
            Assertions.assertDoesNotThrow(() -> parseProps(readCase("cases/props/valid/deep.properties"), 5));
        }
    }

    @Nested
    class TomlCfgTest {
        private static Cfg.CfgObject parseToml(byte[] bytes) {
            return parseToml(bytes, Integer.MAX_VALUE);
        }

        private static Cfg.CfgObject parseToml(byte[] bytes, int maxDepth) {
            return new CfgReader.TomlCfgReader().parse(new ByteArrayInputStream(bytes), maxDepth);
        }

        @Test
        void scalars() {
            Cfg.CfgObject t = parseToml(readCase("cases/toml/valid/scalars.toml"));
            Assertions.assertEquals("hello world", itemValue(t, "str_basic"));
            Assertions.assertEquals("\b\t\n\f\r\"\\\u001B", itemValue(t, "str_escapes"));
            Assertions.assertEquals("Aé😀", itemValue(t, "str_unicode"));
            Assertions.assertEquals("literal \\n no escape", itemValue(t, "str_literal"));
            Assertions.assertEquals("", itemValue(t, "str_empty"));
            Assertions.assertEquals("true", itemValue(t, "bool_true"));
            Assertions.assertEquals("false", itemValue(t, "bool_false"));
            Assertions.assertEquals("42", itemValue(t, "int_dec"));
            Assertions.assertEquals("-17", itemValue(t, "int_neg"));
            Assertions.assertEquals("+99", itemValue(t, "int_pos"));
            Assertions.assertEquals("1000000", itemValue(t, "int_underscore"));
            Assertions.assertEquals("3735928559", itemValue(t, "int_hex"));
            Assertions.assertEquals("493", itemValue(t, "int_oct"));
            Assertions.assertEquals("13", itemValue(t, "int_bin"));
            Assertions.assertEquals("9223372036854775807", itemValue(t, "int_max"));
            Assertions.assertEquals("-9223372036854775808", itemValue(t, "int_min"));
            Assertions.assertEquals("3.14", itemValue(t, "float_frac"));
            Assertions.assertEquals("1E5", itemValue(t, "float_exp"));
            Assertions.assertEquals("1E+5", itemValue(t, "float_exp_plus"));
            Assertions.assertEquals("1E-5", itemValue(t, "float_exp_minus"));
            Assertions.assertEquals("1E5", itemValue(t, "float_upper"));
            Assertions.assertEquals("1.5E3", itemValue(t, "float_mixed"));
            Assertions.assertEquals("1000.55", itemValue(t, "float_underscore"));
            Assertions.assertEquals("0E5", itemValue(t, "float_zero_exp"));
            Assertions.assertEquals("-0.0", itemValue(t, "float_neg_zero"));
            Assertions.assertEquals("+0.0", itemValue(t, "float_pos_zero"));
            Assertions.assertEquals("1.7976931348623157E308", itemValue(t, "float_max"));
            Assertions.assertEquals("4.9406564584124654E-324", itemValue(t, "float_min"));
            Assertions.assertEquals("Infinity", itemValue(t, "inf_pos"));
            Assertions.assertEquals("-Infinity", itemValue(t, "inf_neg"));
            Assertions.assertEquals("Infinity", itemValue(t, "inf_plus"));
            Assertions.assertEquals("NaN", itemValue(t, "nan_pos"));
            Assertions.assertEquals("-NaN", itemValue(t, "nan_neg"));
            Assertions.assertEquals("NaN", itemValue(t, "nan_plus"));
        }

        @Test
        void arrays() {
            Cfg.CfgObject t = parseToml(readCase("cases/toml/valid/arrays.toml"));
            Assertions.assertEquals(List.of(), list(t, "arr_empty").value());
            Assertions.assertEquals(List.of("1", "2", "3"), list(t, "arr_ints").value());
            Assertions.assertEquals(List.of("a", "b", "c"), list(t, "arr_strings").value());
            Assertions.assertEquals(List.of("1", "two", "true", "3.5"), list(t, "arr_mixed").value());
            Assertions.assertEquals(List.of("1", "2", "3"), list(t, "arr_trailing").value());
            Assertions.assertEquals(List.of("-1", "+2", "-0.5"), list(t, "arr_signed").value());
            Assertions.assertEquals(List.of("31", "15", "5"), list(t, "arr_radix").value());
            Assertions.assertEquals(List.of("Infinity", "-Infinity", "NaN"), list(t, "arr_infnan").value());
            Assertions.assertEquals(List.of("x", "y"), list(t, "arr_literal").value());
            Assertions.assertEquals(List.of("1", "2", "3"), list(t, "arr_spaces").value());
        }

        @Test
        void tables() {
            Cfg.CfgObject t = parseToml(readCase("cases/toml/valid/tables.toml"));
            Cfg.CfgObject server = object(t, "server");
            Assertions.assertEquals("localhost", itemValue(server, "host"));
            Assertions.assertEquals("8080", itemValue(server, "port"));
            Assertions.assertEquals("true", itemValue(object(server, "tls"), "enabled"));
            Cfg.CfgObject db = object(t, "database");
            Assertions.assertEquals("app", itemValue(db, "name"));
            Assertions.assertEquals("10", itemValue(object(db, "pool"), "size"));
            Assertions.assertEquals("1", itemValue(object(object(object(t, "deep"), "nested"), "table"), "value"));
            Cfg.CfgObject client = object(t, "client");
            Assertions.assertEquals("x", itemValue(client, "name"));
            Assertions.assertEquals("NYC", itemValue(object(client, "address"), "city"));
            Assertions.assertEquals("10001", itemValue(object(client, "address"), "zip"));
        }

        @Test
        void keys() {
            Cfg.CfgObject t = parseToml(readCase("cases/toml/valid/keys.toml"));
            Assertions.assertEquals("1", itemValue(t, "plain"));
            Assertions.assertEquals("2", itemValue(t, "with-dash"));
            Assertions.assertEquals("3", itemValue(t, "with_underscore"));
            Assertions.assertEquals("4", itemValue(t, "quoted"));
            Assertions.assertEquals("5", itemValue(t, "literal"));
            Assertions.assertEquals("6", itemValue(object(t, "a"), "b"));
            Assertions.assertEquals("7", itemValue(object(t, "a"), "c"));
            Assertions.assertEquals("8", itemValue(object(t, "d"), "b"));
        }

        @Test
        void commentsAndWhitespace() {
            Cfg.CfgObject t = parseToml(readCase("cases/toml/valid/comments_whitespace.toml"));
            Assertions.assertEquals("1", itemValue(t, "a"));
            Assertions.assertEquals("2", itemValue(t, "b"));
            Assertions.assertEquals("3", itemValue(t, "c"));
        }

        @Test
        void crlf() {
            Cfg.CfgObject t = parseToml(readCase("cases/toml/valid/crlf.toml"));
            Assertions.assertEquals("1", itemValue(t, "a"));
            Assertions.assertEquals("2", itemValue(t, "b"));
            Assertions.assertEquals("3", itemValue(object(t, "tab"), "c"));
        }

        @Test
        void overflow() {
            Cfg.CfgObject t = parseToml(readCase("cases/toml/valid/float_overflow.toml"));
            Assertions.assertEquals("1E999", itemValue(t, "a"));
            Cfg.CfgObject u = parseToml(readCase("cases/toml/valid/float_underflow.toml"));
            Assertions.assertEquals("1E-999", itemValue(u, "a"));
        }

        @Test
        void emptyDocuments() {
            Assertions.assertTrue(parseToml(readCase("cases/toml/valid/empty.toml")).value().isEmpty());
        }

        @Test
        void invalidDocuments() {
            for (String fileName : caseFiles("cases/toml/invalid")) {
                Assertions.assertThrows(CfgException.class, () -> parseToml(readCase("cases/toml/invalid/" + fileName)), fileName + " failed");
            }
        }

        @Test
        void maxDepth() {
            // shallow corpus files (depth <= 4) pass at maxDepth=4
            Assertions.assertDoesNotThrow(() -> parseToml(readCase("cases/toml/valid/scalars.toml"), 4));
            Assertions.assertDoesNotThrow(() -> parseToml(readCase("cases/toml/valid/keys.toml"), 4));
            Assertions.assertDoesNotThrow(() -> parseToml(readCase("cases/toml/valid/tables.toml"), 4));
            Assertions.assertDoesNotThrow(() -> parseToml(readCase("cases/toml/valid/empty.toml"), 4));
            // deep.toml needs depth 5 ([a.b.c.d] + value): rejected at maxDepth=4, accepted at 5
            Assertions.assertThrows(CfgException.class, () -> parseToml(readCase("cases/toml/valid/deep.toml"), 4));
            Assertions.assertDoesNotThrow(() -> parseToml(readCase("cases/toml/valid/deep.toml"), 5));
        }

        @Test
        void immutableCopyIsDeep() {
            // mutating the original must not affect the immutable copy
            Cfg.CfgObject toml = parseToml("a = 1\n[tab]\nb = 2".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            Cfg.CfgObject immutable = toml.asImmutable();
            toml.value().put("a", new Cfg.CfgItem("999"));
            Assertions.assertEquals("1", itemValue(immutable, "a"));
            Assertions.assertEquals("2", itemValue(object(immutable, "tab"), "b"));
        }
    }
}
