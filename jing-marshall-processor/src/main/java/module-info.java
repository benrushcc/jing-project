module jing.marshallprocessor {
    requires transitive jing.common;
    requires transitive jing.commonprocessor;
    requires transitive jing.marshall;
    requires transitive java.compiler;

    exports io.jingproject.marshallprocessor;

    provides javax.annotation.processing.Processor with io.jingproject.marshallprocessor.MarshallProcessor;
}