package io.jingproject.log;

import io.jingproject.common.Os;
import io.jingproject.ffm.Libs;

/**
 * ConsoleLogEventHandler is designed to output logs to stdout and stderr
 */
public final class ConsoleLogEventHandler implements LogEventHandler {
    private static final LogBindings LOG_BINDINGS = Libs.getImpl(LogBindings.class);
    private static final String IDEA_RUNTIME_TYPICAL_CLASS_NAME = "com.intellij.rt.compiler.JavacResourcesReader";
    private static final boolean USING_INTELLIJ_IDEA = checkIntellijIdeaEnvironment();

    /**
     * Check if current runtime environment is IntelliJ IDEA IDE, if so, the terminal support ansi color by default
     */
    private static boolean checkIntellijIdeaEnvironment() {
        try {
            Class<?> _ = Class.forName(IDEA_RUNTIME_TYPICAL_CLASS_NAME);
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;
    }

    private static boolean checkAnsiColorEnabled() {
        if (USING_INTELLIJ_IDEA) {
            return true;
        }
        switch (Os.current()) {
            case WINDOWS -> {
                return LOG_BINDINGS.winAnsiSupport() == 0;
            }
            case LINUX, MACOS -> {
                if (System.console() == null) {
                    return false;
                }
                return System.getenv("TERM") != null;
            }
            default -> throw new AssertionError();
        }
    }

    public ConsoleLogEventHandler() {

    }

    @Override
    public void handle(LogEvent logEvent) {

    }
}
