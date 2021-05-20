package dev.rea.rmil.engine;

import java.rmi.RemoteException;

public interface EngineManager {

    void setMaxThreads(int maxThreads);

    String getAddress();

    void unbind() throws RemoteException;

}
