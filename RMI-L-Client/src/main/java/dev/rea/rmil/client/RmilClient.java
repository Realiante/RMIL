package dev.rea.rmil.client;

import dev.rea.rmil.client.manager.RmilGridManagerImpl;

public class RmilClient {

    private static RmilGridManagerImpl manager;

    private RmilClient() {
        //static class
    }

    public static RmilGridManager getManager() {
        return manager;
    }

    public static RmilGridManager createLocalOnlyManager() {
        return createManager(new RmilConfig(), DistributionTactic.LOCAL_ONLY);
    }

    public static RmilGridManager createManager() {
        return createManager(new RmilConfig(), DistributionTactic.STANDARD);
    }

    public static RmilGridManager createManager(RmilConfig config, DistributionTactic tactic) {
        if (manager != null) {
            return manager;
        }
        return new RmilGridManagerImpl(config, tactic);
    }

    public static void fullStop() {
        manager.stop();
        manager = null;
    }
}
