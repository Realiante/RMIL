package dev.rea.rmil.client;

import dev.rea.rmil.client.manager.DistributionManagerImpl;

public class RmilClient {

    private static DistributionManagerImpl manager;

    private RmilClient() {
        //static class
    }

    public static DistributionManager getManager() {
        return manager;
    }

    public static DistributionManager createLocalOnlyManager() {
        return createManager(new RmilConfig(), DistributionTactic.LOCAL_ONLY);
    }

    public static DistributionManager createManager() {
        return createManager(new RmilConfig(), DistributionTactic.STANDARD);
    }

    public static DistributionManager createManager(RmilConfig config, DistributionTactic tactic) {
        if (manager != null) {
            return manager;
        }
        return new DistributionManagerImpl(config, tactic);
    }

    public static void fullStop() {
        manager.stop();
        manager = null;
    }
}
