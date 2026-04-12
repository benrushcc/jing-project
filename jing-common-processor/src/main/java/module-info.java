module jing.commonprocessor {
    requires transitive jing.common;
    requires transitive java.compiler;

    exports io.jingproject.commonprocess;
    provides javax.annotation.processing.Processor with io.jingproject.commonprocess.ProviderProcessor;
}