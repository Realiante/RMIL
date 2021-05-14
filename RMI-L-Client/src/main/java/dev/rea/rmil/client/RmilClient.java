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

    public static DistributionManager createManager(String address) {
        return createManager(address, 51199, new RmilConfig(), DistributionTactic.STANDARD);
    }

    public static DistributionManager createManager(String address, int port, RmilConfig config, DistributionTactic tactic) {
        if (manager != null) {
            return manager;
        }
        return new DistributionManagerImpl(address, port, config, tactic);
    }

    public static void fullStop() {
        manager.stop();
        manager = null;
    }
}
