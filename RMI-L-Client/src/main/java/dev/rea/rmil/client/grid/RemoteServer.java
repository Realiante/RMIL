package dev.rea.rmil.client.grid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rea.dev.rmil.remote.RemoteEngine;
import rea.dev.rmil.remote.ServerConfiguration;
import rea.dev.rmil.remote.ServerConfiguration.Priority;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.*;

class RemoteServer implements RemoteThread {

    private static final Logger logger = LoggerFactory.getLogger(RemoteServer.class);
    private final String address;
    private final Set<RemoteServerThread> additionalThreads;
    private UUID serverID;
    private RemoteEngine executorContainer;
    private Priority priority;

    protected RemoteServer(String address) throws RemoteException, NotBoundException {
        this.address = address;
        this.additionalThreads = new HashSet<>();
        loadContainer(address);
    }

    public static Optional<RemoteServer> load(String address) {
        try {
            var server = new RemoteServer(address);
            return Optional.of(server);
        } catch (RemoteException | NotBoundException exception) {
            logger.debug("Failed to locate server " + address, exception);
            logger.info(String.format("Server %s is not bound or access is not properly configured", address));
        }
        return Optional.empty();
    }

    protected ServerConfiguration loadConfiguration() throws RemoteException {
        return executorContainer.getConfiguration();
    }

    protected void setConfiguration(ServerConfiguration config) {
        this.serverID = config.getServerID();
        int maxThreads = config.getMaxThreads();
        this.priority = config.getPriority();

        for (var i = 1; i < maxThreads; i++) {
            this.additionalThreads.add(new RemoteServerThread(this, i));
        }
    }

    protected void loadContainer(String address) throws RemoteException, NotBoundException {
        var registry = LocateRegistry.getRegistry(address);
        this.executorContainer = (RemoteEngine) registry.lookup("engine");
        setConfiguration(loadConfiguration());
    }

    public Set<RemoteServerThread> getAdditionalThreads() {
        return additionalThreads;
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public UUID getID() {
        return serverID;
    }

    @Override
    public RemoteEngine getExecutorContainer() {
        return executorContainer;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RemoteServer)) return false;
        RemoteServer that = (RemoteServer) o;
        return getAddress().equals(that.getAddress());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAddress());
    }
}
