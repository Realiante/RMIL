package dev.rea.rmil.client.manager;

import dev.rea.rmil.client.RmilGridManager;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

public final class GridBuilder {

    private GridBuilder() {
        //static class
    }

    public static RmilGridManager buildGrid() {
        //todo: create a mechanism to get a set of all server addresses and remove this test set
        Set<ServerAddress> addresses = new HashSet<>();
        try {
            addresses.add(new ServerAddress(InetAddress.getLocalHost().getHostAddress(), 1099));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        int maxLocalTasks = Runtime.getRuntime().availableProcessors();
        return new RmilGridManagerImpl(maxLocalTasks, addresses);
    }


}
