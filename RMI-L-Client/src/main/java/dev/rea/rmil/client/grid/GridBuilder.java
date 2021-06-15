package dev.rea.rmil.client.grid;

import dev.rea.rmil.client.RmilGridManager;

import java.util.HashSet;
import java.util.Set;

public final class GridBuilder {

    private GridBuilder() {
        //static class
    }

    public static RmilGridManager buildGrid() {
        //todo: create a mechanism to get a set of all server addresses
        Set<String> addresses = new HashSet<>();
        int maxLocalTasks = Runtime.getRuntime().availableProcessors();
        return new RmilGridManagerImpl(maxLocalTasks, addresses);
    }


}
