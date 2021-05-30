package dev.rea.rmil.engine.backend;

import dev.rea.rmil.engine.EngineBinding;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

class EngineBindingTest {

    static Registry registry;
    static EngineBinding manager;

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
        Assertions.assertTrue(EngineBuilder.release());
        Assertions.assertThrows(NotBoundException.class, () -> registry.lookup("engine"));
    }

    @Test
    void lookupTest() {
        Assertions.assertDoesNotThrow(() ->
                registry.lookup("engine")
        );
    }

}
