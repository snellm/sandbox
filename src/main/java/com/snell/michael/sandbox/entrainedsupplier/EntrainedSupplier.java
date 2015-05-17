package com.snell.michael.sandbox.entrainedsupplier;

import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

public class EntrainedSupplier<T> implements Supplier<T> {
    private final Supplier<T> supplier;

    private boolean alreadyRunning = false;
    private int waitingCount = 0;
    private CountDownLatch valueObtainedLatch = null;
    private T sharedValue = null;

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
                notify();
            }
        }

        if (currentThreadWillRun) {
            T value = supplier.get();
            synchronized (this) {
                sharedValue = value;
                valueObtainedLatch.countDown();
                awaitWaitingCount(0);
                alreadyRunning = false;
                sharedValue = null;
            }
            return value;
        } else {
            awaitValue();
            synchronized (this) {
                T value = sharedValue;
                waitingCount--;
                if (waitingCount == 0) {
                    notify();
                }
                return value;
            }
        }
    }

    synchronized void awaitWaitingCount(int expectedWaitingCount) {
        while (waitingCount != expectedWaitingCount) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted", e);
            }
        }
    }

    private void awaitValue() {
        try {
            valueObtainedLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted", e);
        }
    }
}
