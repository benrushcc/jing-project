package io.jingproject.marshall;

public interface MarshallTransformer<A, B> {
    B transformTo(A source);

    A transformFrom(B source);
}
