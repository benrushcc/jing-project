package io.jingproject.commontest.test;

import io.jingproject.common.DualLock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

public class DualLockTest {
    private static final int BATCH = 1000;
    private static final int LOCK_TIMES = 1000;
    record IntHolder(int value) {

    }

    @Test
    public void dualLockTest() throws InterruptedException {
        for (int times = 0; times < BATCH; times++) {
            DualLock<IntHolder> dualLock = new DualLock<>(new IntHolder(0));
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(2);
            Thread t1 = Thread.ofPlatform().unstarted(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < LOCK_TIMES; i++) {
                        IntHolder intHolder = dualLock.lock();
                        dualLock.unlock(new IntHolder(intHolder.value() + 1));
                    }
                    endLatch.countDown();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            Thread t2 = Thread.ofPlatform().unstarted(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < LOCK_TIMES; i++) {
                        IntHolder intHolder = dualLock.lock();
                        dualLock.unlock(new IntHolder(intHolder.value() + 1));
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
            Assertions.assertEquals(LOCK_TIMES * 2, dualLock.peek().value());
        }
    }
}
