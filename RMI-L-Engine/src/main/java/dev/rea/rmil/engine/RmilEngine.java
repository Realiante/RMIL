package dev.rea.rmil.engine;

import rea.dev.rmil.remote.BaseTask;
import rea.dev.rmil.remote.DistBiTask;
import rea.dev.rmil.remote.DistTask;
import rea.dev.rmil.remote.RemoteExecutorContainer;
import rea.dev.rmil.remote.items.FunctionPackage;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class RmilEngine extends UnicastRemoteObject implements RemoteExecutorContainer {

    private final UUID localID;
    private final Map<UUID, BaseTask> functionMap = new HashMap<>();

    public RmilEngine(UUID localID) throws RemoteException {
        super();
        this.localID = localID;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RmilEngine rmilEngine = (RmilEngine) o;
        return localID.equals(rmilEngine.localID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), localID);
    }
}
