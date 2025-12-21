package io.jingproject.commontest;

import io.jingproject.common.Ex;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

public class ExTest {
    record IntHolder(int value) {

    }

    @Test
    public void testEx() throws InterruptedException {
        for(int times = 0; times < 1000; times++) {
            Ex<IntHolder> ex = new Ex<>(new IntHolder(0));
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(2);
            Thread t1 = Thread.ofPlatform().unstarted(() -> {
                try {
                    startLatch.await();
                    for(int i = 0; i < 100000; i++) {
                        IntHolder intHolder = ex.lock();
                        ex.unlock(new IntHolder(intHolder.value() + 1));
                    }
                    endLatch.countDown();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            Thread t2 = Thread.ofPlatform().unstarted(() -> {
                try {
                    startLatch.await();
                    for(int i = 0; i < 100000; i++) {
                        IntHolder intHolder = ex.lock();
                        ex.unlock(new IntHolder(intHolder.value() + 1));
                    }
                    endLatch.countDown();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            t1.start();
            t2.start();
            startLatch.countDown();
            endLatch.await();
            Assertions.assertEquals(100000 * 2, ex.peek().value());
        }
    }
}
