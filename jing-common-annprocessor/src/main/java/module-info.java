module jing.common.annprocessor {
    requires transitive java.compiler;
    requires transitive jing.common;
    exports io.jingproject.annprocess;
    provides javax.annotation.processing.Processor with io.jingproject.annprocess.ProviderProcessor;
}