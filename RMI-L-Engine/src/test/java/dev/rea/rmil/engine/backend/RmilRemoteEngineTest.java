package dev.rea.rmil.engine.backend;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rea.dev.rmil.remote.DistTask;
import rea.dev.rmil.remote.items.FunctionPackage;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.UUID;
import java.util.function.Predicate;

class RmilRemoteEngineTest {

    private static RmilRemoteEngine rmilRemoteEngine;
    private static UUID predicateUUID;

    @BeforeAll
    static void setup() throws RemoteException {
        try {
            rmilRemoteEngine = new RmilRemoteEngine(String.format("%s:%s", InetAddress.getLocalHost().getHostAddress(), 0));
        } catch (UnknownHostException e) {
            Assertions.fail(e);
        }

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