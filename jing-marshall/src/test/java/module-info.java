open module jing.marshalltest {
    requires transitive jing.marshall;
    requires jdk.unsupported;
    requires org.junit.jupiter.api;
    requires jmh.core;
    requires java.net.http;
}