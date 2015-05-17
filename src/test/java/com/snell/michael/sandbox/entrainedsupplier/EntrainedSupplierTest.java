package com.snell.michael.sandbox.entrainedsupplier;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
            return EXPECTED_RESULT;
        });

        for (int repeat = 0; repeat < repeatCount; repeat++) {
            releaseValueLock.lock();
            CountDownLatch threadsCompleteLatch = new CountDownLatch(threadCount);

            for (int thread = 0; thread < threadCount; thread++) {
                new Thread(() -> {
                    assertEquals(EXPECTED_RESULT, supplier.get());
                    threadsCompleteLatch.countDown();
                }, "repeat-"+ repeat + "-thread-" + thread).start();
            }

            supplier.awaitWaitingCount(threadCount - 1);

            releaseValueLock.unlock();
            await(threadsCompleteLatch);

            assertEquals(repeat + 1, valueCalculatedCount.get());

        }
    }

    // TODO Read up on best approach
    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted", e);
        }
    }
}