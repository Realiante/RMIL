package dev.rea.rmil.engine.backend;

import dev.rea.rmil.engine.EngineManager;
import dev.rea.rmil.engine.EngineRegistration;

final class EngineManagerImpl implements EngineManager {

    private final RmilRemoteEngine engine;
    private final EngineRegistration registration;

    protected EngineManagerImpl(RmilRemoteEngine engine, EngineRegistration registration) {
        this.engine = engine;
        this.registration = registration;
    }

    @Override
    public EngineRegistration getRegistration() {
        return registration;
    }


}
