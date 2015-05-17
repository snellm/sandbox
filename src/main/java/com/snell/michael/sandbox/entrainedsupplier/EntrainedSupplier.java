package com.snell.michael.sandbox.entrainedsupplier;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class EntrainedSupplier<T> implements Supplier<T> {
    private final Supplier<T> supplier;

    private boolean alreadyRunning = false;
    private int waitingCount = 0;
    private CountDownLatch valueObtainedLatch = null;
    private AtomicReference<T> valueReference = new AtomicReference<>();

    public EntrainedSupplier(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public T get() {
        boolean currentThreadWillRun;
        synchronized (this) {
            if (!alreadyRunning) {
                currentThreadWillRun = true;
                alreadyRunning = true;
                waitingCount = 0;
                valueObtainedLatch = new CountDownLatch(1);
            } else {
                currentThreadWillRun = false;
                waitingCount++;
            }
        }

        if (currentThreadWillRun) {
            T obtainedValue = supplier.get();
            valueReference.set(obtainedValue);
            valueObtainedLatch.countDown();
            synchronized (this) {
                while (waitingCount > 0) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                alreadyRunning = false;
            }
            valueReference.set(null);
            return obtainedValue;
        } else {
            awaitValue();
            T obtainedValue = valueReference.get();
            synchronized (this) {
                waitingCount--;
                if (waitingCount == 0) {
                    notifyAll();
                }
            }
            return obtainedValue;
        }
    }

    synchronized int getWaitingCount() {
        return waitingCount;
    }

    private void awaitValue() {
        try {
            valueObtainedLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted", e);
        }
    }
}
