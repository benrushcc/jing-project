package io.jingproject.logtest;

import io.jingproject.log.FormatPattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;

// the strict pattern parser must reject every malformed pattern with an
// IllegalArgumentException whose message identifies the offending position
public class PatternParserTest {
    private static final String[] LEVELS = {"TRACE", "DEBUG", "INFO", "WARN", "ERROR"};

    private static void assertRejected(String pattern, String fragment) {
        IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class,
                () -> FormatPattern.compile(pattern, ZoneId.of("UTC"), LEVELS));
        Assertions.assertTrue(e.getMessage().contains("at index"),
                "message must carry the position, actual : " + e.getMessage());
        if (fragment != null) {
            Assertions.assertTrue(e.getMessage().contains(fragment),
                    "message must mention '" + fragment + "', actual : " + e.getMessage());
        }
    }

    @Test
    public void testDanglingPercent() {
        assertRejected("%", "dangling");
        assertRejected("hello %", "dangling");
    }

    @Test
    public void testEscapedPercentIsLiteral() {
        FormatPattern.compile("%%msg", ZoneId.of("UTC"), LEVELS);
        FormatPattern.compile("100%% done", ZoneId.of("UTC"), LEVELS);
    }

    @Test
    public void testWidthRequiresDigits() {
        assertRejected("%-level", "width requires digits after '-'");
    }

    @Test
    public void testUnknownConversion() {
        assertRejected("%x", "unknown specifier");
        assertRejected("%y{foo}", "unknown specifier");
        assertRejected("%z", "unknown specifier");
        // a maximal letter run is one name, so %nb is %n glued to a letter, not %n
        assertRejected("a%nb", "unknown specifier");
    }

    @Test
    public void testMissingSpecifierAfterDigits() {
        assertRejected("%5", "unknown conversion");
    }

    @Test
    public void testUnclosedBraces() {
        assertRejected("%d{", "unclosed");
        assertRejected("%logger{20", "unclosed");
        assertRejected("%X{key", "unclosed");
    }

    @Test
    public void testInvalidLoggerBudget() {
        assertRejected("%logger{}", "logger budget");
        assertRejected("%logger{-1}", "logger budget");
        assertRejected("%logger{abc}", "logger budget");
        assertRejected("%logger{1.5}", "logger budget");
        assertRejected("%logger{99999999999}", "too large");
    }

    @Test
    public void testWidthOnUnsupportedSpecifier() {
        assertRejected("%5d{yyyy}", "does not support a width");
        assertRejected("%5t", "does not support a width");
        assertRejected("%5msg", "does not support a width");
        assertRejected("%5ex", "does not support a width");
    }

    @Test
    public void testWidthOnLoggerRejected() {
        assertRejected("%5logger", "does not support a width");
        assertRejected("%-5logger", "does not support a width");
        FormatPattern.compile("%logger", ZoneId.of("UTC"), LEVELS);
        FormatPattern.compile("%logger{36}", ZoneId.of("UTC"), LEVELS);
    }

    @Test
    public void testEmptyDateOptionsRejected() {
        assertRejected("%d{}", "requires options");
        assertRejected("%d{ }", "requires options");
    }

    @Test
    public void testOptionsOnUnsupportedSpecifier() {
        assertRejected("%level{width}", "does not support options");
        assertRejected("%t{9}", "does not support options");
        assertRejected("%m{9}", "does not support options");
        assertRejected("%n{9}", "does not support options");
        assertRejected("%ex{9}", "does not support options");
    }

    @Test
    public void testInvalidDatePattern() {
        assertRejected("%d{yyyy'}", "invalid date pattern");
    }

    @Test
    public void testContextRequiresKey() {
        assertRejected("%X", "requires a non-empty {key}");
        assertRejected("%X{}", "requires a non-empty {key}");
        assertRejected("%context", "requires a non-empty {key}");
    }

    @Test
    public void testLevelTextsLengthValidated() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> FormatPattern.compile("%msg", ZoneId.of("UTC"), new String[]{"TRACE"}));
    }

    @Test
    public void testNullArgumentsRejected() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> FormatPattern.compile(null, ZoneId.of("UTC"), LEVELS));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> FormatPattern.compile("%msg", null, LEVELS));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> FormatPattern.compile("%msg", ZoneId.of("UTC"), null));
    }
}