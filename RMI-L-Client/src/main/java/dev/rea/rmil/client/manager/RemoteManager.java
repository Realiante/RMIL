package dev.rea.rmil.client.manager;

import rea.dev.rmil.remote.DistBiFunction;
import rea.dev.rmil.remote.DistFunction;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class RemoteManager {

    private final Map<UUID, RemoteServer> serverMap = new HashMap<>();
    private final Set<UUID> availableServers = Set.of();
    private final Set<UUID> unavailableServers = Set.of();


    public RemoteManager() {
        //todo: create a mechanism to get a map of all servers.
        //todo: create a mechanism of determining what servers are available
        //todo: create a mechanism that pings unavailable servers
    }

    //todo: sends function to all available servers, queuing it up. When server is ready, it will ask for an item.
    //todo: actually implement that?
    protected <R, T> boolean sendRemoteFunctionTask(DistFunction<T, R> funcTask) {
        availableServers.stream().parallel()
                .forEach(serverID -> {
                    var server = serverMap.get(serverID);
                    var address = server.getAddress();
                    //todo: send function
                });
        throw new UnsupportedOperationException();
    }

    protected <R, T, A> R sendRemoteBiFunctionTask(DistBiFunction<T, A, R> biFuncTask) {
        //todo: implement bi function execution
        throw new UnsupportedOperationException();
    }
}
