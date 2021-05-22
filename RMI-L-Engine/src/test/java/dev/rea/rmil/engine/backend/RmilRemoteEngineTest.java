package dev.rea.rmil.engine.backend;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rea.dev.rmil.remote.DistTask;
import rea.dev.rmil.remote.items.FunctionPackage;

import java.util.UUID;
import java.util.function.Predicate;

class RmilRemoteEngineTest {

    private static RmilRemoteEngine rmilRemoteEngine;
    private static UUID predicateUUID;

    @BeforeAll
    static void setup() {
        rmilRemoteEngine = new RmilRemoteEngine();

        predicateUUID = UUID.randomUUID();
        Predicate<Integer> predicate = integer -> integer > 1;
        DistTask<Integer, Boolean> predicateFunction = predicate::test;
        rmilRemoteEngine.registerFunction(new FunctionPackage(predicateUUID, predicateFunction), false);

    }

    @Test
    void testPredicateExecution() {
        var result1 = rmilRemoteEngine.executeTask(predicateUUID, 2);
        var result2 = rmilRemoteEngine.executeTask(predicateUUID, 0);

        Assertions.assertTrue(result1 instanceof Boolean);
        Assertions.assertTrue((Boolean) result1);

        Assertions.assertTrue(result2 instanceof Boolean);
        Assertions.assertFalse((Boolean) result2);
    }


}