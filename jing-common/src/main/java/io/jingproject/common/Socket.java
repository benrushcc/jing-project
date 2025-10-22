package io.jingproject.common;

/**
 *   Currently we could just make the assumptions that all sockets are int or long
 */
public sealed interface Socket {

    int asInt();

    long asLong();

    static Socket of(int value) {
        return new IntSocket(value);
    }

    static Socket of(long value) {
        return new LongSocket(value);
    }

    record IntSocket(int value) implements Socket {
        @Override
        public int asInt() {
            return value;
        }

        @Override
        public long asLong() {
            return value;
        }
    }

    record LongSocket(long value) implements Socket {
        @Override
        public int asInt() {
            return Math.toIntExact(value);
        }

        @Override
        public long asLong() {
            return value;
        }
    }
}
