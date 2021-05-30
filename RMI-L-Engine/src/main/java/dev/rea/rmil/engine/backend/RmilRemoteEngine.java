package dev.rea.rmil.engine.backend;

import rea.dev.rmil.remote.*;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

final class RmilRemoteEngine implements RemoteEngine {

    private final UUID localIdentifier;
    private final Map<UUID, DistributedMethod> functionMap = new HashMap<>();

    protected RmilRemoteEngine() {
        localIdentifier = UUID.randomUUID();
    }

    @Override
    public ServerConfiguration getConfiguration() throws RemoteException {
        return null;
    }

    @Override
    public void registerFunction(FunctionPackage functionPackage) {
        functionMap.put(functionPackage.getFunctionID(), functionPackage.getFunction());
    }

    @Override
    public boolean removeFunction(UUID functionID) {
        return functionMap.remove(functionID) != null;
    }

    @Override
    public <R> R getItem(UUID itemID) throws RemoteException {
        //todo
        return null;
    }

    @Override
    public boolean removeItem(UUID itemID) throws RemoteException {
        //todo
        return false;
    }

    @Override
    public <R, T> R checkAndReturnValue(UUID functionID, ArgumentPackage<T> argumentPackage) throws RemoteException {
        //todo
        return null;
    }

    @Override
    public <R> R checkAndReturnValue(UUID functionID, UUID itemID) throws RemoteException {
        //todo
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        var rmilRemoteEngine = (RmilRemoteEngine) o;
        return localIdentifier.equals(rmilRemoteEngine.localIdentifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), localIdentifier);
    }

}
