package dev.rea.rmil.engine;

import dev.rea.rmil.engine.backend.EngineManagerBuilder;

import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class EngineFactory {

    private static final Map<Integer, EngineManager> managerMap = new HashMap<>();

    private EngineFactory() {
        //static factory class
    }

    public static EngineManager initializeAndGet(int port)
            throws UnknownHostException, AlreadyBoundException, RemoteException {
        if (!managerMap.containsKey(port)) {
            managerMap.put(port, EngineManagerBuilder.build(port));
        }
        return get(port).orElseThrow();
    }

    public static Optional<EngineManager> get(int port) {
        return Optional.ofNullable(managerMap.get(port));
    }

}
