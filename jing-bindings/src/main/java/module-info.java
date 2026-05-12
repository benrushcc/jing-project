module jing.bindings {
    requires transitive jing.common;
    requires transitive jing.ffm;
    requires static jing.commonprocessor;
    requires static jing.ffmprocessor;

    exports io.jingproject.bindings;
    exports io.jingproject.bindings.alloc;
    exports io.jingproject.bindings.net;
}