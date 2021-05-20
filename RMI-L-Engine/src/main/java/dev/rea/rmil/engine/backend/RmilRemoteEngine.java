package dev.rea.rmil.engine.backend;

import dev.rea.rmil.engine.RmilEngine;
import rea.dev.rmil.remote.BaseTask;
import rea.dev.rmil.remote.DistBiTask;
import rea.dev.rmil.remote.DistTask;
import rea.dev.rmil.remote.RemoteEngine;
import rea.dev.rmil.remote.items.FunctionPackage;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class RmilRemoteEngine extends UnicastRemoteObject implements RemoteEngine, RmilEngine {

    private final String hostAddress;
    private final Map<UUID, BaseTask> functionMap = new HashMap<>();

    protected RmilRemoteEngine(String hostAddress) throws RemoteException {
        super();
        this.hostAddress = hostAddress;
    }

    @Override
    public void registerFunction(FunctionPackage functionPackage, boolean askForItems) {
        functionMap.put(functionPackage.getFunctionID(), functionPackage.getFunction());
        if (askForItems) {
            //todo: ask for an item
        }
    }

    @Override
    public boolean removeFunction(UUID functionID) {
        return functionMap.remove(functionID) != null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R, T> R executeTask(UUID functionID, T argument) {
        DistTask<T, R> function = (DistTask<T, R>) functionMap.get(functionID);
        return function.execute(argument);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R, T, A> R executeBiTask(UUID functionID, T argument, A anotherArgument) {
        DistBiTask<T, A, R> function = (DistBiTask<T, A, R>) functionMap.get(functionID);
        return function.execute(argument, anotherArgument);
    }

    @Override
    public String getAddress() {
        return null;
    }

    @Override
    public void setMaxThreads(int maxThreads) {
        //todo
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        var rmilRemoteEngine = (RmilRemoteEngine) o;
        return hostAddress.equals(rmilRemoteEngine.hostAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), hostAddress);
    }

}
