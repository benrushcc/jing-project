package io.jingproject.common;

import java.io.Serial;

public final class SizeLimitExceededException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public SizeLimitExceededException(int current, int limit) {
        super("requested size exceeds limit, current : " + current + ", limit : " + limit);
    }

    public SizeLimitExceededException(long current, long limit) {
        super("requested size exceeds limit, current : " + current + ", limit : " + limit);
    }
}
