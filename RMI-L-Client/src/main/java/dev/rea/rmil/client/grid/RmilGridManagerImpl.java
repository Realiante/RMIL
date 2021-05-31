package dev.rea.rmil.client.grid;

import dev.rea.rmil.client.DistributedItem;
import dev.rea.rmil.client.RmilGridManager;
import dev.rea.rmil.client.items.DistributedItemFuture;
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
import java.util.concurrent.TimeUnit;
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
    private final Deque<RemoteThread> availableRemoteThreads = new ConcurrentLinkedDeque<>();
    private final Deque<RemoteThread> availableLowPriorityThreads = new ConcurrentLinkedDeque<>();
    private final AtomicInteger localCounter;


    private int retries = 1;
    private long awaitTimeout = 60;
    private TimeUnit awaitTimeunit = TimeUnit.SECONDS;
    private int maxLocalTasks;


    public RmilGridManagerImpl(int maxLocalTasks, Set<ServerAddress> addresses) {
        this.maxLocalTasks = maxLocalTasks;
        this.localCounter = new AtomicInteger(0);
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
        return addresses.parallelStream().map(servAddr -> RemoteServer.load(servAddr.getAddress(), retries))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
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
        return distItem -> check(method, registerMethod(method), (DistributedItemFuture<T>) distItem);
    }

    private <T, R> R check(DistCheck<T, R> checkFunc, UUID methodID, DistributedItemFuture<T> distributedItem) {
        if (distributedItem.getNodeID() == null) {
            var item = distributedItem.getItem();
            if (localCounter.incrementAndGet() < maxLocalTasks) {
                var result = checkFunc.check(item);
                fireListeners(null);
                localCounter.decrementAndGet();
                return result;
            }
            localCounter.decrementAndGet();
            CheckResult<R> checkResult = checkItemFromLocal(new CheckFromLocal<>(methodID, item));
            distributedItem.setNodeID(checkResult.responsibleNode);
            return checkResult.returnValue;
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

    protected <T, R> CheckResult<R> checkItemFromLocal(CheckFromLocal<T, R> checkFromLocal) {
        getNextThread().ifPresentOrElse(remoteThread -> {
            if (checkFromLocal.removeListener()) {
                tryCheckItemFromLocal(checkFromLocal, remoteThread, 0);
            }
        }, () -> checkFromLocal.returnValueRef.set(checkWhenAvailableFromLocal(checkFromLocal)));
        return checkFromLocal.returnValueRef.get();
    }

    protected <T, R> void tryCheckItemFromLocal(CheckFromLocal<T, R> checkFromLocal, RemoteThread remoteThread, int i) {
        try {
            checkFromLocal.returnValueRef.set(checkOnRemote(remoteThread,
                    checkFromLocal.methodID, checkFromLocal.argumentPackage));
        } catch (RemoteException e) {
            logger.error(String.format("Remote exception while attempting to execute a task on %s " +
                    ": tries left=%s", remoteThread.getAddress(), retries - i));
            if (i < retries) {
                tryCheckItemFromLocal(checkFromLocal, remoteThread, ++i);
            } else {
                checkFromLocal.listen();
                checkFromLocal.returnValueRef.set(checkItemFromLocal(checkFromLocal));
            }
        }
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
    protected <T, R> CheckResult<R> checkWhenAvailableFromLocal(CheckFromLocal<T, R> checkFromLocal) {
        try {
            //Awaits listener execution
            if (localCounter.get() <= maxLocalTasks) {
                localCounter.incrementAndGet();
                fireListeners(null);
                localCounter.decrementAndGet();
            }
            if (!checkFromLocal.countDown.await(awaitTimeout, awaitTimeunit)) {
                throw new InterruptedException(
                        "An argument has timed out while awaiting a remote server or a local thread to compute it");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        return checkFromLocal.returnValueRef.get();
    }

    public <T> DistributedItem<T> buildDistributedItem(T object) {
        return new DistributedItemFuture<>(UUID.randomUUID(), object, fetcher);
    }

    @Override
    public void setAwaitTimeout(long timeout, TimeUnit timeUnit) {
        this.awaitTimeout = timeout;
        this.awaitTimeunit = timeUnit;
    }

    @Override
    public void setRetry(int tries) {
        this.retries = tries;
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
        public final AtomicReference<CheckResult<R>> returnValueRef;
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
                        returnValueRef.set(new CheckResult<>(null, function.check(argument)));
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

    protected static class CheckResult<R> {
        public final UUID responsibleNode;
        public final R returnValue;

        public CheckResult(UUID responsibleNode, R returnValue) {
            this.responsibleNode = responsibleNode;
            this.returnValue = returnValue;
        }
    }
}
