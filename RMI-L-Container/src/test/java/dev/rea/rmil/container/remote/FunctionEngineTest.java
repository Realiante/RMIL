package dev.rea.rmil.container.remote;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rea.dev.rmil.remote.DistFunction;
import rea.dev.rmil.remote.items.FunctionPackage;

import java.rmi.RemoteException;
import java.util.UUID;
import java.util.function.Predicate;

class FunctionEngineTest {

    private static FunctionEngine engine;
    private static UUID predicateUUID;

    @BeforeAll
    static void setup() throws RemoteException {
        engine = new FunctionEngine(2, UUID.randomUUID());

        predicateUUID = UUID.randomUUID();
        Predicate<Integer> predicate = integer -> integer > 1;
        DistFunction<Integer, Boolean> predicateFunction = predicate::test;
        engine.registerFunction(new FunctionPackage(predicateUUID, predicateFunction), false);

    }

    @Test
    void testPredicateExecution() {
        var result1 = engine.executeTask(predicateUUID, 2);
        var result2 = engine.executeTask(predicateUUID, 0);

        Assertions.assertTrue(result1 instanceof Boolean);
        Assertions.assertTrue((Boolean) result1);

        Assertions.assertTrue(result2 instanceof Boolean);
        Assertions.assertFalse((Boolean) result2);
    }


}