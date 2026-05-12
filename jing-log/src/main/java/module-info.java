module jing.log {
    requires transitive jing.common;
    requires transitive jing.ffm;
    requires transitive jing.bindings;

    provides io.jingproject.common.LoggerFacade with io.jingproject.log.LoggerFacadeImpl;
    uses io.jingproject.log.LogEventHandler;
}