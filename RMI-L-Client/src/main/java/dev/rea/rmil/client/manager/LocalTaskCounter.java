package dev.rea.rmil.client.manager;

import java.util.concurrent.atomic.AtomicInteger;

public final class LocalTaskCounter {

    private final AtomicInteger atomicValue;

    public LocalTaskCounter(int value) {
        this.atomicValue = new AtomicInteger(value);
    }

    public  int get() {
        return atomicValue.get();
    }

    public int incrementAndGet() {
        return atomicValue.incrementAndGet();
    }

    public int decrementAndGet() {
        //todo: listen
        return atomicValue.decrementAndGet();
    }
}
