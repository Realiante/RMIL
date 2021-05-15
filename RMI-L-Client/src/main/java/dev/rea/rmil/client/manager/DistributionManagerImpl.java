package dev.rea.rmil.client.manager;

import dev.rea.rmil.client.DistributionManager;
import dev.rea.rmil.client.DistributionQueue;
import dev.rea.rmil.client.DistributionTactic;
import dev.rea.rmil.client.RmilConfig;
import rea.dev.rmil.remote.BaseFunction;
import rea.dev.rmil.remote.DistFunction;
import rea.dev.rmil.remote.items.DistributedTransfer;
import rea.dev.rmil.remote.items.FunctionPackage;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class DistributionManagerImpl implements DistributionManager {

    private final DistributionTactic distTactic;
    private final RmilConfig config;
    private final LocalTaskCounter localCounter;
    private final Map<UUID, DistributionQueue<?>> functionQueueMap = new HashMap<>();
    private final Map<UUID, RemoteServer> serverMap = new HashMap<>();
    private final Set<RemoteServer> availableServers = new HashSet<>();
    private final Set<UUID> unavailableServers = new HashSet<>();
    private Set<UUID> taskAvailableServers;

    public DistributionManagerImpl(String address, int port, RmilConfig config, DistributionTactic tactic) {
        this.distTactic = tactic;
        this.config = config;
        this.localCounter = new LocalTaskCounter(0);

        if (distTactic != DistributionTactic.LOCAL_ONLY) {
            availableServers.addAll(getAvailableServers());
            taskAvailableServers = new HashSet<>(){
                @Override
                public boolean add(UUID uuid) {
                    //todo: Listen to this
                    return super.add(uuid);
                }
            };
        }
    }

    protected Set<RemoteServer> getAvailableServers() {
        //todo: create a mechanism to get a set of all server addresses and remove this test set
        Set<ServerAddress> addresses = Set.of(new ServerAddress("localhost", 51199));
        return addresses.stream().map(servAddr -> {
            var server = new RemoteServer(servAddr.getAddress(), servAddr.getPort());
            serverMap.put(server.serverID, server);
            return server;
        }).filter(server -> {
            if (!server.isLoaded()) {
                unavailableServers.add(server.serverID);
                return false;
            }
            return true;
        }).collect(Collectors.toSet());
        //todo: create a mechanism of determining what servers are available after initial loading attempt
        //todo: create a mechanism that pings unavailable servers occasionally
    }

    @SuppressWarnings("java:S128") //suppressing sonarlint warning
    public <T> Predicate<? super T> filterTask(Predicate<? super T> predicate) {
        //todo: add mechanism of recognizing existing functions
        var functionID = UUID.randomUUID();
        DistFunction<? super T, Boolean> ttDistPredicate = predicate::test;
        sendRemoteFunctionTask(functionID, ttDistPredicate);

        return (Predicate<T>) argument -> {
            switch (distTactic) {
                case STANDARD:
                    if (localCounter.get() <= config.getMaxLocalTasks()) {
                        localCounter.incrementAndGet();
                        var result = ttDistPredicate.execute(argument);
                        localCounter.decrementAndGet();
                        return result;
                    }
                    //this non terminated switch case is intentional.
                    //if you need to start a new task over local limit, start it as remote instead.
                case REMOTE_ONLY:
                    return executeFunctionTask(functionID, argument);
                case LOCAL_ONLY:
            }
            return ttDistPredicate.execute(argument);
        };
    }

    protected synchronized <T> void registerQueue(UUID functionID, T argument) {
        final DistributionQueue<T> queue = new DistributionQueue<>();
        functionQueueMap.put(functionID, queue);
        queue.put(new DistributedTransfer<>(argument));
    }

    /**
     * Gets next available server ready for task execution.
     * If no server is available will return an empty optional instead.
     *
     * @return Optional server
     */
    protected synchronized Optional<UUID> getNextTaskAvailableServer() {
        var serverOpt = taskAvailableServers.stream().findFirst();
        serverOpt.ifPresent(taskAvailableServers::remove);
        return serverOpt;
    }

    protected void sendRemoteFunctionTask(UUID functionID, BaseFunction funcTask) {
        availableServers.forEach(remoteServer -> remoteServer.getExecutorContainer()
                .registerFunction(new FunctionPackage(functionID, funcTask), false));
    }

    protected <R, T> R executeFunctionTask(UUID functionID, T argument) {
        var serverID = getNextTaskAvailableServer();
        AtomicReference<R> returnValueRef = new AtomicReference<>();
        serverID.ifPresentOrElse((uuid -> {
            var taskServer = serverMap.get(uuid);
            taskAvailableServers.remove(uuid);
            returnValueRef.set(taskServer.getExecutorContainer().executeTask(functionID, argument));
            taskAvailableServers.add(uuid);
        }), (() -> returnValueRef.set(executeTaskServerUnavailable(functionID, argument))));
        return returnValueRef.get();
    }

    protected <R, T> R executeTaskServerUnavailable(UUID functionID, T argument) {
        //todo: wait until a server becomes available or local becomes available
        throw new UnsupportedOperationException();
    }

    protected <R, T, A> R executeBiFunctionTask(UUID functionID, T argument, A anotherArg) {
        //todo: wait until a server becomes available
        var serverID = getNextTaskAvailableServer().orElseThrow();
        var taskServer = serverMap.get(serverID);
        return taskServer.getExecutorContainer().executeBiTask(functionID, argument, anotherArg);
    }

    public void stop() {
        //todo: release all resources
    }


}
