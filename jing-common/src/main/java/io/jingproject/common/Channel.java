package io.jingproject.common;

public interface Channel<T> {
    Socket socket();

    Enc<T> enc();

    Dec<T> dec();


}
