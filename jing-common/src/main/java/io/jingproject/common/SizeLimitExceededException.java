package io.jingproject.common;

public final class SizeLimitExceededException extends RuntimeException {
    public SizeLimitExceededException(int current, int limit) {
        super("requested size exceeds limit, current : " + current + ", limit : " + limit);
    }

    public SizeLimitExceededException(long current, long limit) {
        super("requested size exceeds limit, current : " + current + ", limit : " + limit);
    }
}
