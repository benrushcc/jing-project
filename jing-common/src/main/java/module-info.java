module jing.common {
    exports io.jingproject.common.anno;
    exports io.jingproject.common;
    exports io.jingproject.common.conf to jing.commontest;
    uses io.jingproject.common.LoggerFacade;
    uses io.jingproject.common.ConfigurationFacade;
    uses io.jingproject.common.NetFacade;
}