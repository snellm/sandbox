package com.snell.michael.sandbox.entrainedsupplier;

import java.util.concurrent.CountDownLatch;

import static java.lang.Thread.currentThread;

class ThreadUtil {
    private ThreadUtil() {}

    static void latchAwait(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            currentThread().interrupt();
        }
    }
}
