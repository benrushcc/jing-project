package io.jingproject.common;

import java.util.function.Supplier;

public interface Logger {
    default void debug(String msg) {
        debug(msg, null);
    }

    default void debug(String msg, Throwable throwable) {
        if (enabled(LogLevel.DEBUG)) {
            log(LogLevel.DEBUG, msg, throwable);
        }
    }

    default void info(String msg) {
        info(msg, null);
    }

    default void info(String msg, Throwable throwable) {
        if (enabled(LogLevel.INFO)) {
            log(LogLevel.INFO, msg, throwable);
        }
    }

    default void error(String msg) {
        error(msg, null);
    }

    default void error(String msg, Throwable throwable) {
        log(LogLevel.ERROR, msg, throwable);
    }

    default void debug(Supplier<String> msgSupplier) {
        debug(msgSupplier, null);
    }

    default void debug(Supplier<String> msgSupplier, Throwable throwable) {
        if (enabled(LogLevel.DEBUG)) {
            log(LogLevel.DEBUG, msgSupplier.get(), throwable);
        }
    }

    default void info(Supplier<String> msgSupplier) {
        info(msgSupplier, null);
    }

    default void info(Supplier<String> msgSupplier, Throwable throwable) {
        if (enabled(LogLevel.INFO)) {
            log(LogLevel.INFO, msgSupplier.get(), throwable);
        }
    }

    default void error(Supplier<String> msgSupplier) {
        error(msgSupplier, null);
    }

    default void error(Supplier<String> msgSupplier, Throwable throwable) {
        log(LogLevel.ERROR, msgSupplier.get(), throwable);
    }

    void setLevel(LogLevel level);

    boolean enabled(LogLevel level);

    void log(LogLevel level, String msg, Throwable throwable);
}
