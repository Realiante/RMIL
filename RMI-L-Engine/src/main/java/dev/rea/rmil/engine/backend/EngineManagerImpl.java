package dev.rea.rmil.engine.backend;

import dev.rea.rmil.engine.EngineManager;
import dev.rea.rmil.engine.RmilEngine;
import dev.rea.rmil.engine.backend.EngineManagerBuilder.EngineRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;

final class EngineManagerImpl implements EngineManager {

    private static final Logger logger = LoggerFactory.getLogger(EngineManagerImpl.class);

    private final RmilEngine engine;
    private final EngineRegistration registration;
    private boolean unbound = false;

    protected EngineManagerImpl(RmilEngine engine, EngineRegistration registration) {
        this.engine = engine;
        this.registration = registration;
    }

    @Override
    public void setMaxThreads(int maxThreads) {
        engine.setMaxThreads(maxThreads);
    }

    @Override
    public String getAddress() {
        return engine.getAddress();
    }

    @Override
    public void unbind() throws RemoteException {
        if (!unbound) {
            try {
                registration.unbind();
            } catch (NotBoundException notBoundException) {
                logger.error("Unexpected exception occurred!", notBoundException);
            }
            unbound = true;
        }
    }

}
