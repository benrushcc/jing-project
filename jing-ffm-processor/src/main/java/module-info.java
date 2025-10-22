module jing.ffmprocessor {
    requires transitive jing.annprocessor;
    requires transitive jing.ffm;
    requires transitive java.compiler;

    exports io.jingproject.ffmprocessor;

    provides javax.annotation.processing.Processor with io.jingproject.ffmprocessor.FfmProcessor;
}