package dev.rea.rmil.engine.backend;

import rea.dev.rmil.remote.BaseTask;
import rea.dev.rmil.remote.DistBiTask;
import rea.dev.rmil.remote.DistTask;
import rea.dev.rmil.remote.RemoteEngine;
import rea.dev.rmil.remote.items.FunctionPackage;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

final class RmilRemoteEngine implements RemoteEngine {

    private final UUID localIdentifier;
    private final Map<UUID, BaseTask> functionMap = new HashMap<>();

    protected RmilRemoteEngine() {
        localIdentifier = UUID.randomUUID();
    }

    @Override
    public void registerFunction(FunctionPackage functionPackage, boolean askForItems) {
        functionMap.put(functionPackage.getFunctionID(), functionPackage.getFunction());
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
        var rmilRemoteEngine = (RmilRemoteEngine) o;
        return localIdentifier.equals(rmilRemoteEngine.localIdentifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), localIdentifier);
    }

}
