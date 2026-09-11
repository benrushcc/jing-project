package io.jingproject.bench.marshalljson;

import io.jingproject.bench.AbstractBench;
import io.jingproject.common.ArrayAccess;
import io.jingproject.common.Utils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class AccessPatternBench extends AbstractBench {
    private static final short SHORT_CONSTANT = Utils.compact((byte) ('0' + 1), (byte) ('0' + 2));
    private static final int INT_CONSTANT = Utils.compact(
            Utils.compact((byte) ('0' + 2), (byte) ('0' + 3)),
            Utils.compact((byte) ('0' + 1), (byte) ('0' + 2))
    );
    private static final long LONG_CONSTANT = Utils.compact(
            Utils.compact(
                    Utils.compact((byte) '0', (byte) ('0' + 1)),
                    Utils.compact((byte) ('0' + 2), (byte) ('0' + 3))
            ),
            Utils.compact(
                    Utils.compact((byte) ('0' + 4), (byte) ('0' + 5)),
                    Utils.compact((byte) ('0' + 6), (byte) ('0' + 7))
            )
    );
    private Random random;
    private byte[] bytes;

    @Setup
    public void setup() {
        random = ThreadLocalRandom.current();
        bytes = new byte[16000];
    }

    @TearDown
    public void tearDown() {
        random = null;
        bytes = null;
    }

    @Benchmark
    public void directAccess(Blackhole blackhole) {
        int position = 0;
        for (int i = 0; i < 4000; i++) {
            int cp = random.nextInt(0x110000);
            bytes[position++] = (byte) (0xF0 | (cp >> 18));
            bytes[position++] = (byte) (0x80 | ((cp >> 12) & 0x3F));
            bytes[position++] = (byte) (0x80 | ((cp >> 6) & 0x3F));
            bytes[position++] = (byte) (0x80 | (cp & 0x3F));
        }
        blackhole.consume(position);
    }

    @Benchmark
    public void directAccessConstant(Blackhole blackhole) {
        int position = 0;
        for (int i = 0; i < 4000; i++) {
            bytes[position++] = (byte) '0';
            bytes[position++] = '0' + 1;
            bytes[position++] = '0' + 2;
            bytes[position++] = '0' + 3;
        }
        blackhole.consume(position);
    }

    @Benchmark
    public void mixedShortAccess(Blackhole blackhole) {
        int position = 0;
        for (int i = 0; i < 4000; i++) {
            int cp = random.nextInt(0x110000);
            bytes[position++] = (byte) (0xF0 | (cp >> 18));
            ArrayAccess.setShort(bytes, position, Utils.compact((byte) (0x80 | ((cp >> 12) & 0x3F)), (byte) (0x80 | ((cp >> 6) & 0x3F))));
            position += 2;
            bytes[position++] = (byte) (0x80 | (cp & 0x3F));
        }
        blackhole.consume(position);
    }

    @Benchmark
    public void mixedShortAccessConstant(Blackhole blackhole) {
        int position = 0;
        for (int i = 0; i < 4000; i++) {
            bytes[position++] = (byte) '0';
            ArrayAccess.setShort(bytes, position, SHORT_CONSTANT);
            position += 2;
            bytes[position++] = '0' + 3;
        }
        blackhole.consume(position);
    }

    @Benchmark
    public void mixedIntAccess(Blackhole blackhole) {
        int position = 0;
        for (int i = 0; i < 2000; i++) {
            int cp = random.nextInt(0x110000);
            int cp2 = random.nextInt(0x110000);
            bytes[position++] = (byte) (0xF0 | (cp >> 18));
            bytes[position++] = (byte) (0x80 | ((cp >> 12) & 0x3F));
            short s1 = Utils.compact((byte) (0x80 | ((cp >> 6) & 0x3F)), (byte) (0x80 | (cp & 0x3F)));
            short s2 = Utils.compact((byte) (0xF0 | (cp2 >> 18)), (byte) (0x80 | ((cp2 >> 12) & 0x3F)));
            ArrayAccess.setInt(bytes, position, Utils.compact(s1, s2));
            position += 4;
            bytes[position++] = (byte) (0x80 | ((cp2 >> 6) & 0x3F));
            bytes[position++] = (byte) (0x80 | (cp2 & 0x3F));
        }
        blackhole.consume(position);
    }

    @Benchmark
    public void mixedIntAccessConstant(Blackhole blackhole) {
        int position = 0;
        for (int i = 0; i < 2000; i++) {
            bytes[position++] = (byte) '0';
            bytes[position++] = '0' + 1;
            ArrayAccess.setInt(bytes, position, INT_CONSTANT);
            position += 4;
            bytes[position++] = '0' + 2;
            bytes[position++] = '0' + 3;
        }
        blackhole.consume(position);
    }

    @Benchmark
    public void longAccess(Blackhole blackhole) {
        int position = 0;
        for (int i = 0; i < 2000; i++) {
            int cp = random.nextInt(0x110000);
            int cp2 = random.nextInt(0x110000);
            short s1 = Utils.compact((byte) (0xF0 | (cp >> 18)), (byte) (0x80 | ((cp >> 12) & 0x3F)));
            short s2 = Utils.compact((byte) (0x80 | ((cp >> 6) & 0x3F)), (byte) (0x80 | (cp & 0x3F)));
            short s3 = Utils.compact((byte) (0xF0 | (cp2 >> 18)), (byte) (0x80 | ((cp2 >> 12) & 0x3F)));
            short s4 = Utils.compact((byte) (0x80 | ((cp2 >> 6) & 0x3F)), (byte) (0x80 | (cp2 & 0x3F)));
            long l = Utils.compact(Utils.compact(s1, s2), Utils.compact(s3, s4));
            ArrayAccess.setLong(bytes, position, l);
            position += 8;
        }
        blackhole.consume(position);
    }

    @Benchmark
    public void longAccessConstant(Blackhole blackhole) {
        int position = 0;
        for (int i = 0; i < 2000; i++) {
            ArrayAccess.setLong(bytes, position, LONG_CONSTANT);
            position += 8;
        }
        blackhole.consume(position);
    }
}
