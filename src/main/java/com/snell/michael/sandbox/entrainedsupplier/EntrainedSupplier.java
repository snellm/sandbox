package com.snell.michael.sandbox.entrainedsupplier;

import java.util.function.Supplier;

public class EntrainedSupplier<T> implements Supplier<T> {
    private final Supplier<T> supplier;

    public EntrainedSupplier(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public T get() {
        return supplier.get();
    }
}
