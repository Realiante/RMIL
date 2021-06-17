package dev.rea.rmil.client.grid;

import java.io.Serializable;
import java.util.function.ToDoubleFunction;

class TestToDouble implements Serializable, ToDoubleFunction<Integer> {
    @Override
    public double applyAsDouble(Integer value) {
        return value * 1.5;
    }
}
