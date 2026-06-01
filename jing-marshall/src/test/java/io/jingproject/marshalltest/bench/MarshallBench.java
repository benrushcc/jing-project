package io.jingproject.marshalltest.bench;

import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.MarshallWriter;
import io.jingproject.marshalltest.entity.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

// 这个测试用来对比直接构造，反射构造以及使用marshall构造之间的性能差距，预期效果是marshall的性能要尽可能逼近直接构造，优于反射构造

@BenchmarkMode(value = Mode.AverageTime)
@Warmup(iterations = 1, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
public class MarshallBench {
    private static final int BATCH_SIZE = 10000;
    private int[] intArray;
    private long[] longArray;
    private String[] strArray;
    private LocalDateTime[] localDateTimeArray;
    private Duration[] durationArray;

    @Setup(Level.Iteration)
    public void setup() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        intArray = new int[BATCH_SIZE];
        longArray = new long[BATCH_SIZE];
        strArray = new String[BATCH_SIZE];
        localDateTimeArray = new LocalDateTime[BATCH_SIZE];
        durationArray = new Duration[BATCH_SIZE];
        long start = LocalDateTime.of(2000, 1, 1, 0, 0, 0).atZone(ZoneId.systemDefault()).toEpochSecond();
        long end = LocalDateTime.of(2026, 1, 1, 0, 0, 0).atZone(ZoneId.systemDefault()).toEpochSecond();
        for (int index = 0; index < BATCH_SIZE; index++) {
            intArray[index] = random.nextInt();
            longArray[index] = random.nextLong();
            long randTime = random.nextLong(start, end);
            LocalDateTime randLocalDateTime = Instant.ofEpochSecond(randTime).atZone(ZoneId.systemDefault()).toLocalDateTime();
            strArray[index] = randLocalDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            localDateTimeArray[index] = randLocalDateTime;
            durationArray[index] = Duration.ofMillis(randTime);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void testBeanDirect(Blackhole blackhole) {
        for (int i = 0; i < BATCH_SIZE; i++) {
            int intValue = intArray[i];
            long longValue = longArray[i];
            String strValue = strArray[i];
            LocalDateTime localDateTimeValue = localDateTimeArray[i];
            BeanEntity beanEntity = new BeanEntity();
            beanEntity.setIntValue(intValue);
            beanEntity.setLongValue(longValue);
            beanEntity.setStrValue(strValue);
            beanEntity.setTimeValue(localDateTimeValue);
            blackhole.consume(beanEntity);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void testBeanReflection(Blackhole blackhole) {
        try {
            Constructor<BeanEntity> constructor = BeanEntity.class.getConstructor();
            Method setIntValue = BeanEntity.class.getMethod("setIntValue", int.class);
            Method setLongValue = BeanEntity.class.getMethod("setLongValue", long.class);
            Method setStrValue = BeanEntity.class.getMethod("setStrValue", String.class);
            Method setTimeValue = BeanEntity.class.getMethod("setTimeValue", LocalDateTime.class);
            for (int i = 0; i < BATCH_SIZE; i++) {
                int intValue = intArray[i];
                long longValue = longArray[i];
                String strValue = strArray[i];
                LocalDateTime localDateTimeValue = localDateTimeArray[i];
                BeanEntity instance = constructor.newInstance();
                setIntValue.invoke(instance, intValue);
                setLongValue.invoke(instance, longValue);
                setStrValue.invoke(instance, strValue);
                setTimeValue.invoke(instance, localDateTimeValue);
                blackhole.consume(instance);
            }
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void testBeanMarshall(Blackhole blackhole) {
        MarshallFacade marshallFacade = new BeanEntityMarshallFacade();
        for (int i = 0; i < BATCH_SIZE; i++) {
            MarshallWriter writer = marshallFacade.newWriter();
            int intValue = intArray[i];
            long longValue = longArray[i];
            String strValue = strArray[i];
            LocalDateTime localDateTimeValue = localDateTimeArray[i];
            writer.setInt(0, intValue);
            writer.setLong(1, longValue);
            writer.setObject(2, strValue);
            writer.setObject(3, localDateTimeValue);
            BeanEntity instance = (BeanEntity) marshallFacade.construct(writer);
            blackhole.consume(instance);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void testExtendDirect(Blackhole blackhole) {
        for (int i = 0; i < BATCH_SIZE; i++) {
            int intValue = intArray[i];
            long longValue = longArray[i];
            String strValue = strArray[i];
            LocalDateTime localDateTimeValue = localDateTimeArray[i];
            Duration durationValue = durationArray[i];
            ExtendEntity extendEntity = new ExtendEntity();
            extendEntity.setIntValue(intValue);
            extendEntity.setLongValue(longValue);
            extendEntity.setStrValue(strValue);
            extendEntity.setTimeValue(localDateTimeValue);
            extendEntity.setDurationValue(durationValue);
            blackhole.consume(extendEntity);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void testExtendEntityReflection(Blackhole blackhole) {
        try {
            Constructor<ExtendEntity> constructor = ExtendEntity.class.getConstructor();
            Method setIntValue = ExtendEntity.class.getMethod("setIntValue", int.class);
            Method setLongValue = ExtendEntity.class.getMethod("setLongValue", long.class);
            Method setStrValue = ExtendEntity.class.getMethod("setStrValue", String.class);
            Method setTimeValue = ExtendEntity.class.getMethod("setTimeValue", LocalDateTime.class);
            Method setDurationValue = ExtendEntity.class.getMethod("setDurationValue", Duration.class);
            for (int i = 0; i < BATCH_SIZE; i++) {
                int intValue = intArray[i];
                long longValue = longArray[i];
                String strValue = strArray[i];
                LocalDateTime localDateTimeValue = localDateTimeArray[i];
                Duration durationValue = durationArray[i];
                ExtendEntity instance = constructor.newInstance();
                setIntValue.invoke(instance, intValue);
                setLongValue.invoke(instance, longValue);
                setStrValue.invoke(instance, strValue);
                setTimeValue.invoke(instance, localDateTimeValue);
                setDurationValue.invoke(instance, durationValue);
                blackhole.consume(instance);
            }
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void testExtendEntityMarshall(Blackhole blackhole) {
        MarshallFacade marshallFacade = new ExtendEntityMarshallFacade();
        for (int i = 0; i < BATCH_SIZE; i++) {
            MarshallWriter writer = marshallFacade.newWriter();
            int intValue = intArray[i];
            long longValue = longArray[i];
            String strValue = strArray[i];
            LocalDateTime localDateTimeValue = localDateTimeArray[i];
            Duration durationValue = durationArray[i];
            writer.setInt(0, intValue);
            writer.setLong(1, longValue);
            writer.setObject(2, strValue);
            writer.setObject(3, localDateTimeValue);
            writer.setObject(4, durationValue);
            ExtendEntity instance = (ExtendEntity) marshallFacade.construct(writer);
            blackhole.consume(instance);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void testRecordDirect(Blackhole blackhole) {
        for (int i = 0; i < BATCH_SIZE; i++) {
            int intValue = intArray[i];
            long longValue = longArray[i];
            String strValue = strArray[i];
            LocalDateTime localDateTimeValue = localDateTimeArray[i];
            RecordEntity recordEntity = new RecordEntity(intValue, longValue, strValue, localDateTimeValue);
            blackhole.consume(recordEntity);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void testRecordReflection(Blackhole blackhole) {
        try {
            Constructor<RecordEntity> CONSTRUCTOR = RecordEntity.class.getConstructor(int.class, long.class, String.class, LocalDateTime.class);
            for (int i = 0; i < BATCH_SIZE; i++) {
                int intValue = intArray[i];
                long longValue = longArray[i];
                String strValue = strArray[i];
                LocalDateTime localDateTimeValue = localDateTimeArray[i];
                RecordEntity instance = CONSTRUCTOR.newInstance(intValue, longValue, strValue, localDateTimeValue);
                blackhole.consume(instance);
            }
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void testRecordMarshall(Blackhole blackhole) {
        MarshallFacade marshallFacade = new RecordEntityMarshallFacade();
        for (int i = 0; i < BATCH_SIZE; i++) {
            MarshallWriter writer = marshallFacade.newWriter();
            int intValue = intArray[i];
            long longValue = longArray[i];
            String strValue = strArray[i];
            LocalDateTime localDateTimeValue = localDateTimeArray[i];
            writer.setInt(0, intValue);
            writer.setLong(1, longValue);
            writer.setObject(2, strValue);
            writer.setObject(3, localDateTimeValue);
            RecordEntity instance = (RecordEntity) marshallFacade.construct(writer);
            blackhole.consume(instance);
        }
    }

    static void main() throws RunnerException {
        Options opt = new OptionsBuilder().include(MarshallBench.class.getSimpleName()).addProfiler(GCProfiler.class).build();
        new Runner(opt).run();
    }
}
