import io.jingproject.ffm.LibFacade;

module jing.ffm {
    requires transitive jing.common;

    exports io.jingproject.ffm;

    uses LibFacade;
}