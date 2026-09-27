open module jing.logtest {
    requires transitive jing.log;
    requires org.junit.jupiter.api;

    provides io.jingproject.log.LogEventHandler with io.jingproject.logtest.CaptureLogEventHandler;
}