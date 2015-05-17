package com.snell.michael.sandbox.entrainedsupplier;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.lang.Thread.currentThread;

public class EntrainedSupplier<T> implements Supplier<T> {
    private final Supplier<T> supplier;
    private final long timeout;
    private final TimeUnit timeUnit;

    private boolean anotherThreadRunning = false;
    private int waitingThreadCount = 0;
    private T sharedValue = null;
    private CountDownLatch valueObtainedLatch = null;

    public EntrainedSupplier(Supplier<T> supplier, long timeout, TimeUnit timeUnit) {
        this.supplier = supplier;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
    }

    @Override
    public T get() {
        if (currentThreadWillObtainValue()) {
            return obtainAndDistributeValue();
        } else {
            return awaitAndRetrieveValue();
        }
    }

    private T obtainAndDistributeValue() {
        T value = supplier.get();
        synchronized (this) {
            sharedValue = value;
            valueObtainedLatch.countDown();
            awaitWaitingThreadCount(0);
            anotherThreadRunning = false;
            sharedValue = null;
        }
        return value;
    }

    private T awaitAndRetrieveValue() {
        try {
            if (valueObtainedLatch.await(timeout, timeUnit)) {
                synchronized (this) {
                    T value = sharedValue;
                    decrementWaitingThreadCount();
                    return value;
                }
            } else {
                throw new RuntimeException("Timed out waiting for value");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interruted", e);
        }

    }

    private synchronized boolean currentThreadWillObtainValue() {
        if (!anotherThreadRunning) {
            anotherThreadRunning = true;
            valueObtainedLatch = new CountDownLatch(1);
            return true;
        } else {
            incrementWaitingThreadCount();
            return false;
        }
    }

    private synchronized void incrementWaitingThreadCount() {
        waitingThreadCount++;
        notify();
    }

    private synchronized void decrementWaitingThreadCount() {
        waitingThreadCount--;
        if (waitingThreadCount == 0) {
            notify();
        }
    }

    synchronized void awaitWaitingThreadCount(int expectedWaitingCount) {
        while (waitingThreadCount != expectedWaitingCount) {
            try {
                wait();
            } catch (InterruptedException e) {
                currentThread().interrupt();
            }
        }
    }
}
