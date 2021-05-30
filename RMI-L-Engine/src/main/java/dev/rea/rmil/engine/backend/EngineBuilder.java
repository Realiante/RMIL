package dev.rea.rmil.engine.backend;

import dev.rea.rmil.engine.EngineBinding;
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

    public static final String DEFAULT_NAME = "engine";
    private static final Logger logger = LoggerFactory.getLogger(EngineBuilder.class);
    private static Registry registry;
    private static EngineBinding engineBinding;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(EngineBuilder::release));
    }

    private EngineBuilder() {
        //static builder class
    }

    public static synchronized EngineBinding buildOrGet() throws RemoteException, AlreadyBoundException {
        if (engineBinding != null) {
            return engineBinding;
        }
        if (registry == null) {
            registry = LocateRegistry.createRegistry(1099);
        }

        var remoteEngine = new RmilRemoteEngine();
        var engineStub = (RemoteEngine) UnicastRemoteObject.exportObject(remoteEngine, 1099);

        registry.bind(DEFAULT_NAME, engineStub);
        return  new EngineBindingImpl(registry, engineStub);
    }

    public static synchronized boolean release() {
        try {
            if (engineBinding != null) {
                engineBinding.unbind();
                engineBinding = null;
            } else {
                var registry = LocateRegistry.getRegistry(null);
                registry.unbind(DEFAULT_NAME);
            }
            return true;
        } catch (RemoteException | NotBoundException e) {
            logger.error("Critical error while attempting to unbind the engine, is the engine bound?", e);
        }
        return false;
    }

}
