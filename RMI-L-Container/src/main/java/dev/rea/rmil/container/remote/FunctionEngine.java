package dev.rea.rmil.container.remote;


import rea.dev.rmil.remote.BaseFunction;
import rea.dev.rmil.remote.DistBiFunction;
import rea.dev.rmil.remote.DistFunction;
import rea.dev.rmil.remote.RemoteExecutorContainer;
import rea.dev.rmil.remote.items.FunctionPackage;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class FunctionEngine extends UnicastRemoteObject implements RemoteExecutorContainer {

    private final UUID localID;
    private final Map<UUID, BaseFunction> functionMap = new HashMap<>();

    public FunctionEngine(int maxThreads, UUID localID) throws RemoteException {
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
        DistFunction<T, R> function = (DistFunction<T, R>) functionMap.get(functionID);
        return function.execute(argument);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R, T, A> R executeBiTask(UUID functionID, T argument, A anotherArgument) {
        DistBiFunction<T, A, R> function = (DistBiFunction<T, A, R>) functionMap.get(functionID);
        return function.execute(argument, anotherArgument);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        FunctionEngine engine = (FunctionEngine) o;
        return localID.equals(engine.localID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), localID);
    }
}
