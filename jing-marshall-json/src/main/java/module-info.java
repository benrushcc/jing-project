module jing.marshalljson {
    requires transitive jing.common;
    requires transitive jing.marshall;
    requires jdk.incubator.vector;

    exports io.jingproject.marshalljson;
}