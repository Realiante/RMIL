package rea.dev.rmil.remote;

import rea.dev.rmil.remote.items.FunctionPackage;

import java.io.Serializable;
import java.rmi.Remote;
import java.util.UUID;

public interface RemoteExecutorContainer extends Remote, Serializable {

    void registerFunction(FunctionPackage functionPackage);

    boolean removeFunction(UUID functionID);

    <R, T> R executeTask(UUID functionID, T argument);

    <R, T, A> R executeBiTask(UUID functionID, T argument, A anotherArgument);

    long heartbeat();

}
