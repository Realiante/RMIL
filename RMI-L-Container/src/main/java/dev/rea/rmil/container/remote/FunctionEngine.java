package dev.rea.rmil.container.remote;

import rea.dev.rmil.remote.DistributedFunction;
import rea.dev.rmil.remote.DistributedItem;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;

public class FunctionEngine extends UnicastRemoteObject implements DistributedFunction {


    public FunctionEngine() throws RemoteException {
        super();
    }

    @Override
    public <T> DistributedItem<T> applyFunction(UUID fID, DistributedItem<T> item) {
        return null;
    }

    @Override
    public <T, R> DistributedItem<T> applyBiFunction(UUID fID, DistributedItem<T> returnTypeItem, DistributedItem<R> modifierItem) {
        return null;
    }
}
