package dev.rea.rmil.engine.backend;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rea.dev.rmil.remote.ArgumentPackage;
import rea.dev.rmil.remote.DistributedMethod.DistCheck;
import rea.dev.rmil.remote.FunctionPackage;

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
        DistCheck<Integer, Boolean> predicateFunction = predicate::test;
        rmilRemoteEngine.registerFunction(new FunctionPackage(predicateUUID, predicateFunction));

    }

    @Test
    void testPredicateExecution() {
        Assertions.assertDoesNotThrow(() -> {
            var result1 = rmilRemoteEngine.checkAndReturnValue(predicateUUID, new ArgumentPackage<>(2, UUID.randomUUID()));
            var result2 = rmilRemoteEngine.checkAndReturnValue(predicateUUID, new ArgumentPackage<>(2, UUID.randomUUID()));

            Assertions.assertTrue(result1 instanceof Boolean);
            Assertions.assertTrue((Boolean) result1);

            Assertions.assertTrue(result2 instanceof Boolean);
            Assertions.assertFalse((Boolean) result2);
        });
    }


}