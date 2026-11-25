module jing.netprocessor {
    requires transitive jing.common;
    requires transitive jing.commonprocessor;
    requires transitive jing.net;
    requires transitive java.compiler;

    exports io.jingproject.netprocessor;

    provides javax.annotation.processing.Processor with io.jingproject.netprocessor.NetHandlerProcessor, io.jingproject.netprocessor.NetResourceProcessor;
}