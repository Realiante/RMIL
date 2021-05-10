package rea.dev.rmil.remote;

import rea.dev.rmil.remote.items.FunctionPackage;

import java.io.Serializable;
import java.rmi.Remote;

public interface RemoteExecutorContainer extends Remote, Serializable {

    boolean registerFunction(FunctionPackage functionPackage);

    long heartbeat();

}
