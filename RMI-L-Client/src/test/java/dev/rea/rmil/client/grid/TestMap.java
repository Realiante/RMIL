package dev.rea.rmil.client.grid;

import java.io.Serializable;
import java.util.function.Function;

public class TestMap implements Serializable, Function<Integer, Integer> {
    @Override
    public Integer apply(Integer integer) {
        return integer+1;
    }
}
