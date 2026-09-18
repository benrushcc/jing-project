import io.jingproject.common.log.LoggerFacade;

module jing.common {
    exports io.jingproject.common.anno;
    exports io.jingproject.common;
    exports io.jingproject.common.conf to jing.commontest;
    exports io.jingproject.common.log;
    uses LoggerFacade;
    uses io.jingproject.common.ConfigurationFacade;
    uses io.jingproject.common.NetFacade;
}