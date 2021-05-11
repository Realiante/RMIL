package dev.rea.rmil.client.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rea.dev.rmil.remote.RemoteExecutorContainer;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Objects;
import java.util.UUID;

public class RemoteServer {

    private static final Logger logger = LoggerFactory.getLogger(RemoteServer.class);

    public final UUID serverID;
    private final String address;
    private final int port;

    private boolean loaded;
    private final int maxTasks;
    private Registry registry;
    private RemoteExecutorContainer executorContainer;

    public RemoteServer(String address, int port) {
        this.address = address;
        this.port = port;
        this.maxTasks = 0;
        this.serverID = UUID.fromString(String.format("%s:%s", address, port));

        try {
            this.registry = LocateRegistry.getRegistry(address, port);
            this.executorContainer = (RemoteExecutorContainer) registry.lookup("engine");
        } catch (RemoteException | NotBoundException exception) {
            var serverName = String.format("%s:%s", address, port);
            logger.debug("Failed to locate server " + serverName, exception);
            logger.info(String.format("Server %s is not bound or RemoteException occurred", serverName));
        }
    }

    public String getAddress() {
        return address;
    }

    public int getMaxTasks() {
        return maxTasks;
    }

    public boolean isLoaded() {
        return loaded;
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
}
