package io.jingproject.common.log;

// facade for obtaining Logger instances
// different providers can implement this interface to supply their own Logger
// when no provider is found, a default implementation is used
public interface LoggerFacade {
    Logger getLogger(Class<?> clazz);
}
