package io.jingproject.common;

@FunctionalInterface
public interface NetFacade {
    void handle(NetEvent event);
}
