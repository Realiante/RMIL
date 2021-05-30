package dev.rea.rmil.client.grid;

import dev.rea.rmil.client.DistributedItem;
import dev.rea.rmil.client.RmilGridManager;
import dev.rea.rmil.client.items.DistributedItemFuture;
import dev.rea.rmil.client.items.DistributedItemLocal;
import dev.rea.rmil.client.items.ItemFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rea.dev.rmil.remote.ArgumentPackage;
import rea.dev.rmil.remote.DistributedMethod;
import rea.dev.rmil.remote.DistributedMethod.DistCheck;
import rea.dev.rmil.remote.FunctionPackage;
import rea.dev.rmil.remote.ServerConfiguration;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;


class RmilGridManagerImpl implements RmilGridManager {

    private static final Logger logger = LoggerFactory.getLogger(RmilGridManagerImpl.class);

    private final GridItemFetcher fetcher = new GridItemFetcher();
    private final Map<UUID, DistributedMethod> functionMap = new ConcurrentHashMap<>();
    private final Map<UUID, RemoteServer> serverMap = new ConcurrentHashMap<>();
    private final Deque<ServerAvailabilityListener> listenerQueue = new ConcurrentLinkedDeque<>();
    private final Deque<RemoteThread> availableRemoteThreads;
    private final Deque<RemoteThread> availableLowPriorityThreads;
    private final AtomicInteger localCounter;


    private int maxLocalTasks;

    public RmilGridManagerImpl(int maxLocalTasks, Set<ServerAddress> addresses) {
        this.maxLocalTasks = maxLocalTasks;
        this.localCounter = new AtomicInteger(0);

        this.availableRemoteThreads = new ConcurrentLinkedDeque<>() {
            @Override
            public boolean add(RemoteThread thread) {
                if (!listenerQueue.isEmpty()) {
                    var listener = listenerQueue.pop();
                    listener.onAvailable(thread);
                    return true;
                }
                return super.add(thread);
            }
        };
        this.availableLowPriorityThreads = new ConcurrentLinkedDeque<>();

        mapServers(addresses);
    }

    private void mapServers(Set<ServerAddress> addresses) {
        List<RemoteThread> threads = new ArrayList<>();
        getAvailableServers(addresses).parallelStream()
                .forEach(server -> {
                    serverMap.put(server.getID(), server);
                    threads.add(server);
                    threads.addAll(server.getAdditionalThreads());
                });
        Collections.shuffle(threads);
        availableRemoteThreads.addAll(threads);
    }

