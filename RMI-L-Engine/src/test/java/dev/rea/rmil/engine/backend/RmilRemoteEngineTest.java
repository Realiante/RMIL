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
    private static UUID existingIntegerID;

    @BeforeAll
    static void setup() {
        rmilRemoteEngine = new RmilRemoteEngine(EngineBuilder.getDefaultConfiguration());

        predicateUUID = UUID.randomUUID();
        Predicate<Integer> predicate = integer -> integer > 1;
        DistCheck<Integer, Boolean> predicateFunction = predicate::test;
        rmilRemoteEngine.registerFunction(new FunctionPackage(predicateUUID, predicateFunction));
        existingIntegerID = UUID.randomUUID();
        rmilRemoteEngine.putArgumentPackage(new ArgumentPackage<>(15, existingIntegerID));
    }

    @Test
    void predicateExecutionPackagesTest() {
        Assertions.assertDoesNotThrow(() -> {
            var result1 = rmilRemoteEngine.checkAndReturnValue(predicateUUID, new ArgumentPackage<>(2, UUID.randomUUID()));
            var result2 = rmilRemoteEngine.checkAndReturnValue(predicateUUID, new ArgumentPackage<>(0, UUID.randomUUID()));

            Assertions.assertTrue(result1 instanceof Boolean);
            Assertions.assertTrue((Boolean) result1);

            Assertions.assertTrue(result2 instanceof Boolean);
            Assertions.assertFalse((Boolean) result2);
        });
    }

    @Test
    void predicateExecutionExistingTest() {
        Assertions.assertDoesNotThrow(() -> {
            var result = rmilRemoteEngine.checkAndReturnValue(predicateUUID, existingIntegerID);
            Assertions.assertTrue(result instanceof Boolean);
            Assertions.assertTrue((Boolean) result);
        });
    }

    @SuppressWarnings("java:S5778") //only the random function id can cause IllegalArgumentException
    @Test
    void noFunctionTest() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> rmilRemoteEngine.checkAndReturnValue(UUID.randomUUID(), existingIntegerID));
    }

    @SuppressWarnings("java:S5778") //only the random item id can cause IllegalArgumentException
    @Test
    void noItemTest() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> rmilRemoteEngine.getItem(UUID.randomUUID()));
    }

}