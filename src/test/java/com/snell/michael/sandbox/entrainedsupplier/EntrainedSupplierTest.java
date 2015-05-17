package com.snell.michael.sandbox.entrainedsupplier;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

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
        AtomicInteger valueCalculatedCount = new AtomicInteger(0);

        int repeatCount = 1000;
        for (int repeat = 0; repeat < repeatCount; repeat++) {
            int threadCount = 1000;
            CountDownLatch releaseValueLatch = new CountDownLatch(1);
            CountDownLatch threadsCompleteLatch = new CountDownLatch(threadCount);
            EntrainedSupplier<String> supplier = new EntrainedSupplier<>(() -> {
                await(releaseValueLatch);
                valueCalculatedCount.getAndIncrement();
                return EXPECTED_RESULT;
            });

            for (int thread = 0; thread < threadCount; thread++) {
                new Thread(() -> {
                    assertEquals(EXPECTED_RESULT, supplier.get());
                    threadsCompleteLatch.countDown();
                }, "repeat-"+ repeat + "-thread-" + thread).start();
            }

            // Wait for all threads to be waiting on value
            while (supplier.getWaitingCount() < threadCount - 1) {
                Thread.yield();
            }

            releaseValueLatch.countDown();

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