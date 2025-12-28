package io.jingproject.log;

import io.jingproject.common.ConfigurationFactory;
import io.jingproject.common.Os;
import io.jingproject.ffm.SharedLibs;

import java.util.Map;

/**
 *   ConsoleLogEventHandler is designed to output logs to stdout and stderr
 */
public final class ConsoleLogEventHandler implements LogEventHandler {
    private static final LogBindings LOG_BINDINGS = SharedLibs.getImpl(LogBindings.class);
    private static final String IDEA_RUNTIME_TYPICAL_CLASS_NAME = "com.intellij.rt.compiler.JavacResourcesReader";
    private static final Boolean USING_INTELLIJ_IDEA = checkIntellijIdeaEnvironment();

    /**
     *   Check if current environment is using IntelliJ IDEA, if so, the terminal support ansi color by default
     */
    private static Boolean checkIntellijIdeaEnvironment() {
        try {
            Class<?> _ = Class.forName(IDEA_RUNTIME_TYPICAL_CLASS_NAME);
        } catch (ClassNotFoundException e) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    private static Boolean checkAnsiColorEnabled() {
        if(USING_INTELLIJ_IDEA) {
            return Boolean.TRUE;
        }
        switch (Os.current()) {
            case WINDOWS -> {
                return LOG_BINDINGS.winAnsiSupport() == 0;
            }
            case LINUX, MACOS -> {
                if(System.console() == null) {
                    return Boolean.FALSE;
                }
                return System.getenv("TERM") != null;
            }
            default -> throw new AssertionError("Should never be reached");
        }
    }

    public ConsoleLogEventHandler() {
        Map<String, String> conf = ConfigurationFactory.items("jing.log.console");

    }

    @Override
    public void handle(LogEvent logEvent) {

    }
}
