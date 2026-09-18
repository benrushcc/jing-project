package io.jingproject.common.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ServiceLoader;

// entry point for obtaining Logger instances
// it loads a LoggerFacade via ServiceLoader
// and falls back to the default implementation when no provider is available
public final class LoggerFactory {

    private static final LazyConstant<LoggerFacade> FACTORY = LazyConstant.of(() -> {
        Optional<LoggerFacade> fc = ServiceLoader.load(LoggerFacade.class).findFirst();
        return fc.orElseGet(DefaultLoggerFacade::new);
    });

    private LoggerFactory() {
        throw new UnsupportedOperationException("utility class");
    }

    public static Logger getLogger(Class<?> clazz) {
        return FACTORY.get().getLogger(clazz);
    }

    // default implementation of LoggerFacade
    // acts as a fallback when no other provider supplies a LoggerFacade
    // prints log messages to standard output by default
    static final class DefaultLoggerFacade implements LoggerFacade {

        @Override
        public Logger getLogger(Class<?> clazz) {
            return new DefaultLogger(clazz);
        }

        static final class DefaultLogger implements Logger {
            private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
            private final Class<?> clazz;
            private volatile LogLevel logLevel;

            DefaultLogger(Class<?> clazz) {
                this.clazz = clazz;
                this.logLevel = LogLevel.fromString(System.getProperty("jing.log.level", "INFO"));
            }

            @Override
            public void setLevel(LogLevel level) {
                this.logLevel = level;
            }

            @Override
            public boolean enabled(LogLevel level) {
                return level.value() >= logLevel.value();
            }

            @Override
            public void log(LogLevel level, String msg, Throwable throwable) {
                if (enabled(level)) {
                    String timestamp = OffsetDateTime.now().format(formatter);
                    String threadName = Thread.currentThread().getName();
                    String className = clazz.getCanonicalName();
                    StringBuilder stringBuilder = new StringBuilder(128)
                            .append(timestamp).append(' ')
                            .append(String.format("%-5s", level.name())).append(' ')
                            .append('[').append(threadName).append("] ")
                            .append(className).append(" - ")
                            .append(msg);
                    if (throwable != null) {
                        StringWriter sw = new StringWriter();
                        throwable.printStackTrace(new PrintWriter(sw));
                        stringBuilder.append(System.lineSeparator()).append(sw);
                    }
                    System.out.println(stringBuilder);
                }
            }
        }
    }
}
