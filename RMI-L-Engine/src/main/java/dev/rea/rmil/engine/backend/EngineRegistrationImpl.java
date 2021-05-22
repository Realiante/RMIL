package dev.rea.rmil.engine.backend;

import dev.rea.rmil.engine.EngineManager;
import dev.rea.rmil.engine.EngineRegistration;
import rea.dev.rmil.remote.RemoteEngine;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;

final class EngineRegistrationImpl implements EngineRegistration {
    private final Registry registry;
    private final RemoteEngine stub;
    private boolean bound;

    public EngineRegistrationImpl(Registry registry, RemoteEngine stub) {

        this.registry = registry;
        this.stub = stub;
        this.bound = true;
    }

    @Override
    public RemoteEngine getStub() {
        return stub;
    }

    @Override
    public final void unbind() throws NotBoundException, RemoteException {
        if (bound) {
            registry.unbind(EngineManager.DEFAULT_NAME);
            bound = false;
        }
    }
}
