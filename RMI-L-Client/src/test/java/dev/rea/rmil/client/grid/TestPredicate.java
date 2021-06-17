package dev.rea.rmil.client.grid;

import java.io.Serializable;
import java.util.function.Predicate;

public class TestPredicate implements Predicate<Integer>, Serializable {

    private final int condition;

    public TestPredicate(int condition) {
        this.condition = condition;
    }

    @Override
    public boolean test(Integer integer) {
        return testCondition(integer);
    }

    private boolean testCondition(Integer integer) {
        switch (condition) {
            case 0:
                return integer > -1;
            case 1:
                return integer < 50;
            case 2:
                return integer > 0;
            case 3:
                return integer < 10;
        }
        throw new IllegalArgumentException("Unexpected condition");
    }
}
