package dev.rea.rmil.client.manager;

import dev.rea.rmil.client.DistributionManager;
import dev.rea.rmil.client.DistributionTactic;
import dev.rea.rmil.client.RmilConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rea.dev.rmil.remote.BaseTask;
import rea.dev.rmil.remote.DistTask;
import rea.dev.rmil.remote.items.FunctionPackage;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class DistributionManagerImpl implements DistributionManager {

    private static final Logger logger = LoggerFactory.getLogger(DistributionManagerImpl.class);

    private final Map<UUID, BaseTask> functionMap = new ConcurrentHashMap<>();
    private final Map<UUID, RemoteServer> serverMap = new ConcurrentHashMap<>();
    private final Set<RemoteServer> availableServers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<UUID> unavailableServers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Deque<ServerAvailabilityListener> listenerQueue = new ConcurrentLinkedDeque<>();

    private final DistributionTactic distTactic;
    private final RmilConfig config;
    private final AtomicInteger localCounter;

    private Set<UUID> taskAvailableServers;

    public DistributionManagerImpl(RmilConfig config, DistributionTactic tactic) {
        this.distTactic = tactic;
        this.config = config;
        this.localCounter = new AtomicInteger(0);

        if (distTactic != DistributionTactic.LOCAL_ONLY) {
            availableServers.addAll(getAvailableServers());
            taskAvailableServers = new HashSet<>() {
                @Override
                public boolean add(UUID uuid) {
                    if (!listenerQueue.isEmpty()) {
                        var listener = listenerQueue.pop();
                        listener.onAvailable(uuid);
                        return true;
                    }
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

    public <T> Predicate<? super T> filterTask(Predicate<? super T> predicate) {
        //todo: add mechanism of recognizing existing functions
        DistTask<? super T, Boolean> ttDistPredicate = predicate::test;
        var functionID = registerFunctionTask(ttDistPredicate);

        return (Predicate<T>) argument -> {
            if (distTactic == DistributionTactic.STANDARD) {
                if (localCounter.incrementAndGet() < config.getMaxLocalTasks()) {
                    var result = ttDistPredicate.execute(argument);
                    executeWaitingFunctions(null);
                    localCounter.decrementAndGet();
                    return result;
                }
                localCounter.decrementAndGet();
                return executeFunctionTask(new FunctionTask<T, Boolean>(functionID, argument));
            }
            return ttDistPredicate.execute(argument);
        };
    }

    private void executeWaitingFunctions(UUID serverID) {
        while (!listenerQueue.isEmpty()) {
            try {
                listenerQueue.pop().onAvailable(serverID);
            } catch (NoSuchElementException ignored) {
                //ignored exception
            }
        }
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

    protected UUID registerFunctionTask(BaseTask baseTask) {
        var functionID = UUID.randomUUID();
        functionMap.put(functionID, baseTask);
        availableServers.removeAll(sendFunctionAndReturnUnavailable(new FunctionPackage(functionID, baseTask)));
        return functionID;
    }

    protected Set<RemoteServer> sendFunctionAndReturnUnavailable(FunctionPackage functionPackage) {
        Set<RemoteServer> upForRemoval = new HashSet<>();
        availableServers.forEach(remoteServer -> {
            try {
                remoteServer.getExecutorContainer()
                        .registerFunction(functionPackage, false);
            } catch (RemoteException e) {
                logger.error("Remote exception while attempting to send a function to " + remoteServer.getAddress(), e);
                var sID = remoteServer.serverID;
                unavailableServers.add(sID);
                upForRemoval.add(remoteServer);
            }
        });
        return upForRemoval;
    }

    protected <T, R> R executeFunctionTask(FunctionTask<T, R> functionTask) {
        var serverID = getNextTaskAvailableServer();
        serverID.ifPresentOrElse((uuid -> {
                    if (functionTask.removeListener()) {
                        try {
                            functionTask.returnValueRef.set(executeTaskOnServer(uuid,
                                    functionTask.functionID, functionTask.argument));
                        } catch (RemoteException e) {
                            logger.error("Remote exception while attempting to execute a task on "
                                    + serverMap.get(uuid).getAddress(), e);
                            //todo: add retry mechanism
                            availableServers.remove(serverMap.get(uuid));
                            functionTask.listen();
                            functionTask.returnValueRef.set(executeFunctionTask(functionTask));
                        }
                    }
                }),
                (() -> functionTask.returnValueRef.set(executeTaskServerUnavailable(functionTask))));
        return functionTask.returnValueRef.get();
    }

    @SuppressWarnings("java:S6212")
    //suppressing sonarlint, since replacing line 2 R with var would cause executeTask to return an Object instead of R
    protected <T, R> R executeTaskOnServer(UUID serverID, UUID functionID, T argument) throws RemoteException {
        var taskServer = serverMap.get(serverID);
        R returnValue = taskServer.getExecutorContainer().executeTask(functionID, argument);
        executeWaitingFunctions(serverID);
        taskAvailableServers.add(serverID);
        return returnValue;
    }

    @SuppressWarnings({"java:S2222"})
    //suppressing sonarlint, since if the condition listener is not called, the system has already entered deadlock
    //which by design will not occur.
    protected <T, R> R executeTaskServerUnavailable(FunctionTask<T, R> functionTask) {
        try {
            //Awaits listener execution
            if (localCounter.get() <= config.getMaxLocalTasks()) {
                localCounter.incrementAndGet();
                executeWaitingFunctions(null);
                localCounter.decrementAndGet();
            }
            functionTask.countDown.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        return functionTask.returnValueRef.get();
    }

    public void stop() {
        //todo: release all resources
    }

    @FunctionalInterface
    private interface ServerAvailabilityListener {

        void onAvailable(UUID server);

    }

    protected class FunctionTask<T, R> {
        public final UUID functionID;
        public final AtomicReference<R> returnValueRef;
        public final CountDownLatch countDown;
        private final ServerAvailabilityListener listener;
        T argument;

        @SuppressWarnings("unchecked")
        public FunctionTask(UUID functionID, T argument) {
            this.functionID = functionID;
            this.argument = argument;
            this.countDown = new CountDownLatch(1);
            this.returnValueRef = new AtomicReference<>();
            this.listener = server -> {
                try {
                    if (server == null) {
                        DistTask<T, R> function = (DistTask<T, R>) functionMap.get(functionID);
                        returnValueRef.set(function.execute(argument));
                        localCounter.decrementAndGet();
                    } else {
                        returnValueRef.set(executeTaskOnServer(server, functionID, argument));
                    }
                } catch (Exception e) {
                    logger.error("Critical exception while attempting execute task for a waiting thread", e);
                } finally {
                    countDown.countDown();
                }
            };
            listenerQueue.add(listener);
        }

        public boolean removeListener() {
            return listenerQueue.remove(listener);
        }

        public void listen() {
            listenerQueue.add(listener);
        }
    }
}
