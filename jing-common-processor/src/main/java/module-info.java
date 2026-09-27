module jing.commonprocessor {
    requires transitive jing.common;
    requires transitive java.compiler;
    requires transitive jdk.incubator.json;

    exports io.jingproject.commonprocess;
    provides javax.annotation.processing.Processor with io.jingproject.commonprocess.ProviderProcessor;
}