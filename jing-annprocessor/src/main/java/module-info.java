module jing.annprocessor {
    requires transitive java.compiler;
    exports io.jingproject.annprocess;
    provides javax.annotation.processing.Processor with io.jingproject.annprocess.ProviderProcessor;
}