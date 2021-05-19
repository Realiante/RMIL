package dev.rea.rmil.client.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rea.dev.rmil.remote.RemoteExecutorContainer;

import java.nio.charset.StandardCharsets;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Objects;
import java.util.UUID;

public class RemoteServer {

    public final UUID serverID;
    private final Logger logger = LoggerFactory.getLogger(RemoteServer.class);
    private final String address;
    private final int port;

    private boolean loaded = false;
    private RemoteExecutorContainer executorContainer;

    public RemoteServer(String address, int port) {
        this.address = address;
        this.port = port;
        this.serverID = UUID.nameUUIDFromBytes(String.format("%s:%s", address, port).getBytes(StandardCharsets.UTF_8));
        attemptLoad();
    }

    public boolean attemptLoad() {
        try {
            var registry = LocateRegistry.getRegistry(address, port);
            this.executorContainer = (RemoteExecutorContainer) registry.lookup("engine");
            this.loaded = true;
        } catch (RemoteException | NotBoundException exception) {
            var serverName = String.format("%s:%s", address, port);
            logger.debug("Failed to locate server " + serverName, exception);
            logger.info(String.format("Server %s is not bound or RemoteException occurred", serverName));
        }
        return loaded;
    }

    public String getAddress() {
        return address;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public RemoteExecutorContainer getExecutorContainer() {
        return executorContainer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RemoteServer that = (RemoteServer) o;
        return port == that.port && address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, port);
    }

    public void release() {
        //todo: release all server resources
    }
}
