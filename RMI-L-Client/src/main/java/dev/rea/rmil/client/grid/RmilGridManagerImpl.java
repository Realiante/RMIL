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
import java.util.function.*;
import java.util.stream.Collectors;

//todo: Add shutdown hook to clean up after yourself
class RmilGridManagerImpl implements RmilGridManager {

    private static final Logger logger = LoggerFactory.getLogger(RmilGridManagerImpl.class);

    private final GridItemFetcher fetcher = new GridItemFetcher();

    private final Map<UUID, DistributedMethod> functionMap = new ConcurrentHashMap<>();
    private final Map<UUID, RemoteServer> serverMap = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<ServerAvailabilityListener>> distSrcListenersMap = new ConcurrentHashMap<>();

    private final Deque<ServerAvailabilityListener> localSrcListeners = new ConcurrentLinkedDeque<>();
    private final Deque<RemoteThread> availableRemoteThreads = new ConcurrentLinkedDeque<>();
    private final Deque<RemoteThread> availableLowPriorityThreads = new ConcurrentLinkedDeque<>();

    private final AtomicInteger localCounter;


    private int retries = 1;
    private long awaitTimeout = 60;
    private TimeUnit awaitTimeunit = TimeUnit.SECONDS;
    private int maxLocalTasks;

    public RmilGridManagerImpl(int maxLocalTasks, Set<String> addresses) {
        this.maxLocalTasks = maxLocalTasks;
        this.localCounter = new AtomicInteger(0);
        mapServers(addresses);
    }

