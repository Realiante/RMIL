package dev.rea.rmil.engine;

import rea.dev.rmil.remote.RemoteEngine;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public interface EngineRegistration {

    RemoteEngine getStub();

    void unbind() throws NotBoundException, RemoteException;
}
