module jing.marshallcbor {
    requires transitive jing.common;
    requires transitive jing.marshall;
    requires jdk.incubator.vector;

    exports io.jingproject.marshallcbor;
}