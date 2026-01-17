module jing.common {
    requires java.xml;
    exports io.jingproject.common;
    uses io.jingproject.common.LoggerFacade;
    uses io.jingproject.common.ConfigurationFacade;
    uses io.jingproject.common.NetFacade;
}