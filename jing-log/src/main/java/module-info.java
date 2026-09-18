import io.jingproject.common.log.LoggerFacade;

module jing.log {
    requires transitive jing.common;
    requires transitive jing.ffm;
    requires transitive jing.bindings;

    provides LoggerFacade with io.jingproject.log.LoggerFacadeImpl;
    uses io.jingproject.log.LogEventHandler;
}