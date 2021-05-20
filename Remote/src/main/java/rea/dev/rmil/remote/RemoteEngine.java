package rea.dev.rmil.remote;

import rea.dev.rmil.remote.items.FunctionPackage;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

public interface RemoteEngine extends Remote, Serializable {

    void registerFunction(FunctionPackage functionPackage, boolean askForItems) throws RemoteException;

    boolean removeFunction(UUID functionID) throws RemoteException;

    <R, T> R executeTask(UUID functionID, T argument) throws RemoteException;

    <R, T, A> R executeBiTask(UUID functionID, T argument, A anotherArgument) throws RemoteException;

}
