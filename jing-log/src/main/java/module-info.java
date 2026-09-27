import io.jingproject.common.log.LoggerFacade;

module jing.log {
    requires transitive jing.common;
    requires transitive jing.ffm;
    requires transitive jing.bindings;

    exports io.jingproject.log;

    provides LoggerFacade with io.jingproject.log.LoggerFacadeImpl;
    provides io.jingproject.log.LogEventHandler with io.jingproject.log.ConsoleLogEventHandler,
            io.jingproject.log.FileLogEventHandler;
    uses io.jingproject.log.LogEventHandler;
}