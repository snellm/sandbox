package com.snell.michael.sandbox.entrainedsupplier;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.snell.michael.sandbox.entrainedsupplier.ThreadUtil.latchAwait;
import static org.junit.Assert.assertEquals;

public class EntrainedSupplierTest {
    private  static final String EXPECTED_RESULT = "Hello world";

    @Test
    public void callSupplier() {
        EntrainedSupplier<String> supplier = new EntrainedSupplier<>(() -> EXPECTED_RESULT);

        assertEquals(EXPECTED_RESULT, supplier.get());
    }

    @Test
    public void supplierCalledOncePerSetOfThreads() {
        final int repeatCount = 1000;
        final int threadCount = 10;

        AtomicInteger valueCalculatedCount = new AtomicInteger(0);
        Lock releaseValueLock = new ReentrantLock();
        EntrainedSupplier<String> supplier = new EntrainedSupplier<>(() -> {
            releaseValueLock.lock();
            valueCalculatedCount.getAndIncrement();
            releaseValueLock.unlock();
            return EXPECTED_RESULT + valueCalculatedCount.get();
        });

        for (int repeat = 0; repeat < repeatCount; repeat++) {
            releaseValueLock.lock();
            CountDownLatch threadsCompleteLatch = new CountDownLatch(threadCount);

            for (int thread = 0; thread < threadCount; thread++) {
                new Thread(() -> {
                    assertEquals(EXPECTED_RESULT + (valueCalculatedCount.get() + 1), supplier.get());
                    threadsCompleteLatch.countDown();
                }, "repeat-"+ repeat + "-thread-" + thread).start();
            }

            supplier.awaitWaitingThreadCount(threadCount - 1);

            releaseValueLock.unlock();
            latchAwait(threadsCompleteLatch);

            assertEquals(repeat + 1, valueCalculatedCount.get());

        }
    }
}