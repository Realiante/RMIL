package dev.rea.rmil.engine.backend;

import dev.rea.rmil.engine.EngineManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import rea.dev.rmil.remote.DistTask;
import rea.dev.rmil.remote.items.FunctionPackage;

import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.UUID;

class EngineManagerTest {

    static Registry registry;
    static EngineManager manager;

    @BeforeEach
    void setup() {
        EngineBuilder.release();
        Assertions.assertDoesNotThrow(() -> {
            manager = EngineBuilder.buildOrGet();
            registry = LocateRegistry.getRegistry(null);
        });
    }

    @RepeatedTest(2)
    void unbindTest() {
        var stub = manager.getRegistration().getStub();
        Assertions.assertDoesNotThrow(() -> {
            var id = UUID.randomUUID();
            stub.registerFunction(new FunctionPackage(id, (DistTask<Integer, Boolean>) (arg) -> arg < 1), false);
            stub.removeFunction(id);
        }, "Fail while accessing stub to confirm binding");
        Assertions.assertTrue(EngineBuilder.release());

        Assertions.assertThrows(NotBoundException.class, () -> registry.lookup("engine"));
    }

}