    protected Set<RemoteServer> getAvailableServers(Set<ServerAddress> addresses) {
        return addresses.stream().map(servAddr -> RemoteServer.load(servAddr.getAddress()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
        //todo: create a mechanism of determining what servers are available after initial loading attempt
        //todo: create a mechanism that pings unavailable servers occasionally
    }

    @Override
    public void setMaxLocalTasks(int max) {
        this.maxLocalTasks = max;
    }

    @Override
    public <T> Function<T, DistributedItem<T>> mapToGrid() {
        return this::buildDistributedItem;
    }

    @Override
    public <T> Function<DistributedItem<T>, T> mapFromGrid() {
        return DistributedItem::getItem;
    }

    @Override
    @SuppressWarnings("java:S1905") //cast is warranted
    public <T> Predicate<DistributedItem<T>> gridPredicate(Predicate<T> predicate) {
        DistCheck<T, Boolean> method = predicate::test;
        return distItem -> check(method, registerMethod(method), distItem);
    }

    private <T, R> R check(DistCheck<T, R> checkFunc, UUID methodID, DistributedItem<T> distributedItem) {
        if (distributedItem.getNodeID() == null) {
            var item = distributedItem.getItem();
            if (localCounter.incrementAndGet() < maxLocalTasks) {
                var result = checkFunc.check(item);
                fireListeners(null);
                localCounter.decrementAndGet();
                return result;
            }
            localCounter.decrementAndGet();
            return checkItemFromLocal(new CheckFromLocal<>(methodID, item));
        }
        //todo:
        throw new UnsupportedOperationException();
    }

    private void fireListeners(RemoteThread thread) {
        while (!listenerQueue.isEmpty()) {
            try {
                listenerQueue.pop().onAvailable(thread);
            } catch (NoSuchElementException ignored) {
                //ignored exception
            }
        }
    }

    protected UUID registerMethod(DistributedMethod distributedMethod) {
        var methodID = UUID.randomUUID();
        functionMap.put(methodID, distributedMethod);
        removeServers(sendFunctionPackage(new FunctionPackage(methodID, distributedMethod)));
        return methodID;
    }

    private void removeServers(Set<UUID> upForRemoval) {
        upForRemoval.forEach(id -> {
            serverMap.remove(id);
            availableRemoteThreads.removeIf(thread -> thread.getID() == id);
            availableLowPriorityThreads.removeIf(thread -> thread.getID() == id);
        });
    }

    protected Set<UUID> sendFunctionPackage(FunctionPackage functionPackage) {
        Set<UUID> upForRemoval = new HashSet<>();
        serverMap.forEach((id, remoteServer) -> {
            try {
                remoteServer.getExecutorContainer()
                        .registerFunction(functionPackage);
            } catch (RemoteException e) {
                logger.error("Remote exception while attempting to send a function to " + remoteServer.getAddress(), e);
                upForRemoval.add(id);
            }
        });
        return upForRemoval;
    }

    protected <T, R> R checkItemFromLocal(CheckFromLocal<T, R> checkFromLocal) {
        getNextThread().ifPresentOrElse(server -> {
            if (checkFromLocal.removeListener()) {
                try {
                    checkFromLocal.returnValueRef.set(checkOnRemote(server,
                            checkFromLocal.methodID, checkFromLocal.argumentPackage));
                } catch (RemoteException e) {
                    logger.error("Remote exception while attempting to execute a task on "
                            + server.getAddress(), e);
                    checkFromLocal.listen();
                    checkFromLocal.returnValueRef.set(checkItemFromLocal(checkFromLocal));
                }
            }
        }, () -> checkFromLocal.returnValueRef.set(checkWhenAvailableFromLocal(checkFromLocal)));
        return checkFromLocal.returnValueRef.get();
    }

    private Optional<RemoteThread> getNextThread() {
        try {
            return Optional.of(availableRemoteThreads.pop());
        } catch (NoSuchElementException elementException) {
            try {
                return Optional.of(availableLowPriorityThreads.pop());
            } catch (NoSuchElementException lowElementException) {
                return Optional.empty();
            }
        }
    }

    @SuppressWarnings("java:S6212")
    //suppressing sonarlint, since replacing line 2 R with var would cause executeTask to return an Object instead of R
    protected <T, R> R checkOnRemote(RemoteThread thread, UUID methodID, ArgumentPackage<T> argumentPackage) throws RemoteException {
        R returnValue = thread.getExecutorContainer().checkAndReturnValue(methodID, argumentPackage);
        finalizeThread(thread);
        return returnValue;
    }

    private void finalizeThread(RemoteThread thread) {
        if (thread.getPriority() != ServerConfiguration.Priority.LOW) {
            fireListeners(thread);
            availableRemoteThreads.add(thread);
        } else {
            availableLowPriorityThreads.add(thread);
        }
    }

    @SuppressWarnings({"java:S2222"})
    //suppressing sonarlint, since if the condition listener is not called, the system has already entered deadlock
    //which by design will not occur.
    protected <T, R> R checkWhenAvailableFromLocal(CheckFromLocal<T, R> checkFromLocal) {
        try {
            //Awaits listener execution
            if (localCounter.get() <= maxLocalTasks) {
                localCounter.incrementAndGet();
                fireListeners(null);
                localCounter.decrementAndGet();
            }
            checkFromLocal.countDown.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        return checkFromLocal.returnValueRef.get();
    }

    public <T> DistributedItem<T> buildDistributedItem(T object) {
        return new DistributedItemLocal<>(UUID.randomUUID(), object);
    }

    private <R> DistributedItemFuture<R> repackageItemFuture(UUID itemID, UUID nodeID) {
        return new DistributedItemFuture<>(itemID, nodeID, fetcher);
    }

    @FunctionalInterface
    private interface ServerAvailabilityListener {

        void onAvailable(RemoteThread server);

    }

    public class GridItemFetcher implements ItemFetcher {
        @Override
        public <R> R getItem(UUID nodeID, UUID itemID) throws RemoteException {
            var server = serverMap.get(nodeID);
            return server.getExecutorContainer().getItem(itemID);
        }

    }

    protected class CheckFromLocal<T, R> {
        public final UUID methodID;
        public final AtomicReference<R> returnValueRef;
        public final CountDownLatch countDown;
        public final ArgumentPackage<T> argumentPackage;
        private final ServerAvailabilityListener listener;

        @SuppressWarnings("unchecked")
        public CheckFromLocal(UUID methodID, T argument) {
            this.methodID = methodID;
            this.argumentPackage = new ArgumentPackage<>(argument, UUID.randomUUID());
            this.countDown = new CountDownLatch(1);
            this.returnValueRef = new AtomicReference<>();
            this.listener = server -> {
                try {
                    if (server == null) {
                        DistCheck<T, R> function = (DistCheck<T, R>) functionMap.get(methodID);
                        returnValueRef.set(function.check(argument));
                        localCounter.decrementAndGet();
                    } else {
                        returnValueRef.set(checkOnRemote(server, methodID, argumentPackage));
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
