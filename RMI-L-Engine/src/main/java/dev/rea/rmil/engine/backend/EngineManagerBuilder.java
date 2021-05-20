package dev.rea.rmil.engine.backend;

import dev.rea.rmil.engine.EngineManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rea.dev.rmil.remote.RemoteEngine;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public final class EngineManagerBuilder {

    private static final Logger logger = LoggerFactory.getLogger(EngineManagerBuilder.class);
    private static Registry localRegistry;

    private EngineManagerBuilder() {
        //static builder class
    }

    public static EngineManager build(int port) throws RemoteException, UnknownHostException, AlreadyBoundException {
        if (localRegistry == null) {
            localRegistry = LocateRegistry.getRegistry(null);
        }
        var host = String.format("%s:%s", InetAddress.getLocalHost().getHostAddress(), port);

        var remoteEngine = new RmilRemoteEngine(host);
        var engineStub = (RemoteEngine) UnicastRemoteObject.exportObject(remoteEngine, port);

        localRegistry.bind(host, engineStub);
        var registration = new EngineRegistration(host);

        logger.info(String.format("Successfully created a new remote engine listening on %s", host));
        return new EngineManagerImpl(remoteEngine, registration);
    }

    public static class EngineRegistration {
        private final String host;

        public EngineRegistration(String host) {
            this.host = host;
        }

        public final void unbind() throws NotBoundException, RemoteException {
            localRegistry.unbind(host);
        }
    }
}
