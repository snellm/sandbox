package com.snell.michael.sandbox.entrainedsupplier;

import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

public class EntrainedSupplier<T> implements Supplier<T> {
    private final Supplier<T> supplier;

    private boolean running = false;
    private CountDownLatch latch = null;
    private T value = null;

    public EntrainedSupplier(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public T get() {
        boolean thisThreadWillRun = false;
        synchronized (this) {
            if (!running) {
                running = true;
                thisThreadWillRun = true;
                latch = new CountDownLatch(1);
            }
        }

        if (thisThreadWillRun) {
            value = supplier.get();
            latch.countDown();
        } else {
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted", e);
            }
        }

        return value;
    }
}