    private void mapServers(Set<String> addresses) {
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

    protected Set<RemoteServer> getAvailableServers(Set<String> addresses) {
        return addresses.parallelStream().map(servAddr -> RemoteServer.load(servAddr, retries))
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
        return distItem -> check(method, registerMethod(method), distItem);
    }

    @Override
    public <T, R> Function<DistributedItem<T>, DistributedItem<R>> gridFunction(Function<T, R> function) {
        return null;
        //todo
    }

    @Override
    public <T> ToIntFunction<DistributedItem<T>> gridToIntFunction(ToIntFunction<DistributedItem<T>> toIntFunction) {
        return null;
        //todo
    }

    @Override
    public <T> ToDoubleFunction<DistributedItem<T>> gridToDoubleFunction(ToDoubleFunction<DistributedItem<T>> toDoubleFunction) {
        return null;
        //todo
    }

    @Override
    public <T> ToLongFunction<DistributedItem<T>> gridToLongFunction(ToLongFunction<DistributedItem<T>> toFloatFunction) {
        return null;
        //todo
    }

    @Override
    public <T> BinaryOperator<DistributedItem<T>> gridBinaryOperator(BinaryOperator<T> biOperator) {
        return null;
        //todo
    }

    private <T, R> R check(DistCheck<T, R> checkFunc, UUID methodID, DistributedItem<T> distributedItem) {
        if (distributedItem.getNodeID() == null) {
            var item = distributedItem.getItem();
            /*todo: rare race condition if a thread checks local counter while a local to dist check was looking at it.
            this event should be quite rare due to to time an if check takes, and i never seen it myself.
            in any case, this only introduces slight overhead and in worst case creates 1 additional remote instance
            this should be investigated, and possibly fixed - daniel 12/06/21 */
            if (localCounter.incrementAndGet() < maxLocalTasks) {
                return localCheck(checkFunc, item);
            }
            return localToDistCheck(methodID, item, distributedItem);
        } else {
            return distToDistCheck(methodID, distributedItem);
        }
    }

    private <T, R> R localCheck(DistCheck<T, R> checkFunc, T item) {
        var result = checkFunc.check(item);
        clearBacklog(null);
        localCounter.decrementAndGet();
        return result;
    }

    private <T, R> R localToDistCheck(UUID methodID, T item, DistributedItem<T> distributedItem) {
        //next line is the possible cause of the issue described in the upper method's comment
        localCounter.decrementAndGet();
        CheckResultLocalSrc<R> checkResultLocalSrc = checkItemFromLocal(new OngoingCheckLocalSrc<>(methodID, item));
        distributedItem.setNodeID(checkResultLocalSrc.responsibleNode);
        return checkResultLocalSrc.returnValue;
    }

    private void clearBacklog(RemoteThread thread) {
        if (thread == null) {
            clearBacklogLocally();
        } else {
            clearBacklogRemotely(thread);
        }
    }

    private void clearBacklogRemotely(RemoteThread thread) {
        var canFromNode = false;
        while (!localSrcListeners.isEmpty() || (canFromNode = thread != null
                && distSrcListenersMap.containsKey(thread.getID())
                && !distSrcListenersMap.get(thread.getID()).isEmpty())) {
            if (canFromNode) {
                clearBacklogFromNode(thread);
            }
            if (thread.getPriority() == ServerConfiguration.Priority.NORMAL) {
                try {
                    localSrcListeners.pop().onAvailable(thread);
                } catch (NoSuchElementException ignored) {
                    //ignored exception
                }
            }
        }
    }

    private void clearBacklogLocally() {
        while (!localSrcListeners.isEmpty()) {
            try {
                localSrcListeners.pop().onAvailable(null);
            } catch (NoSuchElementException ignored) {
                //ignored exception
            }
        }
    }

    private void clearBacklogFromNode(RemoteThread thread) {
        var deque = distSrcListenersMap.get(thread.getID());
        while (!deque.isEmpty()) {
            try {
                deque.pop().onAvailable(thread);
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

    protected <T, R> CheckResultLocalSrc<R> checkItemFromLocal(OngoingCheckLocalSrc<T, R> checkFromLocal) {
        getNextThread().ifPresentOrElse(remoteThread -> {
            if (checkFromLocal.removeListener()) {
                tryCheckItemFromLocal(checkFromLocal, remoteThread, 0);
            }
        }, () -> checkFromLocal.returnValueRef.set(checkWhenAvailableFromLocal(checkFromLocal)));
        return checkFromLocal.returnValueRef.get();
    }

    protected <T, R> void tryCheckItemFromLocal(OngoingCheckLocalSrc<T, R> checkFromLocal, RemoteThread remoteThread, int i) {
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
        clearBacklog(thread);
        if (thread.getPriority() != ServerConfiguration.Priority.LOW) {
            availableRemoteThreads.add(thread);
        } else {
            availableLowPriorityThreads.add(thread);
        }
    }

    @SuppressWarnings({"java:S2222"})
    //suppressing sonarlint, since if the condition listener is not called, the system has already entered deadlock
    //which by design will not occur.
    protected <T, R> CheckResultLocalSrc<R> checkWhenAvailableFromLocal(OngoingCheckLocalSrc<T, R> checkFromLocal) {
        try {
            //Awaits listener execution
            if (localCounter.get() <= maxLocalTasks) {
                localCounter.incrementAndGet();
                clearBacklog(null);
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

    private <T, R> R distToDistCheck(UUID methodID, DistributedItem<T> distributedItem) {
        CheckResultDistSrc<R> checkResultDistSrc =
                checkItemFromDist(new OngoingCheckDistSrc<>(methodID, distributedItem));
        finalizeThread(checkResultDistSrc.thread);
        return checkResultDistSrc.returnValue;
    }

    private <T, R> CheckResultDistSrc<R> checkItemFromDist(OngoingCheckDistSrc<T, R> ongoingCheck) {
        var threadOptional = getFirstServerThread(ongoingCheck.item.getNodeID());
        threadOptional.ifPresentOrElse(thread -> {
            if (ongoingCheck.removeListener())
                ongoingCheck.returnValueRef.set(sendDistToDistCheck(ongoingCheck, thread, retries));
        }, () -> ongoingCheck.returnValueRef.set(checkWhenAvailableFromDist(ongoingCheck)));
        return new CheckResultDistSrc<>(ongoingCheck.thread, ongoingCheck.returnValueRef.get());
    }

    @SuppressWarnings("java:S6212")
    private <T, R> R sendDistToDistCheck(OngoingCheckDistSrc<T, R> ongoingCheck, RemoteThread thread, int i) {
        try {
            R result = thread.getExecutorContainer().checkAndReturnValue(ongoingCheck.methodID, ongoingCheck.item.getItemID());
            ongoingCheck.setResponsibleThread(thread);
            return result;
        } catch (RemoteException e) {
            logger.error(String.format("Remote exception while attempting to execute a dist to dist task on %s " +
                    ": tries left=%s", thread.getAddress(), retries - i));
            if (i > 0) {
                return sendDistToDistCheck(ongoingCheck, thread, --i);
            }
            throw new IllegalStateException("Dist to Dist operation could not be performed and recovery is not yet implemented!", e);
        }
    }

    private <T, R> R checkWhenAvailableFromDist(OngoingCheckDistSrc<T, R> ongoingCheck) {
        try {
            if (!ongoingCheck.countDown.await(awaitTimeout, awaitTimeunit)) {
                throw new InterruptedException(
                        "An argument has timed out while awaiting a remote server to compute it");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        return ongoingCheck.returnValueRef.get();
    }

    private Optional<RemoteThread> getFirstServerThread(UUID parentID) {
        var parent = serverMap.get(parentID);
        if (parent.getPriority() == ServerConfiguration.Priority.NORMAL) {
            if (availableRemoteThreads.removeFirstOccurrence(parent)) {
                return Optional.of(new RemoteServerThread(parent));
            }
        } else if (availableLowPriorityThreads.removeFirstOccurrence(parent)) {
            return Optional.of(new RemoteServerThread(parent));
        }
        return Optional.empty();
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

    @Override
    public void addServers(Set<String> addresses) {
        mapServers(addresses);
    }

    @FunctionalInterface
    private interface ServerAvailabilityListener {

        void onAvailable(RemoteThread server);

    }

    protected static class CheckResultDistSrc<R> {
        public final RemoteThread thread;
        public final R returnValue;

        public CheckResultDistSrc(RemoteThread thread, R returnValue) {
            this.thread = thread;
            this.returnValue = returnValue;
        }
    }

    protected static class CheckResultLocalSrc<R> {
        public final UUID responsibleNode;
        public final R returnValue;

        public CheckResultLocalSrc(UUID responsibleNode, R returnValue) {
            this.responsibleNode = responsibleNode;
            this.returnValue = returnValue;
        }
    }

    public class GridItemFetcher implements ItemFetcher {
        @Override
        public <R> R getItem(UUID nodeID, UUID itemID) throws RemoteException {
            var server = serverMap.get(nodeID);
            return server.getExecutorContainer().getItem(itemID);
        }

    }

    /*todo To save work, these ongoing classes should have had the same parent
      this could save space and work in the future by introducing common logic*/

    protected class OngoingCheckDistSrc<T, R> {
        public final UUID methodID;
        public final AtomicReference<R> returnValueRef;
        public final CountDownLatch countDown;
        private final DistributedItem<T> item;
        private final ServerAvailabilityListener listener;
        private RemoteThread thread;

        public OngoingCheckDistSrc(UUID methodID, DistributedItem<T> item) {
            this.methodID = methodID;
            this.item = item;
            this.countDown = new CountDownLatch(1);
            this.returnValueRef = new AtomicReference<>();
            this.listener = server -> returnValueRef.set(sendDistToDistCheck(this, server, retries));
            listen();
        }

        public RemoteThread getResponsibleThread() {
            return Objects.requireNonNull(thread);
        }

        public void setResponsibleThread(RemoteThread thread) {
            this.thread = thread;
        }

        public boolean removeListener() {
            return distSrcListenersMap.get(item.getNodeID()).remove(listener);
        }

        public void listen() {
            if (!distSrcListenersMap.containsKey(item.getNodeID())) {
                distSrcListenersMap.put(item.getNodeID(), new ConcurrentLinkedDeque<>());
            }
            distSrcListenersMap.get(item.getNodeID()).add(listener);
        }
    }

    protected class OngoingCheckLocalSrc<T, R> {
        public final UUID methodID;
        public final AtomicReference<CheckResultLocalSrc<R>> returnValueRef;
        public final CountDownLatch countDown;
        public final ArgumentPackage<T> argumentPackage;
        private final ServerAvailabilityListener listener;

        @SuppressWarnings("unchecked")
        public OngoingCheckLocalSrc(UUID methodID, T argument) {
            this.methodID = methodID;
            this.argumentPackage = new ArgumentPackage<>(argument, UUID.randomUUID());
            this.countDown = new CountDownLatch(1);
            this.returnValueRef = new AtomicReference<>();
            this.listener = server -> {
                try {
                    if (server == null) {
                        DistCheck<T, R> function = (DistCheck<T, R>) functionMap.get(methodID);
                        returnValueRef.set(new CheckResultLocalSrc<>(null, function.check(argument)));
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
            listen();
        }

        public boolean removeListener() {
            return localSrcListeners.remove(listener);
        }

        public void listen() {
            localSrcListeners.add(listener);
        }
    }
}
