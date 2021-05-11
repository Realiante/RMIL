package dev.rea.rmil.container.remote;


import rea.dev.rmil.remote.RemoteExecutorContainer;
import rea.dev.rmil.remote.items.FunctionPackage;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class FunctionEngine extends UnicastRemoteObject implements RemoteExecutorContainer {

    public FunctionEngine(int maxTasks) throws RemoteException {
        super();
    }


    @Override
    public boolean registerFunction(FunctionPackage functionPackage) {
        return false;
    }

    @Override
    public long heartbeat() {
        return 0;
    }
}
