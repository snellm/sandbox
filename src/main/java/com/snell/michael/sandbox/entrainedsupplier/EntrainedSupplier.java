package com.snell.michael.sandbox.entrainedsupplier;

import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

import static com.snell.michael.sandbox.entrainedsupplier.ThreadUtil.latchAwait;

public class EntrainedSupplier<T> implements Supplier<T> {
    private final Supplier<T> supplier;

    private boolean anotherThreadRunning = false;
    private int waitingThreadCount = 0;
    private T sharedValue = null;
    private CountDownLatch valueObtainedLatch = null;

    public EntrainedSupplier(Supplier<T> supplier) {
        this.supplier = supplier;
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
        latchAwait(valueObtainedLatch);
        synchronized (this) {
            T value = sharedValue;
            decrementWaitingThreadCount();
            return value;
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
                Thread.currentThread().interrupt();
            }
        }
    }
}
