package io.jingproject.common;

public interface Channel<T extends Transmittable<T>> {
    Descriptor rawSocket();


}
