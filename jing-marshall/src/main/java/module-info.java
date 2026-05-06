module jing.marshall {
    requires transitive jing.common;
    requires jdk.incubator.vector;

    exports io.jingproject.marshall;
    exports io.jingproject.marshall.hash;
    exports io.jingproject.marshall.cbor;
    exports io.jingproject.marshall.json;

    uses io.jingproject.marshall.MarshallFacade;
}