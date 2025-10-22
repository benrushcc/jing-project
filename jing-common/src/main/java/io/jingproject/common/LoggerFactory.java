package io.jingproject.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ServiceLoader;

public final class LoggerFactory {

    private LoggerFactory() {
        throw new UnsupportedOperationException("utility class");
    }

    public static Logger getLogger(Class<?> clazz) {
        class Holder {
            static final LoggerFacade INSTANCE = Anchor.compute(LoggerFacade.class, () -> {
                Optional<LoggerFacade> fc = ServiceLoader.load(LoggerFacade.class).findFirst();
                return fc.orElseGet(DefaultLoggerFacade::new);
            });
        }
        return Holder.INSTANCE.getLogger(clazz);
    }

    static final class DefaultLoggerFacade implements LoggerFacade {

        static final class DefaultLogger implements Logger {
            private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
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
                if(enabled(level)) {
                    String timestamp = LocalDateTime.now().format(formatter);
                    String threadName = Thread.currentThread().getName();
                    String className = clazz.getCanonicalName();
                    String logLine = String.format("%s [%s] %-5s %s - %s",
                            timestamp, threadName, level.name(), className, msg);
                    System.out.println(logLine);
                    if(throwable != null) {
                        throwable.printStackTrace(System.out);
                    }
                }
            }
        }

        @Override
        public Logger getLogger(Class<?> clazz) {
            return new DefaultLogger(clazz);
        }
    }
}
