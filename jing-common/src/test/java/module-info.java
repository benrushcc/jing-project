open module jing.commontest {
    exports io.jingproject.commontest;
    exports io.jingproject.commontest.bench;
    requires transitive jing.common;
    requires jmh.core;
    requires jdk.unsupported;
    requires org.junit.jupiter.api;
}