module jing.marshall {
    requires transitive jing.common;
    requires jdk.incubator.vector;

    exports io.jingproject.marshall;
    exports io.jingproject.marshall.hash;

    uses io.jingproject.marshall.MarshallFacade;
    uses io.jingproject.marshall.MarshallTransformerFacade;
}