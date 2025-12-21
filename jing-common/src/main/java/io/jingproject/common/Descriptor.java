package io.jingproject.common;

/**
 *   Currently we could just make the assumptions that all descriptors are int or long
 */
public sealed interface Descriptor {

    int asInt();

    long asLong();

    static Descriptor of(int value) {
        return new IntDescriptor(value);
    }

    static Descriptor of(long value) {
        return new LongDescriptor(value);
    }

    record IntDescriptor(int value) implements Descriptor {
        @Override
        public int asInt() {
            return value;
        }

        @Override
        public long asLong() {
            return value;
        }
    }

    record LongDescriptor(long value) implements Descriptor {
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
