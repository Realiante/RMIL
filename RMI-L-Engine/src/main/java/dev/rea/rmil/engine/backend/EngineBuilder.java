package dev.rea.rmil.engine.backend;

import dev.rea.rmil.engine.EngineManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rea.dev.rmil.remote.RemoteEngine;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public final class EngineBuilder {

    private static final Logger logger = LoggerFactory.getLogger(EngineBuilder.class);
    private static Registry registry;
    private static EngineManager engineManager;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(EngineBuilder::release));
    }

    private EngineBuilder() {
        //static builder class
    }

    public static synchronized EngineManager buildOrGet() throws RemoteException, AlreadyBoundException {
        if (engineManager != null) {
            return engineManager;
        }
        if (registry == null) {
            registry = LocateRegistry.createRegistry(1099);
        }

        var remoteEngine = new RmilRemoteEngine();
        var engineStub = (RemoteEngine) UnicastRemoteObject.exportObject(remoteEngine, 1099);

        registry.bind(EngineManager.DEFAULT_NAME, engineStub);
        var registration = new EngineRegistrationImpl(registry, engineStub);
        var manager = new EngineManagerImpl(remoteEngine, registration);

        engineManager = manager;
        return manager;
    }

    public static synchronized boolean release() {
        try {
            if (engineManager != null) {
                engineManager.getRegistration().unbind();
                engineManager = null;
            } else {
                var registry = LocateRegistry.getRegistry(null);
                registry.unbind(EngineManager.DEFAULT_NAME);
            }
            return true;
        } catch (RemoteException | NotBoundException e) {
            logger.error("Critical error while attempting to unbind the engine, is the engine bound?", e);
        }
        return false;
    }

}
