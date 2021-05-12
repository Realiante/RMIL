package dev.rea.rmil.container.remote;


import rea.dev.rmil.remote.RemoteExecutorContainer;
import rea.dev.rmil.remote.items.FunctionPackage;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;

public class FunctionEngine extends UnicastRemoteObject implements RemoteExecutorContainer {

    public FunctionEngine(int maxTasks) throws RemoteException {
        super();
    }


    @Override
    public void registerFunction(FunctionPackage functionPackage, boolean askForItems) {

    }

    @Override
    public boolean removeFunction(UUID functionID) {
        return false;
    }

    @Override
    public <R, T> R executeTask(UUID functionID, T argument) {
        return null;
    }

    @Override
    public <R, T, A> R executeBiTask(UUID functionID, T argument, A anotherArgument) {
        return null;
    }

    @Override
    public long heartbeat() {
        return 0;
    }
}
