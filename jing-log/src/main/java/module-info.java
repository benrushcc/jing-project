module jing.log {
    requires transitive jing.common;
    requires transitive jing.ffm;
    requires transitive static jing.common.annprocessor;
    requires transitive static jing.ffmprocessor;

    provides io.jingproject.common.LoggerFacade with io.jingproject.log.LoggerFacadeImpl;
    uses io.jingproject.log.LogEventHandler;
}