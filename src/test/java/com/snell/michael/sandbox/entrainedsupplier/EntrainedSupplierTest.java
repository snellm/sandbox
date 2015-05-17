package com.snell.michael.sandbox.entrainedsupplier;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EntrainedSupplierTest {
    @Test
    public void callSupplier() {
        EntrainedSupplier<String> supplier = new EntrainedSupplier<>(() -> "Hello world");

        assertEquals("Hello world", supplier.get());
    }
}