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
    public void nestedSupplierCalledOnce() {
        CountDownLatch releaseValueLatch = new CountDownLatch(1);
        CountDownLatch threadsCompleteLatch = new CountDownLatch(2);
        AtomicInteger valueCalculated = new AtomicInteger(0);
        EntrainedSupplier<String> supplier = new EntrainedSupplier<>(() -> {
            await(releaseValueLatch);
            valueCalculated.getAndIncrement();
            return EXPECTED_RESULT;
        });

        new Thread(() -> {
            assertEquals(EXPECTED_RESULT, supplier.get());
            threadsCompleteLatch.countDown();
        }).start();

        new Thread(() -> {
            assertEquals(EXPECTED_RESULT, supplier.get());
            threadsCompleteLatch.countDown();
        }).start();

        releaseValueLatch.countDown();

        await(threadsCompleteLatch);

        assertEquals(1, valueCalculated.get());
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