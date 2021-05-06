package dev.rea.rmil.container.remote;

import rea.dev.rmil.remote.DistBiFunction;
import rea.dev.rmil.remote.DistFunction;
import rea.dev.rmil.remote.RemoteExecutor;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;

public class FunctionEngine extends UnicastRemoteObject implements RemoteExecutor {


    public FunctionEngine() throws RemoteException {
        super();
    }

    @Override
    public <R, T> R executeFunction(DistFunction<T, R> function, T argument) {
        return null;
    }

    @Override
    public <R, T> R executeFunction(UUID functionID, T argument) {
        return null;
    }

    @Override
    public <R, T> R executeFunction(DistFunction<T, R> function, UUID argumentID) {
        return null;
    }

    @Override
    public <R, T, A> R executeBiFunction(DistBiFunction<T, A, R> function, T argument, A anotherArgument) {
        return null;
    }

    @Override
    public <R, T, A> R executeBiFunction(DistBiFunction<T, A, R> function, UUID argumentID, A anotherArgument) {
        return null;
    }

    @Override
    public <R, T, A> R executeBiFunction(UUID functionID, T argument, A anotherArgument) {
        return null;
    }
}
