module jing.bindings {
    requires transitive jing.common;
    requires transitive jing.ffm;
    requires transitive static jing.commonprocessor;
    requires transitive static jing.ffmprocessor;

    exports io.jingproject.bindings;
    exports io.jingproject.bindings.alloc;
}